package com.github.trosenkrantz.raptor.snmp;

import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.security.SecurityModel;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

public class SnmpUtility {
    public static String pduToString(CommandResponderEvent<?> event, PDU pdu) {
        String version = getSnmpVersion(event);
        Optional<String> communityOptional = getCommunity(event);

        return communityOptional.map(community -> pduToString(pdu, version, community)).orElseGet(() -> pduToString(pdu, version, null));
    }

    public static String pduToString(PDU pdu, String version) {
        return pduToString(pdu, version, null);
    }

    public static String pduToString(PDU pdu, String version, String community) {
        String type = PDU.getTypeString(pdu.getType());
        int requestId = pdu.getRequestID().getValue();
        String bindings = pdu.getVariableBindings().stream()
                .map(binding -> binding.getOid().toString() + "=" + binding.getVariable().toString())
                .collect(Collectors.joining(", "));

        String result = type + " PDU, SNMPv" + version;
        if (community != null) result += ", community " + community;
        result += ", request ID " + requestId + ", variables [" + bindings + "]";

        return result;
    }

    private static String getSnmpVersion(CommandResponderEvent<?> event) {
        int securityModel = event.getSecurityModel();

        if (securityModel == SecurityModel.SECURITY_MODEL_SNMPv1) return "1";
        else if (securityModel == SecurityModel.SECURITY_MODEL_SNMPv2c) return "2c";
        else if (securityModel == SecurityModel.SECURITY_MODEL_USM || securityModel == SecurityModel.SECURITY_MODEL_TSM) return "3";
        else return "unknown";
    }

    private static Optional<String> getCommunity(CommandResponderEvent<?> event) {
        int securityModel = event.getSecurityModel();

        // There is only a community for version 1 and 2c in SNMP
        if (securityModel == SecurityModel.SECURITY_MODEL_SNMPv1 || securityModel == SecurityModel.SECURITY_MODEL_SNMPv2c) return Optional.of(new String(event.getSecurityName(), StandardCharsets.US_ASCII)); // Communities are ASCII strings
        else return Optional.empty();
    }
}
