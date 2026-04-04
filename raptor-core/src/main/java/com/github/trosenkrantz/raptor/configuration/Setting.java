package com.github.trosenkrantz.raptor.configuration;

import java.util.Optional;

/**
 * Something to be configured by a user as well as stored in and read from the CLI configuration.
 * We intend the following flow:
 * <ol>
 *     <li>RAPTOR prompts the user to configure a setting in {@link #configure(Configuration)}</li>
 *     <ul>
 *         <li>While configuring, RAPTOR keeps the state in a {@link Configuration}.</li>
 *         <li>For complex settings, RAPTOR might iteratively print the working value with {@link #valueToString(T)}.</li>
 *     </ul>
 *     <li>RAPTOR reads the configuration with {@link #read(Configuration)} or {@link #readAndRequireOrDefault(Configuration)}, returning POJO objects.</li>
 * </ol>
 *
 * @param <T> Type for what the settings model
 */
public interface Setting<T> {
    /**
     * Read the value for this setting from the configuration.
     * This does not consider a potential default value.
     *
     * @param configuration Configuration to read from
     * @return Optional setting value, empty if not set
     */
    Optional<T> read(Configuration configuration);

    /**
     * Read the value of this setting from the configuration.
     *
     * @param configuration configuration to read
     * @return read value or default if absent from configuration
     * @throws IllegalArgumentException if unset and there is no default
     */
    T readAndRequireOrDefault(Configuration configuration);

    /**
     * Gets a string representation of a value of this setting.
     * Used to display the value to the user.
     *
     * @return String representation of the value
     */
    String valueToString(T value);

    /**
     * Prompts the user to configure this setting.
     * If any, we use the current value as default.
     *
     * @param configuration configuration to read and set this setting with
     */
    void configure(Configuration configuration);
}