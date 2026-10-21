package com.example.orderservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionsTest {

    @Test
    @DisplayName("ProductNotFoundException should carry correct message and 404 ResponseStatus")
    void testProductNotFoundException() {
        ProductNotFoundException ex = new ProductNotFoundException("Product with ID 99 not found");
        assertThat(ex.getMessage()).isEqualTo("Product with ID 99 not found");

        ResponseStatus status = ProductNotFoundException.class.getAnnotation(ResponseStatus.class);
        assertThat(status).isNotNull();
        assertThat(status.value()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("InsufficientStockException should carry correct message and 400 ResponseStatus")
    void testInsufficientStockException() {
        InsufficientStockException ex = new InsufficientStockException("Cannot place order: Insufficient stock");
        assertThat(ex.getMessage()).isEqualTo("Cannot place order: Insufficient stock");

        ResponseStatus status = InsufficientStockException.class.getAnnotation(ResponseStatus.class);
        assertThat(status).isNotNull();
        assertThat(status.value()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("OrderProcessingException should carry correct message, cause, and 500 ResponseStatus")
    void testOrderProcessingException() {
        Throwable cause = new RuntimeException("Database error");
        OrderProcessingException ex = new OrderProcessingException("Order save failed", cause);

        assertThat(ex.getMessage()).isEqualTo("Order save failed");
        assertThat(ex.getCause()).isEqualTo(cause);

        OrderProcessingException ex2 = new OrderProcessingException("Order save failed without cause");
        assertThat(ex2.getMessage()).isEqualTo("Order save failed without cause");

        ResponseStatus status = OrderProcessingException.class.getAnnotation(ResponseStatus.class);
        assertThat(status).isNotNull();
        assertThat(status.value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
