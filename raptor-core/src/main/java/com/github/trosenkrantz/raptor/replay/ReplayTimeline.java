package com.github.trosenkrantz.raptor.replay;

import com.github.trosenkrantz.raptor.logging.CaptureDirection;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Ordered replay events with delays relative to the previous event. */
public final class ReplayTimeline {
    public record Entry(Duration delay, CapturedEvent event) {
    }

    private final List<Entry> entries;

    private ReplayTimeline(List<Entry> entries) {
        this.entries = List.copyOf(entries);
    }

    public static ReplayTimeline fromLogFile(Path path, CaptureDirection direction, double speed) {
        List<CapturedEvent> events = readEvents(path, direction);
        if (events.isEmpty()) {
            throw new IllegalArgumentException("No " + direction.getConfigurationId() + " capture events found in " + path + ".");
        }

        events.sort(Comparator.comparing(CapturedEvent::timestamp));
        List<Entry> entries = new ArrayList<>();
        LocalDateTime previous = events.getFirst().timestamp();
        for (CapturedEvent event : events) {
            long delayMillis = Math.max(0, Math.round(Duration.between(previous, event.timestamp()).toMillis() / speed));
            entries.add(new Entry(Duration.ofMillis(delayMillis), event));
            previous = event.timestamp();
        }
        return new ReplayTimeline(entries);
    }

    public List<Entry> entries() {
        return entries;
    }

    public int size() {
        return entries.size();
    }

    private static List<CapturedEvent> readEvents(Path path, CaptureDirection direction) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read replay log file " + path + ".", e);
        }

        List<CapturedEvent> events = new ArrayList<>();
        for (String line : lines) {
            Optional<CapturedEvent> event = CaptureLogParser.parseLine(line);
            if (event.isPresent() && event.get().direction() == direction) events.add(event.get());
        }
        return events;
    }
}
