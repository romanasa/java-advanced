package ru.ifmo.rain.korobkov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class HelloUDPServer implements HelloServer {
    public static final int TIMEOUT_HOURS = 1;
    private DatagramSocket socket = null;
    private ExecutorService workers = null;

    private void run() {
        final DatagramPacket request;
        try {
            request = Utils.createPacket(socket);
        } catch (final SocketException e) {
            System.err.println("Unable to create packet");
//            e.printStackTrace();
            return;
        }
        final DatagramPacket responsePacket = new DatagramPacket(new byte[0], 0);

        while (!socket.isClosed()) {
            try {
                socket.receive(request);
                if (!socket.isClosed()) {
                    final String responseString = process(Utils.packetToString(request));
                    try {
                        Utils.sendString(socket, responsePacket, request.getSocketAddress(), responseString);
                    } catch (final IOException e) {
                        System.err.println("Failed to send message: " + e.getMessage());
                    }
                }
            } catch (final IOException e) {
                System.err.println("Failed to receive message: " + e.getMessage());
            }
        }
    }

    static String process(final String request) {
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
            workers = Executors.newFixedThreadPool(threads);
            IntStream.range(0, threads).forEach(i -> workers.submit(this::run));
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
        socket.close();
        Utils.shutdownAndAwaitTermination(workers);
    }

    public static void main(final String[] args) {
        Utils.startServer(args, HelloUDPServer::new);
    }
}
