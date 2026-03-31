package com.daily_diary.backend.post.exception;

import com.daily_diary.backend.global.exception.BusinessException;

public class PostNotFoundException extends BusinessException {

    public PostNotFoundException() {
        super("게시글을 찾을 수 없습니다.");
    }
}
