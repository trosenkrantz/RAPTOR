package com.github.trosenkrantz.raptor.io;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

public class IpAddressValidator implements Validator<String> {
    public static final Validator<String> VALIDATOR = new IpAddressValidator();

    @Override
    public Optional<String> validate(String value) {
        InetAddress address;
        try {
            address = InetAddress.getByName(value);
        } catch (UnknownHostException e) {
            return Optional.of("Failed to resolve IP address.");
        }

        if (!isIpV4(address) && !isIpV6(address)) {
            return Optional.of("Must be an IPv4 or IPv6 address.");
        }

        return Optional.empty();
    }

    public static boolean isIpV4(InetAddress address) {
        return address instanceof Inet4Address;
    }

    public static boolean isIpV6(InetAddress address) {
        return address instanceof Inet6Address;
    }
}
