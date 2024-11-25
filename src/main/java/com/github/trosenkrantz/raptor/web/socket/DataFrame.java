package com.github.trosenkrantz.raptor.web.socket;

import com.github.trosenkrantz.raptor.PromptEnum;

public enum DataFrame implements PromptEnum {
    TEXT("t", "Text"),
    BINARY("b", "Binary");

    private final String promptValue;
    private final String description;

    DataFrame(String promptValue, String description) {
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
