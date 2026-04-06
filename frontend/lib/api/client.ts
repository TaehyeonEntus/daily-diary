import axios from 'axios';

const client = axios.create({
  baseURL: '/api',
  withCredentials: true,
});

// 요청 인터셉터: 로컬 스토리지의 accessToken을 헤더에 추가
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 토큰 갱신 중 여부와 대기 중인 요청 큐
let isRefreshing = false;
let failedQueue: { resolve: (token: string) => void; reject: (error: any) => void }[] = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token!);
    }
  });
  failedQueue = [];
};

// 응답 인터셉터: 401 에러 발생 시 토큰 갱신 시도
client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // 401 에러이고, 아직 재시도하지 않았으며, 로그인/회원가입/리프레시 요청이 아닌 경우에만 갱신 시도
    if (
      error.response?.status === 401 && 
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/login') &&
      !originalRequest.url?.includes('/auth/signup') &&
      !originalRequest.url?.includes('/auth/refresh')
    ) {
      if (isRefreshing) {
        // 이미 갱신 중이라면 큐에 추가하고 대기
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return client(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // 토큰 갱신 API 호출
        // 전역 axios 인스턴스를 사용하여 인터셉터 루프 방지
        const response = await axios.post(
          '/api/auth/refresh',
          {},
          { withCredentials: true }
        );

        const { accessToken } = response.data;
        
        if (accessToken) {
          localStorage.setItem('accessToken', accessToken);
          processQueue(null, accessToken);
          
          // 헤더 업데이트 및 원래 요청 재시도
          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          }
          return client(originalRequest);
        } else {
          throw new Error('No access token received');
        }
      } catch (refreshError) {
        processQueue(refreshError, null);
        localStorage.removeItem('accessToken');
        if (typeof window !== 'undefined') {
          window.location.href = '/login';
        }
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default client;
