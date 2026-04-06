import client from './client';
import { Comment, CommentListResponse, CreateCommentRequest } from './types';

export const commentsApi = {
  getList: (postId: number, page = 0, size = 20) =>
    client.get<CommentListResponse>(`/posts/${postId}/comments?page=${page}&size=${size}`),

  create: (postId: number, data: CreateCommentRequest) =>
    client.post<Comment>(`/posts/${postId}/comments`, data),

  update: (postId: number, commentId: number, data: CreateCommentRequest) =>
    client.patch<void>(`/posts/${postId}/comments/${commentId}`, data),

  delete: (postId: number, commentId: number) =>
    client.delete(`/posts/${postId}/comments/${commentId}`),
};
