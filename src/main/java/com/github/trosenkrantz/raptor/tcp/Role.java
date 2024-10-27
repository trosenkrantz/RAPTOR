package com.github.trosenkrantz.raptor.tcp;

import com.github.trosenkrantz.raptor.PromptOption;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Role {
    CLIENT("c", "Client"),
    SERVER("s", "Server");

    private final String promptValue;
    private final String description;

    Role(String promptValue, String description) {
        this.promptValue = promptValue;
        this.description = description;
    }

    public String getPromptValue() {
        return this.promptValue;
    }

    public String getDescription() {
        return this.description;
    }

    public static List<PromptOption<Role>> getPromptOptions() {
        return Stream.of(Role.values())
                .map(value -> new PromptOption<>(value.getPromptValue(), value.getDescription(), value))
                .collect(Collectors.toList());
    }
}
