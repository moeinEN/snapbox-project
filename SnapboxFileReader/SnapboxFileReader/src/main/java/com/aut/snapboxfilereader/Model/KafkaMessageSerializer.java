package com.aut.snapboxfilereader.Model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

import java.util.Map;

public class KafkaMessageSerializer implements Serializer<KafkaMessage> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // No configuration needed
    }

    @Override
    public byte[] serialize(String topic, KafkaMessage data) {
        try {
            if (data == null){
                return null;
            }
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e){
            throw new SerializationException("Error serializing KafkaMessage", e);
        }
    }

    @Override
    public void close() {
        // No resources to close
    }
}
