package com.github.trosenkrantz.raptor.sse;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.logging.CaptureLogger;
import com.github.trosenkrantz.raptor.replay.LoadedReplay;
import com.github.trosenkrantz.raptor.replay.ReplaySession;
import com.github.trosenkrantz.raptor.replay.ReplaySettings;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class ReplaySendStrategy implements SseSendStrategy {
    private LoadedReplay replay;

    @Override
    public void configure(Configuration configuration) {
        ReplaySettings.configure(configuration);
    }

    @Override
    public void load(Configuration configuration) {
        replay = ReplaySettings.load(configuration);
    }

    @Override
    public void start(SseServer server, Runnable shutDownAction) {
        server.onSubscriberConnected(subscriber -> new ReplaySession(replay.timeline(), replay.mode(), () -> !subscriber.isClosed()).startInBackground(event -> {
            byte[] data = event.data();
            Optional<String> id = Optional.ofNullable(event.metadata().get("id"));
            Optional<String> name = Optional.ofNullable(event.metadata().get("event"));
            String formatted = SseEvent.format(data, id, name);
            subscriber.send(formatted.getBytes(StandardCharsets.UTF_8));
            Map<String, String> metadata = new HashMap<>();
            id.ifPresent(value -> metadata.put("id", value));
            name.ifPresent(value -> metadata.put("event", value));
            CaptureLogger.sent(data, metadata);
        }));
    }
}
