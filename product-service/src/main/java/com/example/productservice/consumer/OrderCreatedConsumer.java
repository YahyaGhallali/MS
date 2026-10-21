package com.example.productservice.consumer;

import com.example.productservice.Product;
import com.example.productservice.ProductRepository;
import com.example.productservice.config.KafkaConfig;
import com.example.productservice.event.OrderCreatedEvent;
import com.example.productservice.event.StockRejectedEvent;
import com.example.productservice.event.StockReservedEvent;
import com.example.productservice.idempotency.ProcessedEvent;
import com.example.productservice.idempotency.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
public class OrderCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    private final ProductRepository productRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderCreatedConsumer(ProductRepository productRepository,
                                ProcessedEventRepository processedEventRepository,
                                KafkaTemplate<String, Object> kafkaTemplate,
                                ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.processedEventRepository = processedEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaConfig.ORDER_CREATED_TOPIC, groupId = "product-service-group")
    @Transactional
    public void handleOrderCreated(String message) {
        OrderCreatedEvent event;
        try {
            event = objectMapper.readValue(message, OrderCreatedEvent.class);
        } catch (Exception e) {
            log.error("[product-service] Poison message received, unable to deserialize: {}", message, e);
            throw new RuntimeException("Deserialization failed, trigger retry/DLT", e);
        }

        String eventId = event.getEventId();
        Long orderId = event.getOrderId();
        Long productId = event.getProductId();
        int quantity = event.getQuantity();

        // 1. Idempotency Check: if eventId was already processed, ignore to prevent duplicate stock deduction
        if (processedEventRepository.existsById(eventId)) {
            log.warn("[product-service] Duplicate event detected [eventId={}]. Skipping to ensure idempotency.", eventId);
            return;
        }

        // Record event as processed
        processedEventRepository.save(new ProcessedEvent(eventId));

        // 2. Find Product
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            // Business rejection: Product does not exist (not retried)
            log.warn("[product-service] Product not found [productId={}]. Rejecting stock for orderId={}.", productId, orderId);
            StockRejectedEvent rejectedEvent = new StockRejectedEvent(
                    eventId,
                    orderId,
                    productId,
                    quantity,
                    "Product with ID " + productId + " not found",
                    Instant.now()
            );
            try {
                String rejectedJson = objectMapper.writeValueAsString(rejectedEvent);
                kafkaTemplate.send(KafkaConfig.STOCK_REJECTED_TOPIC, String.valueOf(orderId), rejectedJson);
            } catch (Exception e) {
                log.error("[product-service] Failed to serialize StockRejectedEvent", e);
            }
            return;
        }

        Product product = productOpt.get();

        // 3. Check Stock
        if (product.getStock() < quantity) {
            // Business rejection: Insufficient stock (not retried)
            log.warn("[product-service] Insufficient stock for product {} [available={}, requested={}]. Rejecting stock for orderId={}.",
                    product.getName(), product.getStock(), quantity, orderId);
            StockRejectedEvent rejectedEvent = new StockRejectedEvent(
                    eventId,
                    orderId,
                    productId,
                    quantity,
                    "Insufficient stock for product " + product.getName() + ". Available: " + product.getStock() + ", Requested: " + quantity,
                    Instant.now()
            );
            try {
                String rejectedJson = objectMapper.writeValueAsString(rejectedEvent);
                kafkaTemplate.send(KafkaConfig.STOCK_REJECTED_TOPIC, String.valueOf(orderId), rejectedJson);
            } catch (Exception e) {
                log.error("[product-service] Failed to serialize StockRejectedEvent", e);
            }
            return;
        }

        // 4. Reserve stock successfully
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
        log.info("[product-service] Stock deducted for product {} [orderId={}, newStock={}]. Publishing StockReservedEvent.",
                product.getName(), orderId, product.getStock());

        StockReservedEvent reservedEvent = new StockReservedEvent(
                eventId,
                orderId,
                productId,
                quantity,
                Instant.now()
        );
        try {
            String reservedJson = objectMapper.writeValueAsString(reservedEvent);
            kafkaTemplate.send(KafkaConfig.STOCK_RESERVED_TOPIC, String.valueOf(orderId), reservedJson);
        } catch (Exception e) {
            log.error("[product-service] Failed to serialize StockReservedEvent", e);
        }
    }
}
