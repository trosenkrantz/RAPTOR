# RAPTOR
Rapid Assessment and Protocol-based Testing of Operational Responders (RAPTOR) is a tool for analysing and testing systems that interface through low-level networking protocols.

It runs as a console application, simulating a system, exchanging data with the system under test, either interactively or scripted with command line arguments

Capabilities:

- TCP:
  - Client and server.
- Sending capabilities:
  - Interactively send text or binary data.
  - Send files.
  - Configure auto-replying (when receiving x, send y).
    - Text and binary data.
    - Regex match inputs, even for binary inputs.
    - Define states, acting on a state machine.
- Supports various OSs through Java and bash and CMD (Windows) scripts.
- Logging to capture data exchange

## How to use
1. Download a release and unzip it.
2. Either
   - Download Java Runtime Environment 21 or newer, placing it in a `java` dir, or
   - Install Java 21 or newer on your machine.
3. Run RAPTOR, either the `raptor` bash script or `raptor.cmd` script.

RAPTOR will then guide you how to set it up.

## Auto-Reply

Example (remove comments before usage):
```json5
{
    "startState": "login",
    "states": {
        "login": [
            {
                "input": "login\n", // When receiving this
                "output": "ok\n", // Then output this
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

