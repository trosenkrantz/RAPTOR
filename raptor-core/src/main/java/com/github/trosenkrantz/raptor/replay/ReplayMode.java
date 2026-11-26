package com.github.trosenkrantz.raptor.replay;

import com.github.trosenkrantz.raptor.PromptEnum;
import com.github.trosenkrantz.raptor.configuration.ConfigurableEnum;

/** Whether a replay ends after one pass or starts again immediately. */
public enum ReplayMode implements PromptEnum, ConfigurableEnum {
    ONCE("o", "[O]nce", "once"),
    LOOP("l", "[L]oop", "loop");

    private final String promptValue;
    private final String description;
    private final String configurationId;

    ReplayMode(String promptValue, String description, String configurationId) {
        this.promptValue = promptValue;
        this.description = description;
        this.configurationId = configurationId;
    }

    @Override
    public String getPromptValue() {
        return promptValue;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getConfigurationId() {
        return configurationId;
    }
}
