package com.github.trosenkrantz.raptor.serial.port;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.RootService;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

public class SerialPortRootService implements RootService {
    @Override
    public String getPromptValue() {
        return "sp";
    }

    @Override
    public String getParameterKey() {
        return "serialPort";
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

    private static void configureSendStrategy(Configuration configuration) throws Exception {
        ConsoleIo.write("What data to send to the remote system? ");
        SendStrategy sendStrategy = ConsoleIo.askForOptions(SendStrategy.class);
        configuration.setEnum(sendStrategy);

        if (sendStrategy.equals(SendStrategy.AUTO_REPLY)) {
            StateMachineConfiguration.configureSampleAutoReply(configuration, StateMachineConfiguration.REPLIES_PATH);
        } else {
            sendStrategy.getStrategy().configure(configuration);
        }
    }

    @Override
    public void run(Configuration configuration) throws Exception {
        SerialPortSendStrategy sendStrategy = configuration.requireEnum(SendStrategy.class).getStrategy();
        sendStrategy.load(configuration);
        SerialPortUtility.connectAndStartSendingAndReceiving(configuration, sendStrategy);
    }
}
