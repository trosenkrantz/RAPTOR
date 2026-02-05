package com.github.trosenkrantz.raptor.udp;

import java.net.*;

public class IpAddressMapper {
    public static ProtocolFamily getProtocolFamily(InetAddress address) {
        if (address instanceof Inet4Address) {
            return StandardProtocolFamily.INET;
        } else if (address instanceof Inet6Address) {
            return StandardProtocolFamily.INET6;
        } else {
            throw new IllegalArgumentException("Unknown address family: " + address);
        }
    }

    public static String getWildcard(InetAddress address) {
        if (address instanceof Inet4Address) {
            return "0.0.0.0";
        }  else if (address instanceof Inet6Address) {
            return "::";
        }  else {
            throw new IllegalArgumentException("Unknown address family: " + address);
        }
    }
}
