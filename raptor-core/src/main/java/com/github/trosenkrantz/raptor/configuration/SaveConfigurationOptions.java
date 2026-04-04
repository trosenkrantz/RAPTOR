package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.PromptEnum;

public enum SaveConfigurationOptions implements PromptEnum {
    RUN("r", "[R]un configuration without saving it"),
    SAVE_AND_OPEN("s", "[S]ave and open configuration to edit it");

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
