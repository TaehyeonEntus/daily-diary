'use client';

import { useState, useTransition, ViewTransition } from 'react';
import Link from 'next/link';
import useSWR from 'swr';
import { postsApi } from '@/lib/api/posts';
import { SearchType, OrderType } from '@/lib/api/types';
import { useAuth } from '@/components/providers/root-provider';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import styles from './page.module.css';

export default function Home() {
  const { user } = useAuth();
  const [page, setPage] = useState(0);
  const [searchType, setSearchType] = useState<SearchType>('TITLE');
  const [keyword, setKeyword] = useState('');
  const [orderType, setOrderType] = useState<OrderType>('DATE');
  const [activeSearch, setActiveSearch] = useState({ 
    type: 'TITLE' as SearchType, 
    word: '',
    order: 'DATE' as OrderType
  });
  const [targetPage, setTargetPage] = useState('');
  const [, startTransition] = useTransition();

  // 인기 게시글 조회
  const { data: hotPosts, isLoading: isHotLoading } = useSWR(
    '/posts/hot',
    () => postsApi.getHotPosts().then(res => res.data)
  );

  // 일반 게시글 목록 조회
  const { data: postsData, isLoading: isPostsLoading } = useSWR(
    [`/posts`, page, activeSearch.type, activeSearch.word, activeSearch.order],
    () => postsApi.getList({ 
      page, 
      size: 10, 
      searchType: activeSearch.type,
      keyword: activeSearch.word || undefined,
      orderType: activeSearch.order
    }).then(res => res.data)
  );

  const handlePageChange = (newPage: number) => {
    startTransition(() => {
      setPage(newPage);
      setTargetPage('');
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    startTransition(() => {
      setPage(0);
      setActiveSearch({ 
        type: searchType, 
        word: keyword,
        order: orderType 
      });
    });
  };

  const handleGoToPage = (e: React.FormEvent) => {
    e.preventDefault();
    if (!postsData) return;

    const pageNum = parseInt(targetPage);
    if (isNaN(pageNum) || pageNum < 1 || pageNum > postsData.totalPages) {
      alert(`1부터 ${postsData.totalPages} 사이의 페이지 번호를 입력해주세요.`);
      return;
    }

    handlePageChange(pageNum - 1);
  };

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div>
          <h1 className="text-3xl font-extrabold" style={{ color: 'var(--text-main)' }}>오늘의 일기</h1>
          <p className="mt-1" style={{ color: 'var(--text-muted)' }}>서로의 일상을 공유하는 따뜻한 공간</p>
        </div>
        {user && (
          <Link href="/write" className={styles.writeButton}>
            일기 쓰기
          </Link>
        )}
      </header>

      {/* 검색 섹션 */}
      <div className={styles.searchContainer}>
        <form onSubmit={handleSearch} className={styles.searchForm}>
          <div className={styles.selectGroup}>
            <select 
              className={styles.select}
              aria-label="Search Type"
              value={searchType}
              onChange={(e) => setSearchType(e.target.value as SearchType)}
            >
              <option value="DEFAULT">전체 필터</option>
              <option value="TITLE">제목</option>
              <option value="CONTENT">내용</option>
              <option value="NICKNAME">작성자</option>
            </select>
            <select 
              className={styles.select}
              aria-label="Order Type"
              value={orderType}
              onChange={(e) => setOrderType(e.target.value as OrderType)}
            >
              <option value="DATE">최신순</option>
              <option value="VIEW">조회순</option>
              <option value="LIKE">좋아요순</option>
              <option value="COMMENT">댓글순</option>
            </select>
          </div>
          <Input 
            className="flex-1 min-w-[200px]"
            placeholder="검색어를 입력하세요..."
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
          <Button type="submit">
            검색
          </Button>
        </form>
      </div>

      {/* 인기 게시글 섹션 */}
      {!activeSearch.word && (
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>
            <div className={styles.titleText}>
              <span>🔥</span> 지금 핫한 일기
            </div>
          </h2>
          {isHotLoading ? (
            <div className={styles.loading}>로딩 중...</div>
          ) : (
            <ViewTransition>
              <div className={styles.hotGrid}>
                {hotPosts?.content.map((post) => (
                  <Link key={post.id} href={`/posts/${post.id}`} className={styles.hotCard}>
                    <h3>{post.title}</h3>
                    <div className={styles.hotCardMeta}>
                      <span>{post.nickname}</span>
                      <div className={styles.statList}>
                        <span>👍 {post.likeCount}</span>
                        <span>💬 {post.commentCount}</span>
                      </div>
                    </div>
                  </Link>
                ))}
                {hotPosts?.content.length === 0 && (
                  <div className="col-span-full py-10 text-center text-gray-400 rounded-xl border border-dashed border-[var(--border-color)]">
                    최근 인기 게시글이 없습니다.
                  </div>
                )}
              </div>
            </ViewTransition>
          )}
        </section>
      )}

      {/* 일반 게시글 목록 */}
      <section className={styles.section}>
        <div className={styles.sectionTitle}>
          <div className={styles.titleText}>
            <span>{activeSearch.word ? '🔍' : '📝'}</span> 
            {activeSearch.word ? `'${activeSearch.word}' 검색 결과` : '최신 일기'}
          </div>
        </div>
        {isPostsLoading ? (
          <div className={styles.loading}>일기를 불러오는 중...</div>
        ) : (
          <>
            <ViewTransition>
              <div className={styles.postList}>
                {postsData?.content.map((post) => (
                  <Link key={post.id} href={`/posts/${post.id}`} className={styles.postItem}>
                    <div className={styles.postHeader}>
                      <h3 className={styles.postTitle}>{post.title}</h3>
                      <span className="text-sm text-gray-400">
                        {new Date(post.createdAt).toLocaleString(undefined, {
                          year: 'numeric',
                          month: '2-digit',
                          day: '2-digit',
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </span>
                    </div>
                    <div className={styles.postMeta}>
                      <span>By {post.nickname}</span>
                    </div>
                    <div className={styles.postStats}>
                      <div className={styles.statItem}>
                        <span>👁️</span> {post.viewCount}
                      </div>
                      <div className={styles.statItem}>
                        <span>❤️</span> {post.likeCount}
                      </div>
                      <div className={styles.statItem}>
                        <span>💬</span> {post.commentCount}
                      </div>
                    </div>
                  </Link>
                ))}
                {postsData?.content.length === 0 && (
                  <div className="py-20 text-center text-gray-500">
                    {activeSearch.word ? '검색 결과가 없습니다.' : '아직 작성된 일기가 없습니다.'}
                  </div>
                )}
              </div>
            </ViewTransition>

            {/* 페이지네이션 */}
            {postsData && postsData.totalPages > 1 && (() => {
              const PAGE_GROUP_SIZE = 10;
              const currentGroup = Math.floor(page / PAGE_GROUP_SIZE);
              const startPage = currentGroup * PAGE_GROUP_SIZE;
              const endPage = Math.min(startPage + PAGE_GROUP_SIZE - 1, postsData.totalPages - 1);
              const pages = [];
              for (let i = startPage; i <= endPage; i++) {
                pages.push(i);
              }

              return (
                <div className={styles.pagination}>
                  <button
                    className={styles.pageButton}
                    onClick={() => handlePageChange(0)}
                    disabled={page === 0}
                    aria-label="First Page"
                  >
                    &laquo;
                  </button>
                  <button
                    className={styles.pageButton}
                    onClick={() => handlePageChange(Math.max(0, page - 10))}
                    disabled={page === 0}
                    aria-label="Previous 10 Pages"
                  >
                    &lt;
                  </button>
                  {pages.map((p) => (
                    <button
                      key={p}
                      className={`${styles.pageButton} ${page === p ? styles.pageButtonActive : ''}`}
                      onClick={() => handlePageChange(p)}
                    >
                      {p + 1}
                    </button>
                  ))}
                  <button
                    className={styles.pageButton}
                    onClick={() => handlePageChange(Math.min(postsData.totalPages - 1, page + 10))}
                    disabled={page === postsData.totalPages - 1}
                    aria-label="Next 10 Pages"
                  >
                    &gt;
                  </button>
                  <button
                    className={styles.pageButton}
                    onClick={() => handlePageChange(postsData.totalPages - 1)}
                    disabled={page === postsData.totalPages - 1}
                    aria-label="Last Page"
                  >
                    &raquo;
                  </button>

                  <form className={styles.pageInputContainer} onSubmit={handleGoToPage}>
                    <span className={styles.pageTotal}>총 {postsData.totalPages}쪽</span>
                    <input
                      type="text"
                      className={styles.pageInput}
                      placeholder="번호"
                      value={targetPage}
                      onChange={(e) => setTargetPage(e.target.value)}
                    />
                    <button type="submit" className={styles.goButton}>
                      이동
                    </button>
                  </form>
                </div>
              );
            })()}
          </>
        )}
      </section>
    </div>
  );
}
