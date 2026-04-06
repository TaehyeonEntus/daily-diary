import client from './client';
import { UserResponse, UpdateNicknameRequest, UpdatePasswordRequest } from './types';

export const usersApi = {
  getMe: () => 
    client.get<UserResponse>('/users/me'),
  
  updateNickname: (data: UpdateNicknameRequest) => 
    client.patch<void>('/users/me/nickname', data),
  
  updatePassword: (data: UpdatePasswordRequest) => 
    client.patch<void>('/users/me/password', data),
  
  withdraw: () => 
    client.delete('/users/me').finally(() => {
      localStorage.removeItem('accessToken');
    }),
};
