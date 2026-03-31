package com.daily_diary.backend.post.web;

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
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.list(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    @PostMapping
    public ResponseEntity<PostResponse> create(@AuthenticationPrincipal Long userId,
                                                @Valid @RequestBody CreatePostRequest request) {
        return ResponseEntity.status(201).body(postService.create(userId, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PostResponse> update(@AuthenticationPrincipal Long userId,
                                                @PathVariable Long id,
                                                @Valid @RequestBody UpdatePostRequest request) {
        return ResponseEntity.ok(postService.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId,
                                        @PathVariable Long id) {
        postService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
