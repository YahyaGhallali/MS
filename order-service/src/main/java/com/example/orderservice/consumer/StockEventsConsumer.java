package com.example.orderservice.consumer;

import com.example.orderservice.Order;
import com.example.orderservice.OrderRepository;
import com.example.orderservice.config.KafkaConfig;
import com.example.orderservice.event.StockRejectedEvent;
import com.example.orderservice.event.StockReservedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StockEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockEventsConsumer.class);

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public StockEventsConsumer(OrderRepository orderRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaConfig.STOCK_RESERVED_TOPIC, groupId = "order-service-group")
    @Transactional
    public void handleStockReserved(String message) {
        try {
            StockReservedEvent event = objectMapper.readValue(message, StockReservedEvent.class);
            log.info("[Order Service] Received StockReservedEvent for order ID: {}", event.getOrderId());

            orderRepository.findById(event.getOrderId()).ifPresent(order -> {
                order.setStatus("CONFIRMED");
                orderRepository.save(order);
                log.info("[Order Service] Order ID {} status updated to CONFIRMED", order.getId());
            });
        } catch (Exception e) {
            log.error("[Order Service] Failed to process StockReserved message: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaConfig.STOCK_REJECTED_TOPIC, groupId = "order-service-group")
    @Transactional
    public void handleStockRejected(String message) {
        try {
            StockRejectedEvent event = objectMapper.readValue(message, StockRejectedEvent.class);
            log.info("[Order Service] Received StockRejectedEvent for order ID: {}, Reason: {}",
                    event.getOrderId(), event.getReason());

            orderRepository.findById(event.getOrderId()).ifPresent(order -> {
                order.setStatus("REJECTED");
                orderRepository.save(order);
                log.info("[Order Service] Order ID {} status updated to REJECTED", order.getId());
            });
        } catch (Exception e) {
            log.error("[Order Service] Failed to process StockRejected message: {}", e.getMessage(), e);
        }
    }
}
