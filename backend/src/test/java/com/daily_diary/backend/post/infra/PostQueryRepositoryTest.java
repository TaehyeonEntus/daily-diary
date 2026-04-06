package com.daily_diary.backend.post.infra;

import com.daily_diary.backend.global.config.QueryDslConfig;
import com.daily_diary.backend.post.entity.Post;
import com.daily_diary.backend.post.entity.PostLike;
import com.daily_diary.backend.post.web.OrderType;
import com.daily_diary.backend.post.web.PostDetailResponse;
import com.daily_diary.backend.post.web.PostSearchCondition;
import com.daily_diary.backend.post.web.PostSummaryResponse;
import com.daily_diary.backend.post.web.SearchType;
import com.daily_diary.backend.user.entity.User;
import com.daily_diary.backend.user.infra.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import({QueryDslConfig.class, PostQueryRepository.class})
@ActiveProfiles("test")
class PostQueryRepositoryTest {

    @Autowired
    PostRepository postRepository;

    @Autowired
    PostLikeRepository postLikeRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PostQueryRepository postQueryRepository;

    private User savedUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        savedUser = userRepository.save(User.of("user1", "pw", "홍길동"));
        anotherUser = userRepository.save(User.of("user2", "pw", "김영수"));
    }

    // ─── findPostDetail ───────────────────────────────────────────────────────

    @Test
    void findPostDetail_정상_likeCount_commentCount_반환() {
        // given
        Post post = postRepository.save(Post.of("제목", "내용", savedUser));
        postRepository.increaseLikeCount(post.getId());
        postRepository.increaseCommentCount(post.getId());
        postRepository.flush();

        // when
        PostDetailResponse detail = postQueryRepository.findPostDetail(post.getId(), null);

        // then
        assertThat(detail).isNotNull();
        assertThat(detail.title()).isEqualTo("제목");
        assertThat(detail.likeCount()).isEqualTo(1L);
        assertThat(detail.commentCount()).isEqualTo(1L);
    }

    @Test
    void findPostDetail_liked_false_좋아요_안_누른_경우() {
        // given
        Post post = postRepository.save(Post.of("제목", "내용", savedUser));

        // when
        PostDetailResponse detail = postQueryRepository.findPostDetail(post.getId(), anotherUser.getId());

        // then
        assertThat(detail.like()).isFalse();
    }

    @Test
    void findPostDetail_liked_true_좋아요_누른_경우() {
        // given
        Post post = postRepository.save(Post.of("제목", "내용", savedUser));
        postLikeRepository.save(PostLike.of(post, anotherUser));

        // when
        PostDetailResponse detail = postQueryRepository.findPostDetail(post.getId(), anotherUser.getId());

        // then
        assertThat(detail.like()).isTrue();
    }

    @Test
    void findPostDetail_userId_null이면_liked_false() {
        // given
        Post post = postRepository.save(Post.of("제목", "내용", savedUser));

        // when
        PostDetailResponse detail = postQueryRepository.findPostDetail(post.getId(), null);

        // then
        assertThat(detail.like()).isFalse();
    }

    // ─── getPage — LIKE 검색 (keyword 2글자 이하) ─────────────────────────────

    @Test
    void getPage_keyword_2글자이하_TITLE_LIKE검색() {
        // given
        postRepository.save(Post.of("자바 입문", "내용", savedUser));
        postRepository.save(Post.of("파이썬 기초", "내용", savedUser));

        // when
        Page<PostSummaryResponse> page = postQueryRepository.getPage(
                new PostSearchCondition(SearchType.TITLE, "자바"),
                OrderType.DATE,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).title()).isEqualTo("자바 입문");
    }

    @Test
    void getPage_keyword_2글자이하_NICKNAME_LIKE검색() {
        // given
        postRepository.save(Post.of("게시글1", "내용1", savedUser));
        postRepository.save(Post.of("게시글2", "내용2", anotherUser));

        // when
        Page<PostSummaryResponse> page = postQueryRepository.getPage(
                new PostSearchCondition(SearchType.NICKNAME, "홍길"),
                OrderType.DATE,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).nickname()).isEqualTo("홍길동");
    }

    @Test
    void getPage_keyword_2글자이하_CONTENT_LIKE검색() {
        // given
        postRepository.save(Post.of("제목1", "스프링 부트 내용", savedUser));
        postRepository.save(Post.of("제목2", "파이썬 내용", savedUser));

        // when
        Page<PostSummaryResponse> page = postQueryRepository.getPage(
                new PostSearchCondition(SearchType.CONTENT, "스프"),
                OrderType.DATE,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).title()).isEqualTo("제목1");
    }

    @Test
    void getPage_keyword_없으면_전체_반환() {
        // given
        postRepository.save(Post.of("첫 번째", "내용", savedUser));
        postRepository.save(Post.of("두 번째", "내용", anotherUser));

        // when
        Page<PostSummaryResponse> page = postQueryRepository.getPage(
                new PostSearchCondition(SearchType.DEFAULT, null),
                OrderType.DATE,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    void getPage_DEFAULT_searchType_keyword_있어도_조건_무시() {
        // given
        postRepository.save(Post.of("제목A", "내용A", savedUser));
        postRepository.save(Post.of("제목B", "내용B", savedUser));

        // when
        // DEFAULT 타입에서 keyword가 있어도 2글자 이하면 null 반환 → 전체 조회
        Page<PostSummaryResponse> page = postQueryRepository.getPage(
                new PostSearchCondition(SearchType.DEFAULT, "아"),
                OrderType.DATE,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(page.getContent()).hasSize(2);
    }

    // ─── getPage — keyword 3글자 이상 fulltext ────────────────────────────────

    // H2 인메모리 DB는 MySQL의 FULLTEXT(match_against) 함수를 지원하지 않는다.
    // keyword가 3글자 이상이면 PostQueryRepository가 match_against 표현식 분기를 실행하며,
    // H2에서는 해당 함수가 미등록 상태이므로 예외가 발생한다.
    // 실제 MySQL 환경에서의 fulltext 검색 동작은 별도 통합 테스트에서 검증해야 한다.

    @Test
    void getPage_keyword_3글자이상_H2에서_fulltext_미지원으로_예외_발생() {
        // given
        postRepository.save(Post.of("스프링 부트 입문", "내용", savedUser));

        // when & then
        assertThrows(Exception.class, () ->
                postQueryRepository.getPage(
                        new PostSearchCondition(SearchType.TITLE, "스프링"),
                        OrderType.DATE,
                        PageRequest.of(0, 10)
                )
        );
    }

    // ─── getPage — 정렬 ───────────────────────────────────────────────────────

    @Test
    void getPage_정렬_DATE_최신순() {
        // given
        postRepository.save(Post.of("오래된 글", "내용", savedUser));
        postRepository.save(Post.of("최신 글", "내용", savedUser));

        // when
        Page<PostSummaryResponse> page = postQueryRepository.getPage(
                new PostSearchCondition(SearchType.DEFAULT, null),
                OrderType.DATE,
                PageRequest.of(0, 10)
        );

        // then
        List<PostSummaryResponse> content = page.getContent();
        assertThat(content.get(0).title()).isEqualTo("최신 글");
        assertThat(content.get(1).title()).isEqualTo("오래된 글");
    }

    @Test
    void getPage_정렬_LIKE_좋아요순() {
        // given
        postRepository.save(Post.of("좋아요 적은 글", "내용", savedUser));
        Post post2 = postRepository.save(Post.of("좋아요 많은 글", "내용", savedUser));
        postRepository.increaseLikeCount(post2.getId());
        postRepository.increaseLikeCount(post2.getId());
        postRepository.increaseLikeCount(post2.getId());

        // when
        Page<PostSummaryResponse> page = postQueryRepository.getPage(
                new PostSearchCondition(SearchType.DEFAULT, null),
                OrderType.LIKE,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(page.getContent().get(0).title()).isEqualTo("좋아요 많은 글");
    }

    @Test
    void getPage_정렬_VIEW_조회수순() {
        // given
        postRepository.save(Post.of("조회수 적은 글", "내용", savedUser));
        Post post2 = postRepository.save(Post.of("조회수 많은 글", "내용", savedUser));
        postRepository.increaseViewCount(post2.getId());
        postRepository.increaseViewCount(post2.getId());

        // when
        Page<PostSummaryResponse> page = postQueryRepository.getPage(
                new PostSearchCondition(SearchType.DEFAULT, null),
                OrderType.VIEW,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(page.getContent().get(0).title()).isEqualTo("조회수 많은 글");
    }

    @Test
    void getPage_정렬_COMMENT_댓글수순() {
        // given
        postRepository.save(Post.of("댓글 적은 글", "내용", savedUser));
        Post post2 = postRepository.save(Post.of("댓글 많은 글", "내용", savedUser));
        postRepository.increaseCommentCount(post2.getId());
        postRepository.increaseCommentCount(post2.getId());

        // when
        Page<PostSummaryResponse> page = postQueryRepository.getPage(
                new PostSearchCondition(SearchType.DEFAULT, null),
                OrderType.COMMENT,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(page.getContent().get(0).title()).isEqualTo("댓글 많은 글");
    }

    // ─── findSummariesByIds ───────────────────────────────────────────────────

    @Test
    void findSummariesByIds_정상() {
        // given
        Post post1 = postRepository.save(Post.of("첫 번째", "내용", savedUser));
        Post post2 = postRepository.save(Post.of("두 번째", "내용", savedUser));
        postRepository.save(Post.of("세 번째", "내용", savedUser));

        // when
        List<PostSummaryResponse> summaries = postQueryRepository.findSummariesByIds(
                List.of(post1.getId(), post2.getId())
        );

        // then
        assertThat(summaries).hasSize(2);
        assertThat(summaries).extracting(PostSummaryResponse::id)
                .containsExactlyInAnyOrder(post1.getId(), post2.getId());
    }

    @Test
    void findSummariesByIds_빈_목록() {
        // when
        List<PostSummaryResponse> summaries = postQueryRepository.findSummariesByIds(List.of());

        // then
        assertThat(summaries).isEmpty();
    }
}
