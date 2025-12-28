package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.RootService;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import com.github.trosenkrantz.raptor.io.IpPortValidator;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.function.Function;
import java.util.logging.Logger;

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
                    case UNICAST -> configuration.setString(UdpUtility.PARAMETER_REMOTE_ADDRESS, ConsoleIo.askForString("Hostname / IP address of server socket to send to", UdpUtility.DEFAULT_ADDRESS));
                    case MULTICAST -> configuration.setString(UdpUtility.PARAMETER_REMOTE_ADDRESS, ConsoleIo.askForString("Multicast group to send to", UdpUtility.DEFAULT_MULTICAST_GROUP));
                    case BROADCAST -> {
                    }
                }

                configuration.setInt(UdpUtility.PARAMETER_REMOTE_PORT, ConsoleIo.askForInt("Remote port", UdpUtility.DEFAULT_PORT, IpPortValidator.VALIDATOR));

                ConsoleIo.askForOptionalInt(
                        "Local socket port to bind to",
                        "arbitrary ephemeral port",
                        IpPortValidator.VALIDATOR
                ).ifPresent(port -> configuration.setInt(UdpUtility.PARAMETER_LOCAL_PORT, port));

                configuration.setString(UdpUtility.PARAMETER_PAYLOAD, ConsoleIo.askForString("Payload to send", BytesFormatter.DEFAULT_FULLY_ESCAPED_STRING));
            }
            case RECEIVE -> {
                if (mode == Mode.MULTICAST) {
                    configuration.setString(UdpUtility.PARAMETER_REMOTE_ADDRESS, ConsoleIo.askForString("Multicast group to receive from", UdpUtility.DEFAULT_MULTICAST_GROUP));
                }

                configuration.setInt(UdpUtility.PARAMETER_LOCAL_PORT, ConsoleIo.askForInt("Port of local server socket to listen to", UdpUtility.DEFAULT_PORT, IpPortValidator.VALIDATOR));
            }
        }
    }

    @Override
    public void run(Configuration configuration) throws Exception {
        switch (configuration.requireEnum(Role.class)) {
            case SEND -> runSend(configuration, BytesFormatter.fullyEscapedStringToBytes(configuration.requireString(UdpUtility.PARAMETER_PAYLOAD)));
            case RECEIVE -> runReceive(configuration);
        }
    }

    private static void runSend(Configuration configuration, byte[] payload) throws IOException {
        switch (configuration.requireEnum(Mode.class)) {
            case UNICAST -> {
                try (DatagramSocket socket = UdpUtility.createSocket(configuration)) {
                    UdpUtility.send(configuration, InetAddress.getByName(configuration.requireString(UdpUtility.PARAMETER_REMOTE_ADDRESS)), socket, true, payload);
                }
            }
            case MULTICAST -> {
                try (MulticastSocket socket = UdpUtility.createMulticastSocket(configuration)) {
                    for (NetworkInterface networkInterface : UdpUtility.getAllMulticastCapableInterfaces()) {
                        socket.setNetworkInterface(networkInterface);
                        UdpUtility.send(configuration, InetAddress.getByName(configuration.requireString(UdpUtility.PARAMETER_REMOTE_ADDRESS)), socket, true, payload);
                    }
                }
            }
            case BROADCAST -> {
                // Do directed broadcast on each network
                try (DatagramSocket socket = UdpUtility.createSocket(configuration)) {
                    socket.setBroadcast(true);

                    for (InterfaceAddress address : UdpUtility.getAllBroadcastCapableInterfaceAddresses()) {
                        UdpUtility.send(configuration, address.getBroadcast(), socket, true, payload);
                    }
                }

                // Do limited broadcast (255.255.255.255) on each network
                int port = configuration.getInt(UdpUtility.PARAMETER_LOCAL_PORT).orElse(0); // 0 means ephemeral
                for (InterfaceAddress address : UdpUtility.getAllBroadcastCapableInterfaceAddresses()) {
                    try (DatagramSocket socket = new DatagramSocket(new InetSocketAddress(address.getAddress(), port))) { // Bind explicitly to the address of the network interface, as we cannot automatically bind to the right one based on "255.255.255.255"
                        socket.setBroadcast(true);
                        UdpUtility.send(configuration, InetAddress.getByName("255.255.255.255"), socket, false, payload); // Since we already explicitly bound the socket, do not connect here as well
                    }
                }
            }
        }
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
                    keepReceivingAndPromptUserToCloseSocket(socket, packet -> configuration.requireString(UdpUtility.PARAMETER_REMOTE_ADDRESS) + ":" + socket.getLocalPort());
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
            LOGGER.info("Received " + BytesFormatter.getType(payload) + " from " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + " to " + toPrinter.apply(packet) + ": " + BytesFormatter.bytesToFullyEscapedString(payload));
        }
    }

    private static void promptUserToCloseSocket(DatagramSocket socket) {
        Thread.ofVirtual().start(() -> {
            ConsoleIo.promptUserToExit();
            socket.close();
        });
    }
}
