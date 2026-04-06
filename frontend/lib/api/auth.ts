import client from './client';
import { SignupRequest, LoginRequest, AuthResponse, UserResponse } from './types';

export const authApi = {
  signup: (data: SignupRequest) => 
    client.post<UserResponse>('/auth/signup', data),
  
  login: async (data: LoginRequest) => {
    const response = await client.post<AuthResponse>('/auth/login', data);
    const { accessToken } = response.data;
    localStorage.setItem('accessToken', accessToken);
    return response.data;
  },

  logout: () => 
    client.delete('/auth/logout').finally(() => {
      localStorage.removeItem('accessToken');
    }),

  refresh: () => 
    client.post<AuthResponse>('/auth/refresh'),
};
