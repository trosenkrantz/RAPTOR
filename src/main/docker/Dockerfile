FROM openjdk:21-jdk-slim

# Install socat, used for serial port forwarding
RUN apt-get update && \
    apt-get install -y socat && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

COPY build/distributions /app
WORKDIR /app
RUN chmod +x /app/raptor

CMD ["bash"]
