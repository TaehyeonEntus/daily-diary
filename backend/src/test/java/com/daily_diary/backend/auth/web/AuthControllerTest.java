package com.daily_diary.backend.auth.web;

import com.daily_diary.backend.auth.service.AuthService;
import com.daily_diary.backend.global.security.JwtProvider;
import com.daily_diary.backend.global.security.SecurityConfig;
import com.daily_diary.backend.user.web.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    JwtProvider jwtProvider;

    @Test
    void signup() throws Exception {
        UserResponse response = new UserResponse(1L, "testuser", "테스터", LocalDateTime.now());
        given(authService.signup(any())).willReturn(response);

        mockMvc.perform(post("/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest("testuser", "password123", "테스터"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andDo(document("auth/signup",
                        requestFields(
                                fieldWithPath("username").description("아이디 (4~50자)"),
                                fieldWithPath("password").description("비밀번호 (최소 8자)"),
                                fieldWithPath("nickname").description("닉네임")
                        ),
                        responseFields(
                                fieldWithPath("id").description("사용자 ID"),
                                fieldWithPath("username").description("아이디"),
                                fieldWithPath("nickname").description("닉네임"),
                                fieldWithPath("createdAt").description("가입 일시")
                        )
                ));
    }

    @Test
    void login() throws Exception {
        LoginResponse response = new LoginResponse("access-token", "refresh-token");
        given(authService.login(any())).willReturn(response);

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("testuser", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andDo(document("auth/login",
                        requestFields(
                                fieldWithPath("username").description("아이디"),
                                fieldWithPath("password").description("비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("accessToken").description("액세스 토큰"),
                                fieldWithPath("refreshToken").description("리프레시 토큰")
                        )
                ));
    }

    @Test
    void refresh() throws Exception {
        given(authService.refresh(any())).willReturn(new TokenRefreshResponse("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TokenRefreshRequest("refresh-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andDo(document("auth/refresh",
                        requestFields(
                                fieldWithPath("refreshToken").description("리프레시 토큰")
                        ),
                        responseFields(
                                fieldWithPath("accessToken").description("새 액세스 토큰"),
                                fieldWithPath("refreshToken").description("새 리프레시 토큰 (Rotation)")
                        )
                ));
    }

    @Test
    void logout() throws Exception {
        given(jwtProvider.validate("access-token")).willReturn(true);
        given(jwtProvider.getUserId("access-token")).willReturn(1L);
        willDoNothing().given(authService).logout(any());

        mockMvc.perform(delete("/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("auth/logout"));
    }
}
