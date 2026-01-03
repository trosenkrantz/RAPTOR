package com.github.trosenkrantz.raptor.tcp;

import com.github.trosenkrantz.raptor.*;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.io.*;

public class TcpRootService implements RootService {
    @Override
    public String getPromptValue() {
        return "t";
    }

    @Override
    public String getParameterKey() {
        return "tcp";
    }

    @Override
    public String getDescription() {
        return "TCP";
    }

    @Override
    public void configure(Configuration configuration) throws Exception {
        TcpUtility.configureConnectivity(configuration);
        configureSendStrategy(configuration);
    }

    private static void configureSendStrategy(Configuration configuration) throws IOException {
        SendStrategy sendStrategy = ConsoleIo.askForOptions("What data to send to the remote system", SendStrategy.class);
        configuration.setEnum(sendStrategy);

        if (sendStrategy.equals(SendStrategy.AUTO_REPLY)) {
            StateMachineConfiguration.configureSampleAutoReply(configuration, StateMachineConfiguration.REPLIES_PATH);
        }
    }

    @Override
    public void run(Configuration configuration) throws Exception {
        TcpSendStrategy sendStrategy = configuration.requireEnum(SendStrategy.class).getStrategy();
        sendStrategy.load(configuration);

        TcpUtility.connectAndStartSendingAndReceiving(configuration, sendStrategy);
    }
}
