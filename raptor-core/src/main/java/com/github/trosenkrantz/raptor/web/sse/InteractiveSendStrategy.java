package com.github.trosenkrantz.raptor.web.sse;

import com.github.trosenkrantz.raptor.UserAbortedException;
import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.io.Ansi;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.CommandSubstitutor;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.util.logging.Level;
import java.util.logging.Logger;

class InteractiveSendStrategy implements SseSendStrategy {
    private static final Logger LOGGER = Logger.getLogger(InteractiveSendStrategy.class.getName());

    private int commandSubstitutionTimeout;

    @Override
    public void configure(Configuration configuration) {
        CommandSubstitutor.TIMEOUT_SETTING.configure(configuration);
    }

    @Override
    public void load(Configuration configuration) {
        this.commandSubstitutionTimeout =
                CommandSubstitutor.TIMEOUT_SETTING.readAndRequireOrDefault(configuration);
    }

    @Override
    public void start(SseServer server, Runnable shutDownAction) {
        Thread.ofVirtual().start(() -> {
            try {
                while (true) {
                    String answer = ConsoleIo.askForString(
                            "Event data to send", BytesFormatter.DEFAULT_FULLY_ESCAPED_STRING);
                    byte[] payload = BytesFormatter.raptorEncodingToBytes(answer, commandSubstitutionTimeout);
                    try {
                        server.send(SseEvent.format(payload));
                    } catch (IllegalArgumentException e) {
                        ConsoleIo.writeLine(e.getMessage(), Ansi.ERROR);
                        continue; // Reprompt instead of sending an invalid event
                    }
                    LOGGER.info("Sent " + BytesFormatter.bytesToRaptorEncodingWithType(payload));
                }
            } catch (UserAbortedException ignore) {
                shutDownAction.run();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error occurred.", e);
                shutDownAction.run();
            }
        });
    }
}
