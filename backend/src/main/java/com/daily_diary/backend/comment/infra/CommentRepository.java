package com.daily_diary.backend.comment.infra;

import com.daily_diary.backend.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostId(Long postId, Pageable pageable);

    List<Comment> findAllByPostId(Long postId);

    void deleteAllByPostId(Long postId);

    @Modifying
    @Query("update Comment c set c.likeCount = c.likeCount + 1 where c.id = :id")
    void increaseLikeCount(@Param("id") Long commentId);

    @Modifying
    @Query("update Comment c set c.likeCount = c.likeCount - 1 where c.id = :id and c.likeCount > 0")
    void decreaseLikeCount(@Param("id") Long commentId);
}
