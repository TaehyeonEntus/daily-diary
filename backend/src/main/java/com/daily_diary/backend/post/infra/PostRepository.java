package com.daily_diary.backend.post.infra;

import com.daily_diary.backend.post.entity.Post;
import com.daily_diary.backend.post.exception.PostNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    default Post findOrThrow(Long id) {
        return findById(id).orElseThrow(PostNotFoundException::new);
    }

    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
    void increaseViewCount(@Param("id") Long postId);

    @Modifying
    @Query("update Post p set p.likeCount = p.likeCount + 1 where p.id = :id")
    void increaseLikeCount(@Param("id") Long postId);

    @Modifying
    @Query("update Post p set p.likeCount = p.likeCount - 1 where p.id = :id")
    void decreaseLikeCount(@Param("id") Long postId);
}