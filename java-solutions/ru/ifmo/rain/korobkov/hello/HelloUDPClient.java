package ru.ifmo.rain.korobkov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

public class HelloUDPClient implements HelloClient {

    public static final int TIMEOUT_MILLISECONDS = 500;

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
        final ExecutorService threadPool = Executors.newFixedThreadPool(threads);

        final InetSocketAddress serverAddress;
        try {
            serverAddress = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (final UnknownHostException e) {
            System.err.println("Unknown host: " + e.getMessage());
//            e.printStackTrace();
            return;
        }

        final IntFunction<Callable<Void>> tasks = threadId -> () -> {
            try (final DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(TIMEOUT_MILLISECONDS);
                final DatagramPacket requestPacket = new DatagramPacket(new byte[0], 0);
                final DatagramPacket responsePacket = Utils.createPacket(socket);

                for (int i = 0; i < requests; i++) {
                    try {
                        final String query = createMessage(prefix, threadId, i);
                        String result;
                        while (true) {
                            Utils.sendString(socket, requestPacket, serverAddress, query);
                            try {
                                final String response = Utils.readString(socket, responsePacket);
                                if (check(response, threadId, i)) {
                                    result = response;
                                    break;
                                }
                            } catch (final IOException e) {
                                System.err.println("Wrong received packet: " + e.getMessage());
                            }
                        }
                        System.out.println("Received response: " + result);
                    } catch (final IOException e) {
                        System.err.println("Thread " + threadId + ": Failed to send request");
                    }
                }

            } catch (final SocketException e) {
                e.printStackTrace();
            }
            return null;
        };
        IntStream.range(0, threads).mapToObj(tasks).forEach(threadPool::submit);
        Utils.shutdownAndAwaitTermination(threadPool);
    }

    private boolean check(final String response, final int threadId, final int requestId) {
        return response.matches("[\\D]*" + threadId + "[\\D]*" + requestId + "[\\D]*");
    }

    private String createMessage(final String queryPrefix, final int threadId, final int requestNumber) {
        return queryPrefix + threadId + "_" + requestNumber;
    }

    private static int getArgs(final String[] args, final int ind, final int value) throws NumberFormatException {
        try {
            return Integer.parseInt(args[ind]);
        } catch (final NumberFormatException e) {
            return value;
        }
    }

    public static void main(final String[] args) {
        if (args == null || args.length != 5) {
            throw new IllegalArgumentException("Usage: <host> <port> <prefix> <threads> <perThread>");
        }
        final var host = args[0];
        final var prefix = args[2];
        final int port = getArgs(args, 1, 28888);
        final int threadsCount = getArgs(args, 3, 1);
        final int queriesPerThread = getArgs(args, 4, 1);

        System.out.println("Staring client, host = " + host + ", port = " + port + ", prefix = " + prefix);
        try {
            new HelloUDPClient().run(host, port, prefix, threadsCount, queriesPerThread);
        } catch (final RuntimeException e) {
            System.err.println("Failed to start: " + e.getMessage());
        }
    }
}
