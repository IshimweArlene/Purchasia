package com.example.demo.scheduler;

import com.example.demo.dto.LowStockAlert;
import com.example.demo.models.Product;
import com.example.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockScheduler {

    private final ProductRepository productRepository;

    @Value("${app.stock.low-threshold}")
    private int lowStockThreshold;

    // ── Runs every day at 8:00 AM ────────────────────────

    @Scheduled(cron = "0 0 8 * * *")
    public void checkLowStock() {
        log.info("Running low stock check at {}", LocalDateTime.now());

        List<Product> lowStockProducts = productRepository
                .findByStockLessThanEqual(lowStockThreshold);

        if (lowStockProducts.isEmpty()) {
            log.info("Low stock check complete — all products have sufficient stock");
            return;
        }

        // Build alert list
        List<LowStockAlert> alerts = lowStockProducts.stream()
                .map(product -> new LowStockAlert(
                        product.getId(),
                        product.getName(),
                        product.getSeller().getEmail(),
                        product.getSeller().getName(),
                        product.getStock()
                ))
                .collect(Collectors.toList());

        // Log each alert — replace this with email sending later
        log.warn("LOW STOCK ALERT — {} product(s) need restocking:", alerts.size());
        alerts.forEach(alert ->
                log.warn("  → [{}] '{}' has only {} unit(s) left — seller: {} ({})",
                        alert.getProductId(),
                        alert.getProductName(),
                        alert.getCurrentStock(),
                        alert.getSellerName(),
                        alert.getSellerEmail()
                )
        );

        log.info("Low stock check complete — {} alert(s) generated", alerts.size());
    }

    // ── Runs every hour — useful during development ──────

    @Scheduled(fixedRate = 3600000)
    public void checkLowStockHourly() {
        log.debug("Hourly stock check running at {}", LocalDateTime.now());

        List<Product> lowStockProducts = productRepository
                .findByStockLessThanEqual(lowStockThreshold);

        if (!lowStockProducts.isEmpty()) {
            log.warn("Hourly check — {} product(s) below stock threshold of {}",
                    lowStockProducts.size(), lowStockThreshold);
        }
    }
}