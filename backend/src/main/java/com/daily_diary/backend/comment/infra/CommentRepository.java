package com.daily_diary.backend.comment.infra;

import com.daily_diary.backend.comment.entity.Comment;
import com.daily_diary.backend.comment.exception.CommentNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    default Comment findOrThrow(Long id) {
        return findById(id).orElseThrow(CommentNotFoundException::new);
    }
}
