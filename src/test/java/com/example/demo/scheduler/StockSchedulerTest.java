package com.example.demo.scheduler;

import com.example.demo.models.Product;
import com.example.demo.models.Role;
import com.example.demo.models.User;
import com.example.demo.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockSchedulerTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private StockScheduler stockScheduler;

    private User testSeller;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Set the value of the @Value annotation
        ReflectionTestUtils.setField(stockScheduler, "lowStockThreshold", 10);

        testSeller = User.builder()
                .id(UUID.randomUUID())
                .name("Test Seller")
                .email("seller@example.com")
                .password("password")
                .role(Role.SELLER)
                .build();

        testProduct = Product.builder()
                .id(UUID.randomUUID())
                .name("Low Stock Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(100.00))
                .stock(5) // Below threshold of 10
                .category("Electronics")
                .seller(testSeller)
                .build();
    }

    @Test
    void checkLowStock_WithLowStockProducts_ExecutesSuccessfully() {
        when(productRepository.findByStockLessThanEqual(10)).thenReturn(List.of(testProduct));

        // Execute the scheduled method
        stockScheduler.checkLowStock();

        // Verify that the repository was queried
        verify(productRepository).findByStockLessThanEqual(10);
    }

    @Test
    void checkLowStock_WithNoLowStockProducts_ExecutesSuccessfully() {
        when(productRepository.findByStockLessThanEqual(10)).thenReturn(Collections.emptyList());

        // Execute the scheduled method
        stockScheduler.checkLowStock();

        // Verify that the repository was queried
        verify(productRepository).findByStockLessThanEqual(10);
    }

    @Test
    void checkLowStockHourly_WithLowStockProducts_ExecutesSuccessfully() {
        when(productRepository.findByStockLessThanEqual(10)).thenReturn(List.of(testProduct));

        // Execute the scheduled method
        stockScheduler.checkLowStockHourly();

        // Verify that the repository was queried
        verify(productRepository).findByStockLessThanEqual(10);
    }
}
