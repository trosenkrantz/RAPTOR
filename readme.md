# RAPTOR
Rapid Assessment and Protocol-based Testing of Operational Responders (RAPTOR) is a tool for analysing and testing systems that interface through low-level networking protocols.

It exchanges data with systems, either as a standalone, interactive console application or scripted through command line arguments. It is useful for:

- Experimenting with unfamiliar systems using RAPTOR to interact with them.<br>
  ![](https://img.plantuml.biz/plantuml/dsvg/JOun2i9044NxESMGIejSG4HYaS9AsPWkpR2P90kRMPWTGM_lHX5i7u_tVvEDr1vhXCYJeINKC2-6Or3s5f80UKkAhSn1c1KsJ397nigTR_Uh6sJda-GKULyXPNJ_IYyJhI46qa6wLpPUFquFy0lYiQvFwF205p7MifR1VM5VOFlqIsbnnf6Ce64PjxDl)
- Testing integrations using RAPTOR to simulate systems we may not have access to.<br>
  ![](https://img.plantuml.biz/plantuml/dsvg/JO-n3i8m34JtV8N514D_025K1cPGscvThCQaaL9NYbsf_3r9kh3UdT_vMb5CkYtHHFeew0ef-5d1XKIJXfKDJhecw4cKaYaYiey4f-wRbMNaWHqT_WbGe1lw_vHzlFkNXRE4POkeWVt8j6SKOmIfY4kziyOS7T0qzqA1QuKA15Ro2UipHR-LWCtsSv_cN4C23XLDI-pAa1y0)
- Testing distributed systems using RAPTOR as a gateway to simulate middleware and network impairment we may not have access to.<br>
  ![](https://img.plantuml.biz/plantuml/dsvg/ROz12i8m44NtSugXArrqKPSYqkG2bfXbDw4TOo1D81cHtjucWWYw-_ptFiFqcKtYn-B1qusH8oPdxuopIpInYm7Cn36XfioKq6JK7HHaWlM4pNNT-cKCeYbCV2Cb1drHOzXPm_GqwxugwjzQLYH_Rnq0T8TjUqQ1nmeo23oLGe5w1mcoX4t-A3s5kVpq0W00)

## Capabilities
- UDP:
  - Unicast, multicast, and broadcast.
  - IPv4 and IPv6.
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
    - Update the state machine dynamically. 
- Arbitrary OS support through Java, shell scripts, and .cmd (Windows) scripts.
- Support scripted and automated execution.
- Portable application, can be run from a USB stick.
- Logging to capture data exchange.

## Getting Started
1. Download a RAPTOR release and unzip it.
2. Set up Java Runtime Environment 21 or newer, either
   - Run the `download-java` shell script or the `download-java.cmd` script.
   - Download Java (e.g. [Azul Zulu](https://www.azul.com/downloads/?version=java-21-lts&package=jre#zulu)) or newer, extracting it to a `java` dir:
     ```
     ├── java
     │   ├── bin
     │   ├── conf
     │   └── ...
     ├── libs
     ├── licence
     └── ...
     ```
   - Or install Java 21 or newer on your machine.
3. Run the `raptor` shell script or `raptor.cmd` script.

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

RAPTOR listens to changes to the configuration file.
This enables us to dynamically change all aspects of the state machine, Example use-cases:

- Experiment with different scenarios while RAPTOR keeps the connection to an unfamiliar system, by manually editing the configuration.
- Have RAPTOR react on or produce data can cannot be hardcoded, using a custom script to update `config.json`.<br>
  ![](https://img.plantuml.biz/plantuml/dsvg/JP2n2i9038RtUuhWf8Cl82AAE2jdkxc4QsmFhccvfA1lR-vqS0l_btm9EOfYrcLCBj5JGIV8iHyKkfWfQ9pOOT0fGqEYb5q9aVj4iBg_BHaVt797Nxu25BYtpN-NFzsQguUrn759g97x1zFBL8m9f2esTSx_JvqNqSdS4dASVzvQElSz1BRRGra5kxfP8B9Idx5UNF9zQV26Bwymc9K4EbHqlf2Vp6WxMshClg048uOXChaZSMSl-G00)
  - Timestamp generation tied to wall clock.
  - Unbounded output, like sequence numbers.
  - Probabilistic, random, or other non-deterministic behaviour.
  - Complex branching where a hardcoded state machine would need too many states.

### TCP and Serial Port
For TCP and serial port auto-replies, RAPTOR passes input to the state machine byte by byte. Thus, RAPTOR behaves the same regardless of how data is buffered.

<details>
<summary>Example</summary>

```json5
{
  "startState": "unauthenticated",
  "states": {
    "unauthenticated": [
      {
        "input": "AUTH user pass!", // When RAPTOR receives this
        "output": "AUTH_OK!", // It outputs this, confirming authentication success
        "nextState": "ready1" // And move to this state
      },
      {
        "input": "AUTH .*!", // Utilising the order of transitions to capture invalid credentials
        "output": "INVALID_CREDENTIALS!"
        // Stay unauthenticated with missing nextState
      },
      {
        "input": ".*!", // Capture everything else ending with !
        "output": "UNKNOWN_COMMAND!"
      }
    ],
    "ready1": [
      {
        "input": "STATUS!",
        "output": "STATUS:OK!"
      },
      {
        "input": "DATA!",
        "output": "DATA:1234!",
        "nextState": "ready2" // Alternate between two states to simulate variation
      },
      {
        "input": ".*!",
        "output": "UNKNOWN_COMMAND!"
      }
    ],
    "ready2": [
      {
        "input": "STATUS!",
        "output": "STATUS:OK!"
      },
      {
        "input": "DATA!",
        "output": "DATA:2345!",
        "nextState": "ready1" // Alternate between two states to simulate variation
      },
      {
        "input": ".*!",
        "output": "UNKNOWN_COMMAND!"
      }
    ]
  }
}
```
</details>


### SNMP
For SNMP auto-replies, `input` is the OID in dot notation and `output` is the BER encoding of the response variable.

When processing a GET request with multiple variable bindings, RAPTOR matches each IOD against the current state, to deliver independent response variables matching the corresponding request.
RAPTOR transitions the state machine once, based on the first matched OID with a `nextState`.

If RAPTOR finds no match for an OID, it responds with Null for that variable binding.

<details>
<summary>Example</summary>

```json5
{
  "startState": "ready1",
  "states": {
    "ready1": [
      {
        "input": "1.2.3.4", // For OID 1.2.3.4
        "output": "\\x02\\x01\\x2a", // Deliver type integer (\x02 in BER), length 1 byte, value 42 (\x2a in hex)
        "nextState": "ready2" // Alternate between two states to simulate variation
      },
      {
        "input": "1.2.3.5..+", // Use regex
        "output": "\\x04\\x05Hello" // Type octet string (\x04), length 5 bytes, value "Hello"
      }
      // For SNMP, RAPTOR delivers Null if it finds no match
    ],
    "ready2": [
      {
        "input": "1.2.3.4",
        "output": "\\x02\\x02\\x01\\x2c", // Integer 300
        "nextState": "ready1" // Alternate between two states to simulate variation
      }
    ]
  }
}
```
</details>

### WebSocket
For WebSocket auto-replies, `input` is the whole payload data received, either for a text or binary frame.

## TLS
RAPTOR is purposed for testing and analysis, not for operational usage. When using TLS, it does not verify certificates.

## ANSI
RAPTOR uses ANSI escape codes to colour outputs. We can disable it with a `--no-ansi` argument in case our console does not support it.

## Licence

RAPTOR's source code is licenced under MIT, see [licence](licence) for more details. The release includes the following third-party libraries, which are subject to their own licences:

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
