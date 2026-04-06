'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { authApi } from '@/lib/api/auth';
import { usersApi } from '@/lib/api/users';
import { useAuth } from '@/components/providers/root-provider';
import styles from '../auth.module.css';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  const { mutate } = useAuth();
  const router = useRouter();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    try {
      await authApi.login({ username, password });
      const userRes = await usersApi.getMe();
      mutate(userRes.data);
      router.push('/');
    } catch (err: any) {
      setError(err.response?.data?.message || '로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h1 className={styles.title}>로그인</h1>
        <p className={styles.subTitle}>소중한 일상으로 다시 돌아오세요.</p>
        
        <form onSubmit={handleSubmit}>
          <div className={styles.formGroup}>
            <label className={styles.label}>아이디</label>
            <input
              type="text"
              required
              autoFocus
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className={styles.input}
              placeholder="아이디를 입력하세요"
            />
          </div>
          
          <div className={styles.formGroup}>
            <label className={styles.label}>비밀번호</label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={styles.input}
              placeholder="비밀번호를 입력하세요"
            />
          </div>

          {error && <p className={styles.error}>{error}</p>}

          <button
            type="submit"
            disabled={isLoading}
            className={styles.submitButton}
          >
            {isLoading ? '로그인 중...' : '로그인'}
          </button>
        </form>

        <p className={styles.footer}>
          계정이 없으신가요?
          <Link href="/signup" className={styles.link}>
            회원가입
          </Link>
        </p>
      </div>
    </div>
  );
}
