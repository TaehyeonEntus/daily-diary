'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { authApi } from '@/lib/api/auth';
import styles from '../auth.module.css';

export default function SignupPage() {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    nickname: '',
  });
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  const router = useRouter();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    try {
      await authApi.signup(formData);
      alert('회원가입이 완료되었습니다. 로그인해주세요!');
      router.push('/login');
    } catch (err) {
      const errorResponse = err as { response?: { data?: { message?: string } } };
      setError(errorResponse.response?.data?.message || '회원가입에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h1 className={styles.title}>회원가입</h1>
        <p className={styles.subTitle}>나만의 소중한 일기장을 시작해보세요.</p>
        
        <form onSubmit={handleSubmit}>
          <div className={styles.formGroup}>
            <label className={styles.label}>아이디</label>
            <input
              type="text"
              name="username"
              required
              autoFocus
              value={formData.username}
              onChange={handleChange}
              className={styles.input}
              placeholder="4자 이상 20자 이하"
            />
          </div>
          
          <div className={styles.formGroup}>
            <label className={styles.label}>비밀번호</label>
            <input
              type="password"
              name="password"
              required
              value={formData.password}
              onChange={handleChange}
              className={styles.input}
              placeholder="8자 이상"
            />
          </div>

          <div className={styles.formGroup}>
            <label className={styles.label}>닉네임</label>
            <input
              type="text"
              name="nickname"
              required
              value={formData.nickname}
              onChange={handleChange}
              className={styles.input}
              placeholder="표시될 이름"
            />
          </div>

          {error && <p className={styles.error}>{error}</p>}

          <button
            type="submit"
            disabled={isLoading}
            className={styles.submitButton}
          >
            {isLoading ? '가입 진행 중...' : '회원가입'}
          </button>
        </form>

        <p className={styles.footer}>
          이미 계정이 있으신가요?
          <Link href="/login" className={styles.link}>
            로그인
          </Link>
        </p>
      </div>
    </div>
  );
}
