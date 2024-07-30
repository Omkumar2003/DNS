package com.github.omkumar2003.pkg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

public class DnsResolver {

    private static final String[] ROOT_SERVERS = {
            "198.41.0.4", "199.9.14.201", "192.33.4.12", "199.7.91.13",
            "192.203.230.10", "192.5.5.241", "192.112.36.4", "198.97.190.53"
    };

    public static void handlePacket(DatagramSocket socket, InetAddress addr, byte[] buf) {
        try {
            handlePacketInternal(socket, addr, buf);
        } catch (IOException e) {
            System.out.printf("handlePacket error [%s]: %s\n", addr.getHostAddress(), e.getMessage());
        }
    }

    private static void handlePacketInternal(DatagramSocket socket, InetAddress addr, byte[] buf) throws IOException {
        Message query = new Message(buf);
        Record question = query.getQuestion();
        Message response = dnsQuery(getRootServers(), question);
        response.getHeader().setID(query.getHeader().getID());

        byte[] responseBuffer = response.toWire();
        DatagramPacket packet = new DatagramPacket(responseBuffer, responseBuffer.length, addr, socket.getPort());
        socket.send(packet);
    }

    private static Message dnsQuery(List<InetAddress> servers, Record question) throws IOException {
        System.out.printf("Question: %s\n", question);

        for (int i = 0; i < 3; i++) {
            Message dnsAnswer = outgoingDnsQuery(servers, question);
            if (dnsAnswer.getHeader().getFlag(Message.FLAG_AA)) {
                Message response = new Message();
                response.getHeader().setFlag(Message.FLAG_QR);
                for (Record ans : dnsAnswer.getSectionArray(Section.ANSWER)) {
                    response.addRecord(ans, Section.ANSWER);
                }
                return response;
            }

            Record[] authorities = dnsAnswer.getSectionArray(Section.AUTHORITY);
            if (authorities.length == 0) {
                Message response = new Message();
                response.getHeader().setRcode(3);
                return response;
            }

            List<String> nameservers = new ArrayList<>();
            for (Record authority : authorities) {
                if (authority.getType() == Type.NS) {
                    nameservers.add(authority.rdataToString());
                }
            }

            Record[] additionals = dnsAnswer.getSectionArray(Section.ADDITIONAL);
            List<InetAddress> newServers = new ArrayList<>();
            for (Record additional : additionals) {
                if (additional.getType() == Type.A) {
                    for (String nameserver : nameservers) {
                        if (additional.getName().toString().equals(nameserver)) {
                            newServers.add(InetAddress.getByName(additional.rdataToString()));
                        }
                    }
                }
            }

            if (!newServers.isEmpty()) {
                servers = newServers;
            } else {
                for (String nameserver : nameservers) {
                    Message response = dnsQuery(getRootServers(), Record.newRecord(Name.fromString(nameserver), Type.A, 1));
                    for (Record ans : response.getSectionArray(Section.ANSWER)) {
                        if (ans.getType() == Type.A) {
                            newServers.add(InetAddress.getByName(ans.rdataToString()));
                        }
                    }
                }
                if (!newServers.isEmpty()) {
                    servers = newServers;
                }
            }
        }

        Message response = new Message();
        response.getHeader().setRcode(2);
        return response;
    }

    private static Message outgoingDnsQuery(List<InetAddress> servers, Record question) throws IOException {
        System.out.printf("New outgoing dns query for %s, servers: %s\n", question.getName(), servers);

        SecureRandom random = new SecureRandom();
        int id = random.nextInt(0xffff);

        Message message = new Message();
        message.getHeader().setID(id);
        message.addRecord(question, Section.QUESTION);

        byte[] buf = message.toWire();
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(2000);

        for (InetAddress server : servers) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length, server, 53);
                socket.send(packet);

                byte[] response = new byte[512];
                DatagramPacket responsePacket = new DatagramPacket(response, response.length);
                socket.receive(responsePacket);
                return new Message(responsePacket.getData());
            } catch (IOException e) {
                System.out.printf("Failed to query server %s: %s\n", server, e.getMessage());
            }
        }

        throw new IOException("Failed to make connection to any servers.");
    }

    private static List<InetAddress> getRootServers() {
        List<InetAddress> rootServers = new ArrayList<>();
        for (String rootServer : ROOT_SERVERS) {
            try {
                rootServers.add(InetAddress.getByName(rootServer));
            } catch (UnknownHostException e) {
                System.out.printf("Failed to resolve root server %s: %s\n", rootServer, e.getMessage());
            }
        }
        return rootServers;
    }

    public static void main(String[] args) {
        try {
            DatagramSocket socket = new DatagramSocket(53);
            byte[] buf = new byte[512];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (true) {
                socket.receive(packet);
                new Thread(() -> handlePacket(socket, packet.getAddress(), packet.getData())).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
