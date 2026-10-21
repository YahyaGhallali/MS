package com.example.productservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductController productController;

    private ObjectMapper objectMapper;

    private Product laptop;
    private Product mouse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController).build();
        objectMapper = new ObjectMapper();

        laptop = new Product("Laptop", 999.99, 10);
        laptop.setId(1L);

        mouse = new Product("Mouse", 29.99, 50);
        mouse.setId(2L);
    }

    @Test
    @DisplayName("GET /products - Should return all products")
    void testGetAllProducts() throws Exception {
        when(productRepository.findAll()).thenReturn(Arrays.asList(laptop, mouse));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Laptop")))
                .andExpect(jsonPath("$[0].price", is(999.99)))
                .andExpect(jsonPath("$[0].stock", is(10)))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].name", is("Mouse")));

        verify(productRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("GET /products/{id} - Should return product when found")
    void testGetProductById_Success() throws Exception {
        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Laptop")))
                .andExpect(jsonPath("$.price", is(999.99)))
                .andExpect(jsonPath("$.stock", is(10)));

        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("GET /products/{id} - Should return 404 Not Found when product does not exist")
    void testGetProductById_NotFound() throws Exception {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/products/999"))
                .andExpect(status().isNotFound());

        verify(productRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("POST /products - Should create and return new product")
    void testCreateProduct() throws Exception {
        Product newProduct = new Product("Keyboard", 49.99, 25);
        Product savedProduct = new Product("Keyboard", 49.99, 25);
        savedProduct.setId(3L);

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.name", is("Keyboard")))
                .andExpect(jsonPath("$.price", is(49.99)))
                .andExpect(jsonPath("$.stock", is(25)));

        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("PUT /products/{id} - Should update and return existing product")
    void testUpdateProduct_Success() throws Exception {
        Product updatedData = new Product("Laptop Pro", 1299.99, 15);

        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Laptop Pro")))
                .andExpect(jsonPath("$.price", is(1299.99)))
                .andExpect(jsonPath("$.stock", is(15)));

        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("PUT /products/{id} - Should return 404 when updating non-existent product")
    void testUpdateProduct_NotFound() throws Exception {
        Product updatedData = new Product("NonExistent", 50.0, 10);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/products/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedData)))
                .andExpect(status().isNotFound());

        verify(productRepository, times(1)).findById(999L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("DELETE /products/{id} - Should delete existing product")
    void testDeleteProduct_Success() throws Exception {
        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));
        doNothing().when(productRepository).delete(laptop);

        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Product with ID 1 has been deleted!")));

        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).delete(laptop);
    }

    @Test
    @DisplayName("DELETE /products/{id} - Should return 404 when deleting non-existent product")
    void testDeleteProduct_NotFound() throws Exception {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/products/999"))
                .andExpect(status().isNotFound());

        verify(productRepository, times(1)).findById(999L);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    @DisplayName("POST /products/{id}/deduct-stock - Should deduct stock when available")
    void testDeductStock_Success() throws Exception {
        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/products/1/deduct-stock")
                        .param("quantity", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.stock", is(7))); // 10 - 3 = 7

        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(laptop);
    }

    @Test
    @DisplayName("POST /products/{id}/deduct-stock - Should return 400 Bad Request when stock is insufficient")
    void testDeductStock_InsufficientStock() throws Exception {
        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));

        mockMvc.perform(post("/products/1/deduct-stock")
                        .param("quantity", "999"))
                .andExpect(status().isBadRequest());

        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("POST /products/{id}/deduct-stock - Should return 404 when product not found")
    void testDeductStock_NotFound() throws Exception {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/products/999/deduct-stock")
                        .param("quantity", "2"))
                .andExpect(status().isNotFound());

        verify(productRepository, times(1)).findById(999L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("POST /products/{id}/restore-stock - Should restore stock")
    void testRestoreStock_Success() throws Exception {
        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/products/1/restore-stock")
                        .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.stock", is(15))); // 10 + 5 = 15

        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(laptop);
    }

    @Test
    @DisplayName("POST /products/{id}/restore-stock - Should return 404 when product not found")
    void testRestoreStock_NotFound() throws Exception {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/products/999/restore-stock")
                        .param("quantity", "5"))
                .andExpect(status().isNotFound());

        verify(productRepository, times(1)).findById(999L);
        verify(productRepository, never()).save(any(Product.class));
    }
}
