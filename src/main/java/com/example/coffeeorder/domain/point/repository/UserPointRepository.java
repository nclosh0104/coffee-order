package com.example.coffeeorder.domain.point.repository;

import com.example.coffeeorder.domain.point.entity.UserPoint;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserPointRepository extends JpaRepository<UserPoint, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from UserPoint p where p.userId = :userId")
    Optional<UserPoint> findByUserIdWithLock(@Param("userId") Long userId);
}
