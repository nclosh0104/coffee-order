package com.example.coffeeorder.domain.point.entity;

import com.example.coffeeorder.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "point_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory {
    public enum Type { CHARGE, USE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Type type;
    @Column(nullable = false)
    private Long amount;
    @Column(nullable = false)
    private Long balanceAfter;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PointHistory(User user, Type type, long amount, long balanceAfter) {
        this.user = user;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.createdAt = LocalDateTime.now();
    }
}
