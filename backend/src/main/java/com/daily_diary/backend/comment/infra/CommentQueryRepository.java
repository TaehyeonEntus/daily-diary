package com.daily_diary.backend.comment.infra;

import com.daily_diary.backend.comment.web.CommentSummaryResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.daily_diary.backend.comment.entity.QComment.comment;

@Repository
@RequiredArgsConstructor
public class CommentQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<CommentSummaryResponse> getPage(Long postId, Pageable pageable) {
        List<CommentSummaryResponse> content = queryFactory
                .select(Projections.constructor(CommentSummaryResponse.class,
                        comment.id,
                        comment.content,
                        comment.user.nickname,
                        comment.createdAt
                ))
                .from(comment)
                .join(comment.user)
                .where(comment.post.id.eq(postId))
                .orderBy(comment.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.post.id.eq(postId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}
