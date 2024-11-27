package com.github.trosenkrantz.raptor.conversion;

import com.github.trosenkrantz.raptor.PromptEnum;

public enum Action implements PromptEnum {
    ENCODING("r", "Convert [R]APTOR encoding to a file"),
    FILE("f", "Convert a [f]ile to RAPTOR encoding");

    private final String promptValue;
    private final String description;

    Action(String promptValue, String description) {
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
