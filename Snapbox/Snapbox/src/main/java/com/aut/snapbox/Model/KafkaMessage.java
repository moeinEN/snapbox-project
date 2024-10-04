package com.aut.snapbox.Model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
public class KafkaMessage {
    private List<DeliveryInfo> message;

    public KafkaMessage(List<DeliveryInfo> message) {
        this.message = message;
    }

    public KafkaMessage() {
    }
}
