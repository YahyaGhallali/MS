package com.example.orderservice.outbox;

import com.example.orderservice.config.KafkaConfig;
import com.example.orderservice.event.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           KafkaTemplate<String, Object> kafkaTemplate,
                           ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
        for (OutboxEvent outboxEvent : pendingEvents) {
            try {
                if ("OrderCreated".equals(outboxEvent.getEventType())) {
                    String payload = outboxEvent.getPayload();
                    kafkaTemplate.send(KafkaConfig.ORDER_CREATED_TOPIC, String.valueOf(outboxEvent.getAggregateId()), payload);
                    log.info("[Outbox] Published OrderCreatedEvent [aggregateId={}] to Kafka topic {}",
                            outboxEvent.getAggregateId(), KafkaConfig.ORDER_CREATED_TOPIC);
                }
                outboxEvent.setProcessed(true);
                outboxEventRepository.save(outboxEvent);
            } catch (Exception e) {
                log.error("[Outbox] Failed to publish outbox event id {}: {}", outboxEvent.getId(), e.getMessage());
            }
        }
    }
}
