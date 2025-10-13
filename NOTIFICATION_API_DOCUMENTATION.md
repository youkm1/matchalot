# 📢 알림 시스템 API 문서

## 🎯 백엔드가 제공하는 기능

### 1. **실시간 알림 스트림 (SSE)**
```
GET /api/v1/notifications/stream
```
- **설명**: Server-Sent Events로 실시간 알림 수신
- **인증**: OAuth2 로그인 필요
- **응답**: text/event-stream

**프론트엔드 연동 코드:**
```javascript
const connectNotificationStream = () => {
  const eventSource = new EventSource('/api/v1/notifications/stream', {
    withCredentials: true  // 쿠키 인증 포함
  });

  // 알림 수신
  eventSource.addEventListener('notification', (event) => {
    const notification = JSON.parse(event.data);
    console.log('새 알림:', notification);
    
    // notification 객체 구조:
    // {
    //   id: 123,
    //   type: "MATCH_COMPLETED",
    //   title: "매칭 성사",
    //   message: "홍길동님과 매칭이 완료되었습니다",
    //   isRead: false,
    //   createdAt: "2024-01-13T10:30:00",
    //   relatedEntityId: "456"
    // }
  });

  // 연결 상태 확인용 heartbeat
  eventSource.addEventListener('heartbeat', () => {
    console.log('연결 유지 중...');
  });

  // 에러 처리
  eventSource.onerror = (error) => {
    console.error('SSE 연결 에러:', error);
    // 브라우저가 자동으로 재연결 시도
  };

  return eventSource;
};
```

### 2. **알림 목록 조회**
```
GET /api/v1/notifications
GET /api/v1/notifications?unread=true  // 읽지 않은 것만
```

**응답 예시:**
```json
[
  {
    "id": 1,
    "type": "USER_PROMOTED",
    "title": "회원 등급 상승",
    "message": "축하합니다! 정회원으로 승격되었습니다.",
    "isRead": false,
    "createdAt": "2024-01-13T10:30:00",
    "relatedEntityId": "123"
  },
  {
    "id": 2,
    "type": "MATERIAL_APPROVED",
    "title": "족보 승인 완료",
    "message": "업로드하신 '데이터구조' 족보가 승인되었습니다.",
    "isRead": true,
    "createdAt": "2024-01-13T09:15:00",
    "relatedEntityId": "456"
  }
]
```

### 3. **읽지 않은 알림 개수**
```
GET /api/v1/notifications/unread-count
```

**응답:**
```json
{
  "unreadCount": 5
}
```

### 4. **알림 읽음 처리**
```
PUT /api/v1/notifications/{notificationId}/read
```

**응답:**
```json
{
  "id": 1,
  "type": "MATCH_COMPLETED",
  "title": "매칭 성사",
  "message": "...",
  "isRead": true,  
  "createdAt": "2024-01-13T10:30:00",
  "relatedEntityId": "789"
}
```

### 5. **모든 알림 읽음 처리**
```
PUT /api/v1/notifications/read-all
```

**응답:**
```json
{
  "message": "모든 알림이 읽음 처리되었습니다"
}
```

### 6. **알림 삭제**
```
DELETE /api/v1/notifications/{notificationId}
```

**응답:**
```json
{
  "message": "알림이 삭제되었습니다"
}
```

## 📋 알림 타입

| Type | 설명 | 이동 경로 |
|------|------|----------|
| `USER_PROMOTED` | 회원 등급 상승 | `/profile` |
| `MATERIAL_APPROVED` | 족보 승인 | `/my-materials` |
| `MATERIAL_REJECTED` | 족보 거절 | `/my-materials` |
| `MATCH_COMPLETED` | 매칭 완료 | `/matches` |
| `MATCH_REQUEST_RECEIVED` | 매칭 요청 | `/matches` |
| `SYSTEM` | 시스템 알림 | - |

## 🔧 프론트엔드 구현 체크리스트

### 필수 기능
- [ ] SSE 연결 및 실시간 알림 수신
- [ ] 헤더에 알림 벨 아이콘 + 배지
- [ ] 알림 드롭다운 (최근 5-10개)
- [ ] 읽음/안읽음 상태 표시
- [ ] 알림 클릭시 읽음 처리

### 추가 기능
- [ ] 토스트 알림 팝업
- [ ] 브라우저 알림 권한 요청
- [ ] 알림 전체 보기 페이지 (`/notifications`)
- [ ] 알림 삭제 기능
- [ ] 알림 필터 (전체/읽지않음)

## 🎨 UI/UX 권장사항

### 알림 벨 아이콘
```jsx
<div className="notification-bell">
  <BellIcon />
  {unreadCount > 0 && (
    <span className="badge">
      {unreadCount > 99 ? '99+' : unreadCount}
    </span>
  )}
</div>
```

### 알림 아이템 디자인
```jsx
<div className={`notification-item ${!notification.isRead ? 'unread' : ''}`}>
  <div className="icon">{getIcon(notification.type)}</div>
  <div className="content">
    <h4>{notification.title}</h4>
    <p>{notification.message}</p>
    <time>{formatTime(notification.createdAt)}</time>
  </div>
  {!notification.isRead && <div className="unread-dot" />}
</div>
```

### 시간 표시 포맷
```javascript
function formatTime(timestamp) {
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now - date;
  
  if (diff < 60000) return '방금 전';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}분 전`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}시간 전`;
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}일 전`;
  
  return date.toLocaleDateString();
}
```

## ⚠️ 주의사항

1. **SSE 연결 관리**
   - 페이지 언마운트시 반드시 연결 종료
   - 네트워크 에러시 자동 재연결 처리
   
2. **인증**
   - 모든 API는 로그인 필요
   - 401 에러시 로그인 페이지로 리다이렉트

3. **성능**
   - 알림 목록은 페이지네이션 고려
   - SSE는 단일 연결 유지 (중복 연결 방지)

## 🔐 인증 처리

### Cookie 기반 인증 (현재 시스템)
```javascript
// SSE 연결시 쿠키 자동 포함
const eventSource = new EventSource('/api/v1/notifications/stream', {
  withCredentials: true  // 쿠키 포함
});

// API 호출시 쿠키 포함
const response = await fetch('/api/v1/notifications', {
  credentials: 'include'  // 쿠키 포함
});
```

### 401 Unauthorized 처리
```javascript
const handleAPIResponse = async (response) => {
  if (response.status === 401) {
    // 로그인 페이지로 리다이렉트
    window.location.href = '/login';
    return;
  }
  
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }
  
  return response.json();
};
```

## 🧪 테스트용 알림 생성 (개발용)

백엔드에서 제공하는 테스트 엔드포인트:

```
POST /api/v1/test/notification
{
  "type": "MATCH_COMPLETED",
  "title": "테스트 알림",
  "message": "이것은 테스트 알림입니다"
}

POST /api/v1/test/notification/all-types
# 모든 타입의 알림을 한번에 생성
```

## 📝 완전한 구현 예시

```javascript
// hooks/useNotifications.js
import { useState, useEffect, useCallback } from 'react';

export const useNotifications = () => {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [eventSource, setEventSource] = useState(null);

  // SSE 연결
  useEffect(() => {
    const sse = new EventSource('/api/v1/notifications/stream', {
      withCredentials: true
    });

    sse.addEventListener('notification', (event) => {
      const notification = JSON.parse(event.data);
      handleNewNotification(notification);
    });

    setEventSource(sse);
    fetchUnreadCount();

    return () => sse.close();
  }, []);

  // 새 알림 처리
  const handleNewNotification = (notification) => {
    setNotifications(prev => [notification, ...prev]);
    setUnreadCount(prev => prev + 1);
    
    // 브라우저 알림
    if (Notification.permission === 'granted') {
      new Notification(notification.title, {
        body: notification.message,
        icon: '/logo.png'
      });
    }
  };

  // 읽지 않은 개수 조회
  const fetchUnreadCount = async () => {
    const res = await fetch('/api/v1/notifications/unread-count');
    const data = await res.json();
    setUnreadCount(data.unreadCount);
  };

  // 알림 읽음 처리
  const markAsRead = async (id) => {
    await fetch(`/api/v1/notifications/${id}/read`, { method: 'PUT' });
    setNotifications(prev => 
      prev.map(n => n.id === id ? { ...n, isRead: true } : n)
    );
    setUnreadCount(prev => Math.max(0, prev - 1));
  };

  return {
    notifications,
    unreadCount,
    markAsRead,
    refetch: fetchUnreadCount
  };
};
```