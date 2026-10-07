package com.example.coffeeorder.domain.point.entity;

import com.example.coffeeorder.domain.user.entity.User;
import com.example.coffeeorder.global.exception.BusinessException;
import com.example.coffeeorder.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_points", check = @CheckConstraint(name = "chk_user_points_balance", constraint = "balance >= 0"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPoint {
    @Id
    private Long userId;
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(nullable = false)
    private Long balance;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public UserPoint(User user, long balance) {
        if (balance < 0) throw new IllegalArgumentException("잔액은 음수일 수 없습니다.");
        this.user = user;
        this.balance = balance;
        this.updatedAt = LocalDateTime.now();
    }

    public void charge(Long amount) {
        if (amount == null || amount <= 0 || balance > Long.MAX_VALUE - amount) {
            throw new BusinessException(ErrorCode.INVALID_POINT_AMOUNT);
        }
        balance += amount;
        updatedAt = LocalDateTime.now();
    }

    public void use(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("결제 금액은 양수여야 합니다.");
        if (balance < amount) throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        balance -= amount;
        updatedAt = LocalDateTime.now();
    }
}
