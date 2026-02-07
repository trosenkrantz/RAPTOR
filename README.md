# RAPTOR
Rapid Assessment and Protocol-based Testing of Operational Responders (RAPTOR) is a tool for analysing and testing systems that interface through low-level networking protocols.

It exchanges data with systems, either as a standalone, interactive console application or scripted through command line arguments. It is useful for:

- Experimenting with unfamiliar systems using RAPTOR to interact with them.
![](https://img.plantuml.biz/plantuml/png/JOun2i9044NxESMGIejSG4HYOM6bR4ndC-nEDWkRMPWTGMzleWYsZyVxlwj6QeyvG-RHq0fgYfDXIChUKX4WRqNn9349iyAAIM9cRDpnUhkuksnyaLeoRnkaYkvV6MIaEUL4Ylsh6iyVPG3uzN3VjWTqkCEp6EioLi5zuR10VlffIN75G4RGiOJrwni0)
- Testing integrations using RAPTOR to simulate systems we may not have access to.
![](https://img.plantuml.biz/plantuml/png/JO-n3i8m34JtV8NL2OR-04Ae3CoWjDrOhCQaaL9NYbsf_3r9kh3UdT_vEb5CkYtHHFeew0ef-5d1XKIJXfKDJhec64YKaYaYiey4JV_DoW8ZEphi1b2W7uV_aBs_XgTzyqd2iaNKm1vepoJ62589vtfdZJage6rlXGAN2XK8hEGJrb-AVYq1c-t7FisvXWGSAfgEs9KTFm00)
- Testing distributed systems using RAPTOR as a gateway to simulate middleware and network impairment we may not have access to.
![](https://img.plantuml.biz/plantuml/png/ROz12i8m44NtSugXArrqKPSYqkG2IipIpT2ECT0ca8n8RsyJGGJTVVvxds7wB9JaCKymyKrG8YRlxeAZJpo8Zm5ea9ZGSXPEI1bj1mLHS1DZSrdMVaRL5AI-YL5dUpBNi74a_AHbMnNpPwmhqR-NTW2smxOzei9z1HaK7afXmBo31bt2Ad-KdiDOFde1)

## Capabilities
- UDP:
  - Unicast, multicast, and broadcast.
  - Send and receive.
- TCP:
  - Client and server.
  - With and without TLS.
- SNMP:
  - Version 1 and 2c.
  - GET, SET, and TRAP operations.
  - Arbitrary data support trough Basic Encoding Rules (BER) encoding.
- Serial port.
- WebSocket:
  - Client.
  - Server:
    - Multiple concurrent client connections.
  - With and without TLS (`wss://` and `ws://`).
- Gateway between two systems, forwarding traffic:
  - UDP multicast, TCP, and serial port.
  - Mix protocols across the two systems.
  - Simulate network impairment:
    - Latency, corruption, packet loss, and duplication.
- Sending capabilities:
  - Interactively send text and binary data.
  - Configure auto-replying (when receiving x, then send y).
    - Text and binary data.
    - Use regular expressions to match input, even for binary input.
    - Define an arbitrary state machine.
- Arbitrary OS support through Java, shell scripts, and .cmd (Windows) scripts.
- Support scripted and automated execution.
- Portable application, can be run from a USB stick.
- Logging to capture data exchange.

## Getting Started
1. Download a release and unzip it.
2. Either
   - Download Java Runtime Environment 21 (e.g. [Azul Zulu](https://www.azul.com/downloads/?version=java-21-lts&package=jre#zulu)) or newer, extracting it to a `java` dir:
     ```
     ├── java
     │   ├── bin
     │   ├── conf
     │   └── ...
     ├── libs
     ├── LICENCE
     ├── raptor
     └── ...
     ```
   - Or install Java 21 or newer on your machine.
3. Run either the `raptor` shell script or `raptor.cmd` script.

## Encoding
RAPTOR supports arbitrary bytes, yet allows inputting printable ASCII characters as-is. To support this, we must input to RAPTOR either:
- Printable ASCII characters (except `"` and `\`) as-is, e.g., `abc`.
- The escape sequences `\n`, `\r`, `\t`, `\"`, and `\\`. 
- Escaped hex strings `\\x00` through `\\xff` (any casing).
- A mix of the above, e.g., `\\x00Hello\n` represents the 7 bytes with hex values `00`, `48`, `65`, `6c`, `6c`, `6f`, and `0a`.

RAPTOR outputs and logs with this encoding as well, allowing for easy copy-paste. Additionally, RAPTOR can convert between files and this encoding.

## Configuration

By default, starting RAPTOR will prompt us to configure a concurrentInstance, e.g., sending a UDP packet. When configured, we can either run the configuration immediately or save the configuration.

Saving configurations enables us to reuse, modify, and script scenarios. RAPTOR saves configurations as a JSON file in a `configs` directory:
```
configs
├── tcp-server
│   ├── config.json
│   ├── run
│   └── run.cmd
├── udp-multicast-send
│   ├── config.json
│   ├── run
│   └── run.cmd
```

RAPTOR also creates `run` and `run.cmd` scripts that run RAPTOR with the corresponding configuration.

## Auto-Reply

For two-way communication (inputs and outputs), we can configure RAPTOR to auto-reply using a state machine. Example:
```json5
{
    "startState": "login",
    "states": {
        "login": [
            {
                "input": "login!", // When RAPTOR receives this
                "output": "ok!", // It outputs this
                "nextState": "active" // And moves to this state
            }
        ],
        "active": [
            {
                "input": "set: \\d+!", // Use regexes
                "output": "\\x00\\xff" // Denote arbitrary bytes with \\xhh encoding
                // Absent nextState means stay
            },
            {
                "input": "\\x00(\\x01)*!", // Include arbitrary bytes in regexes
                "output": "\\x00ok\n" // Mix arbitrary bytes, printable characters, and control characters
            }
        ]
    }
}
```
RAPTOR checks for matching inputs in the order of appearance. It ignores simple line and block comments but does not use JSON5.

### TCP and Serial Port
For TCP and serial port auto-replies, RAPTOR passes input to the state machine byte by byte. Thus, RAPTOR behaves the same regardless of how data is buffered.

See [replies.json](src/main/resources/com/github/trosenkrantz/raptor/replies.json) for an example.

### SNMP
For SNMP auto-replies, `input` is the OID in dot notation and `output` is the BER encoding of the response variable.

When processing a GET request with multiple variable bindings, RAPTOR matches each IOD against the current state, to deliver independent response variables matching the corresponding request.
RAPTOR transitions the state machine once, based on the first matched OID.

If RAPTOR finds no match for an OID, it responds with Null for that variable binding.

See [snmp-replies.json](src/main/resources/com/github/trosenkrantz/raptor/snmp-replies.json) for an example.

### WebSocket
For WebSocket auto-replies, `input` is the whole payload data received, either for a text or binary frame.

See [replies.json](src/main/resources/com/github/trosenkrantz/raptor/replies.json) for an example.

## TLS
RAPTOR is purposed for testing and analysis, not for operational usage. When using TLS, it does not verify certificates.

## ANSI
RAPTOR uses ANSI escape codes to colour outputs. We can disable it with a `--no-ansi` argument in case our console does not support it.

## Licence

RAPTOR's source code is licenced under MIT, see [LICENCE](LICENCE) for more details. The release includes the following third-party libraries, which are subject to their own licences:

| Name                                                                    | License                                                  |
|-------------------------------------------------------------------------|----------------------------------------------------------|
| [Jackson Annotations](https://github.com/FasterXML/jackson-annotations) | [Apache-2.0](https://opensource.org/licenses/Apache-2.0) |
| [Jackson Core](https://github.com/FasterXML/jackson-core)               | [Apache-2.0](https://opensource.org/licenses/Apache-2.0) |
| [Jackson Databind](https://github.com/FasterXML/jackson-databind)       | [Apache-2.0](https://opensource.org/licenses/Apache-2.0) |
| [Java WebSockets](https://github.com/TooTallNate/Java-WebSocket)        | [MIT](https://opensource.org/license/mit)                |
| [jSerialComm](https://github.com/Fazecast/jSerialComm)                  | [Apache-2.0](https://opensource.org/licenses/Apache-2.0) |
| [SLF4J API](https://www.slf4j.org)                                      | [MIT](https://opensource.org/license/mit)                |
| [SLF4J JDK14 Provider](https://www.slf4j.org)                           | [MIT](https://opensource.org/license/mit)                |
| [SNMP4J](https://www.snmp4j.org)                                        | [Apache-2.0](https://opensource.org/licenses/Apache-2.0) |
