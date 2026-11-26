package com.github.trosenkrantz.raptor.logging;

import com.github.trosenkrantz.raptor.io.BytesFormatter;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Logs one reversible payload line per data exchange. The same line is human-readable and replayable.
 *
 * <p>Format: {@code Received [key="value",...] "raptor-encoded-data"}. Metadata is optional and sorted by key.</p>
 */
public final class CaptureLogger {
    private static final Logger LOGGER = Logger.getLogger(CaptureLogger.class.getName());

    private CaptureLogger() {
    }

    public static void received(byte[] data) {
        log(CaptureDirection.RECEIVED, data, Map.of());
    }

    public static void received(byte[] data, Map<String, String> metadata) {
        log(CaptureDirection.RECEIVED, data, metadata);
    }

    public static void sent(byte[] data) {
        log(CaptureDirection.SENT, data, Map.of());
    }

    public static void sent(byte[] data, Map<String, String> metadata) {
        log(CaptureDirection.SENT, data, metadata);
    }

    private static void log(CaptureDirection direction, byte[] data, Map<String, String> metadata) {
        LOGGER.info(format(direction, data, metadata));
    }

    /** Formats a capture message without the timestamp and log level prefix. */
    public static String format(CaptureDirection direction, byte[] data, Map<String, String> metadata) {
        StringBuilder message = new StringBuilder(direction.getLogWord());
        Map<String, String> sortedMetadata = new TreeMap<>(metadata);
        if (!sortedMetadata.isEmpty()) {
            message.append(" [");
            boolean first = true;
            for (Map.Entry<String, String> entry : sortedMetadata.entrySet()) {
                if (!first) message.append(',');
                first = false;
                message.append(validateKey(entry.getKey()))
                        .append("=\"")
                        .append(encodeMetadataValue(entry.getValue()))
                        .append('"');
            }
            message.append(']');
        }

        return message.append(" \"")
                .append(BytesFormatter.bytesToRaptorEncoding(data))
                .append('"')
                .toString();
    }

    private static String validateKey(String key) {
        if (key == null || !key.matches("[A-Za-z][A-Za-z0-9_.-]*")) {
            throw new IllegalArgumentException("Invalid capture metadata key: " + key);
        }
        return key;
    }

    private static String encodeMetadataValue(String value) {
        if (value == null) throw new IllegalArgumentException("Capture metadata values cannot be null.");
        return BytesFormatter.bytesToRaptorEncoding(value.getBytes(StandardCharsets.UTF_8));
    }
}
