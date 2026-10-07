package com.example.coffeeorder.domain.order.service;

import com.example.coffeeorder.domain.menu.repository.MenuRepository;
import com.example.coffeeorder.domain.order.dto.OrderResponse;
import com.example.coffeeorder.domain.order.entity.Order;
import com.example.coffeeorder.domain.order.repository.OrderRepository;
import com.example.coffeeorder.domain.point.entity.PointHistory;
import com.example.coffeeorder.domain.point.repository.PointHistoryRepository;
import com.example.coffeeorder.domain.point.repository.UserPointRepository;
import com.example.coffeeorder.domain.user.repository.UserRepository;
import com.example.coffeeorder.global.exception.BusinessException;
import com.example.coffeeorder.global.exception.ErrorCode;
import com.example.coffeeorder.global.external.OrderData;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final UserRepository users;
    private final UserPointRepository points;
    private final MenuRepository menus;
    private final PointHistoryRepository histories;
    private final OrderRepository orders;
    private final ApplicationEventPublisher events;

    @Transactional
    public OrderResponse order(Long userId, Long menuId) {
        var user = users.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        var point = points.findByUserIdWithLock(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_POINT_NOT_FOUND));
        var menu = menus.findById(menuId).orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));
        point.use(menu.getPrice());
        histories.save(new PointHistory(user, PointHistory.Type.USE, menu.getPrice(), point.getBalance()));
        var order = orders.save(new Order(user, menu));
        events.publishEvent(new OrderData(userId, menuId, order.getAmount()));
        return new OrderResponse(order.getId(), menuId, menu.getName(), order.getAmount(), point.getBalance());
    }
}
