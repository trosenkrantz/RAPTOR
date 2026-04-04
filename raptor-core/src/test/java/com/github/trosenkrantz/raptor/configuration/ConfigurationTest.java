package com.github.trosenkrantz.raptor.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class ConfigurationTest {
    @Test
    void toJsonFormattingWithNestedObjects() {
        // Arrange
        Configuration config = Configuration.empty();
        config.setRaptorEncodedString("service", "tcp");

        Configuration replies = Configuration.empty();
        replies.setRaptorEncodedString("startState", "unauthenticated");
        config.setSubConfiguration("replies", replies);

        // Act
        String jsonOutput = config.toJson();

        // Assert
        String expected = """
                {
                  "service": "tcp",
                  "replies": {
                    "startState": "unauthenticated"
                  }
                }""".replace("\n", System.lineSeparator());

        Assertions.assertEquals(expected, jsonOutput);
    }

    @Test
    void toJsonFormattingWithArrays() {
        // Arrange
        Configuration config = Configuration.empty();

        Configuration item1 = Configuration.empty();
        item1.setRaptorEncodedString("input", "a");
        item1.setRaptorEncodedString("output", "b");

        Configuration item2 = Configuration.empty();
        item2.setRaptorEncodedString("input", "c");
        item2.setRaptorEncodedString("output", "d");

        config.setSubConfigurationArray("handlers", List.of(item1, item2));

        // Act
        String jsonOutput = config.toJson();

        // Assert
        String expected = """
                {
                  "handlers": [
                    {
                      "input": "a",
                      "output": "b"
                    },
                    {
                      "input": "c",
                      "output": "d"
                    }
                  ]
                }""".replace("\n", System.lineSeparator());

        Assertions.assertEquals(expected, jsonOutput);
    }

    @Test
    void emptyObjectFormatting() {
        // Arrange
        Configuration config = Configuration.empty();
        config.setSubConfiguration("emptyObj", Configuration.empty());

        // Act
        String jsonOutput = config.toJson();

        // Assert - Verifying the "{ }" spacing for empty objects
        String expected = """
                {
                  "emptyObj": { }
                }""".replace("\n", System.lineSeparator());

        Assertions.assertEquals(expected, jsonOutput);
    }
}