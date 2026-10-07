package com.example.coffeeorder.domain.menu.controller;

import com.example.coffeeorder.domain.menu.dto.MenuResponse;
import com.example.coffeeorder.domain.menu.dto.PopularMenuResponse;
import com.example.coffeeorder.domain.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {
    private final MenuService service;

    @GetMapping
    public List<MenuResponse> menus() { return service.findAll(); }

    @GetMapping("/popular")
    public List<PopularMenuResponse> popular() { return service.findPopular(); }
}
