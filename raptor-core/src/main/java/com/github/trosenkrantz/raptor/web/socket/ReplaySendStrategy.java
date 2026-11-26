package com.github.trosenkrantz.raptor.web.socket;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.replay.LoadedReplay;
import com.github.trosenkrantz.raptor.replay.ReplaySession;
import com.github.trosenkrantz.raptor.replay.ReplaySettings;
import org.java_websocket.WebSocket;

import java.util.Map;
import java.util.function.Consumer;

class ReplaySendStrategy implements WebSocketSendStrategy {
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
    public Consumer<byte[]> initialise(WebSocket socket, Runnable shutDownAction) {
        new ReplaySession(replay.timeline(), replay.mode(), socket::isOpen).startInBackground(event -> {
            byte[] data = event.data();
            String frame = event.metadata().get("frame");
            boolean text = frame != null ? frame.equals("text") : BytesFormatter.isText(data);
            WebSocketService.sendFrame(socket, data, text, Map.of());
        });
        return input -> {
            // Replay ignores incoming data.
        };
    }
}
