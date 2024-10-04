package com.aut.snapbox.Model;

import com.aut.snapbox.Model.KafkaMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class KafkaMessageDeserializer implements Deserializer<KafkaMessage> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // No configuration needed
    }

    @Override
    public KafkaMessage deserialize(String topic, byte[] data) {
        try {
            if (data == null){
                return null;
            }
            return objectMapper.readValue(data, KafkaMessage.class);
        } catch (Exception e){
            throw new SerializationException("Error deserializing KafkaMessage", e);
        }
    }

    @Override
    public void close() {
        // No resources to close
    }
}
