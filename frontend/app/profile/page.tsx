'use client';

import { useAuth } from '@/components/providers/root-provider';
import { useRouter } from 'next/navigation';
import { useEffect, useState, useTransition, ViewTransition } from 'react';
import { authApi } from '@/lib/api/auth';
import { usersApi } from '@/lib/api/users';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardSection } from '@/components/ui/Card';

export default function ProfilePage() {
  const { user, isLoading, mutate } = useAuth();
  const router = useRouter();
  const [nickname, setNickname] = useState('');
  const [isUpdating, setIsUpdating] = useState(false);
  const [isEditingNickname, setIsEditingNickname] = useState(false);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [isUpdatingPassword, setIsUpdatingPassword] = useState(false);
  const [isEditingPassword, setIsEditingPassword] = useState(false);
  const [, startTransition] = useTransition();

  useEffect(() => {
    if (isLoading) return;
    if (!user) {
      router.push('/login');
    } else {
      setNickname(user.nickname);
    }
  }, [user, isLoading, router]);

  const handleLogout = async () => {
    if (!confirm('로그아웃 하시겠습니까?')) return;
    try {
      await authApi.logout();
      mutate(null);
      router.push('/');
    } catch (err) {
      console.error('Logout failed', err);
    }
  };

  const handleUpdateNickname = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    
    if (!isEditingNickname) {
      setIsEditingNickname(true);
      return;
    }

    if (nickname === user.nickname) {
      setIsEditingNickname(false);
      return;
    }

    setIsUpdating(true);
    try {
      await usersApi.updateNickname({ nickname });
      startTransition(() => {
        mutate({ ...user, nickname }, false);
      });
      alert('닉네임이 수정되었습니다.');
      setIsEditingNickname(false);
    } catch (err) {
      console.error('Nickname update failed:', err);
      alert('닉네임 수정에 실패했습니다.');
    } finally {
      setIsUpdating(false);
    }
  };

  const handleUpdatePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;

    if (!isEditingPassword) {
      setIsEditingPassword(true);
      return;
    }

    if (!currentPassword || !newPassword) {
      alert('현재 비밀번호와 새 비밀번호를 모두 입력해주세요.');
      return;
    }

    setIsUpdatingPassword(true);
    try {
      await usersApi.updatePassword({ currentPassword, newPassword });
      alert('비밀번호가 수정되었습니다.');
      setCurrentPassword('');
      setNewPassword('');
      setIsEditingPassword(false);
    } catch (err) {
      console.error('Password update failed:', err);
      alert('비밀번호 수정에 실패했습니다. 현재 비밀번호를 확인해주세요.');
    } finally {
      setIsUpdatingPassword(false);
    }
  };

  if (isLoading || !user) return <div className="text-center py-20 text-gray-500">로딩 중...</div>;

  return (
    <div className="max-w-2xl mx-auto py-16 px-4">
      <ViewTransition>
        <h1 className="text-3xl font-extrabold text-[var(--text-main)] mb-8">내 프로필</h1>
      </ViewTransition>
      
      <ViewTransition>
        <Card className="p-8">
          <CardSection>
            <div className="flex items-center gap-6 mb-8">
              <div className="w-20 h-20 bg-[var(--primary-color)] text-white rounded-full flex items-center justify-center text-3xl font-extrabold shadow-lg">
                {user.nickname[0]}
              </div>
              <div>
                <h2 className="text-2xl font-bold text-[var(--text-main)]">{user.nickname}</h2>
                <p className="text-[var(--text-muted)]">@{user.username}</p>
              </div>
            </div>
          </CardSection>

          <CardSection title="계정 정보">
            <form onSubmit={handleUpdateNickname} className="space-y-4">
              <Input
                label="닉네임"
                required={isEditingNickname}
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
                placeholder="새로운 닉네임을 입력하세요"
                disabled={!isEditingNickname}
              />
              <Button
                type="submit"
                isLoading={isUpdating}
                className="w-full"
              >
                {isEditingNickname ? (isUpdating ? '저장 중...' : '저장하기') : '닉네임 수정'}
              </Button>
              {isEditingNickname && (
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    setIsEditingNickname(false);
                    setNickname(user.nickname);
                  }}
                  className="w-full"
                >
                  취소
                </Button>
              )}
            </form>
          </CardSection>

          <CardSection title="보안">
            <form onSubmit={handleUpdatePassword} className="space-y-4">
              <Input
                label="현재 비밀번호"
                type="password"
                required={isEditingPassword}
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                placeholder="현재 비밀번호를 입력하세요"
                disabled={!isEditingPassword}
              />
              <Input
                label="새 비밀번호"
                type="password"
                required={isEditingPassword}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="새로운 비밀번호를 입력하세요"
                disabled={!isEditingPassword}
              />
              <Button
                type="submit"
                variant="outline"
                isLoading={isUpdatingPassword}
                className="w-full"
              >
                {isEditingPassword ? (isUpdatingPassword ? '변경 중...' : '변경 완료') : '비밀번호 변경'}
              </Button>
              {isEditingPassword && (
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => {
                    setIsEditingPassword(false);
                    setCurrentPassword('');
                    setNewPassword('');
                  }}
                  className="w-full"
                >
                  취소
                </Button>
              )}
            </form>
          </CardSection>

          <div className="mt-8 pt-6 border-t border-[var(--border-color)]">
            <Button
              variant="danger"
              onClick={handleLogout}
              className="w-full"
            >
              로그아웃
            </Button>
          </div>
        </Card>
      </ViewTransition>
    </div>
  );
}
