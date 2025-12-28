package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.PromptEnum;

public enum SaveConfigurationOptions implements PromptEnum {
    SAVE("s", "[S]ave configuration to run it later"),
    SAVE_AND_OPEN("o", "Save configuration and [o]pen it in a text editor to edit it");

    private final String promptValue;
    private final String description;

    SaveConfigurationOptions(String promptValue, String description) {
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
