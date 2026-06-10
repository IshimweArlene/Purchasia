package com.example.demo.service;

import com.example.demo.dto.OrderItemResponse;
import com.example.demo.dto.OrderResponse;
import com.example.demo.dto.UpdateOrderStatusRequest;
import com.example.demo.models.*;
import com.example.demo.repository.*;
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
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // ── Helper: get logged-in user ───────────────────────

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ── Helper: map OrderItem → OrderItemResponse ────────

    private OrderItemResponse toItemResponse(OrderItem item) {
        BigDecimal subtotal = item.getPriceAtPurchase()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getPriceAtPurchase(),
                subtotal
        );
    }

    // ── Helper: map Order → OrderResponse ────────────────

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems()
                .stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getBuyer().getName(),
                items,
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getOrderedAt()
        );
    }

    // ── BUYER: place order from cart ─────────────────────

    @Transactional
    public OrderResponse placeOrder() {
        User buyer = getCurrentUser();

        // Get buyer's cart
        Cart cart = cartRepository.findByBuyer(buyer)
                .orElseThrow(() -> new RuntimeException("Cart is empty"));

        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Cannot place order with empty cart");
        }

        // Calculate total and build order items
        BigDecimal total = BigDecimal.ZERO;

        Order order = Order.builder()
                .buyer(buyer)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        // Save the order first to get an ID
        Order savedOrder = orderRepository.save(order);

        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();

            // Check stock is still available
            if (product.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException(
                        "Not enough stock for: " + product.getName());
            }

            // Deduct stock
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            // Snapshot the price at purchase time
            BigDecimal priceAtPurchase = product.getPrice();
            BigDecimal subtotal = priceAtPurchase
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            total = total.add(subtotal);

            // Build order item
            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .priceAtPurchase(priceAtPurchase)
                    .build();

            savedOrder.getOrderItems().add(orderItem);
        }

        // Update the total on the order
        savedOrder.setTotalAmount(total);
        orderRepository.save(savedOrder);

        // Clear the cart after successful order
        cart.getCartItems().clear();
        cartRepository.save(cart);

        return toOrderResponse(savedOrder);
    }

    // ── BUYER: view their orders ─────────────────────────

    public List<OrderResponse> getMyOrders() {
        User buyer = getCurrentUser();
        return orderRepository.findByBuyer(buyer)
                .stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    // ── BUYER: view single order ─────────────────────────

    public OrderResponse getOrderById(UUID orderId) {
        User buyer = getCurrentUser();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Make sure this order belongs to this buyer
        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new RuntimeException("Access denied");
        }

        return toOrderResponse(order);
    }

    // ── SELLER: view orders containing their products ────

    public List<OrderResponse> getSellerOrders() {
        User seller = getCurrentUser();
        return orderRepository.findOrdersBySeller(seller)
                .stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    // ── SELLER: update order status ──────────────────────

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId,
                                           UpdateOrderStatusRequest request) {
        User seller = getCurrentUser();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Verify this order contains at least one of the seller's products
        boolean sellerOwnsItem = order.getOrderItems().stream()
                .anyMatch(item ->
                        item.getProduct().getSeller().getId()
                                .equals(seller.getId()));

        if (!sellerOwnsItem) {
            throw new RuntimeException("Access denied");
        }

        order.setStatus(request.getStatus());
        return toOrderResponse(orderRepository.save(order));
    }
}