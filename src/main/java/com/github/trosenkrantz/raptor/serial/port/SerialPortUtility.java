package com.github.trosenkrantz.raptor.serial.port;

import com.fazecast.jSerialComm.SerialPort;
import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SerialPortUtility {
    public static final String PARAMETER_PORT = "port";
    public static final String PARAMETER_BAUD_RATE = "baud-rate";
    public static final String PARAMETER_DATA_BITS = "data-bits";
    private static final Logger LOGGER = Logger.getLogger(SerialPortUtility.class.getName());
    
    private static final String DEFAULT_PORT = "COM1";
    private static final int DEFAULT_BAUD_RATE = 9600;
    private static final int DEFAULT_DATA_BITS = 8;
    private static final StopBits DEFAULT_STOP_BITS = StopBits.ONE;
    private static final Parity DEFAULT_PARITY = Parity.NO;

    public static void configureConnectivity(Configuration configuration) {
        // Configure port
        List<String> portNames = Arrays.stream(SerialPort.getCommPorts())
                .map(SerialPort::getSystemPortName)
                .sorted()
                .toList();
        LOGGER.info("Available ports on this machine: " + String.join(", ", portNames));
        String defaultPort = portNames.isEmpty() ? DEFAULT_PORT : portNames.getFirst();
        configuration.setFullyEscapedString(PARAMETER_PORT, ConsoleIo.askForString("port name", defaultPort));

        configuration.setInt(PARAMETER_BAUD_RATE, ConsoleIo.askForInt("Baud rate", DEFAULT_BAUD_RATE));

        configuration.setInt(PARAMETER_DATA_BITS, ConsoleIo.askForInt("Data bits", DEFAULT_DATA_BITS, value -> {
            if (value == 5 || value == 6 || value == 7 || value == 8) {
                return Optional.empty();
            } else {
                return Optional.of("Data bits must be 5, 6, 7, or 8.");
            }
        }));

        configuration.setEnum(ConsoleIo.askForOptions(StopBits.class, DEFAULT_STOP_BITS));

        configuration.setEnum(ConsoleIo.askForOptions(Parity.class, DEFAULT_PARITY));
    }

    /**
     * This call is blocking.
     * @param configuration configuration
     * @param sendStrategy controls what to do when serial port receives data and what and when to send data over the serial port
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted
     */
    public static void connectAndStartSendingAndReceiving(Configuration configuration, SerialPortSendStrategy sendStrategy) throws IOException, InterruptedException {
        String portName = configuration.requireFullyEscapedString(PARAMETER_PORT);

        SerialPort serialPort = SerialPort.getCommPort(portName);
        serialPort.setComPortParameters(
                configuration.requireInt(PARAMETER_BAUD_RATE),
                configuration.requireInt(PARAMETER_DATA_BITS),
                configuration.requireEnum(StopBits.class).getValue(),
                configuration.requireEnum(Parity.class).getValue()
        );

        if (!serialPort.isOpen()) {
            LOGGER.info("Opening port " + portName + "...");
            if (serialPort.openPort()) {
                LOGGER.info("Port " + portName + " opened.");
            } else {
                LOGGER.severe("Failed to open port.");
                return;
            }
        } else {
            LOGGER.info("Port " + portName + " is already open.");
        }

        CountDownLatch shutDownLatch = new CountDownLatch(1);

        Consumer<byte[]> onReceivedData = sendStrategy.start(configuration, serialPort, shutDownLatch::countDown);

        LOGGER.info("Listening to " + portName + "...");
        // We can only have a single listener according to the JDoc
        serialPort.addDataListener(new MySerialPortDataListener(onReceivedData));

        shutDownLatch.await();
        try {
            serialPort.closePort();
            LOGGER.info("Closed port " + portName + ".");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed closing port " + portName + ".", e);
        }
    }

    public static void writeToPort(SerialPort port, byte[] payload) {
        int writeResult = port.writeBytes(payload, payload.length);
        if (writeResult == -1) {
            LOGGER.severe("Failed writing to port " + port.getSystemPortName() + ".");
            return;
        }

        boolean flushResult = port.flushIOBuffers();
        if (!flushResult) {
            LOGGER.severe("Failed flushing port " + port.getSystemPortName() + ".");
            return;
        }

        LOGGER.info("Sent " + BytesFormatter.bytesToRaptorEncodingWithType(payload));
    }
}
