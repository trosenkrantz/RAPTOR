package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.configuration.Setting;
import com.github.trosenkrantz.raptor.configuration.StringSetting;

import java.util.Optional;

public class VariableBindingSettingGroup implements Setting<UnresolvedVariableBinding> {
    private static final StringSetting OID_SETTING = new StringSetting.Builder("o", SnmpService.PARAMETER_OID, "OID", "OID")
            .defaultValue(SnmpService.DEFAULT_OID)
            .build();
    private static final StringSetting VARIABLE_SETTING = new StringSetting.Builder("v", SnmpService.PARAMETER_VARIABLE, "Variable", "Variable as escaped string of BER encoding")
            .defaultValue(SnmpService.DEFAULT_VARIABLE)
            .build();

    @Override
    public Optional<UnresolvedVariableBinding> read(Configuration configuration) {
        Optional<String> oid = OID_SETTING.read(configuration);
        Optional<String> variable = VARIABLE_SETTING.read(configuration);

        if (oid.isPresent() && variable.isPresent()) return Optional.of(new UnresolvedVariableBinding(oid.get(), variable.get()));
        else return Optional.empty();
    }

    @Override
    public void configure(Configuration configuration) {
        OID_SETTING.configure(configuration);
        VARIABLE_SETTING.configure(configuration);
    }

    @Override
    public String valueToString(UnresolvedVariableBinding value) {
        return OID_SETTING.valueToString(value.oid()) + ": " + VARIABLE_SETTING.valueToString(value.variable());
    }

    @Override
    public UnresolvedVariableBinding readAndRequireOrDefault(Configuration configuration) {
        return new UnresolvedVariableBinding(OID_SETTING.readAndRequireOrDefault(configuration), VARIABLE_SETTING.readAndRequireOrDefault(configuration));
    }
}
