package com.github.trosenkrantz.raptor.io;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BytesFormatterTest {
    @Test
    public void quoteSpace() {
        String input = "Hello, World";

        String output = BytesFormatter.escapeCliArgument(input);
        assertEquals("\"Hello, World\"", output);

        String reversedInput = BytesFormatter.unescapeCliArgument(output);
        assertEquals(input, reversedInput);
    }

    @Test
    public void quoteMultipleSpace() {
        String input = "a b c";

        String output = BytesFormatter.escapeCliArgument(input);
        assertEquals("\"a b c\"", output);

        String reversedInput = BytesFormatter.unescapeCliArgument(output);
        assertEquals(input, reversedInput);
    }

    @Test
    public void escapedSpecialCharacters() {
        String input = "Special!@#$%^&*()";
        String output = BytesFormatter.escapeCliArgument(input);
        assertEquals("Special\\!\\@\\#\\$\\%\\^\\&\\*\\(\\)", output);

        String reversedInput = BytesFormatter.unescapeCliArgument(output);
        assertEquals(input, reversedInput);
    }

    @Test
    public void escapedSpecialCharactersAndQuote() {
        String input = "! a";
        String output = BytesFormatter.escapeCliArgument(input);
        assertEquals("\"\\! a\"", output);

        String reversedInput = BytesFormatter.unescapeCliArgument(output);
        assertEquals(input, reversedInput);
    }
}