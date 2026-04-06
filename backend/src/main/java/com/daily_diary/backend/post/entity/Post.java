package com.daily_diary.backend.post.entity;

import com.daily_diary.backend.global.entity.BaseEntity;
import com.daily_diary.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_posts_like_count", columnList = "like_count"),
        @Index(name = "idx_posts_view_count", columnList = "view_count"),
        @Index(name = "idx_posts_comment_count", columnList = "comment_count")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 50)
    private String author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false)
    private long viewCount = 0;

    @Column(nullable = false)
    private long likeCount = 0;

    @Column(nullable = false)
    private long commentCount = 0;

    // ─── 정적 생성자 ──────────────────────────────────────────────────────────

    public static Post of(String title, String content, User user) {
        Post post = new Post();
        post.title = title;
        post.content = content;
        post.author = user.getNickname();
        post.user = user;
        return post;
    }

    // ─── 변경자 ───────────────────────────────────────────────────────────────

    public void changeTitle(String title) {
        this.title = title;
    }

    public void changeContent(String content) {
        this.content = content;
    }

    public void changeAuthor(String author) {
        this.author = author;
    }
}
