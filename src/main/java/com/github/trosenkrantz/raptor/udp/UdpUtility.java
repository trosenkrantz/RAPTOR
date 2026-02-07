package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.CheckedPredicate;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class UdpUtility {
    private static final Logger LOGGER = Logger.getLogger(UdpUtility.class.getName());

    public static final String PARAMETER_LOCAL_PORT = "localPort";
    public static final String PARAMETER_REMOTE_PORT = "remotePort";
    public static final String PARAMETER_REMOTE_ADDRESS = "remoteAddress";
    public static final String PARAMETER_PAYLOAD = "payload";
    public static final String DEFAULT_ADDRESS = "localhost";
    public static final int DEFAULT_PORT = 50000;
    public static final String DEFAULT_MULTICAST_GROUP = "224.0.2.0";
    public static final int MAX_UDP_PAYLOAD_SIZE = 65507;

    static DatagramSocket createSocket(Configuration configuration) throws SocketException {
        Optional<Integer> port = configuration.getInt(PARAMETER_LOCAL_PORT);
        if (port.isPresent()) return new DatagramSocket(port.get());
        else return new DatagramSocket();
    }

    public static void send(Configuration configuration, InetAddress destinationAddress, DatagramSocket socket, boolean connect, byte[] payload) throws IOException {
        int destinationPort = configuration.requireInt(PARAMETER_REMOTE_PORT);

        // Explicitly connect to get the actual source address instead of the wildcard address
        if (connect) socket.connect(destinationAddress, destinationPort);

        DatagramPacket packet = new DatagramPacket(
                payload,
                payload.length,
                destinationAddress,
                destinationPort
        );

        socket.send(packet);
        LOGGER.info("Sent " + BytesFormatter.getType(payload) + " from " + socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort() + " to " + destinationAddress.getHostAddress() + ":" + destinationPort + ": " + BytesFormatter.bytesToFullyEscapedString(payload));
    }

    public static AllReceivingMulticastSocket createReceivingMulticastSocket(Configuration configuration) throws IOException {
        String multicastGroup = configuration.requireFullyEscapedString(PARAMETER_REMOTE_ADDRESS);
        Optional<Integer> port = configuration.getInt(PARAMETER_LOCAL_PORT);
        if (port.isPresent()) return new AllReceivingMulticastSocket(multicastGroup, port.get());
        else return new AllReceivingMulticastSocket(multicastGroup);
    }

    public static List<NetworkInterface> getAllMulticastCapableInterfaces() throws SocketException {
        return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                .filter(CheckedPredicate.wrap(NetworkInterface::isUp))
                .filter(CheckedPredicate.wrap(NetworkInterface::supportsMulticast))
                .filter(CheckedPredicate.wrap(anInterface -> !anInterface.isLoopback())) // Loopback interface does not normally support multicast
                .filter(anInterface -> !anInterface.getInterfaceAddresses().isEmpty()) // Sometimes, there can be interfaces without an address that still claims to support multicast, so we filter those away
                .toList();
    }

    public static List<NetworkInterface> getAllMulticastCapableInterfaces(Class<? extends InetAddress> family) throws SocketException {
        return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                .filter(CheckedPredicate.wrap(NetworkInterface::isUp))
                .filter(CheckedPredicate.wrap(NetworkInterface::supportsMulticast))
                .filter(CheckedPredicate.wrap(anInterface -> !anInterface.isLoopback())) // Loopback interface does not normally support multicast
                .filter(i -> i.getInterfaceAddresses().stream().map(InterfaceAddress::getAddress).anyMatch(family::isInstance)) // Has IP address matching IPv4 / IPv6 family
                .toList();
    }


    static List<InterfaceAddress> getAllBroadcastCapableInterfaceAddresses() throws SocketException {
        return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                .filter(CheckedPredicate.wrap(NetworkInterface::isUp))
                .flatMap(anInterface -> anInterface.getInterfaceAddresses().stream())
                .filter(address -> address.getBroadcast() != null)
                .toList();
    }
}
