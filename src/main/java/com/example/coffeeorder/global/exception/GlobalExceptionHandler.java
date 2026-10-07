package com.example.coffeeorder.global.exception;

import com.example.coffeeorder.domain.point.dto.ChargePointRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    public record ErrorResponse(String code, String message) {}

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException exception) {
        return response(exception.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        return response(exception.getBindingResult().getTarget() instanceof ChargePointRequest
                ? ErrorCode.INVALID_POINT_AMOUNT : ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception exception) {
        return response(ErrorCode.INVALID_REQUEST);
    }

    private ResponseEntity<ErrorResponse> response(ErrorCode code) {
        return ResponseEntity.status(code.getStatus()).body(new ErrorResponse(code.name(), code.getMessage()));
    }
}
