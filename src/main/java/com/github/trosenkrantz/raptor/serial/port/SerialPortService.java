package com.github.trosenkrantz.raptor.serial.port;

import com.fazecast.jSerialComm.SerialPort;
import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.RaptorService;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class SerialPortService implements RaptorService {
    private static final Logger LOGGER = Logger.getLogger(SerialPortService.class.getName());

    private static final String DEFAULT_PORT = "COM1";
    private static final int DEFAULT_BAUD_RATE = 9600;
    private static final int DEFAULT_DATA_BITS = 8;
    private static final StopBits DEFAULT_STOP_BITS = StopBits.ONE;
    private static final Parity DEFAULT_PARITY = Parity.NO;

    public static final String PARAMETER_PORT = "port";
    public static final String PARAMETER_BAUD_RATE = "baud-rate";
    public static final String PARAMETER_REPLY_FILE = "reply-file";
    public static final String PARAMETER_DATA_BITS = "data-bits";

    @Override
    public String getPromptValue() {
        return "sp";
    }

    @Override
    public String getParameterKey() {
        return "serial-port";
    }

    @Override
    public String getDescription() {
        return "Serial Port";
    }

    @Override
    public void configure(Configuration configuration) throws Exception {
        // Configure port
        List<String> portNames = Arrays.stream(SerialPort.getCommPorts()).map(SerialPort::getSystemPortName).toList();
        LOGGER.info("Available ports on this machine: " + String.join(", ", portNames));
        String defaultPort = portNames.isEmpty() ? DEFAULT_PORT : portNames.getFirst();
        configuration.setString(PARAMETER_PORT, ConsoleIo.askForString("port name", defaultPort));

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

        configureSendStrategy(configuration);
    }

    private static void configureSendStrategy(Configuration configuration) throws IOException {
        ConsoleIo.write("What data to send to the remote system? ");
        SendStrategy sendStrategy = ConsoleIo.askForOptions(SendStrategy.class);
        configuration.setEnum(sendStrategy);

        if (sendStrategy.equals(SendStrategy.AUTO_REPLY)) {
            String path = ConsoleIo.askForFile("Absolute or relative file path", "." + File.separator + "replies.json");

            // Load state machine immediately to provide early feedback
            StateMachineConfiguration stateMachine = StateMachineConfiguration.readFromFile(path);
            ConsoleIo.writeLine("Parsed file with " + stateMachine.states().size() + " states and " + stateMachine.states().values().stream().map(List::size).reduce(0, Integer::sum) + " transitions.");

            configuration.setString(PARAMETER_REPLY_FILE, path);
        }
    }

    @Override
    public void run(Configuration configuration) throws Exception {
        String portName = configuration.requireString(PARAMETER_PORT);

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

        Consumer<byte[]> onReceivedData = configuration.requireEnum(SendStrategy.class).getStrategy().start(configuration, serialPort, shutDownLatch::countDown);

        LOGGER.info("Listing to " + portName + "...");
        // We can only have a single listener according to the JDoc
        serialPort.addDataListener(new MySerialPortDataListener(onReceivedData));

        shutDownLatch.await();
        try {
            serialPort.closePort();
            LOGGER.info("Closed port " + portName + ".");
        } catch (Exception e) {
            LOGGER.severe("Failed to close port " + portName + ".");
            ConsoleIo.writeException(e);
        }
    }
}
