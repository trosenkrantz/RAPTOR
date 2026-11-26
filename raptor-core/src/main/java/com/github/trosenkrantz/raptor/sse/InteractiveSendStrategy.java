package com.github.trosenkrantz.raptor.sse;

import com.github.trosenkrantz.raptor.UserAbortedException;
import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.io.Ansi;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.CommandSubstitutor;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import com.github.trosenkrantz.raptor.logging.CaptureLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
        this.commandSubstitutionTimeout = CommandSubstitutor.TIMEOUT_SETTING.readAndRequireOrDefault(configuration);
    }

    @Override
    public void start(SseServer server, Runnable shutDownAction) {
        Thread.ofVirtual().start(() -> {
            try {
                while (true) {
                    String dataAnswer = ConsoleIo.askForString("Event data to send", BytesFormatter.DEFAULT_FULLY_ESCAPED_STRING);
                    String idAnswer = ConsoleIo.askForString("Event id to send (type enter for absent)");
                    String eventAnswer = ConsoleIo.askForString("Event event to send (type enter for absent)");
                    byte[] data = BytesFormatter.raptorEncodingToBytes(dataAnswer, commandSubstitutionTimeout);
                    Optional<String> id = decodeOptionalField("id", idAnswer);
                    Optional<String> event = decodeOptionalField("event", eventAnswer);
                    try {
                        server.send(SseEvent.format(data, id, event));
                    } catch (IllegalArgumentException e) {
                        ConsoleIo.writeLine(e.getMessage(), Ansi.ERROR);
                        continue; // Reprompt instead of sending an invalid event
                    }
                    Map<String, String> metadata = new HashMap<>();
                    id.ifPresent(value -> metadata.put("id", value));
                    event.ifPresent(value -> metadata.put("event", value));
                    CaptureLogger.sent(data, metadata);
                }
            } catch (UserAbortedException ignore) {
                shutDownAction.run();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error occurred.", e);
                shutDownAction.run();
            }
        });
    }

    private Optional<String> decodeOptionalField(String name, String answer) {
        if (answer.isEmpty()) return Optional.empty();
        return Optional.of(SseEvent.decodeField(name, BytesFormatter.raptorEncodingToBytes(answer, commandSubstitutionTimeout)));
    }
}
