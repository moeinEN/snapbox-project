package com.aut.snapbox;

import com.aut.snapbox.Model.DeliveryInfo;
import com.aut.snapbox.Model.KafkaMessage;
import com.aut.snapbox.Utils.ProcessUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.kafka.support.Acknowledgment;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class KafkaService {

    @Autowired
    private ProcessUtils processUtils;

    @KafkaListener(topics = "Snap-Box-Topic", containerFactory = "concurrentKafkaListenerContainerFactory")
    private void receiveMessageFromKafka(List<ConsumerRecord<String, KafkaMessage>> consumerRecords, Acknowledgment acknowledgment) {
        List<List<DeliveryInfo>> deliveryInfos = new ArrayList<>();
        for (ConsumerRecord<String, KafkaMessage> consumerRecord : consumerRecords) {
            KafkaMessage kafkaMessage = consumerRecord.value();
            deliveryInfos.add(kafkaMessage.getMessage());
        }
        boolean resp = processUtils.processOnData(deliveryInfos);
        if (!resp) {
            acknowledgment.nack(Duration.ofMillis(500));
        } else {
            acknowledgment.acknowledge();
        }
    }


}
