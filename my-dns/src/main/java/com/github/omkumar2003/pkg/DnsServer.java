package com.github.omkumar2003.pkg;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DnsServer {

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(53)) {
            byte[] buf = new byte[512];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(packet);
                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();
                    byte[] data = packet.getData();
                    int length = packet.getLength();

                    // Handle the packet in a new thread
                    new Thread(() -> {
                        try {
                            DnsResolver.handlePacket(socket, address, data, length, port);
                        } catch (IOException e) {
                            System.err.printf("Connection error [%s]: %s\n", address, e.getMessage());
                        }
                    }).start();
                } catch (IOException e) {
                    System.err.printf("Connection error: %s\n", e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class DnsResolver {

    public static void handlePacket(DatagramSocket socket, InetAddress addr, byte[] buf, int length, int port) throws IOException {
        // Convert the byte array to the DNS message and handle it as per your existing logic
        // For the sake of this example, we'll simply print out the received data
        byte[] responseBuffer = new byte[length];
        System.arraycopy(buf, 0, responseBuffer, 0, length);

        // You would typically parse the DNS message here and construct a response
        DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length, addr, port);
        socket.send(responsePacket);
    }
}
