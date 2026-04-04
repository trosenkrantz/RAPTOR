package com.github.trosenkrantz.raptor;

public interface RaptorService {
    /**
     * What the user must type to select this service.
     *
     * @return The prompt value.
     */
    String getPromptValue();

    /**
     * The CLI argument representing this service.
     *
     * @return The parameter key.
     */
    String getParameterKey();

    /**
     * A description of what this service does to be shown to the user.
     *
     * @return The description.
     */
    String getDescription();
}
