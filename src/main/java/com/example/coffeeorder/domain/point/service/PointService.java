package com.example.coffeeorder.domain.point.service;

import com.example.coffeeorder.domain.point.dto.PointResponse;
import com.example.coffeeorder.domain.point.entity.PointHistory;
import com.example.coffeeorder.domain.point.repository.PointHistoryRepository;
import com.example.coffeeorder.domain.point.repository.UserPointRepository;
import com.example.coffeeorder.domain.user.repository.UserRepository;
import com.example.coffeeorder.global.exception.BusinessException;
import com.example.coffeeorder.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {
    private final UserRepository users;
    private final UserPointRepository points;
    private final PointHistoryRepository histories;

    @Transactional
    public PointResponse charge(Long userId, Long amount) {
        var user = users.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        var point = points.findByUserIdWithLock(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_POINT_NOT_FOUND));
        point.charge(amount);
        histories.save(new PointHistory(user, PointHistory.Type.CHARGE, amount, point.getBalance()));
        return new PointResponse(userId, point.getBalance());
    }
}
