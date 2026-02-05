package com.github.trosenkrantz.raptor.configuration;

public interface SettingGroup<T> {
    Configuration configure();

    /**
     * Gets a string representation of the value of this setting given a configuration.
     * Used to display the value to the user.
     *
     * @param configuration Configuration to read from
     * @return String representation of the value
     */
    String valueToString(Configuration configuration);

    T readAndRequire(Configuration configuration);
}