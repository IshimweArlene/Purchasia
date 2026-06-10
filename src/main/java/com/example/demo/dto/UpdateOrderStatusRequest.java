package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import com.example.demo.models.OrderStatus;

@Data
public class UpdateOrderStatusRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;
}