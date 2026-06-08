package com.example.demo.repository;

import com.example.demo.models.Product;
import com.example.demo.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    // Seller fetches only their own products
    List<Product> findBySeller(User seller);

    // Seller checks if a product belongs to them before editing/deleting
    boolean existsByIdAndSeller(UUID id, User seller);
}