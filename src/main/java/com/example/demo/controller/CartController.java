package com.example.demo.controller;

import com.example.demo.dto.CartItemRequest;
import com.example.demo.dto.CartResponse;
import com.example.demo.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/buyer/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // GET /api/buyer/cart — view cart
    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        return ResponseEntity.ok(cartService.getCart());
    }

    // POST /api/buyer/cart — add item
    @PostMapping
    public ResponseEntity<CartResponse> addItem(
            @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(request));
    }

    // PUT /api/buyer/cart/{cartItemId} — update quantity
    @PutMapping("/{cartItemId}")
    public ResponseEntity<CartResponse> updateItem(
            @PathVariable UUID cartItemId,
            @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItem(cartItemId, request));
    }

    // DELETE /api/buyer/cart/{cartItemId} — remove one item
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable UUID cartItemId) {
        return ResponseEntity.ok(cartService.removeItem(cartItemId));
    }

    // DELETE /api/buyer/cart — clear entire cart
    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        cartService.clearCart();
        return ResponseEntity.noContent().build();
    }
}