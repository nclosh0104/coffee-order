package com.example.coffeeorder.domain.order.controller;

import com.example.coffeeorder.domain.order.dto.CreateOrderRequest;
import com.example.coffeeorder.domain.order.dto.OrderResponse;
import com.example.coffeeorder.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse order(@Valid @RequestBody CreateOrderRequest request) {
        return service.order(request.userId(), request.menuId());
    }
}
