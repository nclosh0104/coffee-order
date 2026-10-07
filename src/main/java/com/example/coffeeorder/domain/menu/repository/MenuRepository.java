package com.example.coffeeorder.domain.menu.repository;

import com.example.coffeeorder.domain.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {}
