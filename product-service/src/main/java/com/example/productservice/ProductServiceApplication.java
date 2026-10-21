package com.example.productservice;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }

    // Add sample products on startup so you can test GET /products right away in your browser
    @Bean
    public CommandLineRunner initData(ProductRepository productRepository) {
        return args -> {
            productRepository.save(new Product("Laptop", 999.99, 10));
            productRepository.save(new Product("Mouse", 29.99, 50));
            productRepository.save(new Product("Keyboard", 49.99, 25));
        };
    }
}
