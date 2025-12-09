package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.PromptEnum;
import org.snmp4j.mp.SnmpConstants;

public enum Version implements PromptEnum {
    V1("1", "SNMP version 1", SnmpConstants.version1, "1"),
    V2C("2", "SNMP version 2c", SnmpConstants.version2c, "2c");

    private final String promptValue;
    private final String description;
    private final int snmpValue;
    private final String toString;

    Version(String promptValue, String description, int snmpValue, String toString) {
        this.promptValue = promptValue;
        this.description = description;
        this.snmpValue = snmpValue;
        this.toString = toString;
    }

    @Override
    public String getPromptValue() {
        return this.promptValue;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    public int getSnmpValue() {
        return snmpValue;
    }

    @Override
    public String toString() {
        return toString;
    }
}
