package com.example.coffeeorder.domain.user.repository;

import com.example.coffeeorder.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {}
