package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class LowStockAlert {
    private UUID productId;
    private String productName;
    private String sellerEmail;
    private String sellerName;
    private Integer currentStock;
}