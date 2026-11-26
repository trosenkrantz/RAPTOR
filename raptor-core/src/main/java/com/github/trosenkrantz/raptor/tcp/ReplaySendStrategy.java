package com.github.trosenkrantz.raptor.tcp;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.logging.CaptureLogger;
import com.github.trosenkrantz.raptor.replay.LoadedReplay;
import com.github.trosenkrantz.raptor.replay.ReplaySession;
import com.github.trosenkrantz.raptor.replay.ReplaySettings;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.function.Consumer;

class ReplaySendStrategy implements TcpSendStrategy {
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
    public Consumer<byte[]> start(Socket socket, Runnable shutDownAction) throws IOException {
        OutputStream out = socket.getOutputStream();
        new ReplaySession(replay.timeline(), replay.mode(), () -> !socket.isClosed()).startInBackground(event -> {
            byte[] data = event.data();
            out.write(data);
            out.flush();
            CaptureLogger.sent(data);
        });
        return input -> {
            // Replay ignores incoming data.
        };
    }
}
