package com.github.trosenkrantz.raptor.replay;

import com.github.trosenkrantz.raptor.logging.CaptureDirection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class ReplaySessionTest {
    @TempDir
    Path tempDir;

    @Test
    void sendsEachEventOnce() throws Exception {
        ReplayTimeline timeline = timelineWithEventCount(2);
        CountDownLatch sent = new CountDownLatch(2);

        new ReplaySession(timeline, ReplayMode.ONCE, () -> true)
                .startInBackground(event -> sent.countDown());

        Assertions.assertTrue(sent.await(2, TimeUnit.SECONDS));
    }

    @Test
    void stopsLoopWhenInactive() throws Exception {
        ReplayTimeline timeline = timelineWithEventCount(1);
        CountDownLatch sent = new CountDownLatch(3);
        AtomicBoolean active = new AtomicBoolean(true);
        AtomicInteger count = new AtomicInteger();

        new ReplaySession(timeline, ReplayMode.LOOP, active::get).startInBackground(event -> {
            if (count.incrementAndGet() == 3) active.set(false);
            sent.countDown();
        });

        Assertions.assertTrue(sent.await(2, TimeUnit.SECONDS));
        Assertions.assertEquals(3, count.get());
    }

    private ReplayTimeline timelineWithEventCount(int count) throws IOException {
        StringBuilder log = new StringBuilder();
        for (int i = 0; i < count; i++) {
            log.append("2026-01-02T03:04:05.00")
                    .append(i)
                    .append(" [INFO   ] Received \"event-")
                    .append(i)
                    .append("\"\n");
        }

        Path logFile = tempDir.resolve("capture.log");
        Files.writeString(logFile, log.toString(), StandardCharsets.UTF_8);
        return ReplayTimeline.fromLogFile(logFile, CaptureDirection.RECEIVED, 1.0);
    }
}
