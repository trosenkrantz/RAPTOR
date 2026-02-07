package com.github.trosenkrantz.raptor.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Random;

class BytesFormatterTest {
    @Test
    public void encodeEachByte() {
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            byte[] input = new byte[]{(byte) i};
            String encoded = BytesFormatter.bytesToFullyEscapedString(input);
            byte[] roundTrip = BytesFormatter.fullyEscapedStringToBytes(encoded);
            Assertions.assertArrayEquals(input, roundTrip, "Failed for value " + i + ", encoded as " + encoded + ".");
        }
    }

    @Test
    public void decodeWordsWithSpace() {
        String input = "Hello, World";

        byte[] decoded = BytesFormatter.fullyEscapedStringToBytes(input);
        Assertions.assertArrayEquals(input.getBytes(StandardCharsets.US_ASCII), decoded); // Expect input as bytes one-to-one

        String roundTrip = BytesFormatter.bytesToFullyEscapedString(decoded);
        Assertions.assertEquals(input, roundTrip);
    }

    @Test
    public void decodeWordsWithMultipleSpaces() {
        String input = "a b c";

        byte[] decoded = BytesFormatter.fullyEscapedStringToBytes(input);
        Assertions.assertArrayEquals(input.getBytes(StandardCharsets.US_ASCII), decoded); // Expect input as bytes one-to-one

        String roundTrip = BytesFormatter.bytesToFullyEscapedString(decoded);
        Assertions.assertEquals(input, roundTrip);
    }

    @Test
    public void decodeSpecialCharacters() {
        String input = "Special!@#$%^&*()"; // But no slash or quote
        byte[] decoded = BytesFormatter.fullyEscapedStringToBytes(input);
        Assertions.assertArrayEquals(input.getBytes(StandardCharsets.US_ASCII), decoded); // Expect input as bytes one-to-one

        String roundTrip = BytesFormatter.bytesToFullyEscapedString(decoded);
        Assertions.assertEquals(input, roundTrip);
    }

    @Test
    public void decodeEscapedSlash() {
        String input = "abc\\\\";
        byte[] decoded = BytesFormatter.fullyEscapedStringToBytes(input);
        Assertions.assertArrayEquals(new byte[]{97, 98, 99, 92}, decoded);

        String roundTrip = BytesFormatter.bytesToFullyEscapedString(decoded);
        Assertions.assertEquals(input, roundTrip);
    }

    @Test
    public void decodeEscapedQuote() {
        String input = "abc\\\"";
        byte[] decoded = BytesFormatter.fullyEscapedStringToBytes(input);
        Assertions.assertArrayEquals(new byte[]{97, 98, 99, 34}, decoded);

        String roundTrip = BytesFormatter.bytesToFullyEscapedString(decoded);
        Assertions.assertEquals(input, roundTrip);
    }

    @Test
    public void decodeWhiteSpace() {
        String input = "abc\\n\\r\\t";
        byte[] decoded = BytesFormatter.fullyEscapedStringToBytes(input);
        Assertions.assertArrayEquals(new byte[]{97, 98, 99, 10, 13, 9}, decoded);

        String roundTrip = BytesFormatter.bytesToFullyEscapedString(decoded);
        Assertions.assertEquals(input, roundTrip);
    }

    @Test
    public void encodeBytesMatchingHexEscapeString() {
        byte[] input = "\\x00".getBytes(StandardCharsets.US_ASCII); // Four bytes that together hit an edge case
        String encoded = BytesFormatter.bytesToFullyEscapedString(input); // A naive implementation would encode as five characters, \\x00
        byte[] roundTrip = BytesFormatter.fullyEscapedStringToBytes(encoded); // RAPTOR would decode that to a single 0 byte, breaking round-trip
        Assertions.assertArrayEquals(input, roundTrip);
    }

    @Test
    public void encodeBytesMatchingHexEscapeStringPrefixedWithBackslash() {
        byte[] input = "\\\\x00".getBytes(StandardCharsets.US_ASCII);
        String encoded = BytesFormatter.bytesToFullyEscapedString(input);
        byte[] roundTrip = BytesFormatter.fullyEscapedStringToBytes(encoded);
        Assertions.assertArrayEquals(input, roundTrip);
    }

    @Test
    public void encodeBytesMatchingHexEscapeStringPrefixedWithTwoBackslashes() {
        byte[] input = "\\\\\\x00".getBytes(StandardCharsets.US_ASCII);
        String encoded = BytesFormatter.bytesToFullyEscapedString(input);
        byte[] roundTrip = BytesFormatter.fullyEscapedStringToBytes(encoded);
        Assertions.assertArrayEquals(input, roundTrip);
    }

    @Test
    public void encodeRandomBytes() {
        Random random = new Random(0);
        for (int iteration = 0; iteration < 100; iteration++) {
            byte[] input = new byte[256];
            random.nextBytes(input);

            String encoded = BytesFormatter.bytesToFullyEscapedString(input);
            byte[] roundTrip = BytesFormatter.fullyEscapedStringToBytes(encoded);

            Assertions.assertArrayEquals(input, roundTrip, "Failed at iteration " + iteration);
        }
    }
}