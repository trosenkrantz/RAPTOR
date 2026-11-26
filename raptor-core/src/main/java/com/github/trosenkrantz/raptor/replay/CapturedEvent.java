package com.github.trosenkrantz.raptor.replay;

import com.github.trosenkrantz.raptor.logging.CaptureDirection;

import java.time.LocalDateTime;
import java.util.Map;

/** One payload line read from a RAPTOR log file. */
public final class CapturedEvent {
    private final LocalDateTime timestamp;
    private final CaptureDirection direction;
    private final byte[] data;
    private final Map<String, String> metadata;

    public CapturedEvent(LocalDateTime timestamp, CaptureDirection direction, byte[] data, Map<String, String> metadata) {
        this.timestamp = timestamp;
        this.direction = direction;
        this.data = data;
        this.metadata = Map.copyOf(metadata);
    }

    public LocalDateTime timestamp() {
        return timestamp;
    }

    public CaptureDirection direction() {
        return direction;
    }

    public byte[] data() {
        return data;
    }

    public Map<String, String> metadata() {
        return metadata;
    }
}
