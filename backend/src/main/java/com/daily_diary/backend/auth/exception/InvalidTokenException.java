package com.daily_diary.backend.auth.exception;

import com.daily_diary.backend.global.exception.BusinessException;

public class InvalidTokenException extends BusinessException {

    public InvalidTokenException() {
        super("유효하지 않은 토큰입니다.");
    }
}
