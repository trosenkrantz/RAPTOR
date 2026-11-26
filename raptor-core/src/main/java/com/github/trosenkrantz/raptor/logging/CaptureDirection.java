package com.github.trosenkrantz.raptor.logging;

import com.github.trosenkrantz.raptor.PromptEnum;
import com.github.trosenkrantz.raptor.configuration.ConfigurableEnum;

/** Direction of a captured data exchange line in a RAPTOR log file. */
public enum CaptureDirection implements PromptEnum, ConfigurableEnum {
    RECEIVED("r", "Capture [r]eceived data", "received", "Received"),
    SENT("s", "Capture [s]ent data", "sent", "Sent");

    private final String promptValue;
    private final String description;
    private final String configurationId;
    private final String logWord;

    CaptureDirection(String promptValue, String description, String configurationId, String logWord) {
        this.promptValue = promptValue;
        this.description = description;
        this.configurationId = configurationId;
        this.logWord = logWord;
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

    /** Word used at the start of a capture line in the log file. */
    public String getLogWord() {
        return logWord;
    }
}
