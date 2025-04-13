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
docker run -it --network raptor raptor:latest
```
To run RAPTOR as CLI:
```bash
docker run -it --network raptor raptor:latest <args>
```
To get IP address of the container:
```bash
docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' <name>
```