package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.logging.Logger;

public class RaptorReceivingMulticastSocket extends MulticastSocket {
    private static final Logger LOGGER = Logger.getLogger(UdpService.class.getName());
    private List<NetworkInterface> allMulticastCapableInterfaces;
    private SocketAddress group;

    public RaptorReceivingMulticastSocket(String multicastGroup) throws IOException {
        super();
        joinMulticastGroup(multicastGroup);
    }

    public RaptorReceivingMulticastSocket(String multicastGroup, int localPort) throws IOException {
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
        LOGGER.info("Waiting to receive data on multicast group " + multicastAddress.getHostAddress() + " on port " + getLocalPort());
    }

    private void leaveMulticastGroup() throws IOException {
        // Leave multicast group on all multicast capable interfaces
        for (NetworkInterface networkInterface : allMulticastCapableInterfaces) {
            leaveGroup(group, networkInterface);
        }
    }

    @Override
    public void close() {
        try {
            leaveMulticastGroup();
        } catch (IOException e) {
            ConsoleIo.writeException("Failed leaving multicast group.", e);
        }
        super.close();
    }
}
