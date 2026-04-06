export interface UserResponse {
  id: number;
  username: string;
  nickname: string;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
}

export interface SignupRequest {
  username: string;
  password: string;
  nickname: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface Post {
  id: number;
  title: string;
  content?: string;
  nickname: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  like: boolean;
  createdAt: string;
  updatedAt?: string;
}

export type SearchType = 'DEFAULT' | 'NICKNAME' | 'TITLE' | 'CONTENT';
export type OrderType = 'DATE' | 'VIEW' | 'LIKE' | 'COMMENT';

export interface PostListParams {
  searchType?: SearchType;
  keyword?: string;
  orderType?: OrderType;
  page?: number;
  size?: number;
}

export interface PostListResponse {
  content: Post[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreatePostRequest {
  title: string;
  content: string;
}

export interface Comment {
  id: number;
  content: string;
  nickname: string;
  createdAt: string;
  updatedAt?: string;
}

export interface CommentListResponse {
  content: Comment[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateCommentRequest {
  content: string;
}

export interface UpdateNicknameRequest {
  nickname: string;
}

export interface UpdatePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
