package com.example.coffeeorder.domain.point.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChargePointRequest(@NotNull @Positive Long amount) {}
