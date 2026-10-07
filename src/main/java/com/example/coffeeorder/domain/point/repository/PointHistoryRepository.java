package com.example.coffeeorder.domain.point.repository;

import com.example.coffeeorder.domain.point.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {}
