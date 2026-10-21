package com.example.productservice.config;

import com.example.productservice.InsufficientStockException;
import com.example.productservice.ProductNotFoundException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConfig {

    public static final String ORDER_CREATED_TOPIC = "order-created-topic";
    public static final String ORDER_CREATED_DLT = "order-created-topic.DLT";
    public static final String STOCK_RESERVED_TOPIC = "stock-reserved-topic";
    public static final String STOCK_REJECTED_TOPIC = "stock-rejected-topic";

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(ORDER_CREATED_TOPIC)
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCreatedDlt() {
        return TopicBuilder.name(ORDER_CREATED_DLT)
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic stockReservedTopic() {
        return TopicBuilder.name(STOCK_RESERVED_TOPIC)
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic stockRejectedTopic() {
        return TopicBuilder.name(STOCK_REJECTED_TOPIC)
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public RecordMessageConverter jsonConverter() {
        return new StringJsonMessageConverter();
    }

    // Task 6: Retries with exponential backoff, routing exhausted poison messages to DLT
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        // Exponential backoff: initial 1s, multiplier 2.0, max 3 attempts
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(3);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // Business exceptions are not retried or sent to DLT (handled as domain rejection)
        errorHandler.addNotRetryableExceptions(InsufficientStockException.class, ProductNotFoundException.class);

        return errorHandler;
    }
}
