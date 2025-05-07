# Testing

## Integration testing
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