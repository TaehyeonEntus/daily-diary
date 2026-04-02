package com.daily_diary.backend.comment.exception;

import com.daily_diary.backend.global.exception.BusinessException;

public class CommentLikeNotFoundException extends BusinessException {

    public CommentLikeNotFoundException() {
        super("댓글 좋아요를 찾을 수 없습니다.");
    }
}
