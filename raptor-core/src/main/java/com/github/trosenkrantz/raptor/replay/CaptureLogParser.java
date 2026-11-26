package com.github.trosenkrantz.raptor.replay;

import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.logging.CaptureDirection;
import com.github.trosenkrantz.raptor.logging.CaptureLogger;
import com.github.trosenkrantz.raptor.logging.FileFormatter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses the reversible payload lines written by {@link CaptureLogger}. */
public final class CaptureLogParser {
    private static final Pattern PREFIX = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}) \\[[A-Z ]+\\] (.+)$");
    private static final Pattern METADATA_KEY = Pattern.compile("[A-Za-z][A-Za-z0-9_.-]*");

    private CaptureLogParser() {
    }

    /** Returns the captured event when the line has the expected format, otherwise empty. */
    public static Optional<CapturedEvent> parseLine(String line) {
        Matcher prefix = PREFIX.matcher(line);
        if (!prefix.matches()) return Optional.empty();

        LocalDateTime timestamp;
        try {
            timestamp = LocalDateTime.parse(prefix.group(1), FileFormatter.TIMESTAMP_FORMATTER);
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }

        String message = prefix.group(2);
        Optional<CaptureDirection> direction = readDirection(message);
        if (direction.isEmpty()) return Optional.empty();
        CaptureDirection capturedDirection = direction.get();
        message = message.substring(capturedDirection.getLogWord().length()).stripLeading();

        Map<String, String> metadata = new LinkedHashMap<>();
        if (message.startsWith("[")) {
            int metadataEnd = findMatchingBracket(message);
            if (metadataEnd < 0 || !parseMetadata(message.substring(1, metadataEnd), metadata)) return Optional.empty();
            message = message.substring(metadataEnd + 1).stripLeading();
        }

        Optional<String> encodedData = readQuotedString(message);
        if (encodedData.isEmpty()) return Optional.empty();

        byte[] data = BytesFormatter.raptorEncodingToBytesLiteral(encodedData.get());
        return Optional.of(new CapturedEvent(timestamp, capturedDirection, data, metadata));
    }

    private static Optional<CaptureDirection> readDirection(String message) {
        for (CaptureDirection direction : CaptureDirection.values()) {
            String word = direction.getLogWord();
            if (message.startsWith(word + " ")) return Optional.of(direction);
        }
        return Optional.empty();
    }

    private static int findMatchingBracket(String message) {
        int depth = 0;
        boolean inQuote = false;
        boolean escaped = false;

        for (int i = 0; i < message.length(); i++) {
            char current = message.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inQuote = !inQuote;
                continue;
            }
            if (inQuote) continue;

            if (current == '[') {
                depth++;
            } else if (current == ']') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static boolean parseMetadata(String text, Map<String, String> metadata) {
        for (String entry : splitOutsideQuotes(text, ',')) {
            int equals = entry.indexOf('=');
            if (equals < 0) return false;

            String key = entry.substring(0, equals).strip();
            if (!METADATA_KEY.matcher(key).matches() || metadata.containsKey(key)) return false;

            Optional<String> encodedValue = readQuotedString(entry.substring(equals + 1).strip());
            if (encodedValue.isEmpty()) return false;

            String value = new String(BytesFormatter.raptorEncodingToBytesLiteral(encodedValue.get()), StandardCharsets.UTF_8);
            metadata.put(key, value);
        }
        return true;
    }

    private static List<String> splitOutsideQuotes(String text, char delimiter) {
        List<String> parts = new java.util.ArrayList<>();
        int start = 0;
        boolean inQuote = false;
        boolean escaped = false;

        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inQuote = !inQuote;
                continue;
            }
            if (current == delimiter && !inQuote) {
                parts.add(text.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(text.substring(start));
        return parts;
    }

    private static Optional<String> readQuotedString(String text) {
        if (!text.startsWith("\"")) return Optional.empty();

        boolean escaped = false;
        for (int i = 1; i < text.length(); i++) {
            char current = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                if (!text.substring(i + 1).isBlank()) return Optional.empty();
                return Optional.of(text.substring(1, i));
            }
        }
        return Optional.empty();
    }
}
