package com.example.demo.service;

import com.example.demo.dto.ProductRequest;
import com.example.demo.dto.ProductResponse;
import com.example.demo.models.Product;
import com.example.demo.models.Role;
import com.example.demo.models.User;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProductService productService;

    private User testSeller;
    private Product testProduct;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        testSeller = User.builder()
                .id(UUID.randomUUID())
                .name("Test Seller")
                .email("seller@example.com")
                .password("password")
                .role(Role.SELLER)
                .build();

        testProduct = Product.builder()
                .id(UUID.randomUUID())
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(100.00))
                .stock(10)
                .category("Electronics")
                .seller(testSeller)
                .build();

        productRequest = new ProductRequest();
        productRequest.setName("New Product");
        productRequest.setDescription("New Description");
        productRequest.setPrice(BigDecimal.valueOf(150.00));
        productRequest.setStock(20);
        productRequest.setCategory("Electronics");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(testSeller.getEmail());
        when(userRepository.findByEmail(testSeller.getEmail())).thenReturn(Optional.of(testSeller));
    }

    @Test
    void getAllProducts_ReturnsList() {
        when(productRepository.findAll()).thenReturn(List.of(testProduct));

        List<ProductResponse> responses = productService.getAllProducts();

        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
        assertEquals("Test Product", responses.get(0).getName());
    }

    @Test
    void createProduct_Success() {
        mockSecurityContext();
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductResponse response = productService.createProduct(productRequest);

        assertNotNull(response);
        assertEquals("Test Product", response.getName());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_Success() {
        mockSecurityContext();
        when(productRepository.existsByIdAndSeller(testProduct.getId(), testSeller)).thenReturn(true);
        when(productRepository.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductResponse response = productService.updateProduct(testProduct.getId(), productRequest);

        assertNotNull(response);
        assertEquals("New Product", testProduct.getName()); // It modified the entity
        verify(productRepository).save(testProduct);
    }

    @Test
    void deleteProduct_Success() {
        mockSecurityContext();
        when(productRepository.existsByIdAndSeller(testProduct.getId(), testSeller)).thenReturn(true);

        productService.deleteProduct(testProduct.getId());

        verify(productRepository).deleteById(testProduct.getId());
    }
}
