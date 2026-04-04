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

    @PostMapping
    public ResponseEntity<PostDetailResponse> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreatePostRequest request) {

        PostDetailResponse response = postService.create(userDetails.getUserId(), request);

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDetailResponse> getPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {

        Long userId = userDetails == null
                ? null
                : userDetails.getUserId();

        PostDetailResponse response = postService.get(id, userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/hot")
    public ResponseEntity<PostListResponse> getHotList() {
        return ResponseEntity.ok(postService.getHotList());
    }

    @GetMapping
    public ResponseEntity<PostPageResponse> getList(
            @RequestParam(defaultValue = "DEFAULT") SearchType searchType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "DATE") OrderType orderType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PostSearchCondition search = new PostSearchCondition(searchType, keyword);
        PostPageResponse response = postService.getList(search, orderType, page, size);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request) {

        postService.update(userDetails.getUserId(), id, request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {

        postService.delete(userDetails.getUserId(), id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/likes")
    public ResponseEntity<Void> like(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {

        postService.like(userDetails.getUserId(), id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/likes")
    public ResponseEntity<Void> unlike(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {

        postService.unlike(userDetails.getUserId(), id);

        return ResponseEntity.noContent().build();
    }
}
