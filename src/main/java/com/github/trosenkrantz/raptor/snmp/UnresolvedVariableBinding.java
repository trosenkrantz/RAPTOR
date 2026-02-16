package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.io.BytesFormatter;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import java.io.IOException;
import java.util.logging.Level;

/**
 * Variable binding where the variable is defined in RAPTOR encoding, thus, not yet resolved in bytes.
 *
 * @param oid      OID
 * @param variable variable in RAPTOR encoding
 */
public record UnresolvedVariableBinding(String oid, String variable) {
    public VariableBinding resolve(int commandSubstitutionTimeout) {
        byte[] berEncoding = BytesFormatter.raptorEncodingToBytes(variable, commandSubstitutionTimeout);

        try {
            return new VariableBinding(new OID(oid), SnmpService.toVariable(berEncoding));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed parsing " + BytesFormatter.bytesToRaptorEncodedBytes(berEncoding) +" as Basic Encoding Rules.", e);
        }
    }
}
