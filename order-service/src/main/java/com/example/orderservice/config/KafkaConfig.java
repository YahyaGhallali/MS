package com.example.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

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
}
