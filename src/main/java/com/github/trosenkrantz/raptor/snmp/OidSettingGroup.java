package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.configuration.SettingGroup;
import com.github.trosenkrantz.raptor.configuration.StringSetting;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

public class OidSettingGroup implements SettingGroup<VariableBinding> {
    private static final StringSetting OID_SETTING = new StringSetting.Builder("o", "oid", "OID", "OID")
            .defaultValue(SnmpService.DEFAULT_OID)
            .build();

    @Override
    public Configuration configure() {
        Configuration configuration = Configuration.empty();

        OID_SETTING.configure(configuration);

        return configuration;
    }

    @Override
    public String configurationToString(Configuration configuration) {
        return OID_SETTING.valueToString(configuration);
    }

    @Override
    public VariableBinding readAndRequire(Configuration cfg) {
        return new VariableBinding(new OID(OID_SETTING.readAndRequire(cfg)));
    }
}
