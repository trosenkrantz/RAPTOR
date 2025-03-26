FROM openjdk:21-jdk-slim

WORKDIR /app
COPY build/distributions /app

RUN chmod +x /app/raptor
ENTRYPOINT ["/app/raptor"]
