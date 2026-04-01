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
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    @PostMapping
    public ResponseEntity<Void> create(@AuthenticationPrincipal CustomUserDetails userDetails,
                                        @Valid @RequestBody CreatePostRequest request) {
        postService.create(userDetails.userId(), request);
        return ResponseEntity.status(201).build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PostResponse> update(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                @PathVariable Long id,
                                                @Valid @RequestBody UpdatePostRequest request) {
        return ResponseEntity.ok(postService.update(userDetails.userId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails userDetails,
                                        @PathVariable Long id) {
        postService.delete(userDetails.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
