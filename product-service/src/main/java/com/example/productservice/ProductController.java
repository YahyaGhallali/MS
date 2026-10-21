package com.example.productservice;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
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
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        if (product.getStock() < quantity) {
            throw new InsufficientStockException("Insufficient stock for product: " + product.getName()
                    + ". Available: " + product.getStock() + ", Requested: " + quantity);
        }

        product.setStock(product.getStock() - quantity);
        return productRepository.save(product);
    }
}
