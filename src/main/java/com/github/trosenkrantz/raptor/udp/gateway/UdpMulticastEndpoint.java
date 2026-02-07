package com.github.trosenkrantz.raptor.udp.gateway;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.gateway.Endpoint;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.udp.IpAddressMapper;
import com.github.trosenkrantz.raptor.udp.UdpUtility;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UdpMulticastEndpoint implements Endpoint {
    private static final Logger LOGGER = Logger.getLogger(UdpMulticastEndpoint.class.getName());

    private final Configuration sendConfiguration;
    private final String multicastGroupString;
    private final InetAddress multicastGroup;
    private final DatagramChannel sendChannel;
    private final int sendLocalPort;

    public UdpMulticastEndpoint(final Configuration configuration, final Consumer<byte[]> broker, final Runnable onEndpointClosed) throws IOException {
        multicastGroupString = configuration.requireFullyEscapedString(UdpUtility.PARAMETER_REMOTE_ADDRESS);
        multicastGroup = InetAddress.getByName(multicastGroupString);

        // Map configuration to one for receiving
        Configuration receiveConfiguration = configuration.copy();
        receiveConfiguration.setInt(UdpUtility.PARAMETER_LOCAL_PORT, configuration.requireInt(UdpEndpointService.PARAMETER_PORT));

        // Start receiving in a separate thread to not block the main thread
        Thread.ofVirtual().start(() -> {
            try {
                createReceivingChannelAndKeepReceiving(receiveConfiguration, broker);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed receiving multicast.", e);
            } finally {
                // Receiving is blocking, so we inform endpoint closed afterwards
                onEndpointClosed.run();
            }
        });

        // Map configuration to one for sending
        sendConfiguration = configuration.copy();
        sendConfiguration.setInt(UdpUtility.PARAMETER_REMOTE_PORT, configuration.requireInt(UdpEndpointService.PARAMETER_PORT));
        configuration.getInt(UdpEndpointService.PARAMETER_LOCAL_SEND_PORT).ifPresent(port -> sendConfiguration.setInt(UdpUtility.PARAMETER_LOCAL_PORT, port));

        sendChannel = DatagramChannel.open(IpAddressMapper.getProtocolFamily(multicastGroup));

        Optional<Integer> localSendPort = sendConfiguration.getInt(UdpUtility.PARAMETER_LOCAL_PORT);
        if (localSendPort.isPresent()) {
            sendChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            sendChannel.bind(new InetSocketAddress(IpAddressMapper.getWildcard(multicastGroup), localSendPort.get()));
        } else {
            // force ephemeral bind
            sendChannel.bind(new InetSocketAddress(IpAddressMapper.getWildcard(multicastGroup), 0));
        }

        sendLocalPort = ((InetSocketAddress) sendChannel.getLocalAddress()).getPort();
    }

    private void createReceivingChannelAndKeepReceiving(Configuration receiveConfiguration, Consumer<byte[]> broker) throws IOException {
        InetAddress group = multicastGroup;
        int port = receiveConfiguration.requireInt(UdpUtility.PARAMETER_LOCAL_PORT);

        try (DatagramChannel channel = DatagramChannel.open(IpAddressMapper.getProtocolFamily(group))) {
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            channel.bind(new InetSocketAddress(IpAddressMapper.getWildcard(group), port));

            for (NetworkInterface nif : UdpUtility.getAllMulticastCapableInterfaces(group.getClass())) {
                channel.join(group, nif);
            }

            LOGGER.info("Waiting to receive data on multicast group " + multicastGroup.getHostAddress() + " on port " + port + ".");
            keepReceiving(channel, broker);
        }
    }

    private void keepReceiving(DatagramChannel channel, Consumer<byte[]> consumer) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(UdpUtility.MAX_UDP_PAYLOAD_SIZE);

        while (true) {
            buffer.clear();

            InetSocketAddress source = (InetSocketAddress) channel.receive(buffer);

            int sourcePort = source.getPort();

            if (sourcePort == sendLocalPort) {
                // Ignore packets sent from self
                continue;
            }

            buffer.flip();

            byte[] payload = new byte[buffer.remaining()];
            buffer.get(payload);

            InetSocketAddress local = (InetSocketAddress) channel.getLocalAddress();

            LOGGER.info("Received " + BytesFormatter.getType(payload) + " from " + source.getAddress().getHostAddress() + ":" + sourcePort + " to " + multicastGroup.getHostAddress() + ":" + local.getPort() + ": " + BytesFormatter.bytesToFullyEscapedString(payload));

            consumer.accept(payload);
        }
    }

    @Override
    public void sendToExternalSystem(byte[] payload) {
        int destinationPort = sendConfiguration.requireInt(UdpUtility.PARAMETER_REMOTE_PORT);

        try {
            for (NetworkInterface nif : UdpUtility.getAllMulticastCapableInterfaces(multicastGroup.getClass())) {
                sendChannel.setOption(StandardSocketOptions.IP_MULTICAST_IF, nif);
                sendChannel.send(ByteBuffer.wrap(payload), new InetSocketAddress(multicastGroup, destinationPort));

                LOGGER.info("Sent " + BytesFormatter.getType(payload) + " from local port " + ((InetSocketAddress) sendChannel.getLocalAddress()).getPort() + " to " + multicastGroupString + ":" + destinationPort + ": " + BytesFormatter.bytesToFullyEscapedString(payload));
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed sending multicast.", e);
            // Do not consider the endpoint closed on send failure, as it might be a temporary issue
        }
    }
}
