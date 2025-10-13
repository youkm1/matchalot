// 프론트엔드 알림 구현 가이드 (React 예시)

// 1. 🔔 헤더 알림 아이콘 컴포넌트
import { useState, useEffect } from 'react';

const NotificationBell = () => {
  const [unreadCount, setUnreadCount] = useState(0);
  const [showDropdown, setShowDropdown] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [eventSource, setEventSource] = useState(null);

  useEffect(() => {
    // SSE 연결
    const sse = new EventSource('/api/v1/notifications/stream', {
      withCredentials: true
    });

    sse.addEventListener('notification', (event) => {
      const notification = JSON.parse(event.data);
      
      // 새 알림 추가
      setNotifications(prev => [notification, ...prev]);
      setUnreadCount(prev => prev + 1);
      
      // 토스트 알림 표시
      showToast(notification);
    });

    setEventSource(sse);

    // 초기 읽지 않은 개수 로드
    fetchUnreadCount();

    return () => sse.close();
  }, []);

  const fetchUnreadCount = async () => {
    try {
      const response = await fetch('/api/v1/notifications/unread-count', {
        credentials: 'include'  // 쿠키 인증 포함
      });
      
      if (response.status === 401) {
        // 로그인 필요
        window.location.href = '/login';
        return;
      }
      
      const data = await response.json();
      setUnreadCount(data.unreadCount);
    } catch (error) {
      console.error('읽지 않은 알림 개수 조회 실패:', error);
    }
  };

  return (
    <div className="notification-container">
      <button 
        className="notification-bell"
        onClick={() => setShowDropdown(!showDropdown)}
      >
        🔔
        {unreadCount > 0 && (
          <span className="badge">{unreadCount > 99 ? '99+' : unreadCount}</span>
        )}
      </button>

      {showDropdown && (
        <NotificationDropdown 
          notifications={notifications}
          onClose={() => setShowDropdown(false)}
          onMarkAsRead={() => setUnreadCount(0)}
        />
      )}
    </div>
  );
};

// 2. 📋 알림 드롭다운 컴포넌트
const NotificationDropdown = ({ notifications, onClose, onMarkAsRead }) => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchNotifications();
  }, []);

  const fetchNotifications = async () => {
    const response = await fetch('/api/v1/notifications');
    const data = await response.json();
    setItems(data);
    setLoading(false);
  };

  const markAsRead = async (id) => {
    await fetch(`/api/v1/notifications/${id}/read`, { method: 'PUT' });
    setItems(items.map(item => 
      item.id === id ? { ...item, isRead: true } : item
    ));
  };

  const markAllAsRead = async () => {
    await fetch('/api/v1/notifications/read-all', { method: 'PUT' });
    setItems(items.map(item => ({ ...item, isRead: true })));
    onMarkAsRead();
  };

  return (
    <div className="notification-dropdown">
      <div className="dropdown-header">
        <h3>알림</h3>
        <button onClick={markAllAsRead}>모두 읽음</button>
      </div>

      <div className="dropdown-body">
        {loading && <div className="loading">로딩 중...</div>}
        
        {!loading && items.length === 0 && (
          <div className="empty">알림이 없습니다</div>
        )}

        {items.map(notification => (
          <NotificationItem 
            key={notification.id}
            notification={notification}
            onClick={() => markAsRead(notification.id)}
          />
        ))}
      </div>

      <div className="dropdown-footer">
        <a href="/notifications">모든 알림 보기</a>
      </div>
    </div>
  );
};

// 3. 📬 개별 알림 아이템 컴포넌트
const NotificationItem = ({ notification, onClick }) => {
  const getIcon = (type) => {
    switch(type) {
      case 'USER_PROMOTED': return '🎉';
      case 'MATERIAL_APPROVED': return '✅';
      case 'MATERIAL_REJECTED': return '❌';
      case 'MATCH_COMPLETED': return '🤝';
      case 'MATCH_REQUEST_RECEIVED': return '📬';
      default: return '📢';
    }
  };

  const formatTime = (timestamp) => {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now - date;
    
    if (diff < 60000) return '방금 전';
    if (diff < 3600000) return `${Math.floor(diff / 60000)}분 전`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}시간 전`;
    return `${Math.floor(diff / 86400000)}일 전`;
  };

  return (
    <div 
      className={`notification-item ${!notification.isRead ? 'unread' : ''}`}
      onClick={onClick}
    >
      <div className="notification-icon">
        {getIcon(notification.type)}
      </div>
      <div className="notification-content">
        <div className="notification-title">{notification.title}</div>
        <div className="notification-message">{notification.message}</div>
        <div className="notification-time">{formatTime(notification.createdAt)}</div>
      </div>
      {!notification.isRead && <div className="unread-dot" />}
    </div>
  );
};

// 4. 🍞 토스트 알림 컴포넌트
const showToast = (notification) => {
  const toast = document.createElement('div');
  toast.className = 'notification-toast';
  toast.innerHTML = `
    <div class="toast-content">
      <strong>${notification.title}</strong>
      <p>${notification.message}</p>
    </div>
  `;
  
  document.body.appendChild(toast);
  
  // 애니메이션
  setTimeout(() => toast.classList.add('show'), 100);
  
  // 5초 후 제거
  setTimeout(() => {
    toast.classList.remove('show');
    setTimeout(() => document.body.removeChild(toast), 300);
  }, 5000);
};

// 5. 📄 전체 알림 페이지
const NotificationPage = () => {
  const [notifications, setNotifications] = useState([]);
  const [filter, setFilter] = useState('all'); // all, unread
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchNotifications();
  }, [filter]);

  const fetchNotifications = async () => {
    const url = filter === 'unread' 
      ? '/api/v1/notifications?unread=true'
      : '/api/v1/notifications';
    
    const response = await fetch(url);
    const data = await response.json();
    setNotifications(data);
    setLoading(false);
  };

  const deleteNotification = async (id) => {
    await fetch(`/api/v1/notifications/${id}`, { method: 'DELETE' });
    setNotifications(notifications.filter(n => n.id !== id));
  };

  return (
    <div className="notification-page">
      <h1>알림</h1>
      
      <div className="filter-tabs">
        <button 
          className={filter === 'all' ? 'active' : ''}
          onClick={() => setFilter('all')}
        >
          전체
        </button>
        <button 
          className={filter === 'unread' ? 'active' : ''}
          onClick={() => setFilter('unread')}
        >
          읽지 않음
        </button>
      </div>

      <div className="notification-list">
        {notifications.map(notification => (
          <div key={notification.id} className="notification-card">
            <NotificationItem notification={notification} />
            <button 
              className="delete-btn"
              onClick={() => deleteNotification(notification.id)}
            >
              삭제
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};

// CSS 스타일 예시
const styles = `
/* 알림 벨 */
.notification-container {
  position: relative;
}

.notification-bell {
  position: relative;
  font-size: 24px;
  background: none;
  border: none;
  cursor: pointer;
}

.badge {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #ff4444;
  color: white;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 12px;
  font-weight: bold;
}

/* 드롭다운 */
.notification-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  width: 400px;
  max-height: 500px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  z-index: 1000;
}

.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #eee;
}

.dropdown-body {
  max-height: 400px;
  overflow-y: auto;
}

/* 알림 아이템 */
.notification-item {
  display: flex;
  padding: 15px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.notification-item:hover {
  background: #f8f8f8;
}

.notification-item.unread {
  background: #f0f8ff;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background: #4CAF50;
  border-radius: 50%;
  margin-left: auto;
}

/* 토스트 */
.notification-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  background: white;
  border-left: 4px solid #4CAF50;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  padding: 15px 20px;
  max-width: 350px;
  transform: translateX(400px);
  transition: transform 0.3s;
  z-index: 9999;
}

.notification-toast.show {
  transform: translateX(0);
}

/* 반응형 */
@media (max-width: 768px) {
  .notification-dropdown {
    position: fixed;
    top: 60px;
    left: 0;
    right: 0;
    width: 100%;
    border-radius: 0;
  }
  
  .notification-toast {
    left: 10px;
    right: 10px;
    max-width: none;
  }
}
`;