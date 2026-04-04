package com.github.trosenkrantz.raptor.configuration;

public interface ConfigurableEnum {
    /**
     * Used for storing values.
     * Must be unique without the possible values that we are look for.
     * E.g., if reading an enum value, this value must be unique among the values in the enum.
     *
     * @return value
     */
    String getConfigurationId();
}
