package ru.ifmo.rain.korobkov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;

public class HelloUDPNonblockingClient implements HelloClient {

    private static class Context {
        final int threadId;
        int requestId;
        final int requests;
        final String prefix;
        final SocketAddress address;

        final ByteBuffer buffer;

        Context(final int threadId, final int capacity, final int requests, final String prefix, final SocketAddress address) {
            this.threadId = threadId;
            this.requestId = 0;
            this.requests = requests;
            this.prefix = prefix;
            this.address = address;

            buffer = ByteBuffer.allocate(capacity);
        }
    }


    private void handleWrite(final SelectionKey key) {
        final DatagramChannel channel = (DatagramChannel) key.channel();
        final Context context = (Context) key.attachment();

        context.buffer.clear();

        final String message = Utils.createMessage(context.prefix, context.threadId, context.requestId);
        context.buffer.put(message.getBytes(StandardCharsets.UTF_8));

        final int bytesSent = Utils.sendBuffer(channel, context.buffer, context.address);
        if (bytesSent != 0) {
            key.interestOps(SelectionKey.OP_READ);
        }
    }


    private void handleRead(final SelectionKey key) {
        final DatagramChannel channel = (DatagramChannel) key.channel();
        final Context context = (Context) key.attachment();

        final SocketAddress clientAddress = Utils.readBuffer(channel, context.buffer);

        if (clientAddress != null) {
            final String response = StandardCharsets.UTF_8.decode(context.buffer).toString();
            if (Utils.check(response, context.threadId, context.requestId)) {
                context.requestId++;
                if (context.requestId < context.requests) {
                    key.interestOps(SelectionKey.OP_WRITE);
                } else {
                    key.interestOps(0);
                }
                System.out.println("Received response: " + response);
            }
        }
    }

    /**
     * Runs Hello client.
     *
     * @param host     server host
     * @param port     server port
     * @param prefix   request prefix
     * @param threads  number of request threads
     * @param requests number of requests per thread.
     */
    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        final InetSocketAddress serverAddress;
        final Selector selector;
        try {
            serverAddress = new InetSocketAddress(InetAddress.getByName(host), port);
            selector = Selector.open();
        } catch (final UnknownHostException e) {
            System.err.println("Unknown host: " + e.getMessage());
            return;
        } catch (final IOException e) {
            System.err.println("Can't open selector: " + e.getMessage());
            return;
        }

        for (int threadId = 0; threadId < threads; threadId++) {
            try {
                final DatagramChannel channel = DatagramChannel.open();
                final int capacity = channel.getOption(StandardSocketOptions.SO_RCVBUF);

                channel.configureBlocking(false);
                channel.bind(null);
                channel.register(selector, SelectionKey.OP_WRITE,
                        new Context(threadId, capacity, requests, prefix, serverAddress));
            } catch (final IOException e) {
                e.printStackTrace();
                return;
            }
        }
        Utils.run(selector, this::handleRead, this::handleWrite, 1000);
    }

    public static void main(final String[] args) {
        Utils.startClient(args, HelloUDPNonblockingClient::new);
    }
}
