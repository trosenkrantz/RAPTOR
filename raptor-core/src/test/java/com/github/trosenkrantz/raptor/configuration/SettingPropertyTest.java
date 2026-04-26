package com.github.trosenkrantz.raptor.configuration;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class SettingPropertyTest {

    @Property
    void integerSettingRoundTrip(@ForAll int value) {
        Configuration config = Configuration.empty();
        IntegerSetting setting = new IntegerSetting.Builder("p", "k", "n", "d").build();
        config.setInt("k", value);
        
        Optional<Integer> readValue = setting.read(config);
        assertTrue(readValue.isPresent());
        assertEquals(value, readValue.get());
    }

    @Property
    void integerSettingValidator(@ForAll int value, @ForAll @IntRange(min = 0, max = 100) int threshold) {
        Configuration config = Configuration.empty();
        // A validator that ensures value is >= threshold
        IntegerSetting setting = new IntegerSetting.Builder("p", "k", "n", "d")
                .validator(v -> v >= threshold ? Optional.empty() : Optional.of("Too small"))
                .build();
        
        config.setInt("k", value);
        
        Optional<String> validationError = setting.getValidator().validate(value);
        if (value >= threshold) {
            assertTrue(validationError.isEmpty());
        } else {
            assertTrue(validationError.isPresent());
            assertEquals("Too small", validationError.get());
        }
    }
}
