package com.daily_diary.backend.comment.exception;

import com.daily_diary.backend.global.exception.BusinessException;

public class CommentLikeAlreadyExistsException extends BusinessException {

    public CommentLikeAlreadyExistsException() {
        super("이미 좋아요한 댓글입니다.");
    }
}
