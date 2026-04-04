package com.daily_diary.backend.user.web;

import com.daily_diary.backend.global.security.CustomUserDetailsService;
import com.daily_diary.backend.global.security.JwtProvider;
import com.daily_diary.backend.global.security.SecurityConfig;
import com.daily_diary.backend.user.service.UserService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@AutoConfigureRestDocs
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    UserService userService;

    @MockitoBean
    JwtProvider jwtProvider;

    @MockitoBean
    CustomUserDetailsService userDetailsService;

    private void mockAuthUser() {
        given(jwtProvider.validate("access-token")).willReturn(true);
        given(jwtProvider.getUserId("access-token")).willReturn(1L);
    }

    @Test
    void getMe() throws Exception {
        mockAuthUser();
        UserDetailResponse response = new UserDetailResponse(1L, "testuser", "테스터", LocalDateTime.now());
        given(userService.get(1L)).willReturn(response);

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andDo(document("users/get-me",
                        responseFields(
                                fieldWithPath("id").description("사용자 ID"),
                                fieldWithPath("username").description("아이디"),
                                fieldWithPath("nickname").description("닉네임"),
                                fieldWithPath("createdAt").description("가입 일시")
                        )
                ));
    }

    @Test
    void updateMe() throws Exception {
        mockAuthUser();
        willDoNothing().given(userService).update(eq(1L), any());

        mockMvc.perform(patch("/users/me")
                        .with(csrf())
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserNicknameUpdateRequest("새닉네임"))))
                .andExpect(status().isNoContent())
                .andDo(document("users/update-me",
                        requestFields(
                                fieldWithPath("nickname").description("변경할 닉네임")
                        )
                ));
    }

    @Test
    void deleteMe() throws Exception {
        mockAuthUser();
        willDoNothing().given(userService).delete(1L);

        mockMvc.perform(delete("/users/me")
                        .with(csrf())
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("users/delete-me"));
    }
}
