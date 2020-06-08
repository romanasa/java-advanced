package ru.ifmo.rain.korobkov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static ru.ifmo.rain.korobkov.hello.HelloUDPServer.TIMEOUT_HOURS;

public class HelloUDPNonblockingServer implements HelloServer {
    public int CAPACITY = 4096;
    private ExecutorService workers;
    private final ConcurrentLinkedQueue<ByteBuffer> free = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Data> full = new ConcurrentLinkedQueue<>();
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

        final ByteBuffer buffer = free.remove();
        if (free.isEmpty()) {
            key.interestOpsAnd(~SelectionKey.OP_READ);
        }

        final SocketAddress clientAddress = Utils.readBuffer(serverChannel, buffer);
        if (clientAddress != null) {
            workers.submit(() -> {
                final String response = HelloUDPServer.process(
                        StandardCharsets.UTF_8.decode(buffer).toString());

                buffer.clear();
                buffer.put(response.getBytes(StandardCharsets.UTF_8));

                full.add(new Data(buffer, clientAddress));
                key.interestOpsOr(SelectionKey.OP_WRITE);
                selector.wakeup();
            });
        }
    }

    private void handleWrite(final SelectionKey key) {
        final DatagramChannel channel = (DatagramChannel) key.channel();

        final Data data = full.remove();
        if (full.isEmpty()) {
            key.interestOpsAnd(~SelectionKey.OP_WRITE);
        }

        final int bytesSent = Utils.sendBuffer(channel, data.buffer, data.address);
        if (bytesSent != 0) {
            free.add(data.buffer);
            key.interestOpsOr(SelectionKey.OP_READ);
        }
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
            CAPACITY = serverChannel.getOption(StandardSocketOptions.SO_RCVBUF);

            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.register(selector, SelectionKey.OP_READ);

            Stream.generate(() -> ByteBuffer.allocate(CAPACITY)).limit(threads).forEach(free::add);
            workers = Executors.newFixedThreadPool(threads + 1);
            workers.submit(() -> Utils.run(selector, this::handleRead, this::handleWrite, 0));
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
        try {
            selector.close();
            serverChannel.close();
        } catch (final IOException e) {
            System.err.println("Can't close selector");
            e.printStackTrace();
        }
    }

    public static void main(final String[] args)  {
        Utils.startServer(args, HelloUDPNonblockingServer::new);
    }
}
