package com.daily_diary.backend.auth.web;

import com.daily_diary.backend.auth.service.AuthService;
import com.daily_diary.backend.auth.service.Tokens;
import com.daily_diary.backend.global.security.CustomUserDetails;
import com.daily_diary.backend.global.security.CustomUserDetailsService;
import com.daily_diary.backend.global.security.JwtProvider;
import com.daily_diary.backend.global.security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
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

    @MockitoBean
    CustomUserDetailsService userDetailsService;

    private void mockAuthUser() {
        given(jwtProvider.validate("access-token")).willReturn(true);
        given(jwtProvider.getUserId("access-token")).willReturn(1L);
    }

    @Test
    void signup() throws Exception {
        willDoNothing().given(authService).signup(any());

        mockMvc.perform(post("/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest("testuser", "password123", "테스터"))))
                .andExpect(status().isCreated())
                .andDo(document("auth/signup",
                        requestFields(
                                fieldWithPath("username").description("아이디 (4~50자)"),
                                fieldWithPath("password").description("비밀번호 (최소 8자)"),
                                fieldWithPath("nickname").description("닉네임")
                        )
                ));
    }

    @Test
    void refreshWithoutCookie() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andDo(document("auth/refresh-missing-cookie",
                        responseFields(
                                fieldWithPath("message").description("오류 메시지")
                        )
                ));
    }

    @Test
    void login() throws Exception {
        given(authService.login(any())).willReturn(new Tokens("access-token", "refresh-token"));

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("testuser", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=refresh-token")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andDo(document("auth/login",
                        requestFields(
                                fieldWithPath("username").description("아이디"),
                                fieldWithPath("password").description("비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("accessToken").description("액세스 토큰")
                        )
                ));
    }

    @Test
    void refresh() throws Exception {
        given(authService.refresh(any())).willReturn(new Tokens("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=new-refresh-token")))
                .andDo(document("auth/refresh",
                        responseFields(
                                fieldWithPath("accessToken").description("새 액세스 토큰")
                        )
                ));
    }

    @Test
    void logout() throws Exception {
        mockAuthUser();
        willDoNothing().given(authService).logout(any());

        mockMvc.perform(delete("/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andDo(document("auth/logout"));
    }
}
