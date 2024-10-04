
# SnapBox Delivery Processing Application

This project handles processing and calculating delivery fees based on geographical coordinates and timestamps. The application is built on top of a Spring Boot framework, using Apache Kafka for message brokering and communication. The data is processed by multiple services, utilizing Kafka as the central message queue system to achieve scalability and reliability in processing delivery data.

## Project Overview

### Design Approach

The primary objective of this application is to process delivery information, calculate delivery fares, and store the results in CSV files. The application is designed around several key components:

1. **Kafka for Message Brokering**: Kafka is dockerized and used for handling the communication between services. The messages that contain delivery information are published and consumed from a Kafka topic called `Snap-Box-Topic`. Kafka ensures the decoupling of the message producers (file readers) and consumers (processing units), enhancing scalability and fault tolerance.

2. **Spring Boot with Kafka Integration**: The project uses Spring Boot for managing the Kafka consumer and producer components, leveraging dependency injection and configuration properties. The consumer processes batch messages, while the producer reads from CSV files and pushes data into Kafka topics.

3. **Data Processing and Validation**: 
    - The `ProcessUtils` class handles the core logic of data validation and fare calculation. It removes invalid entries (based on speed and time), ensuring that only legitimate delivery data is considered for fare computation.
    - The validation logic first calculates the velocity between consecutive delivery points (tuples). If the calculated speed exceeds 100 km/h, the entry is immediately removed. This quick elimination helps optimize the processing of large datasets by filtering invalid entries early.
    - Different algorithms like **Haversine** and **NormalDistanceCalc** are used for calculating the distance between geographical points, providing an accurate calculation of delivery speed and distance.

4. **Concurrency and Scheduling**: 
    - Concurrency is managed via custom thread pools to handle file processing tasks in parallel. A thread pool executor ensures efficient management of concurrent file reading and message production tasks.
    - A scheduled task is used to read files at specific intervals from a directory, process them, and send the data to Kafka.

5. **CSV File Handling**: The application reads delivery data from CSV files stored in a specific directory, processes them, and stores the results back into CSV files with calculated delivery fees.

### Dockerized Kafka Setup

The application uses a dockerized Kafka instance, defined in the following `docker-compose.yml` file:

```yaml
version: "2.20.3"

services:
  kafka:
    image: ${KAFKA_IMAGE}
    networks:
      - my_network
    ports:
      - 9092:9094
    volumes:
      - kafka_data:/bitnami
    environment:
      - KAFKA_CFG_NODE_ID=0
      - KAFKA_CFG_PROCESS_ROLES=controller,broker
      - KAFKA_CFG_LISTENERS=INTERNAL://:9092,CONTROLLER://:9093,EXTERNAL://:9094
      - KAFKA_CFG_ADVERTISED_LISTENERS=INTERNAL://kafka:9092,EXTERNAL://localhost:9092
      - KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,EXTERNAL:PLAINTEXT,INTERNAL:PLAINTEXT
      - KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=0@kafka:9093
      - KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER
      - KAFKA_CFG_INTER_BROKER_LISTENER_NAME=INTERNAL
      - KAFKA_CFG_LOG_RETENTION_HOURS=10
      - KAFKA_CFG_MESSAGE_MAX_BYTES=52428800
      - KAFKA_CFG_GROUP_INITIAL_REBALANCE_DELAY_MS=0
      - KAFKA_CFG_REPLICA_FETCH_MAX_BYTES=52428800
      - KAFKA_CFG_REPLICA_FETCH_RESPONSE_MAX_BYTES=52428800

  kafka-ui:
    image: ${KAFKA_UI}
    ports:
      - 8080:8080
    environment:
      DYNAMIC_CONFIG_ENABLED: 'true'
    volumes:
      - kafka_ui:/etc/kafkaui
    networks:
      - my_network

volumes:
  kafka_data:
  kafka_ui:

networks:
  my_network:
```

### Key Components

- **KafkaService**: 
  - This service is responsible for consuming messages from Kafka and passing the data to the processing unit (`ProcessUtils`). Messages are consumed in batches, allowing for efficient bulk processing.
  - The acknowledgment mechanism ensures that if a batch of messages is not processed successfully, it is retried after a delay.

- **ProcessUtils**: 
  - This utility class processes delivery data, focusing on the validation and fare calculation logic. The first step in this process is calculating the velocity between consecutive delivery points (tuples). If the calculated speed exceeds 100 km/h, we immediately assume the speed is invalid, thus removing the entry quickly without further checks. This speeds up the process by eliminating invalid data early, improving efficiency in handling large datasets.
  - After the validation, the remaining entries are used to calculate the delivery fare based on the valid data points.
  - It writes the processed results to a CSV file for later reference.

- **ProducerConfiguration & ConsumerConfiguration**: 
  - These configurations define Kafka producers and consumers, setting up properties such as fetch size, poll records, and the custom deserializer for reading Kafka messages.

- **MainService**: 
  - This service schedules and manages file reading tasks from a directory. It reads CSV files, sends the data to Kafka, and renames and moves processed files.

### Future Enhancements

- **Scalability**: Kafka enables the application to scale horizontally by adding more consumers and producers to handle increased delivery data.
- **Error Handling**: Additional mechanisms can be added for better logging, error tracing, and more sophisticated retries in case of processing failures.
- **Security**: Integration of secure protocols (like SSL) for Kafka communication and securing file data processing.

## How to Run

1. Ensure Docker is installed and running.
2. Use the provided `docker-compose.yml` to spin up Kafka and its UI by running:
   ```bash
   docker-compose up -d
   ```
3. Start the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```
4. Ensure the directory path for CSV files is correctly set in `application.properties`.
5. Monitor and manage the Kafka topics using the Kafka UI running on `http://localhost:8080`.

## Conclusion

This project demonstrates an efficient system for processing delivery data and calculating fees using Kafka for message brokering and Spring Boot for service orchestration. The modular design allows for scalability and extension in future iterations.
