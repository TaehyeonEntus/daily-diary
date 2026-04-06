package com.daily_diary.backend.user.exception;

import com.daily_diary.backend.global.exception.BusinessException;

public class InvalidPasswordException extends BusinessException {

    public InvalidPasswordException() {
        super("현재 비밀번호가 올바르지 않습니다.");
    }
}
