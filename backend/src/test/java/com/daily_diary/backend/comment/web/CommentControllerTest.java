package com.daily_diary.backend.comment.web;

import com.daily_diary.backend.comment.service.CommentService;
import com.daily_diary.backend.global.security.CustomUserDetailsService;
import com.daily_diary.backend.global.security.JwtProvider;
import com.daily_diary.backend.global.security.SecurityConfig;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@AutoConfigureRestDocs
class CommentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    CommentService commentService;

    @MockitoBean
    JwtProvider jwtProvider;

    @MockitoBean
    CustomUserDetailsService userDetailsService;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 4, 2, 12, 0, 0);

    private void mockAuthUser() {
        given(jwtProvider.validate("access-token")).willReturn(true);
        given(jwtProvider.getUserId("access-token")).willReturn(1L);
    }

    @Test
    void list() throws Exception {
        CommentPageResponse response = new CommentPageResponse(
                List.of(new CommentSummaryResponse(1L, "댓글 내용", "홍길동", NOW)),
                0, 20, 1L, 1
        );
        given(commentService.getPage(eq(1L), anyInt(), anyInt())).willReturn(response);

        mockMvc.perform(get("/posts/{postId}/comments", 1L)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andDo(document("comments/list",
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID")
                        ),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (기본값: 0)").optional(),
                                parameterWithName("size").description("페이지 크기 (기본값: 20)").optional()
                        ),
                        responseFields(
                                fieldWithPath("content[].id").description("댓글 ID"),
                                fieldWithPath("content[].content").description("댓글 내용"),
                                fieldWithPath("content[].nickname").description("작성자 닉네임"),
                                fieldWithPath("content[].createdAt").description("작성 일시"),
                                fieldWithPath("page").description("현재 페이지 번호"),
                                fieldWithPath("size").description("페이지 크기"),
                                fieldWithPath("totalElements").description("전체 댓글 수"),
                                fieldWithPath("totalPages").description("전체 페이지 수")
                        )
                ));
    }

    @Test
    void create() throws Exception {
        mockAuthUser();
        CommentDetailResponse response = new CommentDetailResponse(1L, "댓글 내용", "홍길동", NOW, NOW);
        given(commentService.create(eq(1L), eq(1L), any())).willReturn(response);

        mockMvc.perform(post("/posts/{postId}/comments", 1L)
                        .with(csrf())
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("댓글 내용"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andDo(document("comments/create",
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID")
                        ),
                        requestFields(
                                fieldWithPath("content").description("댓글 내용")
                        ),
                        responseFields(
                                fieldWithPath("id").description("댓글 ID"),
                                fieldWithPath("content").description("댓글 내용"),
                                fieldWithPath("nickname").description("작성자 닉네임"),
                                fieldWithPath("createdAt").description("작성 일시"),
                                fieldWithPath("updatedAt").description("수정 일시")
                        )
                ));
    }

    @Test
    void create_빈_content_400() throws Exception {
        mockAuthUser();

        mockMvc.perform(post("/posts/{postId}/comments", 1L)
                        .with(csrf())
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_비인증_403() throws Exception {
        mockMvc.perform(post("/posts/{postId}/comments", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("댓글 내용"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void update() throws Exception {
        mockAuthUser();
        willDoNothing().given(commentService).update(eq(1L), eq(1L), any());

        mockMvc.perform(patch("/posts/{postId}/comments/{commentId}", 1L, 1L)
                        .with(csrf())
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCommentRequest("수정된 댓글"))))
                .andExpect(status().isNoContent())
                .andDo(document("comments/update",
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID"),
                                parameterWithName("commentId").description("댓글 ID")
                        ),
                        requestFields(
                                fieldWithPath("content").description("수정할 댓글 내용")
                        )
                ));
    }

    @Test
    void deleteComment() throws Exception {
        mockAuthUser();
        willDoNothing().given(commentService).delete(1L, 1L);

        mockMvc.perform(delete("/posts/{postId}/comments/{commentId}", 1L, 1L)
                        .with(csrf())
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("comments/delete",
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID"),
                                parameterWithName("commentId").description("댓글 ID")
                        )
                ));
    }
}
