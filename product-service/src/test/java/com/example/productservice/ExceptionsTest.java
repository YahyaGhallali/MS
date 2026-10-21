package com.example.productservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionsTest {

    @Test
    @DisplayName("ProductNotFoundException should carry correct message and 404 ResponseStatus")
    void testProductNotFoundException() {
        ProductNotFoundException ex = new ProductNotFoundException("Product not found with id: 123");
        assertThat(ex.getMessage()).isEqualTo("Product not found with id: 123");

        ResponseStatus status = ProductNotFoundException.class.getAnnotation(ResponseStatus.class);
        assertThat(status).isNotNull();
        assertThat(status.value()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("InsufficientStockException should carry correct message and 400 ResponseStatus")
    void testInsufficientStockException() {
        InsufficientStockException ex = new InsufficientStockException("Insufficient stock");
        assertThat(ex.getMessage()).isEqualTo("Insufficient stock");

        ResponseStatus status = InsufficientStockException.class.getAnnotation(ResponseStatus.class);
        assertThat(status).isNotNull();
        assertThat(status.value()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
