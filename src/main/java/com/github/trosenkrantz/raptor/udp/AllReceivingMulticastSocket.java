package com.github.trosenkrantz.raptor.udp;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AllReceivingMulticastSocket extends MulticastSocket {
    private static final Logger LOGGER = Logger.getLogger(AllReceivingMulticastSocket.class.getName());
    private List<NetworkInterface> allMulticastCapableInterfaces;
    private SocketAddress group;

    private volatile boolean closed = false;

    public AllReceivingMulticastSocket(String multicastGroup) throws IOException {
        super();
        joinMulticastGroup(multicastGroup);
    }

    public AllReceivingMulticastSocket(String multicastGroup, int localPort) throws IOException {
        super(localPort);
        joinMulticastGroup(multicastGroup);
    }

    private void joinMulticastGroup(String multicastGroup) throws IOException {
        // Join multicast group on all multicast capable interfaces
        InetAddress multicastAddress = InetAddress.getByName(multicastGroup);
        group = new InetSocketAddress(multicastAddress, 0);
        allMulticastCapableInterfaces = UdpUtility.getAllMulticastCapableInterfaces();
        for (NetworkInterface networkInterface : allMulticastCapableInterfaces) {
            joinGroup(group, networkInterface);
        }
        LOGGER.info("Waiting to receive data on multicast group " + multicastAddress.getHostAddress() + " on port " + getLocalPort() + ".");
    }

    private void leaveMulticastGroup() throws IOException {
        // Leave multicast group on all multicast capable interfaces
        for (NetworkInterface networkInterface : allMulticastCapableInterfaces) {
            leaveGroup(group, networkInterface);
        }
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;

        try {
            leaveMulticastGroup();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed leaving multicast group.", e);
        }
        super.close();
    }
}
