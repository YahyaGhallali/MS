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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCreatedConsumerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ObjectMapper objectMapper;
    private OrderCreatedConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        consumer = new OrderCreatedConsumer(productRepository, processedEventRepository, kafkaTemplate, objectMapper);
    }

    @Test
    @DisplayName("handleOrderCreated - Should deduct stock and publish StockReservedEvent on success")
    void testHandleOrderCreated_Success() throws Exception {
        OrderCreatedEvent event = new OrderCreatedEvent("evt-100", 1L, 10L, 2, Instant.now());
        String json = objectMapper.writeValueAsString(event);

        Product product = new Product("Laptop", 999.99, 10);
        product.setId(10L);

        when(processedEventRepository.existsById("evt-100")).thenReturn(false);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        consumer.handleOrderCreated(json);

        assertThat(product.getStock()).isEqualTo(8); // 10 - 2
        verify(productRepository, times(1)).save(product);
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
        verify(kafkaTemplate, times(1)).send(eq(KafkaConfig.STOCK_RESERVED_TOPIC), eq("1"), any(StockReservedEvent.class));
    }

    @Test
    @DisplayName("handleOrderCreated - Should be idempotent and skip duplicate events without changing stock")
    void testHandleOrderCreated_Idempotency() throws Exception {
        OrderCreatedEvent event = new OrderCreatedEvent("evt-duplicate", 1L, 10L, 2, Instant.now());
        String json = objectMapper.writeValueAsString(event);

        when(processedEventRepository.existsById("evt-duplicate")).thenReturn(true);

        consumer.handleOrderCreated(json);

        verify(productRepository, never()).findById(any());
        verify(productRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("handleOrderCreated - Should publish StockRejectedEvent when stock is insufficient (business rejection)")
    void testHandleOrderCreated_InsufficientStock() throws Exception {
        OrderCreatedEvent event = new OrderCreatedEvent("evt-insufficient", 2L, 10L, 999, Instant.now());
        String json = objectMapper.writeValueAsString(event);

        Product product = new Product("Laptop", 999.99, 5);
        product.setId(10L);

        when(processedEventRepository.existsById("evt-insufficient")).thenReturn(false);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        consumer.handleOrderCreated(json);

        assertThat(product.getStock()).isEqualTo(5); // unchanged
        verify(productRepository, never()).save(product);
        verify(kafkaTemplate, times(1)).send(eq(KafkaConfig.STOCK_REJECTED_TOPIC), eq("2"), any(StockRejectedEvent.class));
    }

    @Test
    @DisplayName("handleOrderCreated - Should publish StockRejectedEvent when product is not found")
    void testHandleOrderCreated_ProductNotFound() throws Exception {
        OrderCreatedEvent event = new OrderCreatedEvent("evt-notfound", 3L, 999L, 1, Instant.now());
        String json = objectMapper.writeValueAsString(event);

        when(processedEventRepository.existsById("evt-notfound")).thenReturn(false);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        consumer.handleOrderCreated(json);

        verify(productRepository, never()).save(any());
        verify(kafkaTemplate, times(1)).send(eq(KafkaConfig.STOCK_REJECTED_TOPIC), eq("3"), any(StockRejectedEvent.class));
    }
}
