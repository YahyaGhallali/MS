package com.example.productservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    @Test
    @DisplayName("Should create product with parameterized constructor and verify getters")
    void testParameterizedConstructorAndGetters() {
        Product product = new Product("Monitor", 299.99, 15);
        product.setId(10L);

        assertThat(product.getId()).isEqualTo(10L);
        assertThat(product.getName()).isEqualTo("Monitor");
        assertThat(product.getPrice()).isEqualTo(299.99);
        assertThat(product.getStock()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should create product with default constructor and verify setters")
    void testDefaultConstructorAndSetters() {
        Product product = new Product();
        product.setId(20L);
        product.setName("Headphones");
        product.setPrice(79.99);
        product.setStock(40);

        assertThat(product.getId()).isEqualTo(20L);
        assertThat(product.getName()).isEqualTo("Headphones");
        assertThat(product.getPrice()).isEqualTo(79.99);
        assertThat(product.getStock()).isEqualTo(40);
    }
}
