package com.daily_diary.backend.post.infra;

import com.daily_diary.backend.post.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    @Modifying
    @Query(value = """
            UPDATE posts p
            JOIN (
                SELECT post_id, COUNT(*) AS cnt
                FROM post_likes
                GROUP BY post_id
            ) t ON p.id = t.post_id
            SET p.like_count = t.cnt
            """, nativeQuery = true)
    void syncLikeCount();
}
