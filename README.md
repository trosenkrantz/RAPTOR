# RAPTOR
Rapid Assessment and Protocol-based Testing of Operational Responders (RAPTOR) is a tool for analysing and testing systems that interface through low-level networking protocols.

It exchanges data with a system under test, either as a standalone, interactive console application or scripted through command line arguments. This makes it useful for:

- Experimenting with unfamiliar systems by using RAPTOR to interact with them.
- Testing systems by configuring RAPTOR to simulate other systems we may not have easy access to.

## Capabilities
- UDP:
  - Unicast, multicast, and broadcast.
  - Send and receive.
- TCP:
  - Client and server.
  - With and without TLS.
- SNMP:
  - GET, SET, and TRAP operations.
  - Listening and responding.
  - Arbitrary data support trough Basic Encoding Rules (BER) encoding.
- WebSocket:
  - Client.
  - server:
    - Multiple concurrent client connections.
  - With and without TLS (`wss://` and `ws://`)
  - Text and binary data.
- Can be used as CLI.
- Sending capabilities:
  - Interactively send text or binary data.
  - Send files.
  - Configure auto-replying (when receiving x, then send y).
    - Text and binary data.
    - Regex match inputs, even for binary inputs.
    - Define states, running a state machine.
- Supports various OSs through Java, Bash, and .cmd (Windows) scripts.
- Portable application, can we run from a USB stick.
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
     ├── LICENSE
     ├── raptor
     └── ...
     ```
   - Or install Java 21 or newer on your machine.
3. Run either the `raptor` bash script or `raptor.cmd` script.

RAPTOR will then guide you how to set it up.

## Encoding
RAPTOR supports arbitrary bytes, yet allows inputting printable ASCII characters as-is. To support this, we must input to RAPTOR either:
- Printable ASCII characters (except `"` and `\`) as-is, e.g., `abc`.
- The escape sequences `\n`, `\r`, `\t`, `\"`, and `\\`. 
- Escaped hex strings `\\x00` through `\\xff` (any casing).
- A mix of the above, e.g., `\\x00Hello\n` represents the 7 bytes with hex values `00`, `48`, `65`, `6c`, `6c`, `6f`, and `0a`.

RAPTOR outputs and logs with this encoding as well, allowing for easy copy-paste. Additionally, RAPTOR can convert between files and this encoding.

## CLI

Starting RAPTOR without arguments will prompt us to configure it. After configuration, it will output and log the configuration as CLI arguments, escaped and quoted as needed, e.g.:
```
Using arguments:
--service=udp --mode=unicast --role=send --destination-address=localhost --destination-port=50000 "--payload=Hello, World\!"
```

We can now run RAPTOR with the arguments directly without user interaction.

## Auto-Reply

For two-way communication (inputs and outputs), we can configure RAPTOR to auto-reply using a JSON file. Example:
```json5
{
    "startState": "login",
    "states": {
        "login": [
            {
                "input": "login!", // When RAPTOR receives this
                "output": "ok!", // It output this
                "nextState": "active" // And move to this state
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

## SNMP
For SNMP auto-replies, `input` is the OID in dot notation and `output` is the BER encoding of the response variable.

If RAPTOR finds no output to an SNMP auto-reply, it responds with Null.

See [snmp-replies.json](src/main/distributions/snmp-replies.json) for an example.

## TCP
For TCP auto-replies, RAPTOR passes input to the state machine byte by byte. Thus, RAPTOR behaves the same regardless of how data is buffered.

See [replies.json](src/main/distributions/replies.json) for an example.

## WebSocket
For WebSocket auto-replies, `input` is the whole payload data received, either for a text or binary frame.

See [replies.json](src/main/distributions/replies.json) for an example.

## TLS
RAPTOR is purposed for testing and analysis, not for operational usage. When using TLS, it does not verify certificates nor hostnames.

## Licence

This project is licenced under MIT, see [LICENSE](LICENSE) for more details. The project uses the following third-party libraries, which are subject to their own licences:

| Name                                                                    | License                                                  |
|-------------------------------------------------------------------------|----------------------------------------------------------|
| [Jackson Annotations](https://github.com/FasterXML/jackson-annotations) | [Apache-2.0](https://opensource.org/licenses/Apache-2.0) |
| [Jackson Core](https://github.com/FasterXML/jackson-core)               | [Apache-2.0](https://opensource.org/licenses/Apache-2.0) |
| [Jackson Databind](https://github.com/FasterXML/jackson-databind)       | [Apache-2.0](https://opensource.org/licenses/Apache-2.0) |
| [Java WebSockets](https://github.com/TooTallNate/Java-WebSocket)        | [MIT](https://opensource.org/license/mit)                |
| [SLF4J API](https://www.slf4j.org)                                      | [MIT](https://opensource.org/license/mit)                |
| [SLF4J JDK14 Provider](https://www.slf4j.org)                           | [MIT](https://opensource.org/license/mit)                |
| [SNMP4J](https://www.snmp4j.org)                                        | [Apache-2.0](https://opensource.org/licenses/Apache-2.0) |
