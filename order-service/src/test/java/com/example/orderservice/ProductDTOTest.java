package com.example.orderservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDTOTest {

    @Test
    @DisplayName("Should create ProductDTO with parameterized constructor and verify getters")
    void testParameterizedConstructorAndGetters() {
        ProductDTO dto = new ProductDTO(1L, "Laptop", 999.99, 10);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Laptop");
        assertThat(dto.getPrice()).isEqualTo(999.99);
        assertThat(dto.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should create ProductDTO with default constructor and test setters")
    void testDefaultConstructorAndSetters() {
        ProductDTO dto = new ProductDTO();
        dto.setId(2L);
        dto.setName("Mouse");
        dto.setPrice(29.99);
        dto.setStock(50);

        assertThat(dto.getId()).isEqualTo(2L);
        assertThat(dto.getName()).isEqualTo("Mouse");
        assertThat(dto.getPrice()).isEqualTo(29.99);
        assertThat(dto.getStock()).isEqualTo(50);
    }
}
