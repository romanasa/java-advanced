package ru.ifmo.rain.korobkov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static ru.ifmo.rain.korobkov.hello.HelloUDPServer.TIMEOUT_HOURS;

public class Utils {
    public static String packetToString(final DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    public static void setStringToPacket(final DatagramPacket packet, final String string, final SocketAddress address) {
        final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        packet.setData(bytes);
        packet.setLength(bytes.length);
        packet.setSocketAddress(address);
    }


    public static String readString(final DatagramSocket socket, final DatagramPacket packet) throws IOException {
        socket.receive(packet);
        return packetToString(packet);
    }

    public static DatagramPacket createPacket(final DatagramSocket socket) throws SocketException {
        final byte[] receiveBuffer = new byte[socket.getReceiveBufferSize()];
        return new DatagramPacket(receiveBuffer, receiveBuffer.length);
    }


    public static void sendString(final DatagramSocket socket, final DatagramPacket packet,
                                  final SocketAddress serverAddress, final String request) throws IOException {
        setStringToPacket(packet, request, serverAddress);
        socket.send(packet);
    }


    public static String createMessage(final String queryPrefix, final int threadId, final int requestNumber) {
        return queryPrefix + threadId + "_" + requestNumber;
    }


    public static void shutdownAndAwaitTermination(final ExecutorService pool) {
        pool.shutdown();
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

    static int sendBuffer(final DatagramChannel channel, final ByteBuffer buffer, final SocketAddress address) {
        buffer.flip();
        final int bytesSent;
        try {
            bytesSent = channel.send(buffer, address);
            return bytesSent;
        } catch (final IOException e) {
            System.err.println("Can't send message: " + e.getMessage());
            return 0;
        }
    }

    static SocketAddress readBuffer(final DatagramChannel channel, final ByteBuffer buffer) {
        buffer.clear();
        final SocketAddress clientAddress;
        try {
            clientAddress = channel.receive(buffer);
            buffer.flip();
            return clientAddress;
        } catch (final IOException e) {
            System.err.println("Can't receive message: " + e.getMessage());
            return null;
        }
    }

    static void run(final Selector selector, final Consumer<SelectionKey> read,
                    final Consumer<SelectionKey> write, final long timeout) {
        while (selector.isOpen()) {
            try {
                final int updates = selector.select(timeout);
                if (updates > 0) {
                    for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                        final SelectionKey key = i.next();
                        try {
                            if (key.isReadable() && (key.interestOps() & SelectionKey.OP_READ) != 0) {
                                read.accept(key);
                            }
                            if (key.isValid() && key.isWritable() && (key.interestOps() & SelectionKey.OP_WRITE) != 0) {
                                write.accept(key);
                            }
                        } finally {
                            i.remove();
                        }
                    }
                } else {
                    boolean was = false;
                    for (final SelectionKey key : selector.keys()) {
                        if (key.interestOps() > 0) {
                            was = true;
                            key.interestOps(SelectionKey.OP_WRITE);
                        }
                    }
                    if (!was) {
                        break;
                    }
                }
            } catch (final IOException e) {
                System.err.println("Can't select");
                e.printStackTrace();
                return;
            }
        }
    }

    public static boolean check(final String response, final int threadId, final int requestId) {
        return response.matches("[\\D]*" + threadId + "[\\D]*" + requestId + "[\\D]*");
    }


    public static void startServer(final String[] args, final Supplier<HelloServer> supplier) {
        if (args == null || args.length != 2) {
            throw new IllegalArgumentException("Usage: java helloUDPNonblockingServer <port> <threads>");
        }
        try {
            final int port = Integer.parseInt(args[0]);
            final int threads = Integer.parseInt(args[1]);
            try (final HelloServer helloServer = supplier.get()) {
                helloServer.start(port, threads);
                TimeUnit.HOURS.sleep(TIMEOUT_HOURS);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Expected integer arguments");
        }
    }

    private static int getArgs(final String[] args, final int ind, final int value) throws NumberFormatException {
        try {
            return Integer.parseInt(args[ind]);
        } catch (final NumberFormatException e) {
            return value;
        }
    }

    public static void startClient(final String[] args, final Supplier<HelloClient> supplier) {
        if (args == null || args.length != 5) {
            throw new IllegalArgumentException("Usage: <host> <port> <prefix> <threads> <perThread>");
        }
        final String host = args[0];
        final String prefix = args[2];
        final int port = getArgs(args, 1, 28888);
        final int threadsCount = getArgs(args, 3, 1);
        final int queriesPerThread = getArgs(args, 4, 1);

        System.out.println("Staring client, host = " + host + ", port = " + port + ", prefix = " + prefix);
        try {
            supplier.get().run(host, port, prefix, threadsCount, queriesPerThread);
        } catch (final RuntimeException e) {
            System.err.println("Failed to start: " + e.getMessage());
        }
    }
}
