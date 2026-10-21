package com.example.productservice.event;

import java.io.Serializable;
import java.time.Instant;

public class StockReservedEvent implements Serializable {

    private String eventId;
    private Long orderId;
    private Long productId;
    private int quantity;
    private Instant timestamp;

    public StockReservedEvent() {
    }

    public StockReservedEvent(String eventId, Long orderId, Long productId, int quantity, Instant timestamp) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
