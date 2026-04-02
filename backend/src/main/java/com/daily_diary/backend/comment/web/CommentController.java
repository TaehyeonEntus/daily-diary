package com.daily_diary.backend.comment.web;

import com.daily_diary.backend.comment.service.CommentService;
import com.daily_diary.backend.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<CommentListResponse> list(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = userDetails != null ? userDetails.userId() : null;
        return ResponseEntity.ok(commentService.list(postId, userId, page, size));
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentDetailResponse> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        return ResponseEntity.ok(commentService.update(commentId, userDetails.userId(), request));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        commentService.delete(commentId, userDetails.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{commentId}/likes")
    public ResponseEntity<Void> like(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        commentService.like(commentId, userDetails.userId());
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/{commentId}/likes")
    public ResponseEntity<Void> unlike(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        commentService.unlike(commentId, userDetails.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<Void> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request) {
        commentService.create(postId, userDetails.userId(), request);
        return ResponseEntity.status(201).build();
    }
}
