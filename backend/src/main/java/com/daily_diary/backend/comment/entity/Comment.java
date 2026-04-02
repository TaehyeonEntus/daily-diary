package com.daily_diary.backend.comment.entity;

import com.daily_diary.backend.global.entity.BaseEntity;
import com.daily_diary.backend.post.entity.Post;
import com.daily_diary.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private long likeCount = 0;

    public static Comment of(String content, Post post, User user) {
        Comment comment = new Comment();
        comment.content = content;
        comment.post = post;
        comment.user = user;
        return comment;
    }

    public void changeContent(String content) {
        this.content = content;
    }
}
