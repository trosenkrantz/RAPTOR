# RAPTOR
Rapid Assessment and Protocol-based Testing of Operational Responders (RAPTOR) is a tool for analysing and testing systems that interface through low-level networking protocols.

It runs as a console application, simulating a system, exchanging data with the system under test, either interactively or scripted with command line arguments

Capabilities:

- TCP:
  - Client and server.
  - TLS.
- SNMP
  - GET requests and responses
- Sending capabilities:
  - Interactively send text or binary data.
  - Send files.
  - Configure auto-replying (when receiving x, then send y).
    - Text and binary data.
    - Regex match inputs, even for binary inputs.
    - Define states, running a state machine.
- Supports various OSs through Java, bash, and CMD (Windows) scripts.
- Logging to capture data exchange.

## How to use
1. Download a release and unzip it.
2. Either
   - Download Java Runtime Environment 21 or newer, extracting it to a `java` dir, or
   - Install Java 21 or newer on your machine.
3. Run RAPTOR, either the `raptor` bash script or `raptor.cmd` script.

RAPTOR will then guide you how to set it up.

### Encoding
RAPTOR can handle arbitrary bytes, yet allows us to input printable ASCII characters as-is as well. To handle this, our input to RAPTOR must be either:
- Printable characters (except `"` and `\`) as-is, e.g., `abc`.
- The escaped sequences `\n`, `\r`, `\t`, `\"`, and `\\`. 
- Escaped hex strings `\\x00` through `\\xff` (any casing).
- A mix of the above, e.g., `\\x00Hello\n` represents the 7 bytes with hex values `00`, `48`, `65`, `6c`, `6c`, `6f`, and `0a`.

See below JSON as an example.

RAPTOR logs with this encoding as well, allowing for easy copy-paste.

### Auto-Reply

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
                "output": "\\x00\\xff" // Denote arbitrary bytes with \xhh hex
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
RAPTOR checks for matching inputs in the order of appearance. It ignores simple line and block comments.

### SNMP
For SNMP auto-replies, `input` is the OID in dot notation and `output` is the Basic Encoding Rules (BER) encoding of the response variable.

If RAPTOR finds no output to an SNMP auto-reply, it responds with Null.

See [snmp-replies.json](src/main/distributions/snmp-replies.json) for an example.

### TCP
For TCP auto-replies, it passes TCP input to the state machine byte by byte. Thus, RAPTOR behaves the same regardless of how data is buffered.

See [tcp-replies.json](src/main/distributions/tcp-replies.json) for an example.

## Licence

This project is licenced under MIT, see [LICENSE](LICENSE) for more details. The project uses the following third-party libraries, which are subject to their own licences:

| Name                                                                    | License                                                          |
|-------------------------------------------------------------------------|------------------------------------------------------------------|
| [Jackson Annotations](https://github.com/FasterXML/jackson-annotations) | [Apache License 2.0](https://opensource.org/licenses/Apache-2.0) |
| [Jackson Core](https://github.com/FasterXML/jackson-core)               | [Apache License 2.0](https://opensource.org/licenses/Apache-2.0) |
| [Jackson Databind](https://github.com/FasterXML/jackson-databind)       | [Apache License 2.0](https://opensource.org/licenses/Apache-2.0) |
| [SNMP4J](https://www.snmp4j.org/)                                       | [Apache License 2.0](https://opensource.org/licenses/Apache-2.0) |
