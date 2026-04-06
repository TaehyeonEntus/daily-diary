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
    @Query("update Post p set p.author = :author where p.user.id = :userId")
    void updateAuthorByUserId(@Param("userId") Long userId, @Param("author") String author);

    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
    void increaseViewCount(@Param("id") Long postId);

    @Modifying
    @Query("update Post p set p.likeCount = p.likeCount + 1 where p.id = :id")
    void increaseLikeCount(@Param("id") Long postId);

    @Modifying
    @Query("update Post p set p.likeCount = p.likeCount - 1 where p.id = :id")
    void decreaseLikeCount(@Param("id") Long postId);

    @Modifying
    @Query("update Post p set p.commentCount = p.commentCount + 1 where p.id = :id")
    void increaseCommentCount(@Param("id") Long postId);

    @Modifying
    @Query("update Post p set p.commentCount = p.commentCount - 1 where p.id = :id")
    void decreaseCommentCount(@Param("id") Long postId);

    //정합용 스케줄링 메서드
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

    //정합용 스케줄링 메서드
    @Modifying
    @Query(value = """
            UPDATE posts p
            JOIN (
                SELECT post_id, COUNT(*) AS cnt
                FROM comments
                GROUP BY post_id
            ) t ON p.id = t.post_id
            SET p.comment_count = t.cnt
            """, nativeQuery = true)
    void syncCommentCount();
}