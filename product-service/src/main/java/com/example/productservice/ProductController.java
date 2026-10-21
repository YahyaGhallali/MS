package com.example.productservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductRepository productRepository;

    @Value("${server.port:8081}")
    private String serverPort;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    private String getInstanceId() {
        String host = System.getenv("HOSTNAME");
        return (host != null && !host.isBlank()) ? host : "port-" + serverPort;
    }

    // 1. Get all products -> GET http://localhost:8081/products
    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // 2. Get a single product by ID -> GET http://localhost:8081/products/1
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }

    // 3. Create a new product -> POST http://localhost:8081/products
    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }

    // 4. Update an existing product -> PUT http://localhost:8081/products/1
    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product updatedProduct) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        product.setName(updatedProduct.getName());
        product.setPrice(updatedProduct.getPrice());
        product.setStock(updatedProduct.getStock());

        return productRepository.save(product);
    }

    // 5. Delete a product -> DELETE http://localhost:8081/products/1
    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        productRepository.delete(product);
        return "Product with ID " + id + " has been deleted!";
    }

    // 6. Deduct stock atomically -> POST http://localhost:8081/products/1/deduct-stock?quantity=2
    @PostMapping("/{id}/deduct-stock")
    public Product deductStock(@PathVariable Long id, @RequestParam int quantity) {
        log.info("[product-service:{}] Deducting stock for product ID {}, quantity {}", getInstanceId(), id, quantity);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        if (product.getStock() < quantity) {
            throw new InsufficientStockException("Insufficient stock for product: " + product.getName()
                    + ". Available: " + product.getStock() + ", Requested: " + quantity);
        }

        product.setStock(product.getStock() - quantity);
        return productRepository.save(product);
    }

    // 7. Compensating transaction: Restore stock -> POST http://localhost:8081/products/1/restore-stock?quantity=2
    @PostMapping("/{id}/restore-stock")
    public Product restoreStock(@PathVariable Long id, @RequestParam int quantity) {
        log.info("[product-service:{}] Restoring stock for product ID {}, quantity {}", getInstanceId(), id, quantity);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        product.setStock(product.getStock() + quantity);
        return productRepository.save(product);
    }
}
