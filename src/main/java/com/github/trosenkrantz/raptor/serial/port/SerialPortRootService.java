package com.github.trosenkrantz.raptor.serial.port;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.RootService;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SerialPortRootService implements RootService {
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
        SerialPortUtility.configureConnectivity(configuration);
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

            configuration.setString(SerialPortUtility.PARAMETER_REPLY_FILE, path);
        }
    }

    @Override
    public void run(Configuration configuration) throws Exception {
        SerialPortSendStrategy sendStrategy = configuration.requireEnum(SendStrategy.class).getStrategy();
        SerialPortUtility.connectAndStartSendingAndReceiving(configuration, sendStrategy);
    }
}
