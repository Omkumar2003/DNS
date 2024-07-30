package com.github.omkumar2003.pkg;


import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class DnsResolverTest {

    static class MockDatagramSocket extends DatagramSocket {
        @Override
        public void send(DatagramPacket p) {
            // Mock behavior for send
        }

        @Override
        public void receive(DatagramPacket p) {
            // Mock behavior for receive
        }
    }

    @Test
    public void testHandlePacket() throws Exception {
        List<String> names = List.of("www.google.com.", "www.amazon.com.");
        for (String name : names) {
            SecureRandom random = new SecureRandom();
            int id = random.nextInt(0xffff);

            org.xbill.DNS.Message message = new org.xbill.DNS.Message();
            message.getHeader().setID(id);
            message.getHeader().setOpcode(0);
            message.addRecord(org.xbill.DNS.Record.newRecord(org.xbill.DNS.Name.fromString(name), org.xbill.DNS.Type.A, 1), org.xbill.DNS.Section.QUESTION);

            byte[] buf = message.toWire();

            DnsResolver.handlePacket(new MockDatagramSocket(), InetAddress.getByName("127.0.0.1"), buf);
        }
    }

    @Test
    public void testOutgoingDnsQuery() throws Exception {
        org.xbill.DNS.Record question = org.xbill.DNS.Record.newRecord(org.xbill.DNS.Name.fromString("com."), org.xbill.DNS.Type.NS, 1);
        List<InetAddress> rootServers = DnsResolver.getRootServers();
        if (rootServers.isEmpty()) {
            fail("No root servers found");
        }
        List<InetAddress> servers = List.of(rootServers.get(0));
        org.xbill.DNS.Message dnsAnswer = DnsResolver.outgoingDnsQuery(servers, question);
        org.xbill.DNS.Header header = dnsAnswer.getHeader();
        if (header == null) {
            fail("No header found");
        }
        if (dnsAnswer == null) {
            fail("No answer found");
        }
        if (header.getRcode() != org.xbill.DNS.Rcode.NOERROR) {
            fail("Response was not successful (maybe the DNS server has changed?)");
        }
        if (dnsAnswer.getSectionArray(org.xbill.DNS.Section.ANSWER).length == 0) {
            fail("No answers received");
        }
    }
}
