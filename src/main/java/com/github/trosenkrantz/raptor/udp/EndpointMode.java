package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.PromptEnum;

public enum EndpointMode implements PromptEnum {
    MULTICAST("m", "Multicast"),
    BROADCAST("b", "Broadcast");

    private final String promptValue;
    private final String description;

    EndpointMode(String promptValue, String description) {
        this.promptValue = promptValue;
        this.description = description;
    }

    @Override
    public String getPromptValue() {
        return this.promptValue;
    }

    @Override
    public String getDescription() {
        return this.description;
    }
}
