package com.daily_diary.backend.comment.exception;

import com.daily_diary.backend.global.exception.BusinessException;

public class CommentNotFoundException extends BusinessException {

    public CommentNotFoundException() {
        super("댓글을 찾을 수 없습니다.");
    }
}
