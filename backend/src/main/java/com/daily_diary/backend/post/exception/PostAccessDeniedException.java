package com.daily_diary.backend.post.exception;

import com.daily_diary.backend.global.exception.BusinessException;

public class PostAccessDeniedException extends BusinessException {

    public PostAccessDeniedException() {
        super("게시글에 접근할 권한이 없습니다.");
    }

    public PostAccessDeniedException(String message) {
        super(message);
    }
}
