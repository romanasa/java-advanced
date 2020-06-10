package ru.ifmo.rain.korobkov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import info.kgeorgiy.java.advanced.hello.Util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HelloUDPNonblockingServer implements HelloServer {
    private ExecutorService workers;
    private Queue<ByteBuffer> free;
    private final Queue<Data> full = new ConcurrentLinkedQueue<>();
    private Selector selector;
    private DatagramChannel serverChannel;

    private static class Data {
        ByteBuffer buffer;
        SocketAddress address;

        public Data(final ByteBuffer buffer, final SocketAddress address) {
            this.buffer = buffer;
            this.address = address;
        }
    }

    private void handleRead(final SelectionKey key) {
        final DatagramChannel serverChannel = (DatagramChannel) key.channel();
        final ByteBuffer buffer = getData(key, free, SelectionKey.OP_READ);

        final SocketAddress clientAddress = Utils.readBuffer(serverChannel, buffer);
        if (clientAddress != null) {
            workers.submit(() -> {
                final String response = HelloUDPServer.process(
                        StandardCharsets.UTF_8.decode(buffer).toString());

                buffer.clear();
                buffer.put(response.getBytes(StandardCharsets.UTF_8));

                synchronized (HelloUDPNonblockingServer.this) {
                    full.add(new Data(buffer, clientAddress));
                    if ((key.interestOps() & SelectionKey.OP_WRITE) == 0) {
                        key.interestOpsOr(SelectionKey.OP_WRITE);
                        selector.wakeup();
                    }
                }
            });
        }
    }

    private void handleWrite(final SelectionKey key) {
        final DatagramChannel channel = (DatagramChannel) key.channel();
        final Data data = getData(key, full, SelectionKey.OP_WRITE);

        final int bytesSent = Utils.sendBuffer(channel, data.buffer, data.address);
        if (bytesSent != 0) {
            free.add(data.buffer);
            key.interestOpsOr(SelectionKey.OP_READ);
        }
    }

    private synchronized <T> T getData(final SelectionKey key, final Queue<T> queue, final int mask) {
        final T data = queue.remove();
        if (queue.isEmpty()) {
            key.interestOpsAnd(~mask);
        }
        return data;
    }

    /**
     * Starts a new Hello server.
     *
     * @param port    server port.
     * @param threads number of working threads.
     */
    @Override
    public void start(final int port, final int threads) {
        try {
            selector = Selector.open();
            serverChannel = DatagramChannel.open();
            final int capacity = serverChannel.getOption(StandardSocketOptions.SO_RCVBUF);

            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.register(selector, SelectionKey.OP_READ);

            free = Stream.generate(() -> ByteBuffer.allocate(capacity))
                    .limit(threads)
                    .collect(Collectors.toCollection(ConcurrentLinkedQueue::new));
            workers = Executors.newFixedThreadPool(threads + 1);
            workers.submit(() -> Utils.run(selector, this::handleRead, this::handleWrite, 0, false));
        } catch (final IOException e) {
            System.err.println("Can't start server");
            e.printStackTrace();
        }
    }

    /**
     * Stops server and deallocates all resources.
     */
    @Override
    public void close() {
        Utils.close(selector);
        Utils.close(serverChannel);
        Utils.shutdownAndAwaitTermination(workers);
    }

    public static void main(final String[] args) {
        Utils.startServer(args, HelloUDPNonblockingServer::new);
    }
}
