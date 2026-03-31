package com.daily_diary.backend.post.exception;

import com.daily_diary.backend.global.exception.BusinessException;

public class PostAccessDeniedException extends BusinessException {

    public PostAccessDeniedException(String message) {
        super(message);
    }
}
