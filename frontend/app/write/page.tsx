'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { postsApi } from '@/lib/api/posts';
import { useAuth } from '@/components/providers/root-provider';
import styles from './page.module.css';

export default function WritePage() {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const { user, isLoading: isAuthLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isAuthLoading && !user) {
      alert('로그인이 필요한 서비스입니다.');
      router.push('/login');
    }
  }, [user, isAuthLoading, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) {
      alert('제목과 내용을 모두 입력해주세요.');
      return;
    }

    setIsLoading(true);
    try {
      await postsApi.create({ title, content });
      router.push('/');
    } catch (err: any) {
      alert(err.response?.data?.message || '일기 저장에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  if (isAuthLoading || !user) return <div className="text-center py-20 text-gray-500">로딩 중...</div>;

  return (
    <div className={styles.container}>
      <header className={styles.titleSection}>
        <h1 className={styles.mainTitle}>오늘의 일기 쓰기</h1>
        <p className={styles.subTitle}>당신의 소중한 오늘을 기록해보세요.</p>
      </header>
      
      <form onSubmit={handleSubmit} className={styles.formCard}>
        <div className={styles.formGroup}>
          <label className={styles.label}>제목</label>
          <input
            type="text"
            required
            autoFocus
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className={styles.input}
            placeholder="오늘 하루를 한 문장으로 표현한다면?"
          />
        </div>
        
        <div className={styles.formGroup}>
          <label className={styles.label}>본문</label>
          <textarea
            required
            value={content}
            onChange={(e) => setContent(e.target.value)}
            className={styles.textarea}
            placeholder="어떤 일들이 있었나요? 당신의 생각과 감정을 자유롭게 남겨주세요."
          />
        </div>

        <div className={styles.actions}>
          <button
            type="button"
            onClick={() => router.back()}
            className={styles.cancelButton}
          >
            취소
          </button>
          <button
            type="submit"
            disabled={isLoading}
            className={styles.submitButton}
          >
            {isLoading ? '소중한 기록 저장 중...' : '기록하기'}
          </button>
        </div>
      </form>
    </div>
  );
}
