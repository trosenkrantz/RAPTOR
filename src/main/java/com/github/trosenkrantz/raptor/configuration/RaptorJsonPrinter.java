package com.github.trosenkrantz.raptor.configuration;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

public class RaptorJsonPrinter extends DefaultPrettyPrinter {
    public RaptorJsonPrinter() {
        super._arrayIndenter = new DefaultIndenter();
        super._objectFieldValueSeparatorWithSpaces = _separators.getObjectFieldValueSeparator() + " ";
    }

    @Override
    public RaptorJsonPrinter createInstance() {
        return new RaptorJsonPrinter();
    }
}