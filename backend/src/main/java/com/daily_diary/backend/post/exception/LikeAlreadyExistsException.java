package com.daily_diary.backend.post.exception;

import com.daily_diary.backend.global.exception.BusinessException;

public class LikeAlreadyExistsException extends BusinessException {

    public LikeAlreadyExistsException() {
        super("이미 좋아요를 누른 게시글입니다.");
    }
}
