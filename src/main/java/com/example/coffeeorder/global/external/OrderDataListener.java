package com.example.coffeeorder.global.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDataListener {
    private final OrderDataPlatform platform;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(OrderData data) {
        try {
            platform.send(data);
        } catch (RuntimeException exception) {
            // 이미 커밋된 주문의 성공 응답을 전송 오류가 변경하지 않도록 한다.
            log.error("주문 데이터 전송 실패: {}", data, exception);
        }
    }
}
