package ru.ifmo.rain.korobkov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class WebCrawler implements Crawler {
    public static final int TIMEOUT_MINUTES = 1;
    private final Downloader downloader;
    private final ExecutorService downloadersPool;
    private final ExecutorService extractorsPool;
    private final int perHost;

    public WebCrawler(final Downloader downloader, final int downloaders, final int extractors, final int perHost) {
        this.downloader = downloader;
        downloadersPool = Executors.newFixedThreadPool(downloaders);
        extractorsPool = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    /**
     * Downloads web site up to specified depth.
     *
     * @param url   start <a href="http://tools.ietf.org/html/rfc3986">URL</a>.
     * @param depth download depth.
     * @return download result.
     */
    @Override
    public Result download(final String url, final int depth) {
        return new CrawlerInfo().start(url, depth);
    }

    private class CrawlerInfo {
        final Map<String, HostDownloader> hosts = new ConcurrentHashMap<>();
        final Set<String> used = ConcurrentHashMap.newKeySet();
        final Map<String, IOException> errors = new ConcurrentHashMap<>();
        final Phaser phaser = new Phaser(1);

        public Result start(final String url, final int depth) {
            downloadTask(url, depth);
            phaser.arriveAndAwaitAdvance();

            return new Result(
                    used.stream().filter(s -> !errors.containsKey(s)).collect(Collectors.toList()),
                    errors);
        }

        private void downloadTask(final String url, final int depth) {
            if (used.add(url)) {
                try {
                    final String host = URLUtils.getHost(url);
                    final HostDownloader hostDownloader = hosts.computeIfAbsent(host,
                            key -> new HostDownloader(perHost, downloadersPool));

                    phaser.register();
                    hostDownloader.add(() -> {
                        try {
                            final Document document = downloader.download(url);
                            if (depth > 1) {
                                extractTask(url, depth, document);
                            }
                        } catch (final IOException e) {
                            errors.put(url, e);
                        } finally {
                            phaser.arrive();
                        }
                    });
                } catch (final MalformedURLException e) {
                    //
                }
            }
        }

        private void extractTask(final String url, final int depth, final Document document) {
            phaser.register();
            extractorsPool.submit(() -> {
                try {
                    document.extractLinks().forEach(newUrl -> downloadTask(newUrl, depth - 1));
                } catch (final IOException e) {
                    errors.put(url, e);
                } finally {
                    phaser.arrive();
                }
            });
        }
    }

    private static class HostDownloader {
        private final int limit;
        private final Queue<Runnable> runnables = new ArrayDeque<>();
        private final ExecutorService service;
        private int connections = 0;

        public HostDownloader(final int limit, final ExecutorService service) {
            this.limit = limit;
            this.service = service;
        }

        public synchronized void add(final Runnable r) {
            runnables.add(r);
            tryRun();
        }

        private synchronized void tryRun() {
            if (!runnables.isEmpty() && connections < limit) {
                final Runnable runnable = runnables.poll();
                connections++;
                service.submit(() -> {
                    try {
                        runnable.run();
                    } finally {
                        release();
                    }
                });
            }
        }

        private synchronized void release() {
            connections--;
            tryRun();
        }
    }

    /**
     * Closes this web-crawler, relinquishing any allocated resources.
     */
    @Override
    public void close() {
        downloadersPool.shutdown();
        extractorsPool.shutdown();

        wait(downloadersPool);
        wait(extractorsPool);
    }

    private void wait(final ExecutorService pool) {
        try {
            if (!pool.awaitTermination(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                pool.shutdownNow();
                pool.awaitTermination(TIMEOUT_MINUTES, TimeUnit.MINUTES);
            }
        } catch (final InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private int getArg(final String[] args, final int ind, final int defaultValue) {
        return ind < args.length ? Integer.parseInt(args[ind]) : defaultValue;
    }

    public void main(final String[] args) {
        if (args == null || args.length < 1 || args.length > 5) {
            System.out.println("Usage: WebCrawler url [depth [downloaders [extractors [perHost]]]]");
            return;
        }
        final String url = args[0];
        final int depth = getArg(args, 1, 1);
        final int downloaders = getArg(args, 2, 5);
        final int extractors = getArg(args, 3, 5);
        final int perHost = getArg(args, 4, 20);
        try (final Crawler crawler = new WebCrawler(new CachingDownloader(Paths.get(url)), downloaders, extractors, perHost)) {
            final Result result = crawler.download(url, depth);
            System.out.println("Downloaded:");
            result.getDownloaded().forEach(System.out::println);
            System.out.println("Errors:");
            result.getErrors().entrySet().stream()
                    .map(entry -> entry.getValue() + " " + entry.getKey()).forEach(System.out::println);
        } catch (final IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
