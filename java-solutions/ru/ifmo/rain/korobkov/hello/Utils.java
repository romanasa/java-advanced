package ru.ifmo.rain.korobkov.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class Utils {
    public static String packetToString(final DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    public static void setStringToPacket(final DatagramPacket packet, final String string, final InetSocketAddress address) {
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
                                  final InetSocketAddress serverAddress, final String request) throws IOException {
        setStringToPacket(packet, request, serverAddress);
        socket.send(packet);
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
}
