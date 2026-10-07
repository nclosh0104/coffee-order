package com.example.coffeeorder.domain.order.entity;

import com.example.coffeeorder.domain.menu.entity.Menu;
import com.example.coffeeorder.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity(name = "CoffeeOrder")
@Table(name = "orders", indexes = @Index(name = "idx_orders_created_menu", columnList = "created_at,menu_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;
    @Column(nullable = false, updatable = false)
    private Long amount;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Order(User user, Menu menu) {
        this.user = user;
        this.menu = menu;
        this.amount = menu.getPrice();
        this.createdAt = LocalDateTime.now();
    }
}
