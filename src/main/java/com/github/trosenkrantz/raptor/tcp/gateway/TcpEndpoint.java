package com.github.trosenkrantz.raptor.tcp.gateway;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.gateway.DelayedConsumer;
import com.github.trosenkrantz.raptor.gateway.Endpoint;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.tcp.TcpSendStrategy;
import com.github.trosenkrantz.raptor.tcp.TcpUtility;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TcpEndpoint implements Endpoint {
    private static final Logger LOGGER = Logger.getLogger(TcpEndpoint.class.getName());

    private final DelayedConsumer<byte[]> fromBroker = new DelayedConsumer<>(); // As we connect async, buffer data from broker until connected

    public TcpEndpoint(final Configuration configuration, final Consumer<byte[]> broker, final Runnable onEndpointClosed) throws IOException {
        // Start receiving in a separate thread to not block the main thread
        Thread.ofVirtual().start(() -> {
            try {
                TcpUtility.connectAndStartSendingAndReceiving(configuration, new TcpSendStrategy() {
                    @Override
                    public Consumer<byte[]> start(Socket socket, Runnable shutDownAction) throws IOException {
                        OutputStream out = socket.getOutputStream();

                        // Now that we are connected, we can set what to do with data from the broker
                        fromBroker.setDelegate(payload -> {
                            try {
                                out.write(payload);
                                LOGGER.info("Sent " + BytesFormatter.bytesToFullyEscapedStringWithType(payload));
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });

                        return broker; // When TCP socket receives data, pass to broker
                    }
                });
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed creating TCP connection or receiving data on it.", e);
            } finally {
                // Receiving is blocking, so we inform endpoint closed afterwards
                onEndpointClosed.run();
            }
        });
    }

    @Override
    public void sendToExternalSystem(byte[] payload) {
        fromBroker.accept(payload);
    }
}
