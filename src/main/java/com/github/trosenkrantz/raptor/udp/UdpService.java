package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.RootService;
import com.github.trosenkrantz.raptor.gateway.Endpoint;
import com.github.trosenkrantz.raptor.gateway.EndpointService;
import com.github.trosenkrantz.raptor.io.*;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class UdpService implements RootService, EndpointService {
    private static final Logger LOGGER = Logger.getLogger(UdpService.class.getName());

    private static final String PARAMETER_LOCAL_PORT = "local-port";
    private static final String PARAMETER_REMOTE_PORT = "remote-port";
    private static final String PARAMETER_REMOTE_ADDRESS = "remote-address";
    private static final String PARAMETER_PAYLOAD = "payload";

    private static final String DEFAULT_ADDRESS = "localhost";
    private static final int DEFAULT_PORT = 50000;
    private static final String DEFAULT_MULTICAST_GROUP = "224.0.2.0";
    public static final int MAX_UDP_PAYLOAD_SIZE = 65507;

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

        switch (role) {
            case SEND -> {
                // Configure remote address
                switch (mode) {
                    case UNICAST -> configuration.setString(PARAMETER_REMOTE_ADDRESS, ConsoleIo.askForString("Hostname / IP address of server socket to send to", DEFAULT_ADDRESS));
                    case MULTICAST -> configuration.setString(PARAMETER_REMOTE_ADDRESS, ConsoleIo.askForString("Multicast group to send to", DEFAULT_MULTICAST_GROUP));
                    case BROADCAST -> {
                    }
                }

                configureRemotePort(configuration);

                configureLocalPortAllowingForEphemeral(configuration);

                configuration.setString(PARAMETER_PAYLOAD, ConsoleIo.askForString("Payload to send", BytesFormatter.DEFAULT_FULLY_ESCAPED_STRING));
            }
            case RECEIVE -> {
                if (mode == Mode.MULTICAST) {
                    configuration.setString(PARAMETER_REMOTE_ADDRESS, ConsoleIo.askForString("Multicast group to receive from", DEFAULT_MULTICAST_GROUP));
                }

                configuration.setInt(PARAMETER_LOCAL_PORT, ConsoleIo.askForInt("Port of local server socket to listen to", DEFAULT_PORT, IpPortValidator.VALIDATOR));
            }
        }
    }

    private static void configureLocalPortAllowingForEphemeral(Configuration configuration) {
        ConsoleIo.askForOptionalInt(
                "Local socket port to bind to",
                "arbitrary ephemeral port",
                IpPortValidator.VALIDATOR
        ).ifPresent(port -> configuration.setInt(PARAMETER_LOCAL_PORT, port));
    }

    private static void configureRemotePort(Configuration configuration) {
        configuration.setInt(PARAMETER_REMOTE_PORT, ConsoleIo.askForInt("Remote port", DEFAULT_PORT, IpPortValidator.VALIDATOR));
    }

    @Override
    public void configureEndpoint(Configuration configuration) {
        EndpointMode mode = ConsoleIo.askForOptions(EndpointMode.class);
        configuration.setEnum("mode", mode);

        if (mode == EndpointMode.MULTICAST) {
            configuration.setString(PARAMETER_REMOTE_ADDRESS, ConsoleIo.askForString("Multicast group to use", DEFAULT_MULTICAST_GROUP));
        }

        configureRemotePort(configuration);

        configureLocalPortAllowingForEphemeral(configuration);
    }

    @Override
    public void run(Configuration configuration) throws Exception {
        switch (configuration.requireEnum(Role.class)) {
            case SEND -> runSend(configuration);
            case RECEIVE -> runReceive(configuration);
        }
    }

    private static void runSend(Configuration configuration) throws IOException {
        switch (configuration.requireEnum(Mode.class)) {
            case UNICAST -> {
                try (DatagramSocket socket = createSocket(configuration)) {
                    send(configuration, InetAddress.getByName(configuration.requireString(PARAMETER_REMOTE_ADDRESS)), socket, true);
                }
            }
            case MULTICAST -> {
                try (MulticastSocket socket = createMulticastSocket(configuration)) {
                    for (NetworkInterface networkInterface : getAllMulticastCapableInterfaces()) {
                        socket.setNetworkInterface(networkInterface);
                        send(configuration, InetAddress.getByName(configuration.requireString(PARAMETER_REMOTE_ADDRESS)), socket, true);
                    }
                }
            }
            case BROADCAST -> {
                // Do directed broadcast on each network
                try (DatagramSocket socket = createSocket(configuration)) {
                    socket.setBroadcast(true);

                    for (InterfaceAddress address : getAllBroadcastCapableInterfaceAddresses()) {
                        send(configuration, address.getBroadcast(), socket, true);
                    }
                }

                // Do limited broadcast (255.255.255.255) on each network
                int port = configuration.getInt(PARAMETER_LOCAL_PORT).orElse(0); // 0 means ephemeral
                for (InterfaceAddress address : getAllBroadcastCapableInterfaceAddresses()) {
                    try (DatagramSocket socket = new DatagramSocket(new InetSocketAddress(address.getAddress(), port))) { // Bind explicitly to the address of the network interface, as we cannot automatically bind to the right one based on "255.255.255.255"
                        socket.setBroadcast(true);
                        send(configuration, InetAddress.getByName("255.255.255.255"), socket, false); // Since we already explicitly bound the socket, do not connect here as well
                    }
                }
            }
        }
    }

    private static void runReceive(Configuration configuration) {
        switch (configuration.requireEnum(Mode.class)) {
            case UNICAST -> {
                try (DatagramSocket socket = createSocket(configuration)) {
                    LOGGER.info("Waiting to receive data on port " + socket.getLocalPort());
                    keepReceivingAndPromptUserToCloseSocket(socket);
                } catch (SocketException e) {
                    if (!"Socket closed".equals(e.getMessage())) ConsoleIo.writeException(e); // Probably closed by user
                } catch (IOException e) {
                    ConsoleIo.writeException(e);
                }
            }
            case MULTICAST -> {
                try (MulticastSocket socket = createMulticastSocket(configuration)) {
                    // Join multicast group on all multicast capable interfaces
                    InetAddress multicastAddress = InetAddress.getByName(configuration.requireString(PARAMETER_REMOTE_ADDRESS));
                    SocketAddress group = new InetSocketAddress(multicastAddress, 0);
                    List<NetworkInterface> allMulticastCapableInterfaces = getAllMulticastCapableInterfaces();
                    for (NetworkInterface networkInterface : allMulticastCapableInterfaces) {
                        socket.joinGroup(group, networkInterface);
                    }
                    LOGGER.info("Waiting to receive data on multicast group " + multicastAddress.getHostAddress() + " on port " + socket.getLocalPort());

                    keepReceivingAndPromptUserToCloseSocket(socket);

                    // Leave multicast group on all multicast capable interfaces
                    for (NetworkInterface networkInterface : allMulticastCapableInterfaces) {
                        socket.leaveGroup(group, networkInterface);
                    }
                } catch (SocketException e) {
                    if (!"Socket closed".equals(e.getMessage())) ConsoleIo.writeException(e); // Probably closed by user
                } catch (IOException e) {
                    ConsoleIo.writeException(e);
                }
            }
            case BROADCAST -> {
                try (DatagramSocket socket = createSocket(configuration)) {
                    socket.setBroadcast(true);
                    LOGGER.info("Waiting to receive broadcast data on port " + socket.getLocalPort());
                    keepReceivingAndPromptUserToCloseSocket(socket);
                } catch (SocketException e) {
                    if (!"Socket closed".equals(e.getMessage())) ConsoleIo.writeException(e); // Probably closed by user
                } catch (IOException e) {
                    ConsoleIo.writeException(e);
                }
            }
        }
    }

    private static void keepReceivingAndPromptUserToCloseSocket(DatagramSocket socket) throws IOException {
        byte[] buffer = new byte[MAX_UDP_PAYLOAD_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        promptUserToCloseSocket(socket);

        while (!socket.isClosed()) {
            socket.receive(packet);
            byte[] payload = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
            LOGGER.info("Received " + BytesFormatter.getType(payload) + " from " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + " to " + packet.getSocketAddress() + ": " + BytesFormatter.bytesToFullyEscapedString(payload));
        }
    }

    private static void promptUserToCloseSocket(DatagramSocket socket) {
        Thread.ofVirtual().start(() -> {
            ConsoleIo.promptUserToExit();
            socket.close();
        });
    }

    private static DatagramSocket createSocket(Configuration configuration) throws SocketException {
        Optional<Integer> port = configuration.getInt(PARAMETER_LOCAL_PORT);
        if (port.isPresent()) return new DatagramSocket(port.get());
        else return new DatagramSocket();
    }

    private static MulticastSocket createMulticastSocket(Configuration configuration) throws IOException {
        Optional<Integer> port = configuration.getInt(PARAMETER_LOCAL_PORT);
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

    @Override
    public Endpoint createEndpoint(Configuration configuration, Consumer<byte[]> broker) {
        return null;
    }
}
