package ru.ifmo.rain.korobkov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket = null;
    private ExecutorService workers = null;
    private volatile boolean closed = false;

    private void run() {
        while (!socket.isClosed() && !closed) {
            try {
                final DatagramPacket packet = Utils.readPacket(socket);
                workers.submit(() -> {
                    if (!socket.isClosed()) {
                        packet.setData(process(Utils.packetToString(packet)).getBytes(StandardCharsets.UTF_8));
                        try {
                            socket.send(packet);
                        } catch (final IOException e) {
                            System.err.println("Failed to send message: " + e.getMessage());
                        }
                    }
                });

            } catch (final IOException e) {
                if (!closed) {
                    System.err.println("Failed to receive message: " + e.getMessage());
                }
            }
        }
    }

    private String process(final String request) {
        return "Hello, " + request;
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
            socket = new DatagramSocket(port);
            socket.setSoTimeout(1000);

            workers = Executors.newFixedThreadPool(threads + 1);
            workers.submit(this::run);
        } catch (final SocketException e) {
            System.err.println("Failed to create socket: " + e.getMessage());
//            e.printStackTrace();
        }
    }

    /**
     * Stops server and deallocates all resources.
     */
    @Override
    public void close() {
        closed = true;
        socket.close();
        Utils.shutdownAndAwaitTermination(workers);
    }

    public static void main(final String[] args) throws InterruptedException {
        if (args == null || args.length != 2) {
            throw new IllegalArgumentException("Usage: java HelloUDPServer <port> <threads>");
        }
        try {
            final int port = Integer.parseInt(args[0]);
            final int threads = Integer.parseInt(args[1]);
            try (final HelloUDPServer helloUDPServer = new HelloUDPServer()) {
                helloUDPServer.start(port, threads);
                TimeUnit.MINUTES.sleep(1);
            }
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Expected integer arguments");
        }
    }
}
