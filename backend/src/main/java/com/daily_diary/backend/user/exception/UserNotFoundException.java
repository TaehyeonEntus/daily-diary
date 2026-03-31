package com.daily_diary.backend.user.exception;

import com.daily_diary.backend.global.exception.BusinessException;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException() {
        super("사용자를 찾을 수 없습니다.");
    }
}
