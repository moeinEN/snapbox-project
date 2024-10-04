package com.aut.snapboxfilereader.Model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class KafkaMessage {
    private List<DeliveryInfo> message;
}
