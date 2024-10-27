# RAPTOR
Rapid Assessment and Protocol-based Testing of Operational Responders (RAPTOR) is a tool for analysing and testing systems that interface through low-level networking protocols.

It runs as a console application, simulating a system, exchanging data with the system under test, either interactively or scripted with command line arguments

Capabilities:

- TCP:
  - Client and server.
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
   - Download Java Runtime Environment 21 or newer, extracting it to the `java` dir, or
   - Install Java 21 or newer on your machine.
3. Run RAPTOR, either the `raptor` bash script or `raptor.cmd` script.

RAPTOR will then guide you how to set it up.

## Auto-Reply

For two-way communication (inputs and outputs), we can configure RAPTOR to auto-reply using a JSON file. Example (remove comments before usage):
```json5
{
    "startState": "login",
    "states": {
        "login": [
            {
                "input": "login\n", // When RAPTOR receives this
                "output": "ok\n", // it output this
                "nextState": "active" // And move to this state
            }
        ],
        "active": [
            {
                "input": "set: \\d+\n", // Use regex
                "output": "\\x00ok\\xff" // Denote arbitrary bytes with \xhh hex
                // Absent nextState means stay
            },
            {
                "input": "\\x00(\\x01)*\\xff", // Include arbitrary bytes in regex
                "output": "ok\n"
            }
        ]
    }
}
```

RAPTOR checks for matching inputs in the order of appearance.

### SNMP
For SNMP auto-replies, `input` is the OID in dot notation and `output` is a Basic Encoding Rules (BER) encoding. Example (remove comments before usage):

```json5
{
    "startState": "S1",
    "states": {
        "S1": [
            {
                "input": "1.2.3.4", // For requests with OID 1.2.3.4
                "output": "\\x02\\x01\\x2a", // Type integer (\x02), length 1 byte, value 42 (\x2a)
                "nextState": "S2"
            },
            {
                "input": "1.2.3.5..+", // Use regex
                "output": "\\x04\\x02ok", // Type octet string (\x04), length 2 bytes, value "ok"
            }
        ],
        "S2": [
            {
                "input": "1.2.3.4",
                "output": "\\x02\\x02\\x01\\x2c", // Integer 300
                "nextState": "S1"
            }
        ]
    }
}
```

If RAPTOR finds no output to an SNMP auto-reply, it responds with Null.

Scenario: RAPTOR is configured with above auto-reply, and an SNMP manager keeps requesting RAPTOR with OID 1.2.3.4. RAPTOR will respond alternating with 42 and 300.

## Licence

This project is licenced under MIT, see [LICENSE](LICENSE) for more details. The project uses the following third-party libraries, which are subject to their own licences:

| Name                                                                    | License                                                          |
|-------------------------------------------------------------------------|------------------------------------------------------------------|
| [Jackson Annotations](https://github.com/FasterXML/jackson-annotations) | [Apache License 2.0](https://opensource.org/licenses/Apache-2.0) |
| [Jackson Core](https://github.com/FasterXML/jackson-core)               | [Apache License 2.0](https://opensource.org/licenses/Apache-2.0) |
| [Jackson Databind](https://github.com/FasterXML/jackson-databind)       | [Apache License 2.0](https://opensource.org/licenses/Apache-2.0) |
| [SNMP4J](https://www.snmp4j.org/)                                       | [Apache License 2.0](https://opensource.org/licenses/Apache-2.0) |
