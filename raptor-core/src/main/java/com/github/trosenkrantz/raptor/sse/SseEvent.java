package com.github.trosenkrantz.raptor.sse;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Converts payload bytes to the text-only SSE wire format.
 *
 * <p>SSE data fields must be valid UTF-8 and cannot contain a carriage return, as that would terminate the field.
 * Line feeds are preserved by splitting the payload into multiple {@code data:} fields, which SSE clients join back together with line feeds.</p>
 */
final class SseEvent {
    private SseEvent() {
    }

    /**
     * Formats a payload as a complete SSE event, including the terminating blank line.
     *
     * @param payload bytes to send
     * @return the event encoded as UTF-8 text
     * @throws IllegalArgumentException if the payload cannot be represented as an SSE event
     */
    static String format(byte[] payload) {
        String text = decodeUtf8(payload);
        if (text.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Carriage return cannot be sent in a Server-Sent Event.");
        }

        StringBuilder event = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            event.append("data: ").append(line).append('\n');
        }
        return event.append('\n').toString();
    }

    private static String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Server-Sent Events require valid UTF-8 data.", e);
        }
    }
}
