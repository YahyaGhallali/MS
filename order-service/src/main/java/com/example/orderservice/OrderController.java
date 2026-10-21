package com.example.orderservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final RestClient restClient;
    private final String productServiceUrl;

    public OrderController(OrderRepository orderRepository,
                           RestClient restClient,
                           @Value("${product.service.url:http://localhost:8081}") String productServiceUrl) {
        this.orderRepository = orderRepository;
        this.restClient = restClient;
        this.productServiceUrl = productServiceUrl.endsWith("/")
                ? productServiceUrl.substring(0, productServiceUrl.length() - 1)
                : productServiceUrl;
    }

    // 1. Create Order -> POST http://localhost:8082/orders
    @PostMapping
    public Order createOrder(@RequestBody Order order) {
        ProductDTO product;

        try {
            // Deduct stock atomically in product-service (returns updated Product with price)
            product = restClient.post()
                    .uri(productServiceUrl + "/products/{id}/deduct-stock?quantity={qty}",
                            order.getProductId(), order.getQuantity())
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new ProductNotFoundException("Product with ID " + order.getProductId() + " not found in product-service!");
                    })
                    .onStatus(status -> status.value() == 400, (request, response) -> {
                        throw new InsufficientStockException("Cannot place order: Insufficient stock for product ID " + order.getProductId());
                    })
                    .body(ProductDTO.class);

        } catch (HttpClientErrorException.NotFound e) {
            throw new ProductNotFoundException("Product with ID " + order.getProductId() + " not found in product-service!");
        } catch (HttpClientErrorException.BadRequest e) {
            throw new InsufficientStockException("Cannot place order: Insufficient stock for product ID " + order.getProductId());
        }

        if (product == null) {
            throw new ProductNotFoundException("Product with ID " + order.getProductId() + " not found in product-service!");
        }

        // Copy price into unitPrice and compute totalPrice
        order.setUnitPrice(product.getPrice());
        order.setTotalPrice(product.getPrice() * order.getQuantity());

        // Default status if not provided
        if (order.getStatus() == null || order.getStatus().isBlank()) {
            order.setStatus("CREATED");
        }

        // Save order to H2 database with Compensating Transaction (Saga pattern)
        try {
            return orderRepository.save(order);
        } catch (Exception ex) {
            // Compensating Transaction: If local save fails, restore the previously deducted stock in product-service
            compensateStock(order.getProductId(), order.getQuantity());
            throw new OrderProcessingException(
                    "Failed to save order locally. Compensating transaction executed: Stock for product ID "
                            + order.getProductId() + " has been restored.", ex);
        }
    }

    private void compensateStock(Long productId, int quantity) {
        try {
            restClient.post()
                    .uri(productServiceUrl + "/products/{id}/restore-stock?quantity={qty}", productId, quantity)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            System.err.println("CRITICAL: Failed to compensate stock for product ID " + productId + ": " + e.getMessage());
        }
    }

    // 2. Get all orders -> GET http://localhost:8082/orders
    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // 3. Get single order by ID -> GET http://localhost:8082/orders/1
    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable Long id) {
        return orderRepository.findById(id).orElse(null);
    }
}
