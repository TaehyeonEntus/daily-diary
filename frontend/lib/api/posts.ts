import client from './client';
import { Post, PostListResponse, CreatePostRequest, PostListParams } from './types';

export const postsApi = {
  getList: (params: PostListParams = {}) => {
    const query = new URLSearchParams();
    if (params.searchType) query.append('searchType', params.searchType);
    if (params.keyword) query.append('keyword', params.keyword);
    if (params.orderType) query.append('orderType', params.orderType);
    if (params.page !== undefined) query.append('page', params.page.toString());
    if (params.size !== undefined) query.append('size', params.size.toString());
    
    return client.get<PostListResponse>(`/posts?${query.toString()}`);
  },
  
  getHotPosts: () => 
    client.get<PostListResponse>('/posts/hot'),

  getOne: (id: number) => 
    client.get<Post>(`/posts/${id}`),
  
  create: (data: CreatePostRequest) => 
    client.post<Post>('/posts', data),
  
  update: (id: number, data: CreatePostRequest) => 
    client.patch<Post>(`/posts/${id}`, data),
  
  delete: (id: number) => 
    client.delete(`/posts/${id}`),

  like: (id: number) =>
    client.post(`/posts/${id}/likes`),

  unlike: (id: number) =>
    client.delete(`/posts/${id}/likes`),
};
