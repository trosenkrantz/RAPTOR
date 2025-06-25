package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import com.github.trosenkrantz.raptor.io.Validator;

import java.util.Optional;
import java.util.logging.Logger;

public class IntervalSetting extends Setting<Interval> {
    private static final Logger LOGGER = Logger.getLogger(IntervalSetting.class.getName());
    private final Validator<Integer> minValidator;

    private IntervalSetting(Builder builder, Validator<Integer> minValidator) {
        super(builder);
        this.minValidator = minValidator;
    }

    @Override
    public Optional<Interval> read(Configuration configuration) {

        Optional<Integer> minValue = configuration.getInt(getMinKey());
        Optional<Integer> maxValue = configuration.getInt(getMaxKey());
        if (minValue.isPresent()) {
            if (maxValue.isPresent()) {
                return Optional.of(new Interval(minValue.get(), maxValue.get()));
            } else {
                LOGGER.warning(getMinKey() + " is set but " + getMaxKey() + " is not. ignoring both.");
                return Optional.empty();
            }
        } else {
            if (maxValue.isPresent()) {
                LOGGER.warning(getMaxKey() + " is set but " + getMinKey() + " is not. ignoring both.");
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
        return read(configuration).map(Interval::toString).orElse(Setting.EMPTY_VALUE_TO_STRING);
    }

    @Override
    public void configure(Configuration configuration) {
        Optional<Interval> current = read(configuration);
        int min, max;
        if (current.isPresent()) {
            min = ConsoleIo.askForInt(getDescription() + ", minimum value", current.get().min(), minValidator);
            max = ConsoleIo.askForInt(getDescription() + ", maximum value", current.get().max(), maxEntered -> getValidator().validate(new Interval(min, maxEntered)));
        } else {
            min = ConsoleIo.askForInt(getDescription() + ", minimum value", minValidator);
            max = ConsoleIo.askForInt(getDescription() + ", maximum value", maxEntered -> getValidator().validate(new Interval(min, maxEntered)));
        }

        configuration.setInt(getMinKey(), min);
        configuration.setInt(getMaxKey(), max);
    }

    public static class Builder extends Setting.Builder<Interval, Builder> {
        private Validator<Integer> minValidator;

        public Builder(String promptValue, String parameterKey, String name, String description) {
            super(promptValue, parameterKey, name, description);
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public IntervalSetting build() {
            return new IntervalSetting(this, minValidator);
        }

        public Builder minValidator(Validator<Integer> minValidator) {
            this.minValidator = minValidator;
            return this;
        }
    }
}

