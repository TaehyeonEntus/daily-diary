package com.daily_diary.backend.post.exception;

import com.daily_diary.backend.global.exception.BusinessException;

public class LikeNotFoundException extends BusinessException {

    public LikeNotFoundException() {
        super("좋아요를 누르지 않은 게시글입니다.");
    }
}
