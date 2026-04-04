package com.daily_diary.backend.post.infra;

import com.daily_diary.backend.post.web.OrderType;
import com.daily_diary.backend.post.web.PostDetailResponse;
import com.daily_diary.backend.post.web.PostSearchCondition;
import com.daily_diary.backend.post.web.PostSummaryResponse;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

import static com.daily_diary.backend.post.entity.QPost.post;
import static com.daily_diary.backend.post.entity.QPostLike.postLike;

@Repository
@RequiredArgsConstructor
public class PostQueryRepository {
    private final JPAQueryFactory queryFactory;

    public PostDetailResponse findPostDetail(Long postId, Long userId) {
        //익명 유저 용
        if (userId == null)
            return queryFactory
                    .select(Projections.constructor(PostDetailResponse.class,
                            post.id,
                            post.title,
                            post.content,
                            post.user.nickname,
                            post.viewCount,
                            post.likeCount,
                            Expressions.constant(false),
                            post.createdAt,
                            post.updatedAt
                    ))
                    .from(post)
                    .join(post.user)
                    .where(post.id.eq(postId))
                    .fetchOne();
            //로그인 유저
        else
            return queryFactory
                    .select(Projections.constructor(PostDetailResponse.class,
                            post.id,
                            post.title,
                            post.content,
                            post.user.nickname,
                            post.viewCount,
                            post.likeCount,
                            postLike.isNotNull(),
                            post.createdAt,
                            post.updatedAt
                    ))
                    .from(post)
                    .join(post.user)
                    .leftJoin(postLike)
                    .on(
                            postLike.post.id.eq(postId),
                            postLike.user.id.eq(userId)
                    )
                    .where(post.id.eq(postId))
                    .fetchOne();
    }

    public Page<PostSummaryResponse> getPage(PostSearchCondition search, OrderType orderType, Pageable pageable) {
        List<PostSummaryResponse> content = queryFactory
                .selectFrom(post)
                .join(post.user).fetchJoin()
                .where(searchBy(search))
                .orderBy(orderBy(orderType))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch()
                .stream()
                .map(PostSummaryResponse::from)
                .toList();

        Long total = queryFactory
                .select(post.count())
                .from(post)
                .join(post.user)
                .where(searchBy(search))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    public List<PostSummaryResponse> findSummariesByIds(List<Long> ids) {
        return queryFactory
                .selectFrom(post)
                .join(post.user).fetchJoin()
                .where(post.id.in(ids))
                .orderBy(post.likeCount.desc(), post.createdAt.desc())
                .fetch()
                .stream()
                .map(PostSummaryResponse::from)
                .toList();
    }

    public List<Long> findHotPosts() {
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        return queryFactory
                .select(post.id)
                .from(post)
                .where(post.createdAt.goe(since))
                .orderBy(post.likeCount.desc(), post.createdAt.desc())
                .limit(3)
                .fetch();
    }

    // ─── private ──────────────────────────────────────────────────────────────

    private BooleanExpression searchBy(PostSearchCondition search) {
        if (!StringUtils.hasText(search.keyword()))
            return null;
        return switch (search.searchType()) {
            case NICKNAME -> post.user.nickname.containsIgnoreCase(search.keyword());
            case TITLE -> post.title.containsIgnoreCase(search.keyword());
            case CONTENT -> post.content.containsIgnoreCase(search.keyword());
            default -> null;
        };
    }

    private OrderSpecifier<?>[] orderBy(OrderType orderType) {
        return switch (orderType) {
            case VIEW -> new OrderSpecifier[]{post.viewCount.desc(), post.createdAt.desc()};
            case LIKE -> new OrderSpecifier[]{post.likeCount.desc(), post.createdAt.desc()};
            default -> new OrderSpecifier[]{post.createdAt.desc()};
        };
    }
}
