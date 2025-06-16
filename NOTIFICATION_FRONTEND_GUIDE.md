# 📱 Notification System - Frontend Integration Guide

## 📋 Tổng quan
Tài liệu này hướng dẫn Frontend Developer triển khai hệ thống notification real-time với các tính năng:
- ✅ Nhận notifications real-time qua WebSocket
- ✅ Hiển thị danh sách notifications với pagination
- ✅ Đánh dấu đã đọc khi user click
- ✅ Badge hiển thị số notifications chưa đọc
- ✅ Quản lý notifications (xem, xóa)

---

## 🏗️ Kiến trúc System

### Flow hoạt động:
```
1. WebSocket Connection → Nhận real-time notifications
2. REST API → Quản lý notifications (CRUD, pagination)
3. Database → Persistent storage cho notifications
4. UI Components → Hiển thị và tương tác
```

### Notification Types:
- `VIDEO_NEEDS_URGENT_FIX`: Video cần sửa gấp (gửi cho Staff)
- `VIDEO_FIXED_COMPLETED`: Video đã sửa xong (gửi cho Admin/Sale)

---

## 🔌 WebSocket Integration

### 1. Dependencies cần thiết
```bash
# For React/Vue/Angular
npm install sockjs-client @stomp/stompjs

# For plain JavaScript  
npm install sockjs-client stompjs
```

### 2. WebSocket Connection Setup

#### React Example:
```jsx
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class NotificationService {
  constructor() {
    this.client = null;
    this.connected = false;
    this.subscriptions = new Map();
  }

  // Kết nối WebSocket
  connect(username, onNotificationReceived) {
    // Tạo SockJS connection
    const socket = new SockJS('http://localhost:8080/ws');
    
    this.client = new Client({
      webSocketFactory: () => socket,
      debug: (str) => console.log('STOMP: ' + str),
      reconnectDelay: 5000, // Auto-reconnect after 5s
      
      onConnect: (frame) => {
        console.log('Connected to WebSocket:', frame);
        this.connected = true;
        
        // Subscribe to user's notification channel
        this.subscribeToNotifications(username, onNotificationReceived);
      },
      
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
      },
      
      onDisconnect: () => {
        console.log('Disconnected from WebSocket');
        this.connected = false;
      }
    });

    this.client.activate();
  }

  // Subscribe to notifications
  subscribeToNotifications(username, callback) {
    if (!this.client || !this.connected) {
      console.error('WebSocket not connected');
      return;
    }

    const destination = `/user/${username}/notifications`;
    
    const subscription = this.client.subscribe(destination, (message) => {
      try {
        const notification = JSON.parse(message.body);
        console.log('Received notification:', notification);
        
        // Callback to handle notification in UI
        callback(notification);
        
        // Optional: Send acknowledgment
        this.sendAcknowledgment(notification.id);
        
      } catch (error) {
        console.error('Error parsing notification:', error);
      }
    });

    this.subscriptions.set('notifications', subscription);
  }

  // Send acknowledgment (optional)
  sendAcknowledgment(notificationId) {
    if (this.client && this.connected) {
      this.client.publish({
        destination: '/app/notification-received',
        body: notificationId.toString()
      });
    }
  }

  // Disconnect
  disconnect() {
    if (this.client) {
      this.client.deactivate();
    }
  }
}

// Usage in React Component
const NotificationComponent = () => {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const notificationService = useRef(new NotificationService());

  useEffect(() => {
    const username = getCurrentUsername(); // Get from your auth context
    
    // Handle received notifications
    const handleNotification = (notification) => {
      // Show toast notification
      showToast(notification.title, notification.message);
      
      // Add to local state
      setNotifications(prev => [notification, ...prev]);
      
      // Update unread count
      setUnreadCount(prev => prev + 1);
      
      // Optional: Play sound
      playNotificationSound();
    };

    // Connect WebSocket
    notificationService.current.connect(username, handleNotification);

    // Cleanup on unmount
    return () => {
      notificationService.current.disconnect();
    };
  }, []);

  return (
    <div>
      {/* Your notification UI components */}
    </div>
  );
};
```

#### Vue.js Example:
```vue
<template>
  <div>
    <!-- Notification Bell Icon -->
    <div class="notification-bell" @click="toggleNotificationPanel">
      <i class="fas fa-bell"></i>
      <span v-if="unreadCount > 0" class="badge">{{ unreadCount }}</span>
    </div>

    <!-- Notification Panel -->
    <div v-if="showPanel" class="notification-panel">
      <!-- Content here -->
    </div>
  </div>
</template>

<script>
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export default {
  data() {
    return {
      client: null,
      connected: false,
      notifications: [],
      unreadCount: 0,
      showPanel: false
    };
  },

  mounted() {
    this.connectWebSocket();
  },

  beforeUnmount() {
    this.disconnectWebSocket();
  },

  methods: {
    connectWebSocket() {
      const socket = new SockJS('http://localhost:8080/ws');
      const username = this.$store.getters.currentUser.name;
      
      this.client = new Client({
        webSocketFactory: () => socket,
        
        onConnect: () => {
          this.connected = true;
          this.subscribeToNotifications(username);
        },
        
        onStompError: (frame) => {
          console.error('STOMP error:', frame);
        }
      });

      this.client.activate();
    },

    subscribeToNotifications(username) {
      const destination = `/user/${username}/notifications`;
      
      this.client.subscribe(destination, (message) => {
        const notification = JSON.parse(message.body);
        this.handleNewNotification(notification);
      });
    },

    handleNewNotification(notification) {
      // Show toast
      this.$toast.info(notification.title);
      
      // Update state
      this.notifications.unshift(notification);
      this.unreadCount++;
    },

    disconnectWebSocket() {
      if (this.client) {
        this.client.deactivate();
      }
    }
  }
};
</script>
```

---

## 🔗 REST API Integration

### 1. API Base Configuration
```javascript
const API_BASE_URL = 'http://localhost:8080/api/v1';

// Axios interceptor để thêm JWT token
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('authToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

### 2. Notification API Service

```javascript
class NotificationAPI {
  
  // Lấy danh sách notifications với pagination
  static async getNotifications(params = {}) {
    const {
      page = 0,
      size = 10,
      sortBy = 'createdAt',
      sortDirection = 'desc',
      isRead = null // null = all, true = read only, false = unread only
    } = params;

    const queryParams = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      sortBy,
      sortDirection
    });

    if (isRead !== null) {
      queryParams.append('isRead', isRead.toString());
    }

    try {
      const response = await axios.get(
        `${API_BASE_URL}/notifications?${queryParams}`
      );
      return response.data;
    } catch (error) {
      console.error('Error fetching notifications:', error);
      throw error;
    }
  }

  // Đánh dấu notification đã đọc
  static async markAsRead(notificationId) {
    try {
      const response = await axios.put(
        `${API_BASE_URL}/notifications/${notificationId}/read`
      );
      return response.data;
    } catch (error) {
      console.error('Error marking notification as read:', error);
      throw error;
    }
  }

  // Đánh dấu tất cả đã đọc
  static async markAllAsRead() {
    try {
      const response = await axios.put(
        `${API_BASE_URL}/notifications/mark-all-read`
      );
      return response.data;
    } catch (error) {
      console.error('Error marking all as read:', error);
      throw error;
    }
  }

  // Lấy số notifications chưa đọc
  static async getUnreadCount() {
    try {
      const response = await axios.get(
        `${API_BASE_URL}/notifications/unread-count`
      );
      return response.data.data.unreadCount;
    } catch (error) {
      console.error('Error fetching unread count:', error);
      throw error;
    }
  }

  // Lấy notifications gần đây
  static async getRecentNotifications(limit = 10) {
    try {
      const response = await axios.get(
        `${API_BASE_URL}/notifications/recent?limit=${limit}`
      );
      return response.data;
    } catch (error) {
      console.error('Error fetching recent notifications:', error);
      throw error;
    }
  }

  // Xóa notification
  static async deleteNotification(notificationId) {
    try {
      const response = await axios.delete(
        `${API_BASE_URL}/notifications/${notificationId}`
      );
      return response.data;
    } catch (error) {
      console.error('Error deleting notification:', error);
      throw error;
    }
  }

  // Lấy chi tiết notification
  static async getNotificationById(notificationId) {
    try {
      const response = await axios.get(
        `${API_BASE_URL}/notifications/${notificationId}`
      );
      return response.data;
    } catch (error) {
      console.error('Error fetching notification details:', error);
      throw error;
    }
  }
}
```

### 3. API Response Formats

#### Success Response:
```json
{
  "success": true,
  "message": "Lấy danh sách notifications thành công",
  "data": [
    {
      "id": 123,
      "videoId": 456,
      "type": "VIDEO_NEEDS_URGENT_FIX",
      "title": "Video cần sửa gấp!",
      "message": "Video của khách hàng 'ABC Corp' cần được sửa gấp...",
      "customerName": "ABC Corp",
      "sender": "admin_user",
      "recipient": "staff_user",
      "newStatus": "CAN_SUA_GAP",
      "oldStatus": "DA_GUI",
      "isRead": false,
      "createdAt": "2024-01-15T10:30:00",
      "readAt": null,
      "tenantId": "tenant_001"
    }
  ],
  "pagination": {
    "currentPage": 0,
    "totalPages": 5,
    "totalElements": 45,
    "pageSize": 10,
    "hasNext": true,
    "hasPrevious": false,
    "isFirst": true,
    "isLast": false
  },
  "timestamp": 1705312200000
}
```

#### Error Response:
```json
{
  "success": false,
  "message": "Không tìm thấy notification với ID 123 cho user staff_user",
  "data": null,
  "timestamp": 1705312200000,
  "status": 404,
  "error": "Not Found"
}
```

---

## 🎨 UI Components

### 1. Notification Bell Component

```jsx
// React Component
const NotificationBell = () => {
  const [unreadCount, setUnreadCount] = useState(0);
  const [showDropdown, setShowDropdown] = useState(false);
  const [recentNotifications, setRecentNotifications] = useState([]);

  // Fetch unread count on mount
  useEffect(() => {
    loadUnreadCount();
    loadRecentNotifications();
  }, []);

  const loadUnreadCount = async () => {
    try {
      const count = await NotificationAPI.getUnreadCount();
      setUnreadCount(count);
    } catch (error) {
      console.error('Failed to load unread count:', error);
    }
  };

  const loadRecentNotifications = async () => {
    try {
      const response = await NotificationAPI.getRecentNotifications(5);
      setRecentNotifications(response.data);
    } catch (error) {
      console.error('Failed to load recent notifications:', error);
    }
  };

  const handleNotificationClick = async (notification) => {
    if (!notification.isRead) {
      try {
        await NotificationAPI.markAsRead(notification.id);
        setUnreadCount(prev => Math.max(0, prev - 1));
        
        // Update local state
        setRecentNotifications(prev => 
          prev.map(n => 
            n.id === notification.id 
              ? { ...n, isRead: true, readAt: new Date().toISOString() }
              : n
          )
        );
      } catch (error) {
        console.error('Failed to mark as read:', error);
      }
    }

    // Navigate to video detail or other action
    if (notification.videoId) {
      navigate(`/videos/${notification.videoId}`);
    }

    setShowDropdown(false);
  };

  return (
    <div className="notification-bell-container">
      <button 
        className="notification-bell"
        onClick={() => setShowDropdown(!showDropdown)}
      >
        <i className="fas fa-bell"></i>
        {unreadCount > 0 && (
          <span className="notification-badge">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {showDropdown && (
        <div className="notification-dropdown">
          <div className="notification-header">
            <h4>Notifications</h4>
            <button onClick={() => setShowDropdown(false)}>
              <i className="fas fa-times"></i>
            </button>
          </div>

          <div className="notification-list">
            {recentNotifications.length > 0 ? (
              recentNotifications.map(notification => (
                <NotificationItem 
                  key={notification.id}
                  notification={notification}
                  onClick={handleNotificationClick}
                />
              ))
            ) : (
              <div className="no-notifications">
                <p>Không có notifications mới</p>
              </div>
            )}
          </div>

          <div className="notification-footer">
            <button onClick={() => navigate('/notifications')}>
              Xem tất cả
            </button>
            {unreadCount > 0 && (
              <button onClick={markAllAsRead}>
                Đánh dấu tất cả đã đọc
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
```

### 2. Notification Item Component

```jsx
const NotificationItem = ({ notification, onClick }) => {
  const getNotificationIcon = (type) => {
    switch (type) {
      case 'VIDEO_NEEDS_URGENT_FIX':
        return <i className="fas fa-exclamation-triangle text-warning"></i>;
      case 'VIDEO_FIXED_COMPLETED':
        return <i className="fas fa-check-circle text-success"></i>;
      default:
        return <i className="fas fa-info-circle text-info"></i>;
    }
  };

  const formatTimeAgo = (timestamp) => {
    const now = new Date();
    const notificationTime = new Date(timestamp);
    const diffInMinutes = Math.floor((now - notificationTime) / (1000 * 60));

    if (diffInMinutes < 1) return 'Vừa xong';
    if (diffInMinutes < 60) return `${diffInMinutes} phút trước`;
    if (diffInMinutes < 1440) return `${Math.floor(diffInMinutes / 60)} giờ trước`;
    return `${Math.floor(diffInMinutes / 1440)} ngày trước`;
  };

  return (
    <div 
      className={`notification-item ${notification.isRead ? 'read' : 'unread'}`}
      onClick={() => onClick(notification)}
    >
      <div className="notification-icon">
        {getNotificationIcon(notification.type)}
      </div>
      
      <div className="notification-content">
        <div className="notification-title">{notification.title}</div>
        <div className="notification-message">{notification.message}</div>
        <div className="notification-meta">
          <span className="customer-name">{notification.customerName}</span>
          <span className="timestamp">{formatTimeAgo(notification.createdAt)}</span>
        </div>
      </div>

      {!notification.isRead && (
        <div className="unread-indicator"></div>
      )}
    </div>
  );
};
```

### 3. Notification List Page

```jsx
const NotificationListPage = () => {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    currentPage: 0,
    totalPages: 0,
    totalElements: 0,
    pageSize: 10
  });
  const [filter, setFilter] = useState({
    isRead: null, // null = all, true = read, false = unread
    sortBy: 'createdAt',
    sortDirection: 'desc'
  });

  useEffect(() => {
    loadNotifications();
  }, [pagination.currentPage, filter]);

  const loadNotifications = async () => {
    setLoading(true);
    try {
      const response = await NotificationAPI.getNotifications({
        page: pagination.currentPage,
        size: pagination.pageSize,
        ...filter
      });

      setNotifications(response.data);
      setPagination(response.pagination);
    } catch (error) {
      console.error('Failed to load notifications:', error);
      // Show error toast
    } finally {
      setLoading(false);
    }
  };

  const handleNotificationClick = async (notification) => {
    if (!notification.isRead) {
      try {
        await NotificationAPI.markAsRead(notification.id);
        
        // Update local state
        setNotifications(prev => 
          prev.map(n => 
            n.id === notification.id 
              ? { ...n, isRead: true, readAt: new Date().toISOString() }
              : n
          )
        );
      } catch (error) {
        console.error('Failed to mark as read:', error);
      }
    }

    // Navigate or show details
    if (notification.videoId) {
      navigate(`/videos/${notification.videoId}`);
    }
  };

  const handleDeleteNotification = async (notificationId) => {
    try {
      await NotificationAPI.deleteNotification(notificationId);
      setNotifications(prev => prev.filter(n => n.id !== notificationId));
      // Show success toast
    } catch (error) {
      console.error('Failed to delete notification:', error);
      // Show error toast
    }
  };

  const handleFilterChange = (newFilter) => {
    setFilter({ ...filter, ...newFilter });
    setPagination(prev => ({ ...prev, currentPage: 0 }));
  };

  return (
    <div className="notification-list-page">
      <div className="page-header">
        <h1>Notifications</h1>
        
        {/* Filter Controls */}
        <div className="filter-controls">
          <select 
            value={filter.isRead === null ? 'all' : filter.isRead.toString()}
            onChange={(e) => {
              const value = e.target.value;
              handleFilterChange({
                isRead: value === 'all' ? null : value === 'true'
              });
            }}
          >
            <option value="all">Tất cả</option>
            <option value="false">Chưa đọc</option>
            <option value="true">Đã đọc</option>
          </select>

          <button 
            onClick={() => NotificationAPI.markAllAsRead().then(loadNotifications)}
            disabled={notifications.every(n => n.isRead)}
          >
            Đánh dấu tất cả đã đọc
          </button>
        </div>
      </div>

      {/* Notification List */}
      <div className="notification-list">
        {loading ? (
          <div className="loading-spinner">Loading...</div>
        ) : notifications.length > 0 ? (
          notifications.map(notification => (
            <div key={notification.id} className="notification-row">
              <NotificationItem 
                notification={notification}
                onClick={handleNotificationClick}
              />
              <button 
                className="delete-btn"
                onClick={() => handleDeleteNotification(notification.id)}
              >
                <i className="fas fa-trash"></i>
              </button>
            </div>
          ))
        ) : (
          <div className="empty-state">
            <p>Không có notifications</p>
          </div>
        )}
      </div>

      {/* Pagination */}
      {pagination.totalPages > 1 && (
        <Pagination 
          currentPage={pagination.currentPage}
          totalPages={pagination.totalPages}
          onPageChange={(page) => setPagination(prev => ({ ...prev, currentPage: page }))}
        />
      )}
    </div>
  );
};
```

---

## 🎨 CSS Styles

```css
/* Notification Bell */
.notification-bell-container {
  position: relative;
  display: inline-block;
}

.notification-bell {
  position: relative;
  background: none;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: background-color 0.2s;
}

.notification-bell:hover {
  background-color: rgba(0, 0, 0, 0.1);
}

.notification-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  background: #ff4444;
  color: white;
  border-radius: 50%;
  min-width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.7rem;
  font-weight: bold;
}

/* Notification Dropdown */
.notification-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  width: 350px;
  max-height: 500px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  overflow: hidden;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #eee;
  background: #f8f9fa;
}

.notification-header h4 {
  margin: 0;
  font-size: 1.1rem;
}

.notification-list {
  max-height: 300px;
  overflow-y: auto;
}

.notification-footer {
  padding: 12px 16px;
  border-top: 1px solid #eee;
  background: #f8f9fa;
  display: flex;
  justify-content: space-between;
}

.notification-footer button {
  background: none;
  border: none;
  color: #007bff;
  cursor: pointer;
  font-size: 0.9rem;
}

.notification-footer button:hover {
  text-decoration: underline;
}

/* Notification Item */
.notification-item {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
  position: relative;
}

.notification-item:hover {
  background-color: #f8f9fa;
}

.notification-item.unread {
  background-color: #fff3cd;
}

.notification-item.unread:hover {
  background-color: #ffeaa7;
}

.notification-icon {
  margin-right: 12px;
  font-size: 1.2rem;
  display: flex;
  align-items: flex-start;
  padding-top: 2px;
}

.notification-content {
  flex: 1;
}

.notification-title {
  font-weight: 600;
  font-size: 0.9rem;
  margin-bottom: 4px;
  color: #333;
}

.notification-message {
  font-size: 0.8rem;
  color: #666;
  margin-bottom: 6px;
  line-height: 1.3;
}

.notification-meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.75rem;
  color: #999;
}

.unread-indicator {
  position: absolute;
  right: 12px;
  top: 12px;
  width: 8px;
  height: 8px;
  background: #007bff;
  border-radius: 50%;
}

/* Notification List Page */
.notification-list-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.filter-controls {
  display: flex;
  gap: 12px;
  align-items: center;
}

.filter-controls select {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
}

.filter-controls button {
  padding: 6px 12px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
}

.filter-controls button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.notification-row {
  display: flex;
  align-items: center;
  border: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 8px;
  background: white;
}

.notification-row .notification-item {
  flex: 1;
  border: none;
  margin: 0;
}

.delete-btn {
  padding: 8px 12px;
  background: none;
  border: none;
  color: #dc3545;
  cursor: pointer;
  border-radius: 0 8px 8px 0;
}

.delete-btn:hover {
  background: #f8f9fa;
}

.loading-spinner {
  text-align: center;
  padding: 40px;
  color: #666;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
}

/* Responsive */
@media (max-width: 768px) {
  .notification-dropdown {
    width: 300px;
  }
  
  .notification-list-page {
    padding: 12px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
}
```

---

## 🔔 Toast Notifications

### Implementation với react-toastify:
```bash
npm install react-toastify
```

```jsx
import { toast } from 'react-toastify';

// In your notification handler
const handleNewNotification = (notification) => {
  // Show toast based on notification type
  const toastOptions = {
    position: "top-right",
    autoClose: 5000,
    hideProgressBar: false,
    closeOnClick: true,
    pauseOnHover: true,
    draggable: true,
  };

  switch (notification.type) {
    case 'VIDEO_NEEDS_URGENT_FIX':
      toast.warn(
        <NotificationToast notification={notification} />, 
        toastOptions
      );
      break;
    
    case 'VIDEO_FIXED_COMPLETED':
      toast.success(
        <NotificationToast notification={notification} />, 
        toastOptions
      );
      break;
    
    default:
      toast.info(
        <NotificationToast notification={notification} />, 
        toastOptions
      );
  }
};

const NotificationToast = ({ notification }) => (
  <div className="notification-toast">
    <div className="toast-title">{notification.title}</div>
    <div className="toast-message">{notification.message}</div>
    <div className="toast-customer">Khách hàng: {notification.customerName}</div>
  </div>
);
```

---

## 🔧 Advanced Features

### 1. Sound Notifications
```javascript
class SoundManager {
  constructor() {
    this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
    this.sounds = {};
  }

  async loadSound(name, url) {
    try {
      const response = await fetch(url);
      const audioBuffer = await response.arrayBuffer();
      const decodedAudio = await this.audioContext.decodeAudioData(audioBuffer);
      this.sounds[name] = decodedAudio;
    } catch (error) {
      console.error(`Failed to load sound ${name}:`, error);
    }
  }

  playSound(name) {
    if (this.sounds[name]) {
      const source = this.audioContext.createBufferSource();
      source.buffer = this.sounds[name];
      source.connect(this.audioContext.destination);
      source.start();
    }
  }
}

// Usage
const soundManager = new SoundManager();
soundManager.loadSound('urgent', '/sounds/urgent-notification.mp3');
soundManager.loadSound('success', '/sounds/success-notification.mp3');

// In notification handler
const handleNewNotification = (notification) => {
  // Play appropriate sound
  if (notification.type === 'VIDEO_NEEDS_URGENT_FIX') {
    soundManager.playSound('urgent');
  } else {
    soundManager.playSound('success');
  }
};
```

### 2. Browser Push Notifications
```javascript
class PushNotificationManager {
  static async requestPermission() {
    if ('Notification' in window) {
      const permission = await Notification.requestPermission();
      return permission === 'granted';
    }
    return false;
  }

  static showBrowserNotification(notification) {
    if (Notification.permission === 'granted') {
      const browserNotification = new Notification(notification.title, {
        body: notification.message,
        icon: '/icons/notification-icon.png',
        badge: '/icons/notification-badge.png',
        tag: `notification-${notification.id}`,
        data: notification
      });

      browserNotification.onclick = () => {
        window.focus();
        // Navigate to video or notification detail
        if (notification.videoId) {
          window.location.href = `/videos/${notification.videoId}`;
        }
        browserNotification.close();
      };

      // Auto close after 10 seconds
      setTimeout(() => browserNotification.close(), 10000);
    }
  }
}

// Request permission on app start
useEffect(() => {
  PushNotificationManager.requestPermission();
}, []);

// Use in notification handler
const handleNewNotification = (notification) => {
  // Show browser notification if tab is not active
  if (document.hidden) {
    PushNotificationManager.showBrowserNotification(notification);
  }
  
  // Show in-app notification regardless
  showToast(notification);
};
```

### 3. Notification Preferences
```jsx
const NotificationSettings = () => {
  const [preferences, setPreferences] = useState({
    enableSound: true,
    enableBrowserNotifications: true,
    enableEmailNotifications: false,
    notificationTypes: {
      VIDEO_NEEDS_URGENT_FIX: true,
      VIDEO_FIXED_COMPLETED: true
    }
  });

  const updatePreference = (key, value) => {
    setPreferences(prev => ({
      ...prev,
      [key]: value
    }));
    
    // Save to localStorage or backend
    localStorage.setItem('notificationPreferences', JSON.stringify({
      ...preferences,
      [key]: value
    }));
  };

  return (
    <div className="notification-settings">
      <h3>Notification Preferences</h3>
      
      <div className="setting-item">
        <label>
          <input
            type="checkbox"
            checked={preferences.enableSound}
            onChange={(e) => updatePreference('enableSound', e.target.checked)}
          />
          Enable notification sounds
        </label>
      </div>

      <div className="setting-item">
        <label>
          <input
            type="checkbox"
            checked={preferences.enableBrowserNotifications}
            onChange={(e) => updatePreference('enableBrowserNotifications', e.target.checked)}
          />
          Enable browser notifications
        </label>
      </div>

      <div className="setting-section">
        <h4>Notification Types</h4>
        {Object.entries(preferences.notificationTypes).map(([type, enabled]) => (
          <div key={type} className="setting-item">
            <label>
              <input
                type="checkbox"
                checked={enabled}
                onChange={(e) => updatePreference('notificationTypes', {
                  ...preferences.notificationTypes,
                  [type]: e.target.checked
                })}
              />
              {type === 'VIDEO_NEEDS_URGENT_FIX' ? 'Video cần sửa gấp' : 'Video đã sửa xong'}
            </label>
          </div>
        ))}
      </div>
    </div>
  );
};
```

---

## 🚀 Testing

### 1. WebSocket Connection Test
```javascript
// Test endpoint: GET /api/v1/notifications/test
const testNotification = async () => {
  try {
    const response = await axios.get(
      `${API_BASE_URL}/notifications/test?username=testuser&message=Test message`
    );
    console.log('Test notification result:', response.data);
  } catch (error) {
    console.error('Test failed:', error);
  }
};
```

### 2. Manual Testing Scenarios

1. **Basic Connection Test:**
   - Open browser dev tools
   - Connect WebSocket
   - Check console for connection logs
   - Send test notification via API

2. **Notification Flow Test:**
   - Login as Admin → Update video delivery status to "CAN_SUA_GAP"
   - Check if Staff user receives real-time notification
   - Login as Staff → Update video status to "DA_SUA_XONG"  
   - Check if Admin receives notification

3. **UI Interaction Test:**
   - Click notification → Should mark as read
   - Check unread badge updates
   - Test pagination in notification list
   - Test delete notification functionality

---

## ⚠️ Error Handling & Best Practices

### 1. Connection Management
```javascript
class RobustNotificationService {
  constructor() {
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectInterval = 5000;
  }

  connect() {
    // ... connection code ...
    
    this.client.onStompError = (frame) => {
      console.error('STOMP error:', frame);
      this.handleConnectionError();
    };

    this.client.onWebSocketClose = () => {
      console.log('WebSocket closed');
      this.handleConnectionError();
    };
  }

  handleConnectionError() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      
      setTimeout(() => {
        this.connect();
      }, this.reconnectInterval);
    } else {
      console.error('Max reconnection attempts reached');
      // Show user notification about connection issues
      this.showConnectionErrorNotification();
    }
  }

  showConnectionErrorNotification() {
    toast.error('Mất kết nối với server. Vui lòng refresh trang.', {
      autoClose: false,
      closeOnClick: false
    });
  }
}
```

### 2. API Error Handling
```javascript
const handleApiError = (error, action) => {
  console.error(`Error during ${action}:`, error);
  
  if (error.response) {
    // Server responded with error status
    const { status, data } = error.response;
    
    switch (status) {
      case 401:
        // Unauthorized - redirect to login
        localStorage.removeItem('authToken');
        window.location.href = '/login';
        break;
        
      case 403:
        toast.error('Bạn không có quyền thực hiện hành động này');
        break;
        
      case 404:
        toast.error('Không tìm thấy notification');
        break;
        
      case 500:
        toast.error('Lỗi server. Vui lòng thử lại sau.');
        break;
        
      default:
        toast.error(data.message || 'Có lỗi xảy ra');
    }
  } else if (error.request) {
    // Network error
    toast.error('Lỗi kết nối mạng. Vui lòng kiểm tra internet.');
  } else {
    // Other error
    toast.error('Có lỗi không xác định xảy ra');
  }
};
```

### 3. Performance Optimization
```javascript
// Debounce API calls
const debounce = (func, wait) => {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
};

// Memoize notification components
const NotificationItem = React.memo(({ notification, onClick }) => {
  // Component implementation
});

// Virtual scrolling for large notification lists
import { FixedSizeList as List } from 'react-window';

const VirtualizedNotificationList = ({ notifications }) => {
  const Row = ({ index, style }) => (
    <div style={style}>
      <NotificationItem notification={notifications[index]} />
    </div>
  );

  return (
    <List
      height={400}
      itemCount={notifications.length}
      itemSize={80}
    >
      {Row}
    </List>
  );
};
```

---

## 📱 Mobile Responsive Design

```css
/* Mobile optimizations */
@media (max-width: 768px) {
  .notification-dropdown {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    width: 100%;
    max-height: 100%;
    border-radius: 0;
    z-index: 9999;
  }

  .notification-header {
    position: sticky;
    top: 0;
    z-index: 1;
  }

  .notification-list {
    max-height: calc(100vh - 120px);
  }

  .notification-item {
    padding: 16px;
  }

  .notification-message {
    font-size: 0.85rem;
  }
}

/* Touch-friendly buttons */
@media (hover: none) {
  .notification-item {
    padding: 20px 16px;
  }
  
  .notification-bell {
    padding: 12px;
    min-width: 44px;
    min-height: 44px;
  }
}
```

---

## 🔐 Security Considerations

### 1. JWT Token Management
```javascript
// Auto-refresh token before it expires
const tokenManager = {
  refreshToken: async () => {
    try {
      const response = await axios.post('/auth/refresh');
      const newToken = response.data.token;
      localStorage.setItem('authToken', newToken);
      return newToken;
    } catch (error) {
      // Redirect to login if refresh fails
      window.location.href = '/login';
    }
  },

  scheduleTokenRefresh: (token) => {
    const payload = JSON.parse(atob(token.split('.')[1]));
    const expirationTime = payload.exp * 1000;
    const refreshTime = expirationTime - Date.now() - 300000; // 5 minutes before expiry

    setTimeout(() => {
      tokenManager.refreshToken();
    }, refreshTime);
  }
};
```

### 2. Input Validation
```javascript
const sanitizeNotificationData = (notification) => {
  return {
    ...notification,
    title: DOMPurify.sanitize(notification.title),
    message: DOMPurify.sanitize(notification.message),
    customerName: DOMPurify.sanitize(notification.customerName)
  };
};
```

---

## 📖 Complete Integration Example

```jsx
// App.jsx - Main application component
import React, { useEffect, useRef, useState } from 'react';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

const App = () => {
  const notificationService = useRef(new NotificationService());
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    const initializeNotifications = async () => {
      const username = getCurrentUserFromAuth();
      
      if (username) {
        // Connect WebSocket
        notificationService.current.connect(username, handleNewNotification);
        
        // Load initial unread count
        try {
          const count = await NotificationAPI.getUnreadCount();
          setUnreadCount(count);
        } catch (error) {
          console.error('Failed to load unread count:', error);
        }
      }
    };

    const handleNewNotification = (notification) => {
      // Show toast
      toast.info(
        <NotificationToast notification={notification} />,
        { autoClose: 8000 }
      );
      
      // Update unread count
      setUnreadCount(prev => prev + 1);
      
      // Play sound if enabled
      const preferences = getNotificationPreferences();
      if (preferences.enableSound) {
        playNotificationSound();
      }
      
      // Show browser notification if tab hidden
      if (document.hidden && preferences.enableBrowserNotifications) {
        showBrowserNotification(notification);
      }
    };

    initializeNotifications();

    // Cleanup on unmount
    return () => {
      notificationService.current.disconnect();
    };
  }, []);

  return (
    <div className="app">
      <header>
        <nav>
          <div className="nav-brand">Your App</div>
          <div className="nav-items">
            <NotificationBell unreadCount={unreadCount} />
            <UserMenu />
          </div>
        </nav>
      </header>

      <main>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/notifications" element={<NotificationListPage />} />
          <Route path="/videos/:id" element={<VideoDetailPage />} />
          {/* Other routes */}
        </Routes>
      </main>

      <ToastContainer
        position="top-right"
        autoClose={5000}
        hideProgressBar={false}
        newestOnTop={false}
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
      />
    </div>
  );
};

export default App;
```

---

## 🎯 Conclusion

Tài liệu này cung cấp hướng dẫn đầy đủ để triển khai notification system với:

✅ **Real-time WebSocket notifications**  
✅ **REST API cho CRUD operations**  
✅ **UI components và styling**  
✅ **Mark as read functionality**  
✅ **Unread count badge**  
✅ **Error handling và best practices**  
✅ **Mobile responsive design**  
✅ **Performance optimizations**  

### Next Steps:
1. **Implement WebSocket connection**
2. **Create notification components**  
3. **Integrate REST APIs**
4. **Add styling và responsive design**
5. **Test thoroughly**
6. **Deploy và monitor**

Nếu có câu hỏi hoặc cần hỗ trợ thêm, vui lòng liên hệ team Backend! 🚀