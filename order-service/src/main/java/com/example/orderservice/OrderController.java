package com.example.orderservice;

import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.outbox.OutboxEvent;
import com.example.orderservice.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderController(OrderRepository orderRepository,
                           OutboxEventRepository outboxEventRepository,
                           ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    // 1. Create Order -> POST /orders (Returns 202 Accepted, writes Order + OutboxEvent atomically)
    @PostMapping
    @Transactional
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        // Set initial status to PENDING
        order.setStatus("PENDING");

        // Save order to database
        Order savedOrder = orderRepository.save(order);

        // Generate unique event ID
        String eventId = UUID.randomUUID().toString();

        // Build OrderCreatedEvent contract
        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId,
                savedOrder.getId(),
                savedOrder.getProductId(),
                savedOrder.getQuantity(),
                Instant.now()
        );

        // Transactional Outbox Pattern: Save event to outbox table within the SAME DB transaction
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = new OutboxEvent(
                    eventId,
                    "ORDER",
                    savedOrder.getId(),
                    "OrderCreated",
                    payload
            );
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize OrderCreatedEvent to outbox table", e);
        }

        // Return 202 Accepted so client can poll GET /orders/{id}
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(savedOrder);
    }

    // 2. Get all orders -> GET /orders
    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // 3. Get single order by ID -> GET /orders/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
