package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.PromptEnum;

public enum Role implements PromptEnum {
    SEND("s", "Send"),
    RECEIVE("r", "Receive");

    private final String promptValue;
    private final String description;

    Role(String promptValue, String description) {
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
