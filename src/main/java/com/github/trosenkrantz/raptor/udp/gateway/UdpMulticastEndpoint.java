package com.github.trosenkrantz.raptor.udp.gateway;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.gateway.Endpoint;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.udp.UdpUtility;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UdpMulticastEndpoint implements Endpoint {
    private static final Logger LOGGER = Logger.getLogger(UdpMulticastEndpoint.class.getName());

    private final Configuration sendConfiguration;
    private final String multicastGroup;
    private final MulticastSocket sendSocket;
    private final int sendLocalPort;

    public UdpMulticastEndpoint(final Configuration configuration, final Consumer<byte[]> broker, final Runnable onEndpointClosed) throws IOException {
        // Map configuration to one for receiving
        Configuration receiveConfiguration = configuration.copy();
        receiveConfiguration.setInt(UdpUtility.PARAMETER_LOCAL_PORT, configuration.requireInt(UdpEndpointService.PARAMETER_PORT));

        // Start receiving in a separate thread to not block the main thread
        Thread.ofVirtual().start(() -> {
            try {
                createReceivingSocketAndKeepReceiving(receiveConfiguration, broker);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed creating or receiving from socket.", e);
            } finally {
                // Receiving is blocking, so we inform endpoint closed afterwards
                onEndpointClosed.run();
            }
        });

        // Map configuration to one for sending
        sendConfiguration = configuration.copy();
        sendConfiguration.setInt(UdpUtility.PARAMETER_REMOTE_PORT, configuration.requireInt(UdpEndpointService.PARAMETER_PORT));
        configuration.getInt(UdpEndpointService.PARAMETER_LOCAL_SEND_PORT).ifPresent(port -> sendConfiguration.setInt(UdpUtility.PARAMETER_LOCAL_PORT, port));

        multicastGroup = configuration.requireString(UdpUtility.PARAMETER_REMOTE_ADDRESS);
        sendSocket = UdpUtility.createMulticastSocket(configuration);
        sendLocalPort = sendSocket.getLocalPort();
    }

    private void createReceivingSocketAndKeepReceiving(Configuration receiveConfiguration, Consumer<byte[]> broker) throws IOException {
        try (MulticastSocket socket = UdpUtility.createReceivingMulticastSocket(receiveConfiguration)) {
            keepReceiving(socket, broker);
        } catch (SocketException e) {
            // If "Socket closed", it was probably closed by user, so ignore
            if (!"Socket closed".equals(e.getMessage())) throw e;
        }
    }

    private void keepReceiving(DatagramSocket socket, Consumer<byte[]> consumer) throws IOException {
        byte[] buffer = new byte[UdpUtility.MAX_UDP_PAYLOAD_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (!socket.isClosed()) {
            socket.receive(packet);

            if (packet.getPort() == sendLocalPort) {
                // Ignore packets sent from self
                continue;
            }

            byte[] payload = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
            LOGGER.info("Received " + BytesFormatter.getType(payload) + " from " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + " to " + multicastGroup + ":" + socket.getLocalPort() + ": " + BytesFormatter.bytesToFullyEscapedString(payload));
            consumer.accept(payload);
        }
    }

    @Override
    public void sendToExternalSystem(byte[] payload) {
        try {
            for (NetworkInterface networkInterface : UdpUtility.getAllMulticastCapableInterfaces()) {
                sendSocket.setNetworkInterface(networkInterface);
                UdpUtility.send(sendConfiguration, InetAddress.getByName(multicastGroup), sendSocket, true, payload);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed sending to external system.", e);
            // Do not consider the endpoint closed on send failure, as it might be a temporary issue
        }
    }
}
