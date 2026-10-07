package com.example.coffeeorder.domain.point.controller;

import com.example.coffeeorder.domain.point.dto.ChargePointRequest;
import com.example.coffeeorder.domain.point.dto.PointResponse;
import com.example.coffeeorder.domain.point.service.PointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/points")
public class PointController {
    private final PointService service;

    @PostMapping
    public PointResponse charge(@PathVariable Long userId, @Valid @RequestBody ChargePointRequest request) {
        return service.charge(userId, request.amount());
    }
}
