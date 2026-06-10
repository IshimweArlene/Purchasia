package com.example.demo.controller;

import com.example.demo.dto.OrderResponse;
import com.example.demo.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/buyer/orders")
@RequiredArgsConstructor
public class BuyerOrderController {

    private final OrderService orderService;

    // POST /api/buyer/orders — place order from cart
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder() {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderService.placeOrder());
    }

    // GET /api/buyer/orders — list all my orders
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders() {
        return ResponseEntity.ok(orderService.getMyOrders());
    }

    // GET /api/buyer/orders/{id} — single order detail
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }
}