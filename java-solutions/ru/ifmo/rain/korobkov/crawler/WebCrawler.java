package ru.ifmo.rain.korobkov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class WebCrawler implements Crawler {
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
        final Map<String, HostLimiter> hosts = new ConcurrentHashMap<>();
        final Map<String, Boolean> used = new ConcurrentHashMap<>();
        final Map<String, IOException> errors = new ConcurrentHashMap<>();
        final Phaser phaser = new Phaser();

        phaser.register();
        downloadTask(url, phaser, used, errors, depth, hosts);
        phaser.arriveAndAwaitAdvance();

        return new Result(
                used.keySet().stream().filter(s -> !errors.containsKey(s)).collect(Collectors.toList()),
                errors);
    }

    private static class HostLimiter {
        private final int limit;
        private int connections = 0;

        HostLimiter(final int limit) {
            this.limit = limit;
        }

        synchronized void getConnection() throws InterruptedException {
            while (connections >= limit) {
                wait();
            }
            connections++;
        }

        synchronized void release() {
            connections--;
            notify();
        }
    }

    private void downloadTask(final String url, final Phaser phaser, final Map<String, Boolean> used,
                              final Map<String, IOException> errors, final int depth, final Map<String, HostLimiter> hosts) {
        phaser.register();
        downloadersPool.submit(() -> {
            if (used.putIfAbsent(url, Boolean.TRUE) == null) {
                try {
                    final String host = URLUtils.getHost(url);
                    final HostLimiter hostLimiter = hosts.computeIfAbsent(host, key -> new HostLimiter(perHost));

                    try {
                        hostLimiter.getConnection();
                    } catch (final InterruptedException e) {
                        return;
                    }

                    try {
                        final Document document = downloader.download(url);
                        if (depth > 1) {
                            extractTask(url, phaser, used, errors, depth, hosts, document);
                        }
                    } catch (final IOException e) {
                        errors.put(url, e);
                    } finally {
                        hostLimiter.release();
                    }
                } catch (final MalformedURLException e) {
                    //
                }
            }
            phaser.arrive();
        });
    }

    private void extractTask(final String url, final Phaser phaser, final Map<String, Boolean> used,
                             final Map<String, IOException> errors, final int depth, final Map<String, HostLimiter> hosts,
                             final Document document) {
        phaser.register();
        extractorsPool.submit(() -> {
            try {
                document.extractLinks().forEach(newUrl -> downloadTask(newUrl, phaser, used, errors, depth - 1, hosts));
            } catch (final IOException e) {
                errors.put(url, e);
            } finally {
                phaser.arrive();
            }
        });
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
            if (!pool.awaitTermination(1, TimeUnit.MINUTES)) {
                pool.shutdownNow();
                pool.awaitTermination(1, TimeUnit.MINUTES);
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
            result.getErrors().entrySet().stream().map(entry -> entry.getValue() + " " + entry.getKey()).forEach(System.out::println);
        } catch (final IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
