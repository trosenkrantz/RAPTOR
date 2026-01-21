package com.github.trosenkrantz.raptor.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BytesFormatterTest {
    @Test
    public void quoteSpace() {
        String input = "Hello, World";

        String output = BytesFormatter.escapeCliArgument(input);
        Assertions.assertEquals("\"Hello, World\"", output);

        String reversedInput = BytesFormatter.unescapeCliArgument(output);
        Assertions.assertEquals(input, reversedInput);
    }

    @Test
    public void quoteMultipleSpace() {
        String input = "a b c";

        String output = BytesFormatter.escapeCliArgument(input);
        Assertions.assertEquals("\"a b c\"", output);

        String reversedInput = BytesFormatter.unescapeCliArgument(output);
        Assertions.assertEquals(input, reversedInput);
    }

    @Test
    public void escapedSpecialCharacters() {
        String input = "Special!@#$%^&*()";
        String output = BytesFormatter.escapeCliArgument(input);
        Assertions.assertEquals("Special\\!\\@\\#\\$\\%\\^\\&\\*\\(\\)", output);

        String reversedInput = BytesFormatter.unescapeCliArgument(output);
        Assertions.assertEquals(input, reversedInput);
    }

    @Test
    public void escapedSpecialCharactersAndQuote() {
        String input = "! a";
        String output = BytesFormatter.escapeCliArgument(input);
        Assertions.assertEquals("\"\\! a\"", output);

        String reversedInput = BytesFormatter.unescapeCliArgument(output);
        Assertions.assertEquals(input, reversedInput);
    }

    @Test
    public void canEncodeAndDecodeEachByte() {
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            byte[] input = new byte[]{(byte) i};
            String encoded = BytesFormatter.bytesToFullyEscapedString(input);
            byte[] output = BytesFormatter.fullyEscapedStringToBytes(encoded);
            Assertions.assertArrayEquals(input, output, "Failed for value " + i + ".");
        }
    }
}