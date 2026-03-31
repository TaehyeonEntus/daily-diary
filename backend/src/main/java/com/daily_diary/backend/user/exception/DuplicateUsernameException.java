package com.daily_diary.backend.user.exception;

import com.daily_diary.backend.global.exception.BusinessException;

public class DuplicateUsernameException extends BusinessException {

    public DuplicateUsernameException() {
        super("이미 사용 중인 아이디입니다.");
    }
}
