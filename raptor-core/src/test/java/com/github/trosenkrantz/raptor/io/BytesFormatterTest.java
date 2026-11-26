package com.github.trosenkrantz.raptor.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Random;

class BytesFormatterTest {
    public static final int COMMAND_SUBSTITUTION_TIMEOUT = 1000;

    @Test
    public void decodeEscapedSlash() {
        String input = "abc\\\\";
        byte[] decoded = BytesFormatter.raptorEncodingToBytes(input, COMMAND_SUBSTITUTION_TIMEOUT);
        Assertions.assertArrayEquals(new byte[]{97, 98, 99, 92}, decoded);

        String roundTrip = BytesFormatter.bytesToRaptorEncoding(decoded);
        Assertions.assertEquals(input, roundTrip);
    }

    @Test
    public void decodeEscapedQuote() {
        String input = "abc\\\"";
        byte[] decoded = BytesFormatter.raptorEncodingToBytes(input, COMMAND_SUBSTITUTION_TIMEOUT);
        Assertions.assertArrayEquals(new byte[]{97, 98, 99, 34}, decoded);

        String roundTrip = BytesFormatter.bytesToRaptorEncoding(decoded);
        Assertions.assertEquals(input, roundTrip);
    }

    @Test
    public void decodeWhiteSpace() {
        String input = "abc\\n\\r\\t";
        byte[] decoded = BytesFormatter.raptorEncodingToBytes(input, COMMAND_SUBSTITUTION_TIMEOUT);
        Assertions.assertArrayEquals(new byte[]{97, 98, 99, 10, 13, 9}, decoded);

        String roundTrip = BytesFormatter.bytesToRaptorEncoding(decoded);
        Assertions.assertEquals(input, roundTrip);
    }

    @Test
    public void encodeBytesMatchingHexEscapeString() {
        byte[] input = "\\x00".getBytes(StandardCharsets.US_ASCII); // Four bytes that together hit an edge case
        String encoded = BytesFormatter.bytesToRaptorEncoding(input); // A naive implementation would encode as five characters, \\x00
        byte[] roundTrip = BytesFormatter.raptorEncodingToBytes(encoded, COMMAND_SUBSTITUTION_TIMEOUT); // RAPTOR would decode that to a single 0 byte, breaking round-trip
        Assertions.assertArrayEquals(input, roundTrip);
    }

    @Test
    public void encodeBytesMatchingHexEscapeStringPrefixedWithBackslash() {
        byte[] input = "\\\\x00".getBytes(StandardCharsets.US_ASCII);
        String encoded = BytesFormatter.bytesToRaptorEncoding(input);
        byte[] roundTrip = BytesFormatter.raptorEncodingToBytes(encoded, COMMAND_SUBSTITUTION_TIMEOUT);
        Assertions.assertArrayEquals(input, roundTrip);
    }

    @Test
    public void encodeBytesMatchingHexEscapeStringPrefixedWithTwoBackslashes() {
        byte[] input = "\\\\\\x00".getBytes(StandardCharsets.US_ASCII);
        String encoded = BytesFormatter.bytesToRaptorEncoding(input);
        byte[] roundTrip = BytesFormatter.raptorEncodingToBytes(encoded, COMMAND_SUBSTITUTION_TIMEOUT);
        Assertions.assertArrayEquals(input, roundTrip);
    }

    @Test
    public void literalDecoderRoundTripsText() {
        byte[] input = "Hello, World!\n\"quoted\"\\".getBytes(StandardCharsets.UTF_8);
        String encoded = BytesFormatter.bytesToRaptorEncoding(input);

        Assertions.assertArrayEquals(input, BytesFormatter.raptorEncodingToBytesLiteral(encoded));
    }

    @Test
    public void literalDecoderRoundTripsArbitraryBytes() {
        byte[] input = new byte[]{0, 1, 2, 0x7f, (byte) 0x80, (byte) 0xff};
        String encoded = BytesFormatter.bytesToRaptorEncoding(input);

        Assertions.assertArrayEquals(input, BytesFormatter.raptorEncodingToBytesLiteral(encoded));
    }

    @Test
    public void literalDecoderDoesNotExecuteCommandSubstitution() {
        byte[] decoded = BytesFormatter.raptorEncodingToBytesLiteral("\\$(echo hi)");

        Assertions.assertArrayEquals("\\$(echo hi)".getBytes(StandardCharsets.UTF_8), decoded);
    }
}