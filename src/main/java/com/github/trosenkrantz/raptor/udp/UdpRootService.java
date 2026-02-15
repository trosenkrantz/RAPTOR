package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.RootService;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import com.github.trosenkrantz.raptor.io.IpAddressValidator;
import com.github.trosenkrantz.raptor.io.IpPortValidator;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class UdpRootService implements RootService {
    private static final Logger LOGGER = Logger.getLogger(UdpRootService.class.getName());

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
                    case UNICAST -> {
                        configuration.setFullyEscapedString(UdpUtility.PARAMETER_REMOTE_ADDRESS, ConsoleIo.askForString("Hostname / IP address of server socket to send to", UdpUtility.DEFAULT_ADDRESS));
                    }
                    case MULTICAST -> {
                        configuration.setFullyEscapedString(UdpUtility.PARAMETER_REMOTE_ADDRESS, ConsoleIo.askForString("IPv4 or IPv6 multicast group to send to", UdpUtility.DEFAULT_MULTICAST_GROUP, MulticastGroupValidator.VALIDATOR));
                    }
                    case BROADCAST -> {
                    }
                }

                configuration.setInt(UdpUtility.PARAMETER_REMOTE_PORT, ConsoleIo.askForInt("Remote port", UdpUtility.DEFAULT_PORT, IpPortValidator.VALIDATOR));

                String defaultDescription = mode == Mode.BROADCAST ? "every available address" : "primary address of the interface";
                ConsoleIo.askForOptionalString(
                        "Local socket address to send from",
                        defaultDescription,
                        IpAddressValidator.VALIDATOR
                ).ifPresent(address -> configuration.setFullyEscapedString(UdpUtility.PARAMETER_LOCAL_ADDRESS, address));

                ConsoleIo.askForOptionalInt(
                        "Local socket port to send from",
                        "arbitrary ephemeral port",
                        IpPortValidator.VALIDATOR
                ).ifPresent(port -> configuration.setInt(UdpUtility.PARAMETER_LOCAL_PORT, port));

                configuration.setFullyEscapedString(UdpUtility.PARAMETER_PAYLOAD, ConsoleIo.askForString("Payload to send", BytesFormatter.DEFAULT_FULLY_ESCAPED_STRING));
            }
            case RECEIVE -> {
                if (mode == Mode.MULTICAST) {
                    configuration.setFullyEscapedString(UdpUtility.PARAMETER_REMOTE_ADDRESS, ConsoleIo.askForString("Multicast group to receive from", UdpUtility.DEFAULT_MULTICAST_GROUP, MulticastGroupValidator.VALIDATOR));
                }

                configuration.setInt(UdpUtility.PARAMETER_LOCAL_PORT, ConsoleIo.askForInt("Port of local server socket to listen to", UdpUtility.DEFAULT_PORT, IpPortValidator.VALIDATOR));
            }
        }
    }

    @Override
    public void run(Configuration configuration) throws Exception {
        switch (configuration.requireEnum(Role.class)) {
            case SEND -> runSend(configuration, BytesFormatter.raptorEncodingToBytes(configuration.requireFullyEscapedString(UdpUtility.PARAMETER_PAYLOAD)));
            case RECEIVE -> runReceive(configuration);
        }
    }

    private static void runSend(Configuration configuration, byte[] payload) throws IOException {
        switch (configuration.requireEnum(Mode.class)) {
            case UNICAST -> runSendUnicast(configuration, payload);
            case MULTICAST -> runSendMulticast(configuration, payload);
            case BROADCAST -> runSendBroadcast(configuration, payload);
        }
    }

    private static void runSendUnicast(Configuration configuration, byte[] payload) throws IOException {
        String remoteAddressString = configuration.requireFullyEscapedString(UdpUtility.PARAMETER_REMOTE_ADDRESS);
        InetAddress remoteAddress = InetAddress.getByName(remoteAddressString);
        try (DatagramSocket socket = UdpUtility.createSocket(configuration, remoteAddress.getClass())) {
            UdpUtility.send(configuration, remoteAddress, socket, true, payload);
        }
    }

    private static void runSendBroadcast(Configuration configuration, byte[] payload) throws IOException {
        Optional<String> localAddress = configuration.getFullyEscapedString(UdpUtility.PARAMETER_LOCAL_ADDRESS);

        List<InterfaceAddress> foundAddresses = UdpUtility.getAllBroadcastCapableInterfaceAddresses();
        if (localAddress.isPresent()) {
            // Keep only those having the configured IP address
            foundAddresses = foundAddresses.stream().filter(address -> address.getAddress().getHostAddress().equals(localAddress.get())).toList();
        }

        // Do directed broadcast on each network
        try (DatagramSocket socket = UdpUtility.createSocket(configuration)) {
            socket.setBroadcast(true);

            for (InterfaceAddress address : foundAddresses) {
                UdpUtility.send(configuration, address.getBroadcast(), socket, true, payload);
            }
        }

        // Do limited broadcast (255.255.255.255) on each network
        int port = configuration.getInt(UdpUtility.PARAMETER_LOCAL_PORT).orElse(0); // 0 means ephemeral
        for (InterfaceAddress address : foundAddresses) {
            try (DatagramSocket socket = new DatagramSocket(new InetSocketAddress(address.getAddress(), port))) { // Bind explicitly to the address of the network interface, as we cannot automatically bind to the right one based on "255.255.255.255"
                socket.setBroadcast(true);
                UdpUtility.send(configuration, InetAddress.getByName("255.255.255.255"), socket, false, payload); // Since we already explicitly bound the socket, do not connect here as well
            }
        }
    }

    private static void runSendMulticast(Configuration configuration, byte[] payload) throws IOException {
        String groupString = configuration.requireFullyEscapedString(UdpUtility.PARAMETER_REMOTE_ADDRESS);
        InetAddress group = InetAddress.getByName(groupString);

        Optional<String> localAddress = configuration.getFullyEscapedString(UdpUtility.PARAMETER_LOCAL_ADDRESS);
        InetAddress bindAddress = localAddress.isPresent() ? InetAddress.getByName(localAddress.get()) : InetAddress.getByName(IpAddressMapper.getWildcard(group));

        int localPort = configuration.getInt(UdpUtility.PARAMETER_LOCAL_PORT).orElse(0); // 0 means ephemeral
        int destinationPort = configuration.requireInt(UdpUtility.PARAMETER_REMOTE_PORT);

        try (DatagramChannel channel = DatagramChannel.open(IpAddressMapper.getProtocolFamily(group))) {
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            channel.bind(new InetSocketAddress(bindAddress, localPort));

            List<NetworkInterface> foundInterfaces = UdpUtility.getAllMulticastCapableInterfaces(group.getClass());
            if (localAddress.isPresent()) {
                // Keep only those having the configured IP address
                foundInterfaces = foundInterfaces.stream().filter(networkInterface -> Collections.list(networkInterface.getInetAddresses()).stream().anyMatch(address -> address.equals(bindAddress))).toList();
            }

            int successCount = 0;
            for (NetworkInterface networkInterface : foundInterfaces) {
                LOGGER.fine("Sending through network interface " + networkInterfaceToString(networkInterface) + ".");
                try {
                    channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
                    channel.send(ByteBuffer.wrap(payload), new InetSocketAddress(group, destinationPort));
                    successCount++;
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed sending on network interface " + networkInterfaceToString(networkInterface) + ".", e);
                    // And continue to try other interfaces
                }
            }

            LOGGER.info("Sent " + BytesFormatter.getType(payload) + " from local port " + ((InetSocketAddress) channel.getLocalAddress()).getPort() + " to " + groupString + ":" + destinationPort + " through " + successCount + " interface" + (successCount == 1 ? "" : "s") + ": " + BytesFormatter.bytesToRaptorEncoding(payload));
        }
    }

    private static String networkInterfaceToString(NetworkInterface networkInterface) {
        String addresses = Collections.list(networkInterface.getInetAddresses()).stream()
                .map(a -> a.getHostAddress()
                        + " ("
                        + (a.isLinkLocalAddress() ? "link-local " : "")
                        + (a instanceof Inet6Address ? "IPv6" : "IPv4")
                        + ")")
                .collect(Collectors.joining(", "));

        return networkInterface.getDisplayName() + " with addresses [" + addresses + "]";
    }

    private static void runReceive(Configuration configuration) throws IOException {
        switch (configuration.requireEnum(Mode.class)) {
            case UNICAST -> {
                try (DatagramSocket socket = UdpUtility.createSocket(configuration)) {
                    int localPort = socket.getLocalPort();
                    LOGGER.info("Waiting to receive data on port " + localPort);
                    keepReceivingAndPromptUserToCloseSocket(socket, packet -> ":" + localPort);
                } catch (SocketException e) {
                    // If "Socket closed", it was probably closed by user, so ignore
                    if (!"Socket closed".equals(e.getMessage())) throw e;
                }
            }
            case MULTICAST -> {
                try (MulticastSocket socket = UdpUtility.createReceivingMulticastSocket(configuration)) {
                    keepReceivingAndPromptUserToCloseSocket(socket, packet -> configuration.requireFullyEscapedString(UdpUtility.PARAMETER_REMOTE_ADDRESS) + ":" + socket.getLocalPort());
                } catch (SocketException e) {
                    // If "Socket closed", it was probably closed by user, so ignore
                    if (!"Socket closed".equals(e.getMessage())) throw e;
                }
            }
            case BROADCAST -> {
                try (DatagramSocket socket = UdpUtility.createSocket(configuration)) {
                    socket.setBroadcast(true);
                    int localPort = socket.getLocalPort();
                    LOGGER.info("Waiting to receive broadcast data on port " + localPort);
                    keepReceivingAndPromptUserToCloseSocket(socket, packet -> ":" + localPort);
                } catch (SocketException e) {
                    // If "Socket closed", it was probably closed by user, so ignore
                    if (!"Socket closed".equals(e.getMessage())) throw e;
                }
            }
        }
    }

    private static void keepReceivingAndPromptUserToCloseSocket(DatagramSocket socket, Function<DatagramPacket, String> toPrinter) throws IOException {
        byte[] buffer = new byte[UdpUtility.MAX_UDP_PAYLOAD_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        promptUserToCloseSocket(socket);

        while (!socket.isClosed()) {
            socket.receive(packet);
            byte[] payload = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
            LOGGER.info("Received " + BytesFormatter.getType(payload) + " from " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + " to " + toPrinter.apply(packet) + ": " + BytesFormatter.bytesToRaptorEncoding(payload));
        }
    }

    private static void promptUserToCloseSocket(DatagramSocket socket) {
        Thread.ofVirtual().start(() -> {
            ConsoleIo.promptUserToExit();
            socket.close();
        });
    }
}
