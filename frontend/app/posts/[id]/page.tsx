'use client';

import { useState, useTransition, ViewTransition } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import useSWR, { mutate } from 'swr';
import { postsApi } from '@/lib/api/posts';
import { commentsApi } from '@/lib/api/comments';
import { useAuth } from '@/components/providers/root-provider';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import styles from './page.module.css';

export default function PostDetailPage() {
  const { id } = useParams();
  const router = useRouter();
  const { user } = useAuth();
  const postId = Number(id);
  const [, startTransition] = useTransition();
  
  const [isEditingPost, setIsEditingPost] = useState(false);
  const [editPostForm, setEditPostForm] = useState({ title: '', content: '' });
  const [commentPage, setCommentPage] = useState(0);
  const [commentContent, setCommentContent] = useState('');
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
  const [editCommentContent, setEditCommentContent] = useState('');

  const { data: post, isLoading: isPostLoading } = useSWR(
    [`/posts`, postId],
    () => postsApi.getOne(postId).then(res => {
      setEditPostForm({ title: res.data.title, content: res.data.content || '' });
      return res.data;
    })
  );

  const { data: commentsData } = useSWR(
    [`/posts/${postId}/comments`, commentPage],
    () => commentsApi.getList(postId, commentPage, 10).then(res => res.data)
  );

  const handleLike = async () => {
    if (!user || !post) return;
    const optimisticData = { ...post, like: !post.like, likeCount: post.like ? post.likeCount - 1 : post.likeCount + 1 };
    startTransition(() => {
      mutate([`/posts`, postId], optimisticData, false);
    });
    try {
      post.like ? await postsApi.unlike(postId) : await postsApi.like(postId);
    } catch (_err) {
      mutate([`/posts`, postId]);
    }
  };

  const handlePostUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!post) return;
    const optimisticData = { ...post, ...editPostForm };
    startTransition(() => {
      mutate([`/posts`, postId], optimisticData, false);
      setIsEditingPost(false);
    });
    try {
      await postsApi.update(postId, editPostForm);
    } catch (_err) {
      mutate([`/posts`, postId]);
      alert('수정에 실패했습니다.');
    }
  };

  const handlePostDelete = async () => {
    if (!confirm('정말로 이 일기를 삭제하시겠습니까?')) return;
    try {
      await postsApi.delete(postId);
      router.push('/');
    } catch (_err) {
      alert('삭제에 실패했습니다.');
    }
  };

  const handleCommentSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentContent.trim() || !commentsData) return;
    setCommentContent('');
    try {
      const res = await commentsApi.create(postId, { content: commentContent });
      // 새 댓글을 목록 맨 앞에 추가하고 totalElements 증가 (서버 요청 없이)
      const optimisticComments = {
        ...commentsData,
        content: [res.data, ...commentsData.content].slice(0, 10),
        totalElements: commentsData.totalElements + 1
      };
      mutate([`/posts/${postId}/comments`, commentPage], optimisticComments, false);
      
      if (post) {
        mutate([`/posts`, postId], { ...post, commentCount: post.commentCount + 1 }, false);
      }
    } catch (err) {
      mutate([`/posts/${postId}/comments`, commentPage]);
      alert('댓글 작성에 실패했습니다.');
    }
  };

  const handleCommentUpdate = async (commentId: number) => {
    if (!editCommentContent.trim() || !commentsData) return;
    const optimisticComments = {
      ...commentsData,
      content: commentsData.content.map(c => c.id === commentId ? { ...c, content: editCommentContent } : c)
    };
    mutate([`/posts/${postId}/comments`, commentPage], optimisticComments, false);
    setEditingCommentId(null);
    try {
      await commentsApi.update(postId, commentId, { content: editCommentContent });
    } catch (_err) {
      mutate([`/posts/${postId}/comments`, commentPage]);
      alert('댓글 수정에 실패했습니다.');
    }
  };

  const handleCommentDelete = async (commentId: number) => {
    if (!confirm('댓글을 삭제하시겠습니까?') || !commentsData) return;
    const optimisticComments = {
      ...commentsData,
      content: commentsData.content.filter(c => c.id !== commentId),
      totalElements: commentsData.totalElements - 1
    };
    mutate([`/posts/${postId}/comments`, commentPage], optimisticComments, false);
    if (post) {
      mutate([`/posts`, postId], { ...post, commentCount: Math.max(0, post.commentCount - 1) }, false);
    }
    try {
      await commentsApi.delete(postId, commentId);
    } catch (_err) {
      mutate([`/posts/${postId}/comments`, commentPage]);
      alert('댓글 삭제에 실패했습니다.');
    }
  };

  if (isPostLoading) return <div className="text-center py-20 text-gray-500">불러오는 중...</div>;
  if (!post) return <div className="text-center py-20">게시글을 찾을 수 없습니다.</div>;

  return (
    <div className={styles.container}>
      <ViewTransition>
        <article className={styles.postCard}>
          {isEditingPost ? (
            <form onSubmit={handlePostUpdate} className="space-y-4">
              <Input value={editPostForm.title} onChange={(e) => setEditPostForm(prev => ({ ...prev, title: e.target.value }))} className="text-2xl font-extrabold" />
              <textarea className={styles.textarea} style={{ minHeight: '300px' }} value={editPostForm.content} onChange={(e) => setEditPostForm(prev => ({ ...prev, content: e.target.value }))} />
              <div className="flex justify-end gap-2">
                <Button type="button" variant="secondary" onClick={() => setIsEditingPost(false)}>취소</Button>
                <Button type="submit">수정 완료</Button>
              </div>
            </form>
          ) : (
            <>
              <header className={styles.postHeader}>
                <h1 className={styles.title}>{post.title}</h1>
                <div className={styles.meta}>
                  <div><span className={styles.commentNickname} style={{ color: 'var(--text-main)' }}>{post.nickname}</span><span className="mx-2 text-gray-300">|</span><span>{new Date(post.createdAt).toLocaleString(undefined, {
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit',
                  })}</span></div>
                  <div className="flex gap-4 items-center">
                    <span>👁️ {post.viewCount}</span>
                    {user?.nickname === post.nickname && (
                      <div className={styles.adminActions}>
                        <Button variant="secondary" size="sm" onClick={() => setIsEditingPost(true)}>수정</Button>
                        <Button variant="danger" size="sm" onClick={handlePostDelete}>삭제</Button>
                      </div>
                    )}
                  </div>
                </div>
              </header>
              <div className={styles.content}>{post.content}</div>
              <div className={styles.actions}>
                <Button variant="outline" onClick={handleLike}>{post.like ? '❤️' : '🤍'} {post.likeCount}</Button>
              </div>
            </>
          )}
        </article>
      </ViewTransition>

      <section className={styles.commentSection}>
        <h2 className={styles.commentTitle}>댓글 {post.commentCount}</h2>
        {user ? (
          <form onSubmit={handleCommentSubmit} className={styles.commentInput}>
            <textarea className={styles.textarea} placeholder="따뜻한 댓글을 남겨주세요." value={commentContent} onChange={(e) => setCommentContent(e.target.value)} />
            <div className="flex justify-end mt-2">
              <Button type="submit">등록</Button>
            </div>
          </form>
        ) : (
          <div className="p-6 bg-gray-50 rounded-xl text-center text-gray-500 mb-8 border border-dashed" style={{ backgroundColor: 'rgba(0,0,0,0.05)', borderColor: 'var(--border-color)' }}>
            댓글을 작성하려면 <Link href="/login" className={styles.link} style={{ color: 'var(--primary-color)', fontWeight: '600' }}>로그인</Link>이 필요합니다.
          </div>
        )}

        <ViewTransition>
          <div className={styles.commentList}>
            {commentsData?.content.map((comment) => (
              <div key={comment.id} className={styles.commentItem}>
                <div className={styles.commentHeader}>
                  <div><span className={styles.commentNickname}>{comment.nickname}</span><span className={styles.commentDate}> {new Date(comment.createdAt).toLocaleString(undefined, {
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit',
                  })}</span></div>
                  {user?.nickname === comment.nickname && (
                    <div className={styles.adminActions}>
                      {editingCommentId === comment.id ? (
                        <div className="flex gap-2">
                          <Button size="sm" onClick={() => handleCommentUpdate(comment.id)}>저장</Button>
                          <Button size="sm" variant="secondary" onClick={() => setEditingCommentId(null)}>취소</Button>
                        </div>
                      ) : (
                        <div className="flex gap-2">
                          <Button size="sm" variant="secondary" onClick={() => { setEditingCommentId(comment.id); setEditCommentContent(comment.content); }}>수정</Button>
                          <Button size="sm" variant="danger" onClick={() => handleCommentDelete(comment.id)}>삭제</Button>
                        </div>
                      )}
                    </div>
                  )}
                </div>
                {editingCommentId === comment.id ? (
                  <textarea className={styles.textarea} style={{ minHeight: '80px', marginTop: '0.5rem' }} value={editCommentContent} onChange={(e) => setEditCommentContent(e.target.value)} autoFocus />
                ) : (
                  <p className={styles.commentContent}>{comment.content}</p>
                )}
              </div>
            ))}
          </div>
        </ViewTransition>

        {commentsData && commentsData.totalPages > 1 && (() => {
          const PAGE_GROUP_SIZE = 10;
          const currentGroup = Math.floor(commentPage / PAGE_GROUP_SIZE);
          const startPage = currentGroup * PAGE_GROUP_SIZE;
          const endPage = Math.min(startPage + PAGE_GROUP_SIZE - 1, commentsData.totalPages - 1);
          const pages = [];
          for (let i = startPage; i <= endPage; i++) pages.push(i);
          return (
            <div className={styles.pagination}>
              <button className={styles.pageButton} onClick={() => setCommentPage(0)} disabled={commentPage === 0} aria-label="First Page">&laquo;</button>
              <button className={styles.pageButton} onClick={() => setCommentPage(Math.max(0, commentPage - 10))} disabled={commentPage === 0} aria-label="Previous 10 Pages">&lt;</button>
              {pages.map((p) => (
                <button key={p} className={`${styles.pageButton} ${commentPage === p ? styles.pageButtonActive : ''}`} onClick={() => setCommentPage(p)}>{p + 1}</button>
              ))}
              <button className={styles.pageButton} onClick={() => setCommentPage(Math.min(commentsData.totalPages - 1, commentPage + 10))} disabled={commentPage === commentsData.totalPages - 1} aria-label="Next 10 Pages">&gt;</button>
              <button className={styles.pageButton} onClick={() => setCommentPage(commentsData.totalPages - 1)} disabled={commentPage === commentsData.totalPages - 1} aria-label="Last Page">&raquo;</button>
            </div>
          );
        })()}
      </section>

      <div className="mt-12 text-center">
        <button onClick={() => router.push('/')} className="text-gray-500 hover:text-indigo-600 font-medium">← 목록으로 돌아가기</button>
      </div>
    </div>
  );
}
