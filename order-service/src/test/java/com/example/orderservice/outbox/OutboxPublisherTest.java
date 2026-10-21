package com.example.orderservice.outbox;

import com.example.orderservice.config.KafkaConfig;
import com.example.orderservice.event.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ObjectMapper objectMapper;
    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        outboxPublisher = new OutboxPublisher(outboxEventRepository, kafkaTemplate, objectMapper);
    }

    @Test
    @DisplayName("Should poll pending outbox events, publish to Kafka, and mark them processed")
    void testPublishPendingEvents() throws Exception {
        OrderCreatedEvent event = new OrderCreatedEvent("evt-123", 10L, 1L, 2, Instant.now());
        String payload = objectMapper.writeValueAsString(event);
        OutboxEvent outboxEvent = new OutboxEvent("evt-123", "ORDER", 10L, "OrderCreated", payload);

        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(outboxEvent));

        outboxPublisher.publishPendingEvents();

        verify(kafkaTemplate, times(1)).send(eq(KafkaConfig.ORDER_CREATED_TOPIC), eq("10"), eq(payload));
        verify(outboxEventRepository, times(1)).save(outboxEvent);
        assertThat(outboxEvent.isProcessed()).isTrue();
    }

    @Test
    @DisplayName("Should do nothing when no pending outbox events exist")
    void testPublishPendingEvents_Empty() {
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(Collections.emptyList());

        outboxPublisher.publishPendingEvents();

        verifyNoInteractions(kafkaTemplate);
        verify(outboxEventRepository, never()).save(any());
    }
}
