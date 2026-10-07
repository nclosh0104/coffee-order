package com.example.coffeeorder.domain.menu.service;

import com.example.coffeeorder.domain.menu.dto.MenuResponse;
import com.example.coffeeorder.domain.menu.dto.PopularMenuResponse;
import com.example.coffeeorder.domain.menu.repository.MenuRepository;
import com.example.coffeeorder.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {
    private final MenuRepository menus;
    private final OrderRepository orders;
    private final Clock clock;

    public List<MenuResponse> findAll() {
        return menus.findAll(Sort.by("id")).stream()
                .map(menu -> new MenuResponse(menu.getId(), menu.getName(), menu.getPrice())).toList();
    }

    public List<PopularMenuResponse> findPopular() {
        return orders.findPopular(LocalDateTime.now(clock).minusDays(7), PageRequest.of(0, 3));
    }
}
