package com.example.coffeeorder.domain.order.dto;

public record OrderResponse(Long orderId, Long menuId, String menuName, Long amount, Long remainingPoint) {}
