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
            StateMachineConfiguration.configureSampleAutoReply(configuration, StateMachineConfiguration.REPLIES_PATH);
        }
    }

    @Override
    public void run(Configuration configuration) throws Exception {
        SerialPortSendStrategy sendStrategy = configuration.requireEnum(SendStrategy.class).getStrategy();
        SerialPortUtility.connectAndStartSendingAndReceiving(configuration, sendStrategy);
    }
}
