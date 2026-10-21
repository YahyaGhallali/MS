package com.example.orderservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderRepository orderRepository;

    private MockRestServiceServer mockServer;
    private OrderController orderController;
    private ObjectMapper objectMapper;

    private static final String PRODUCT_SERVICE_URL = "http://product-service";

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        orderController = new OrderController(orderRepository, restClient);
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /orders - Should successfully place order when stock is deducted")
    void testCreateOrder_Success() throws Exception {
        Order requestOrder = new Order(1L, 2);

        ProductDTO productDTO = new ProductDTO(1L, "Laptop", 999.99, 8);

        // Expect deduct-stock call from product-service
        mockServer.expect(MockRestRequestMatchers.requestTo(PRODUCT_SERVICE_URL + "/products/1/deduct-stock?quantity=2"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess(objectMapper.writeValueAsString(productDTO), MediaType.APPLICATION_JSON));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(100L);
            return o;
        });

        mockMvc.perform(MockMvcRequestBuilders.post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestOrder)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", is(100)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.productId", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.quantity", is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.unitPrice", is(999.99)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalPrice", is(1999.98)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status", is("CREATED")));

        mockServer.verify();
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("POST /orders - Should preserve custom status if provided in request")
    void testCreateOrder_PreservesCustomStatus() throws Exception {
        Order requestOrder = new Order(1L, 1, 999.99, 999.99, "CUSTOM_STATUS");

        ProductDTO productDTO = new ProductDTO(1L, "Laptop", 999.99, 9);

        mockServer.expect(MockRestRequestMatchers.requestTo(PRODUCT_SERVICE_URL + "/products/1/deduct-stock?quantity=1"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess(objectMapper.writeValueAsString(productDTO), MediaType.APPLICATION_JSON));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(101L);
            return o;
        });

        mockMvc.perform(MockMvcRequestBuilders.post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestOrder)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status", is("CUSTOM_STATUS")));

        mockServer.verify();
    }

    @Test
    @DisplayName("POST /orders - Should return 400 Bad Request when product-service reports insufficient stock")
    void testCreateOrder_InsufficientStock() throws Exception {
        Order requestOrder = new Order(1L, 999);

        mockServer.expect(MockRestRequestMatchers.requestTo(PRODUCT_SERVICE_URL + "/products/1/deduct-stock?quantity=999"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withBadRequest());

        mockMvc.perform(MockMvcRequestBuilders.post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestOrder)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        mockServer.verify();
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("POST /orders - Should return 404 Not Found when product does not exist")
    void testCreateOrder_ProductNotFound() throws Exception {
        Order requestOrder = new Order(999L, 1);

        mockServer.expect(MockRestRequestMatchers.requestTo(PRODUCT_SERVICE_URL + "/products/999/deduct-stock?quantity=1"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withRawStatus(404));

        mockMvc.perform(MockMvcRequestBuilders.post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestOrder)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());

        mockServer.verify();
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("POST /orders - Should execute compensating transaction (restore stock) when local database save fails")
    void testCreateOrder_CompensatingTransactionOnSaveFailure() throws Exception {
        Order requestOrder = new Order(1L, 2);
        ProductDTO productDTO = new ProductDTO(1L, "Laptop", 999.99, 8);

        // 1. Stock deduction succeeds
        mockServer.expect(MockRestRequestMatchers.requestTo(PRODUCT_SERVICE_URL + "/products/1/deduct-stock?quantity=2"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess(objectMapper.writeValueAsString(productDTO), MediaType.APPLICATION_JSON));

        // 2. Compensating transaction is expected because local save will fail
        mockServer.expect(MockRestRequestMatchers.requestTo(PRODUCT_SERVICE_URL + "/products/1/restore-stock?quantity=2"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess());

        // Local database save throws an exception
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("Database error: Disk full or connection lost"));

        mockMvc.perform(MockMvcRequestBuilders.post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestOrder)))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError());

        // Verify that compensating call was executed
        mockServer.verify();
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("GET /orders - Should return all orders")
    void testGetAllOrders() throws Exception {
        Order o1 = new Order(1L, 2, 999.99, 1999.98, "CREATED");
        o1.setId(1L);
        Order o2 = new Order(2L, 1, 29.99, 29.99, "CREATED");
        o2.setId(2L);

        when(orderRepository.findAll()).thenReturn(Arrays.asList(o1, o2));

        mockMvc.perform(MockMvcRequestBuilders.get("/orders"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].productId", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].totalPrice", is(1999.98)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].id", is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].productId", is(2)));

        verify(orderRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("GET /orders/{id} - Should return order when found")
    void testGetOrderById_Success() throws Exception {
        Order o1 = new Order(1L, 2, 999.99, 1999.98, "CREATED");
        o1.setId(1L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(o1));

        mockMvc.perform(MockMvcRequestBuilders.get("/orders/1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.productId", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.quantity", is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.unitPrice", is(999.99)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalPrice", is(1999.98)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status", is("CREATED")));

        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("GET /orders/{id} - Should return null/empty when not found")
    void testGetOrderById_NotFound() throws Exception {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.get("/orders/999"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(""));

        verify(orderRepository, times(1)).findById(999L);
    }
}
