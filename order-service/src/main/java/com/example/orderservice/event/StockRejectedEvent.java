package com.example.orderservice.event;

import java.io.Serializable;
import java.time.Instant;

public class StockRejectedEvent implements Serializable {

    private String eventId;
    private Long orderId;
    private Long productId;
    private int quantity;
    private String reason;
    private Instant timestamp;

    public StockRejectedEvent() {
    }

    public StockRejectedEvent(String eventId, Long orderId, Long productId, int quantity, String reason, Instant timestamp) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.reason = reason;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
