package com.example.demo.repository;

import com.example.demo.models.Order;
import com.example.demo.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    // Buyer sees their own orders
    List<Order> findByBuyer(User buyer);

    // Seller sees orders that contain their products
    @Query("SELECT DISTINCT o FROM Order o " +
            "JOIN o.orderItems oi " +
            "JOIN oi.product p " +
            "WHERE p.seller = :seller")
    List<Order> findOrdersBySeller(@Param("seller") User seller);
}