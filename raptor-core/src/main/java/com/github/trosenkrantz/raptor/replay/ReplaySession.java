package com.github.trosenkrantz.raptor.replay;

import java.io.IOException;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Sends a timeline on a background virtual thread. */
public final class ReplaySession {
    private static final Logger LOGGER = Logger.getLogger(ReplaySession.class.getName());

    @FunctionalInterface
    public interface Sender {
        void send(CapturedEvent event) throws Exception;
    }

    private final ReplayTimeline timeline;
    private final ReplayMode mode;
    private final BooleanSupplier active;

    public ReplaySession(ReplayTimeline timeline, ReplayMode mode, BooleanSupplier active) {
        this.timeline = timeline;
        this.mode = mode;
        this.active = active;
    }

    public void startInBackground(Sender sender) {
        Thread.ofVirtual().start(() -> {
            try {
                replay(sender);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Replay stopped because the connection closed.", e);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Replay failed.", e);
            }
        });
    }

    private void replay(Sender sender) throws Exception {
        do {
            for (ReplayTimeline.Entry entry : timeline.entries()) {
                if (!active.getAsBoolean()) return;
                if (!entry.delay().isZero()) Thread.sleep(entry.delay());
                if (!active.getAsBoolean()) return;
                sender.send(entry.event());
            }
        } while (mode == ReplayMode.LOOP && active.getAsBoolean());
    }
}
