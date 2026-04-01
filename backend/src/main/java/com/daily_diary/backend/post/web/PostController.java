package com.daily_diary.backend.post.web;

import com.daily_diary.backend.global.security.CustomUserDetails;
import com.daily_diary.backend.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<PostListResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(postService.list(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDetailResponse> getPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        Long userId = userDetails != null ? userDetails.userId() : null;
        return ResponseEntity.ok(postService.getPost(id, userId));
    }

    @PostMapping
    public ResponseEntity<Void> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreatePostRequest request) {
        postService.create(userDetails.userId(), request);
        return ResponseEntity.status(201).build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PostDetailResponse> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request) {
        return ResponseEntity.ok(postService.update(userDetails.userId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        postService.delete(userDetails.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/likes")
    public ResponseEntity<Void> like(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        postService.like(userDetails.userId(), id);
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/{id}/likes")
    public ResponseEntity<Void> unlike(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        postService.unlike(userDetails.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
