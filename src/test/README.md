# Testing

## Integration Testing
We run automated integration tests using testcontainers.

The concept is to create a Docker network and run containers in it.

We can do the same manually.
To create a Docker network:
```bash
docker network create --driver bridge raptor
```

To run RAPTOR interactively with it:
```bash
docker run -it --rm --name raptor1 --network raptor raptor:latest ./raptor <potential args>
```

To get the IP address of a container:
```bash
docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' <name>
```

For serial port communication, we can use socat to create a virtual serial port and connect it to a TCP socket.
By doing this on two containers and connecting the TCP sockets to each other, we can simulate serial communication:
```bash
# On container 1
# Create a TCP server socket and bridge it to a virtual serial port
socat -d -d TCP-LISTEN:50000,reuseaddr,fork PTY,link=/dev/ttyS1,raw &

# On container 2
# Create a virtual serial port and bridge it to TCP client socket that connects to the server
socat -d -d PTY,link=/dev/ttyS1,raw TCP:<IP address or hostname of container 1>:50000 &
```

### Parallelisation
To reduce test execution time, we run integration tests in parallel.
This is enabled in [junit-platform.properties](./resources/junit-platform.properties).

The vast majority of computation time is spent in testcontainers containers on non-JUnit threads.
Without throttling those containers, testcontainers would start containers faster than the containers than keep up with, causing flaky timeouts.

As the containers run on non-JUnit threads, we cannot use JUnit's parallelisation to throttle.
Instead, we use a semaphore that each test-case acquires.
See [RaptorIntegrationTest.java](./java/com/github/trosenkrantz/raptor/RaptorIntegrationTest.java).
We have a single JUnit thread, so at most one thread is waiting for a permit at a time.