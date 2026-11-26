package com.github.trosenkrantz.raptor.replay;

import com.github.trosenkrantz.raptor.logging.CaptureDirection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

class ReplayTimelineTest {
    @TempDir
    Path tempDir;

    @Test
    void trimsLeadingSilenceAndPreservesGaps() throws IOException {
        Path log = writeLog(
                "2026-01-02T03:04:05.000 [INFO   ] Sent \"ignored\"",
                "2026-01-02T03:04:07.000 [INFO   ] Received \"first\"",
                "2026-01-02T03:04:09.500 [INFO   ] Received \"second\"",
                "2026-01-02T03:04:10.000 [INFO   ] Sent \"ignored again\""
        );

        ReplayTimeline timeline = ReplayTimeline.fromLogFile(log, CaptureDirection.RECEIVED, 1.0);

        Assertions.assertEquals(2, timeline.size());
        List<ReplayTimeline.Entry> entries = timeline.entries();
        Assertions.assertEquals(Duration.ZERO, entries.get(0).delay());
        Assertions.assertEquals("first", new String(entries.get(0).event().data(), StandardCharsets.UTF_8));
        Assertions.assertEquals(Duration.ofMillis(2500), entries.get(1).delay());
        Assertions.assertEquals("second", new String(entries.get(1).event().data(), StandardCharsets.UTF_8));
    }

    @Test
    void appliesSpeedFactor() throws IOException {
        Path log = writeLog(
                "2026-01-02T03:04:05.000 [INFO   ] Received \"first\"",
                "2026-01-02T03:04:07.000 [INFO   ] Received \"second\""
        );

        ReplayTimeline timeline = ReplayTimeline.fromLogFile(log, CaptureDirection.RECEIVED, 2.0);

        Assertions.assertEquals(Duration.ofSeconds(1), timeline.entries().get(1).delay());
    }

    @Test
    void sortsEventsByTimestamp() throws IOException {
        Path log = writeLog(
                "2026-01-02T03:04:09.000 [INFO   ] Received \"second\"",
                "2026-01-02T03:04:07.000 [INFO   ] Received \"first\""
        );

        ReplayTimeline timeline = ReplayTimeline.fromLogFile(log, CaptureDirection.RECEIVED, 1.0);

        Assertions.assertEquals("first", new String(timeline.entries().get(0).event().data(), StandardCharsets.UTF_8));
        Assertions.assertEquals("second", new String(timeline.entries().get(1).event().data(), StandardCharsets.UTF_8));
    }

    @Test
    void rejectsLogWithoutEventsInDirection() throws IOException {
        Path log = writeLog("2026-01-02T03:04:05.000 [INFO   ] Sent \"only sent\"");

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> ReplayTimeline.fromLogFile(log, CaptureDirection.RECEIVED, 1.0)
        );
    }

    private Path writeLog(String... lines) throws IOException {
        Path log = tempDir.resolve("capture.log");
        Files.writeString(log, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
        return log;
    }
}
