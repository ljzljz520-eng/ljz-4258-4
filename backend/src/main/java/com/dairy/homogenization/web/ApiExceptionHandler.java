package com.dairy.homogenization.web;

import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String,Object> conflict(RuntimeException e) {
        return Map.of("error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
    }
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String,Object> optimistic(Exception e) {
        return Map.of("error", "并发版本冲突，请刷新后重试；已批准证据不会被后台修改。");
    }
}
