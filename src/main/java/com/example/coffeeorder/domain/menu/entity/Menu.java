package com.example.coffeeorder.domain.menu.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "menus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private Long price;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Menu(String name, long price) {
        if (price <= 0) throw new IllegalArgumentException("메뉴 가격은 양수여야 합니다.");
        this.name = name;
        this.price = price;
        this.createdAt = LocalDateTime.now();
    }
}
