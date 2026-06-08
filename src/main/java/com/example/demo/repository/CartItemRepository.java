package com.example.demo.repository;

import com.example.demo.models.Cart;
import com.example.demo.models.CartItem;
import com.example.demo.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    // Check if a product is already in the cart
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
}