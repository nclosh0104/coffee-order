package com.example.coffeeorder.global.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MockOrderDataPlatform implements OrderDataPlatform {
    @Override
    public void send(OrderData data) {
        log.info("Mock 주문 데이터 전송: userId={}, menuId={}, paymentAmount={}",
                data.userId(), data.menuId(), data.paymentAmount());
    }
}
