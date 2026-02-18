package com.github.trosenkrantz.raptor.serial.port;

import com.fazecast.jSerialComm.SerialPort;
import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.configuration.EnumSetting;
import com.github.trosenkrantz.raptor.configuration.IntegerSetting;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.CommandSubstitutor;
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

    private static final Logger LOGGER = Logger.getLogger(SerialPortUtility.class.getName());

    private static final String DEFAULT_PORT = "COM1";
    private static final int DEFAULT_BAUD_RATE = 9600;

    private static final IntegerSetting DATA_BITS_SETTING = new IntegerSetting.Builder("d", "dataBits", "Data bits", "Data bits")
            .defaultValue(8)
            .validator(value -> {
                if (value == 5 || value == 6 || value == 7 || value == 8) {
                    return Optional.empty();
                } else {
                    return Optional.of("Data bits must be 5, 6, 7, or 8.");
                }
            }).build();
    private static final EnumSetting<StopBits> STOP_BITS_SETTING = new EnumSetting.Builder<>("s", "stopBits", "Stop bits", "Stop bits", StopBits.class)
            .defaultValue(StopBits.ONE).build();
    private static final EnumSetting<Parity> PARITY_SETTING = new EnumSetting.Builder<>("p", "parity", "Parity", "Parity", Parity.class)
            .defaultValue(Parity.NO).build();

    public static void configureConnectivity(Configuration configuration) {
        // Configure port
        List<String> portNames = Arrays.stream(SerialPort.getCommPorts())
                .map(SerialPort::getSystemPortName)
                .sorted()
                .toList();
        LOGGER.info("Available ports on this machine: " + String.join(", ", portNames));
        String defaultPort = portNames.isEmpty() ? DEFAULT_PORT : portNames.getFirst();
        configuration.setRaptorEncodedString(PARAMETER_PORT, ConsoleIo.askForString("port name", defaultPort));

        configuration.setInt(PARAMETER_BAUD_RATE, ConsoleIo.askForInt("Baud rate", DEFAULT_BAUD_RATE));

        ConsoleIo.configureAdvancedSettings(List.of(
                DATA_BITS_SETTING,
                STOP_BITS_SETTING,
                PARITY_SETTING,
                CommandSubstitutor.TIMEOUT_SETTING
        ), configuration);
    }

    /**
     * This call is blocking.
     *
     * @param configuration configuration
     * @param sendStrategy  controls what to do when serial port receives data and what and when to send data over the serial port
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted
     */
    public static void connectAndStartSendingAndReceiving(Configuration configuration, SerialPortSendStrategy sendStrategy) throws IOException, InterruptedException {
        String portName = configuration.requireRaptorEncodedString(PARAMETER_PORT);

        SerialPort serialPort = SerialPort.getCommPort(portName);
        serialPort.setComPortParameters(
                configuration.requireInt(PARAMETER_BAUD_RATE),
                DATA_BITS_SETTING.readAndRequireOrDefault(configuration),
                STOP_BITS_SETTING.readAndRequireOrDefault(configuration).getValue(),
                PARITY_SETTING.readAndRequireOrDefault(configuration).getValue()
        );

        int commandSubstitutionTimeout = CommandSubstitutor.TIMEOUT_SETTING.readAndRequireOrDefault(configuration);

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

        Consumer<byte[]> onReceivedData = sendStrategy.start(configuration, serialPort, shutDownLatch::countDown, commandSubstitutionTimeout);

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
