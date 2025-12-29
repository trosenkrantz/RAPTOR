package com.github.trosenkrantz.raptor.auto.reply;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.trosenkrantz.raptor.io.JsonUtility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record StateMachineConfiguration(String startState, Map<String, List<Transition>> states) {

    public static StateMachineConfiguration readFromFile(final String path) throws IOException {
        return new ObjectMapper().readValue(
                JsonUtility.removeComments(Files.readString(Path.of(path))), // TODO When moving to main JSON, remove comments at main root instead
                StateMachineConfiguration.class
        );
    }
}
