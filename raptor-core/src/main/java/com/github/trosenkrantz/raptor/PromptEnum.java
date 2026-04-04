package com.github.trosenkrantz.raptor;

/**
 * Used to prompt the user to choose an enum value.
 */
public interface PromptEnum {
    /**
     * Value for user to enter.
     *
     * @return value
     */
    String getPromptValue();

    /**
     * Value to display for the user.
     *
     * @return value
     */
    String getDescription();
}
