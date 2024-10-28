package com.github.trosenkrantz.raptor.auto.reply;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public record StateMachineConfiguration(String startState, Map<String, List<Transition>> states) {
    private static final Pattern COMMENTS_PATTERN = Pattern.compile("(//.*?$)|(/\\*.*?\\*/)", Pattern.DOTALL | Pattern.MULTILINE);

    public static StateMachineConfiguration readFromFile(final String path) throws IOException {
        return new ObjectMapper().readValue(
                COMMENTS_PATTERN.matcher(
                        Files.readString(Path.of(path))
                ).replaceAll(""), // Remove comments, which are unsupported in Jackson ObjectMapper
                StateMachineConfiguration.class
        );
    }
}
