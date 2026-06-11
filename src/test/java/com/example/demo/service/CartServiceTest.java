package com.example.demo.service;

import com.example.demo.dto.CartItemRequest;
import com.example.demo.dto.CartResponse;
import com.example.demo.models.Cart;
import com.example.demo.models.CartItem;
import com.example.demo.models.Product;
import com.example.demo.models.Role;
import com.example.demo.models.User;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.CartRepository;
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
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CartService cartService;

    private User testBuyer;
    private Product testProduct;
    private Cart testCart;
    private CartItemRequest cartItemRequest;

    @BeforeEach
    void setUp() {
        testBuyer = User.builder()
                .id(UUID.randomUUID())
                .name("Test Buyer")
                .email("buyer@example.com")
                .password("password")
                .role(Role.BUYER)
                .build();

        testProduct = Product.builder()
                .id(UUID.randomUUID())
                .name("Test Product")
                .price(BigDecimal.valueOf(50.0))
                .stock(100)
                .build();

        testCart = Cart.builder()
                .id(UUID.randomUUID())
                .buyer(testBuyer)
                .cartItems(new ArrayList<>())
                .build();

        cartItemRequest = new CartItemRequest();
        cartItemRequest.setProductId(testProduct.getId());
        cartItemRequest.setQuantity(2);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(testBuyer.getEmail());
        when(userRepository.findByEmail(testBuyer.getEmail())).thenReturn(Optional.of(testBuyer));
    }

    @Test
    void getCart_Success() {
        mockSecurityContext();
        when(cartRepository.findByBuyer(testBuyer)).thenReturn(Optional.of(testCart));

        CartResponse response = cartService.getCart();

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getTotal());
        assertTrue(response.getItems().isEmpty());
    }

    @Test
    void addItem_Success() {
        mockSecurityContext();
        when(cartRepository.findByBuyer(testBuyer)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
        when(cartItemRepository.findByCartAndProduct(testCart, testProduct)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArguments()[0]);

        // Add an item manually to the mock cart list to simulate refresh behavior
        CartItem mockedSavedItem = CartItem.builder()
                .cart(testCart)
                .product(testProduct)
                .quantity(2)
                .build();
        testCart.getCartItems().add(mockedSavedItem);
        
        CartResponse response = cartService.addItem(cartItemRequest);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals(BigDecimal.valueOf(100.0), response.getTotal());
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addItem_NotEnoughStock_ThrowsException() {
        mockSecurityContext();
        cartItemRequest.setQuantity(200); // More than stock (100)
        when(cartRepository.findByBuyer(testBuyer)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.addItem(cartItemRequest);
        });

        assertEquals("Not enough stock available", exception.getMessage());
    }
}
