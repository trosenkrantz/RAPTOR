package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.PromptOption;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Role {
    GET_REQUEST("g", "Manager / client sending [g]et request to an agent"),
    GET_RESPOND("r", "Agent / server [r]esonding to a get requests from managers"),
    SUBSCRIBE("l", "Manager / client [s]ubscribing to traps from an agent"),
    TRAP("t", "Agent / server sending periodic traps to managers");

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
