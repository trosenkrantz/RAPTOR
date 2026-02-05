package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.io.Validator;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

public class MulticastGroupValidator implements Validator<String> {
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

        if (!address.isMulticastAddress()) {
            return Optional.of("Must be a multicast address; within 224.0.0.0/4 for IPv4, ff00::/8 for IPv6.");
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
