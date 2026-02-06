package com.github.trosenkrantz.raptor.conversion;

import com.github.trosenkrantz.raptor.PromptEnum;
import com.github.trosenkrantz.raptor.configuration.ConfigurableEnum;

public enum Action implements PromptEnum, ConfigurableEnum {
    ENCODING("r", "Convert [R]APTOR encoding to a file", "encoding"),
    FILE("f", "Convert a [f]ile to RAPTOR encoding", "file");

    private final String promptValue;
    private final String description;
    private final String configurationId;

    Action(String promptValue, String description, String configurationId) {
        this.promptValue = promptValue;
        this.description = description;
        this.configurationId = configurationId;
    }

    @Override
    public String getPromptValue() {
        return this.promptValue;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String getConfigurationId() {
        return this.configurationId;
    }
}
