package com.example.orderservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    @DisplayName("Should create Order with 2-parameter constructor")
    void testTwoParamConstructor() {
        Order order = new Order(5L, 3);
        assertThat(order.getProductId()).isEqualTo(5L);
        assertThat(order.getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should create Order with full constructor and verify getters")
    void testFullConstructor() {
        Order order = new Order(2L, 4, 25.0, 100.0, "CONFIRMED");
        order.setId(10L);

        assertThat(order.getId()).isEqualTo(10L);
        assertThat(order.getProductId()).isEqualTo(2L);
        assertThat(order.getQuantity()).isEqualTo(4);
        assertThat(order.getUnitPrice()).isEqualTo(25.0);
        assertThat(order.getTotalPrice()).isEqualTo(100.0);
        assertThat(order.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("Should create Order with default constructor and test setters")
    void testSetters() {
        Order order = new Order();
        order.setId(20L);
        order.setProductId(1L);
        order.setQuantity(2);
        order.setUnitPrice(50.0);
        order.setTotalPrice(100.0);
        order.setStatus("CREATED");

        assertThat(order.getId()).isEqualTo(20L);
        assertThat(order.getProductId()).isEqualTo(1L);
        assertThat(order.getQuantity()).isEqualTo(2);
        assertThat(order.getUnitPrice()).isEqualTo(50.0);
        assertThat(order.getTotalPrice()).isEqualTo(100.0);
        assertThat(order.getStatus()).isEqualTo("CREATED");
    }
}
