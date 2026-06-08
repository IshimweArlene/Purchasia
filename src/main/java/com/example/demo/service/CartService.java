package com.example.demo.service;

import com.example.demo.dto.CartItemRequest;
import com.example.demo.dto.CartItemResponse;
import com.example.demo.dto.CartResponse;
import com.example.demo.models.Cart;
import com.example.demo.models.CartItem;
import com.example.demo.models.Product;
import com.example.demo.models.User;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // ── Helper: get logged-in buyer ──────────────────────

    private User getCurrentBuyer() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ── Helper: get or create cart for buyer ─────────────

    private Cart getOrCreateCart(User buyer) {
        return cartRepository.findByBuyer(buyer)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().buyer(buyer).build()
                ));
    }

    // ── Helper: map CartItem → CartItemResponse ──────────

    private CartItemResponse toItemResponse(CartItem item) {
        BigDecimal subtotal = item.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getPrice(),
                item.getQuantity(),
                subtotal
        );
    }

    // ── Helper: map Cart → CartResponse ──────────────────

    private CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> items = cart.getCartItems()
                .stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        BigDecimal total = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getId(), items, total);
    }

    // ── View cart ────────────────────────────────────────

    public CartResponse getCart() {
        User buyer = getCurrentBuyer();
        Cart cart = getOrCreateCart(buyer);
        return toCartResponse(cart);
    }

    // ── Add item to cart ─────────────────────────────────

    @Transactional
    public CartResponse addItem(CartItemRequest request) {
        User buyer = getCurrentBuyer();
        Cart cart = getOrCreateCart(buyer);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Check stock
        if (product.getStock() < request.getQuantity()) {
            throw new RuntimeException("Not enough stock available");
        }

        // If product already in cart — increase quantity
        // If not — add a new cart item
        CartItem cartItem = cartItemRepository
                .findByCartAndProduct(cart, product)
                .map(existing -> {
                    existing.setQuantity(
                            existing.getQuantity() + request.getQuantity());
                    return existing;
                })
                .orElseGet(() -> CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .quantity(request.getQuantity())
                        .build());

        cartItemRepository.save(cartItem);

        // Refresh cart from DB to get updated items
        Cart updatedCart = cartRepository.findByBuyer(buyer)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        return toCartResponse(updatedCart);
    }

    // ── Update item quantity ─────────────────────────────

    @Transactional
    public CartResponse updateItem(UUID cartItemId, CartItemRequest request) {
        User buyer = getCurrentBuyer();
        Cart cart = getOrCreateCart(buyer);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        // Make sure this cart item belongs to this buyer's cart
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Access denied");
        }

        // Check stock
        if (cartItem.getProduct().getStock() < request.getQuantity()) {
            throw new RuntimeException("Not enough stock available");
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        Cart updatedCart = cartRepository.findByBuyer(buyer)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        return toCartResponse(updatedCart);
    }

    // ── Remove item from cart ────────────────────────────

    @Transactional
    public CartResponse removeItem(UUID cartItemId) {
        User buyer = getCurrentBuyer();
        Cart cart = getOrCreateCart(buyer);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        // Make sure this cart item belongs to this buyer's cart
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Access denied");
        }

        cartItemRepository.delete(cartItem);

        Cart updatedCart = cartRepository.findByBuyer(buyer)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        return toCartResponse(updatedCart);
    }

    // ── Clear entire cart ────────────────────────────────

    @Transactional
    public void clearCart() {
        User buyer = getCurrentBuyer();
        Cart cart = getOrCreateCart(buyer);
        cart.getCartItems().clear();
        cartRepository.save(cart);
    }
}