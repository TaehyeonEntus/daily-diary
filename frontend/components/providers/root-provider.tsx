'use client';

import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { SWRConfig } from 'swr';
import client from '@/lib/api/client';
import { usersApi } from '@/lib/api/users';
import { UserResponse } from '@/lib/api/types';

interface AuthContextType {
  user: UserResponse | null;
  isLoading: boolean;
  mutate: (user: UserResponse | null) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function RootProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // 앱 로드시 현재 사용자 정보 가져오기 및 시간 테마 설정
  useEffect(() => {
    const initAuth = async () => {
      // 시간대별 테마 설정
      const hour = new Date().getHours();
      let theme = 'day';
      if (hour >= 5 && hour < 8) theme = 'dawn';
      else if (hour >= 17 && hour < 20) theme = 'sunset';
      else if (hour >= 20 || hour < 5) theme = 'night';
      
      document.documentElement.setAttribute('data-theme', theme);

      const token = localStorage.getItem('accessToken');
      if (token) {
        try {
          const res = await usersApi.getMe();
          setUser(res.data);
        } catch (err) {
          console.error('인증 실패:', err);
          localStorage.removeItem('accessToken');
        }
      }
      setIsLoading(false);
    };

    initAuth();
  }, []);

  return (
    <SWRConfig
      value={{
        fetcher: (url: string) => client.get(url).then((res) => res.data),
        shouldRetryOnError: false,
        revalidateOnFocus: false, // 창 포커스 시 자동 요청 방지
        revalidateOnReconnect: false, // 네트워크 재연결 시 자동 요청 방지
      }}
    >
      <AuthContext.Provider value={{ user, isLoading, mutate: setUser }}>
        {children}
      </AuthContext.Provider>
    </SWRConfig>
  );
}

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within a RootProvider');
  }
  return context;
};
