package com.github.trosenkrantz.raptor.serial.port.gateway;

import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.gateway.DelayedConsumer;
import com.github.trosenkrantz.raptor.gateway.Endpoint;
import com.github.trosenkrantz.raptor.serial.port.SerialPortUtility;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SerialPortEndpoint implements Endpoint {
    private static final Logger LOGGER = Logger.getLogger(SerialPortEndpoint.class.getName());

    private final DelayedConsumer<byte[]> fromBroker = new DelayedConsumer<>(); // As we connect async, buffer data from broker until serial port is open

    public SerialPortEndpoint(Configuration configuration, Consumer<byte[]> broker, Runnable onEndpointClosed) {
        // Start receiving in a separate thread to not block the main thread
        Thread.ofVirtual().start(() -> {
            try {
                SerialPortUtility.connectAndStartSendingAndReceiving(configuration, (ignore1, port, ignore2) -> {
                    // Now that the port is open, we can set what to do with data from the broker
                    fromBroker.setDelegate(payload -> SerialPortUtility.writeToPort(port, payload));

                    return broker; // When serial port receives data, pass to broker
                });
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed creating or receiving serial port.", e);
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
