package com.example.orderservice.consumer;

import com.example.orderservice.Order;
import com.example.orderservice.OrderRepository;
import com.example.orderservice.event.StockRejectedEvent;
import com.example.orderservice.event.StockReservedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockEventsConsumerTest {

    @Mock
    private OrderRepository orderRepository;

    private ObjectMapper objectMapper;
    private StockEventsConsumer stockEventsConsumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        stockEventsConsumer = new StockEventsConsumer(orderRepository, objectMapper);
    }

    @Test
    @DisplayName("handleStockReserved - Should update order status to CONFIRMED")
    void testHandleStockReserved() throws Exception {
        StockReservedEvent event = new StockReservedEvent("evt-1", 100L, 1L, 2, Instant.now());
        String json = objectMapper.writeValueAsString(event);

        Order order = new Order(1L, 2);
        order.setId(100L);
        order.setStatus("PENDING");

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        stockEventsConsumer.handleStockReserved(json);

        assertThat(order.getStatus()).isEqualTo("CONFIRMED");
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("handleStockRejected - Should update order status to REJECTED")
    void testHandleStockRejected() throws Exception {
        StockRejectedEvent event = new StockRejectedEvent("evt-2", 101L, 1L, 999, "Insufficient stock", Instant.now());
        String json = objectMapper.writeValueAsString(event);

        Order order = new Order(1L, 999);
        order.setId(101L);
        order.setStatus("PENDING");

        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));

        stockEventsConsumer.handleStockRejected(json);

        assertThat(order.getStatus()).isEqualTo("REJECTED");
        verify(orderRepository, times(1)).save(order);
    }
}
