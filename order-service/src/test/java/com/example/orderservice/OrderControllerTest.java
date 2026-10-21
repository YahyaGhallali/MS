package com.example.orderservice;

import com.example.orderservice.outbox.OutboxEvent;
import com.example.orderservice.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OrderController orderController;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        orderController = new OrderController(orderRepository, outboxEventRepository, objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
    }

    @Test
    @DisplayName("POST /orders - Should create order with PENDING status, write OutboxEvent, and return 202 Accepted")
    void testCreateOrder_Success() throws Exception {
        Order requestOrder = new Order(1L, 2);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(100L);
            return o;
        });
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(MockMvcRequestBuilders.post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestOrder)))
                .andExpect(MockMvcResultMatchers.status().isAccepted())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", is(100)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.productId", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.quantity", is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status", is("PENDING")));

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("GET /orders - Should return all orders")
    void testGetAllOrders() throws Exception {
        Order o1 = new Order(1L, 2, 999.99, 1999.98, "CONFIRMED");
        o1.setId(1L);
        Order o2 = new Order(2L, 1, 29.99, 29.99, "PENDING");
        o2.setId(2L);

        when(orderRepository.findAll()).thenReturn(Arrays.asList(o1, o2));

        mockMvc.perform(MockMvcRequestBuilders.get("/orders"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].status", is("CONFIRMED")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].id", is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].status", is("PENDING")));

        verify(orderRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("GET /orders/{id} - Should return order when found")
    void testGetOrderById_Success() throws Exception {
        Order o1 = new Order(1L, 2, 999.99, 1999.98, "CONFIRMED");
        o1.setId(1L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(o1));

        mockMvc.perform(MockMvcRequestBuilders.get("/orders/1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status", is("CONFIRMED")));

        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("GET /orders/{id} - Should return 404 Not Found when order does not exist")
    void testGetOrderById_NotFound() throws Exception {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.get("/orders/999"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());

        verify(orderRepository, times(1)).findById(999L);
    }
}
