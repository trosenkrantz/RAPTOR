package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.RaptorService;
import com.github.trosenkrantz.raptor.ThrowingRunnable;
import com.github.trosenkrantz.raptor.io.*;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;

public class UdpService implements RaptorService {
    private static final Logger LOGGER = Logger.getLogger(UdpService.class.getName());

    private static final String PARAMETER_DESTINATION_ADDRESS = "destination-address";
    private static final String PARAMETER_SOURCE_PORT = "source-port";
    private static final String PARAMETER_DESTINATION_PORT = "destination-port";
    private static final String PARAMETER_PAYLOAD = "payload";

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 50000;
    private static final String DEFAULT_MULTICAST_GROUP = "224.0.2.0";

    @Override
    public String getPromptValue() {
        return "u";
    }

    @Override
    public String getParameterKey() {
        return "udp";
    }

    @Override
    public String getDescription() {
        return "UDP";
    }

    @Override
    public void configure(Configuration configuration) {
        Mode mode = ConsoleIo.askForOptions(Mode.class);
        configuration.setEnum(mode);

        Role role = ConsoleIo.askForOptions(Role.class);
        configuration.setEnum(role);

        (switch (role) {
            case SEND -> (Runnable) () -> {
                // Configure destination address
                (switch (mode) {
                    case UNICAST -> (Runnable) () -> {
                        configuration.setString(PARAMETER_DESTINATION_ADDRESS, ConsoleIo.askForString("Hostname / IP address of server socket to send to", DEFAULT_HOST));
                    };
                    case MULTICAST -> (Runnable) () -> {
                        configuration.setString(PARAMETER_DESTINATION_ADDRESS, ConsoleIo.askForString("Multicast group to send to", DEFAULT_MULTICAST_GROUP));
                    };
                    case BROADCAST -> (Runnable) () -> {
                    };
                }).run();

                configuration.setInt(PARAMETER_DESTINATION_PORT, ConsoleIo.askForInt("Destination port", DEFAULT_PORT, IpPortValidator.VALIDATOR));

                ConsoleIo.askForOptionalInt(
                        "Local socket to bind to",
                        "arbitrary ephemeral port",
                        IpPortValidator.VALIDATOR
                ).ifPresent(port -> configuration.setInt(PARAMETER_SOURCE_PORT, port));

                configuration.setString(PARAMETER_PAYLOAD, ConsoleIo.askForString("Payload to send", BytesFormatter.DEFAULT_FULLY_ESCAPED_STRING));
            };
            case RECEIVE -> (Runnable) () -> configuration.setInt(PARAMETER_SOURCE_PORT, ConsoleIo.askForInt("Port of local server socket to listen to", DEFAULT_PORT, IpPortValidator.VALIDATOR));
        }).run();
    }

    @Override
    public void run(Configuration configuration) throws Exception {
        (switch (configuration.requireEnum(Role.class)) {
            case SEND -> (switch (configuration.requireEnum(Mode.class)) {
                case UNICAST -> (ThrowingRunnable) () -> {
                    try (DatagramSocket socket = createSocket(configuration)) {
                        send(configuration, InetAddress.getByName(configuration.requireString(PARAMETER_DESTINATION_ADDRESS)), socket, true);
                    }
                };
                case MULTICAST -> (ThrowingRunnable) () -> {
                    try (MulticastSocket socket = createMulticastSocket(configuration)) {
                        for (NetworkInterface networkInterface : getAllMulticastCapableInterfaces()) {
                            socket.setNetworkInterface(networkInterface);
                            send(configuration, InetAddress.getByName(configuration.requireString(PARAMETER_DESTINATION_ADDRESS)), socket, true);
                        }
                    }
                };
                case BROADCAST -> (ThrowingRunnable) () -> {
                    // Do directed broadcast on each network
                    try (DatagramSocket socket = createSocket(configuration)) {
                        socket.setBroadcast(true);

                        for (InterfaceAddress address : getAllBroadcastCapableInterfaceAddresses()) {
                            send(configuration, address.getBroadcast(), socket, true);
                        }
                    }

                    // Do limited broadcast on each network
                    int port = configuration.getInt(PARAMETER_SOURCE_PORT).orElse(0); // 0 means ephemeral
                    for (InterfaceAddress address : getAllBroadcastCapableInterfaceAddresses()) {
                        try (DatagramSocket socket = new DatagramSocket(new InetSocketAddress(address.getAddress(), port))) { // Bind explicitly to the address of the network interface, as we cannot automatically bind to the right one based on "255.255.255.255"
                            socket.setBroadcast(true);
                            send(configuration, InetAddress.getByName("255.255.255.255"), socket, false); // Since we already explicitly bound the socket, do not connect here as well
                        }
                    }
                };
            });
            case RECEIVE -> (switch (configuration.requireEnum(Mode.class)) {
                case UNICAST -> (ThrowingRunnable) () -> {

                };
                case MULTICAST -> (ThrowingRunnable) () -> {

                };
                case BROADCAST -> (ThrowingRunnable) () -> {

                };
            });
        }).run();
    }

    private static DatagramSocket createSocket(Configuration configuration) throws SocketException {
        Optional<Integer> port = configuration.getInt(PARAMETER_SOURCE_PORT);
        if (port.isPresent()) return new DatagramSocket(port.get());
        else return new DatagramSocket();
    }

    private static MulticastSocket createMulticastSocket(Configuration configuration) throws IOException {
        Optional<Integer> port = configuration.getInt(PARAMETER_SOURCE_PORT);
        if (port.isPresent()) return new MulticastSocket(port.get());
        else return new MulticastSocket();
    }

    private static List<NetworkInterface> getAllMulticastCapableInterfaces() throws SocketException {
        return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                .filter(CheckedPredicate.wrap(NetworkInterface::isUp))
                .filter(CheckedPredicate.wrap(NetworkInterface::supportsMulticast))
                .filter(CheckedPredicate.wrap(anInterface -> !anInterface.isLoopback())) // Loopback interface does not normally support multicast
                .filter(anInterface -> !anInterface.getInterfaceAddresses().isEmpty()) // Sometimes, there can be interfaces without an address that still claims to support multicast, so we filter those away
                .toList();
    }

    private static List<InterfaceAddress> getAllBroadcastCapableInterfaceAddresses() throws SocketException {
        return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                .filter(CheckedPredicate.wrap(NetworkInterface::isUp))
                .flatMap(anInterface -> anInterface.getInterfaceAddresses().stream())
                .filter(address -> address.getBroadcast() != null)
                .toList();
    }

    private static void send(Configuration configuration, InetAddress destinationAddress, DatagramSocket socket, boolean connect) throws IOException {
        byte[] payload = BytesFormatter.fullyEscapedStringToBytes(configuration.requireString(PARAMETER_PAYLOAD));

        int destinationPort = configuration.requireInt(PARAMETER_DESTINATION_PORT);

        // Explicitly connect to get the actual source address instead of the wildcard address
        if (connect) socket.connect(destinationAddress, destinationPort);

        DatagramPacket packet = new DatagramPacket(
                payload,
                payload.length,
                destinationAddress,
                destinationPort
        );

        socket.send(packet);
        LOGGER.info("Sent " + (BytesFormatter.isText(payload) ? "text" : "bytes") + " from " + socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort() + " to " + destinationAddress.getHostAddress() + ":" + destinationPort + ": " + BytesFormatter.bytesToFullyEscapedString(payload));
    }
}
