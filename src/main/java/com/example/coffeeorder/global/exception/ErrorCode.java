package com.example.coffeeorder.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_POINT_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자 포인트를 찾을 수 없습니다."),
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "메뉴를 찾을 수 없습니다."),
    INVALID_POINT_AMOUNT(HttpStatus.BAD_REQUEST, "충전 금액은 양수이며 잔액의 저장 범위를 넘을 수 없습니다."),
    INSUFFICIENT_POINT(HttpStatus.CONFLICT, "포인트가 부족합니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이나 입력값이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String message;
}
