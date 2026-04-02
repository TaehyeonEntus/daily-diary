package com.daily_diary.backend.post.web;

import com.daily_diary.backend.global.security.CustomUserDetailsService;
import com.daily_diary.backend.global.security.JwtProvider;
import com.daily_diary.backend.global.security.SecurityConfig;
import com.daily_diary.backend.post.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@AutoConfigureRestDocs
class PostControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    PostService postService;

    @MockitoBean
    JwtProvider jwtProvider;

    @MockitoBean
    CustomUserDetailsService userDetailsService;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 31, 12, 0, 0);

    private void mockAuthUser() {
        given(jwtProvider.validate("access-token")).willReturn(true);
        given(jwtProvider.getUserId("access-token")).willReturn(1L);
    }

    @Test
    void list() throws Exception {
        PostListResponse response = new PostListResponse(
                List.of(new PostSummaryResponse(1L, "첫 번째 게시글", "홍길동", 10L, 3L, NOW)),
                0, 10, 1L, 1
        );
        given(postService.list(0, 10)).willReturn(response);

        mockMvc.perform(get("/posts")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andDo(document("posts/list",
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (기본값: 0)"),
                                parameterWithName("size").description("페이지 크기 (기본값: 10)")
                        ),
                        responseFields(
                                fieldWithPath("content[].id").description("게시글 ID"),
                                fieldWithPath("content[].title").description("제목"),
                                fieldWithPath("content[].nickname").description("작성자 닉네임"),
                                fieldWithPath("content[].viewCount").description("조회수"),
                                fieldWithPath("content[].likeCount").description("좋아요 수"),
                                fieldWithPath("content[].createdAt").description("작성 일시"),
                                fieldWithPath("page").description("현재 페이지 번호"),
                                fieldWithPath("size").description("페이지 크기"),
                                fieldWithPath("totalElements").description("전체 게시글 수"),
                                fieldWithPath("totalPages").description("전체 페이지 수")
                        )
                ));
    }

    @Test
    void getPost() throws Exception {
        mockAuthUser();
        PostDetailResponse response = new PostDetailResponse(1L, "첫 번째 게시글", "내용입니다.", "홍길동", 10L, 5L, true, NOW, NOW);
        given(postService.getPost(eq(1L), eq(1L))).willReturn(response);

        mockMvc.perform(get("/posts/{id}", 1L)
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andDo(document("posts/get",
                        pathParameters(
                                parameterWithName("id").description("게시글 ID")
                        ),
                        responseFields(
                                fieldWithPath("id").description("게시글 ID"),
                                fieldWithPath("title").description("제목"),
                                fieldWithPath("content").description("내용"),
                                fieldWithPath("nickname").description("작성자 닉네임"),
                                fieldWithPath("viewCount").description("조회수"),
                                fieldWithPath("likeCount").description("좋아요 수"),
                                fieldWithPath("likedByMe").description("내가 좋아요를 눌렀는지 여부"),
                                fieldWithPath("createdAt").description("작성 일시"),
                                fieldWithPath("updatedAt").description("수정 일시")
                        )
                ));
    }

    @Test
    void create() throws Exception {
        mockAuthUser();
        willDoNothing().given(postService).create(eq(1L), any());

        mockMvc.perform(post("/posts")
                        .with(csrf())
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePostRequest("제목", "내용"))))
                .andExpect(status().isCreated())
                .andDo(document("posts/create",
                        requestFields(
                                fieldWithPath("title").description("제목"),
                                fieldWithPath("content").description("내용")
                        )
                ));
    }

    @Test
    void update() throws Exception {
        mockAuthUser();
        PostDetailResponse response = new PostDetailResponse(1L, "수정된 제목", "수정된 내용", "홍길동", 10L, 0L, false, NOW, NOW);
        given(postService.update(eq(1L), eq(1L), any())).willReturn(response);

        mockMvc.perform(patch("/posts/{id}", 1L)
                        .with(csrf())
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePostRequest("수정된 제목", "수정된 내용"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 제목"))
                .andDo(document("posts/update",
                        pathParameters(
                                parameterWithName("id").description("게시글 ID")
                        ),
                        requestFields(
                                fieldWithPath("title").description("수정할 제목"),
                                fieldWithPath("content").description("수정할 내용")
                        ),
                        responseFields(
                                fieldWithPath("id").description("게시글 ID"),
                                fieldWithPath("title").description("제목"),
                                fieldWithPath("content").description("내용"),
                                fieldWithPath("nickname").description("작성자 닉네임"),
                                fieldWithPath("viewCount").description("조회수"),
                                fieldWithPath("likeCount").description("좋아요 수"),
                                fieldWithPath("likedByMe").description("내가 좋아요를 눌렀는지 여부"),
                                fieldWithPath("createdAt").description("작성 일시"),
                                fieldWithPath("updatedAt").description("수정 일시")
                        )
                ));
    }

    @Test
    void deletePost() throws Exception {
        mockAuthUser();
        willDoNothing().given(postService).delete(1L, 1L);

        mockMvc.perform(delete("/posts/{id}", 1L)
                        .with(csrf())
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("posts/delete",
                        pathParameters(
                                parameterWithName("id").description("게시글 ID")
                        )
                ));
    }

    @Test
    void like() throws Exception {
        mockAuthUser();
        willDoNothing().given(postService).like(1L, 1L);

        mockMvc.perform(post("/posts/{id}/likes", 1L)
                        .with(csrf())
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isCreated())
                .andDo(document("posts/like",
                        pathParameters(
                                parameterWithName("id").description("게시글 ID")
                        )
                ));
    }

    @Test
    void unlike() throws Exception {
        mockAuthUser();
        willDoNothing().given(postService).unlike(1L, 1L);

        mockMvc.perform(delete("/posts/{id}/likes", 1L)
                        .with(csrf())
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("posts/unlike",
                        pathParameters(
                                parameterWithName("id").description("게시글 ID")
                        )
                ));
    }
}
