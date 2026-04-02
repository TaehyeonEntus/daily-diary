package com.daily_diary.backend.comment.exception;

import com.daily_diary.backend.global.exception.BusinessException;

public class CommentAccessDeniedException extends BusinessException {

    public CommentAccessDeniedException() {
        super("댓글에 접근할 권한이 없습니다.");
    }
}
