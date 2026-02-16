package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.configuration.SettingGroup;
import com.github.trosenkrantz.raptor.configuration.StringSetting;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import java.io.IOException;

public class VariableBindingSettingGroup implements SettingGroup<UnresolvedVariableBinding> {
    private static final StringSetting OID_SETTING = new StringSetting.Builder("o", SnmpService.PARAMETER_OID, "OID", "OID")
            .defaultValue(SnmpService.DEFAULT_OID)
            .build();
    private static final StringSetting VARIABLE_SETTING = new StringSetting.Builder("v", SnmpService.PARAMETER_VARIABLE, "Variable", "Variable as escaped string of BER encoding")
            .defaultValue(SnmpService.DEFAULT_VARIABLE)
            .build();

    @Override
    public Configuration configure() {
        Configuration configuration = Configuration.empty();

        OID_SETTING.configure(configuration);
        VARIABLE_SETTING.configure(configuration);

        return configuration;
    }

    @Override
    public String valueToString(Configuration configuration) {
        return OID_SETTING.valueToString(configuration) + ": " + VARIABLE_SETTING.valueToString(configuration);
    }

    @Override
    public UnresolvedVariableBinding readAndRequire(Configuration configuration) {
        return new UnresolvedVariableBinding(OID_SETTING.readAndRequire(configuration), VARIABLE_SETTING.readAndRequire(configuration));
    }
}
