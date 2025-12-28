package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.io.ConsoleIo;
import com.github.trosenkrantz.raptor.io.Validator;

import java.util.Optional;
import java.util.logging.Logger;

public class IntegerIntervalSetting extends Setting<IntegerInterval> {
    private static final Logger LOGGER = Logger.getLogger(IntegerIntervalSetting.class.getName());
    private final Validator<Integer> minValidator;

    private IntegerIntervalSetting(Builder builder, Validator<Integer> minValidator) {
        super(builder);
        this.minValidator = minValidator;
    }

    @Override
    public Optional<IntegerInterval> read(Configuration configuration) {
        Optional<Integer> minValue = configuration.getInt(getMinKey());
        Optional<Integer> maxValue = configuration.getInt(getMaxKey());
        if (minValue.isPresent()) {
            if (maxValue.isPresent()) {
                return Optional.of(new IntegerInterval(minValue.get(), maxValue.get()));
            } else {
                LOGGER.warning(getMinKey() + " is set but " + getMaxKey() + " is not. Ignoring both.");
                return Optional.empty();
            }
        } else {
            if (maxValue.isPresent()) {
                LOGGER.warning(getMaxKey() + " is set but " + getMinKey() + " is not. Ignoring both.");
                return Optional.empty();
            } else {
                return Optional.empty();
            }
        }
    }

    private String getMaxKey() {
        return getParameterKey() + "-max";
    }

    private String getMinKey() {
        return getParameterKey() + "-min";
    }

    @Override
    public String valueToString(Configuration configuration) {
        return read(configuration).map(IntegerInterval::toString).orElse(Setting.EMPTY_VALUE_TO_STRING);
    }

    @Override
    public void configure(Configuration configuration) {
        Optional<IntegerInterval> current = readOrDefault(configuration);
        int min, max;
        if (current.isPresent()) {
            min = ConsoleIo.askForInt(getDescription() + ", minimum value", current.get().min(), minValidator);
            max = ConsoleIo.askForInt(getDescription() + ", maximum value", current.get().max(), maxEntered -> getValidator().validate(new IntegerInterval(min, maxEntered)));
        } else {
            min = ConsoleIo.askForInt(getDescription() + ", minimum value", minValidator);
            max = ConsoleIo.askForInt(getDescription() + ", maximum value", maxEntered -> getValidator().validate(new IntegerInterval(min, maxEntered)));
        }

        configuration.setInt(getMinKey(), min);
        configuration.setInt(getMaxKey(), max);
    }

    public static class Builder extends Setting.Builder<IntegerInterval, Builder> {
        private Validator<Integer> minValidator;

        public Builder(String promptValue, String parameterKey, String name, String description) {
            super(promptValue, parameterKey, name, description);
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public IntegerIntervalSetting build() {
            return new IntegerIntervalSetting(this, minValidator);
        }

        public Builder minValidator(Validator<Integer> minValidator) {
            this.minValidator = minValidator;
            return this;
        }
    }
}

