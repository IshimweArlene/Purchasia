package com.example.demo.service;

import com.example.demo.dto.ProductRequest;
import com.example.demo.dto.ProductResponse;
import com.example.demo.models.Product;
import com.example.demo.models.User;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // ── Helper: get the logged-in seller from the JWT ────

    private User getCurrentSeller() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ── Helper: map Product → ProductResponse ────────────

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getImageUrl(),
                product.getSeller().getName(),
                product.getCreatedAt()
        );
    }

    // ── PUBLIC: all buyers can browse these ──────────────

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return toResponse(product);
    }

    // ── SELLER: manage their own products ────────────────

    public ProductResponse createProduct(ProductRequest request) {
        User seller = getCurrentSeller();

        Product product = Product.builder()
                .seller(seller)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .imageUrl(request.getImageUrl())
                .build();

        return toResponse(productRepository.save(product));
    }

    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        User seller = getCurrentSeller();

        // Make sure this product belongs to this seller
        if (!productRepository.existsByIdAndSeller(id, seller)) {
            throw new RuntimeException("Product not found or access denied");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(request.getImageUrl());

        return toResponse(productRepository.save(product));
    }

    public void deleteProduct(UUID id) {
        User seller = getCurrentSeller();

        if (!productRepository.existsByIdAndSeller(id, seller)) {
            throw new RuntimeException("Product not found or access denied");
        }

        productRepository.deleteById(id);
    }

    public List<ProductResponse> getMyProducts() {
        User seller = getCurrentSeller();
        return productRepository.findBySeller(seller)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}