package com.daily_diary.backend.post.infra;

import com.daily_diary.backend.post.entity.Post;
import com.daily_diary.backend.post.web.OrderType;
import com.daily_diary.backend.post.web.PostDetailResponse;
import com.daily_diary.backend.post.web.PostSearchCondition;
import com.daily_diary.backend.post.web.PostSummaryResponse;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
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
        return queryFactory
                .select(Projections.constructor(PostDetailResponse.class,
                        post.id,
                        post.title,
                        post.content,
                        post.author,
                        post.viewCount,
                        post.likeCount,
                        post.commentCount,
                        userId != null
                                ? JPAExpressions.selectOne()
                                        .from(postLike)
                                        .where(postLike.post.id.eq(postId), postLike.user.id.eq(userId))
                                        .exists()
                                : Expressions.asBoolean(false),
                        post.createdAt,
                        post.updatedAt
                ))
                .from(post)
                .where(post.id.eq(postId))
                .fetchOne();
    }

    public Page<PostSummaryResponse> getPage(PostSearchCondition search, OrderType orderType, Pageable pageable) {
        List<PostSummaryResponse> content = queryFactory
                .select(Projections.constructor(PostSummaryResponse.class,
                        post.id,
                        post.title,
                        post.author,
                        post.viewCount,
                        post.likeCount,
                        post.commentCount,
                        post.createdAt
                ))
                .from(post)
                .where(searchBy(search))
                .orderBy(orderBy(orderType))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(post.count())
                .from(post)
                .where(searchBy(search))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    public List<PostSummaryResponse> findSummariesByIds(List<Long> ids) {
        return queryFactory
                .select(Projections.constructor(PostSummaryResponse.class,
                        post.id,
                        post.title,
                        post.author,
                        post.viewCount,
                        post.likeCount,
                        post.commentCount,
                        post.createdAt
                ))
                .from(post)
                .where(post.id.in(ids))
                .orderBy(post.likeCount.desc(), post.id.desc())
                .fetch();
    }

    public List<Long> findHotPostIds() {
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        return queryFactory
                .select(post.id)
                .from(post)
                .where(post.createdAt.goe(since))
                .orderBy(post.likeCount.desc(), post.id.desc())
                .limit(3)
                .fetch();
    }

    // ─── private ──────────────────────────────────────────────────────────────

    private BooleanExpression searchBy(PostSearchCondition search) {
        String keyword = search.keyword();
        if (!StringUtils.hasText(keyword))
            return null;


        //LIKE 검색
        if (keyword.length() < 3)
            return switch (search.searchType()) {
                case NICKNAME -> post.author.containsIgnoreCase(keyword);
                case TITLE -> post.title.containsIgnoreCase(keyword);
                case CONTENT -> post.content.containsIgnoreCase(keyword);
                default -> null;
            };
        //Ngram Fulltext 검색
        else
            return switch (search.searchType()) {
                case NICKNAME -> Expressions.booleanTemplate("match_against({0}, {1})", post.author, keyword);
                case TITLE -> Expressions.booleanTemplate("match_against({0}, {1})", post.title, keyword);
                case CONTENT -> Expressions.booleanTemplate("match_against({0}, {1})", post.content, keyword);
                default -> null;
            };
    }

    private OrderSpecifier<?>[] orderBy(OrderType orderType) {
        return switch (orderType) {
            case VIEW    -> new OrderSpecifier[]{post.viewCount.desc(), post.id.desc()};
            case LIKE    -> new OrderSpecifier[]{post.likeCount.desc(), post.id.desc()};
            case COMMENT -> new OrderSpecifier[]{post.commentCount.desc(), post.id.desc()};
            default      -> new OrderSpecifier[]{post.id.desc()};
        };
    }

}
