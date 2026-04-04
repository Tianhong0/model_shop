import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])
  let notificationId = 0

  // 未读通知数量
  const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

  // 添加通知
  const addNotification = (notification) => {
    const id = ++notificationId
    const newNotification = {
      id,
      type: notification.type || 'info', // success, warning, info, error
      title: notification.title || '通知',
      message: notification.message || '',
      duration: notification.duration || 5000, // 自动关闭时间，0 表示不自动关闭
      read: false,
      createdAt: new Date().toISOString(),
      ...notification
    }
    notifications.value.unshift(newNotification)

    // 自动关闭
    if (newNotification.duration > 0) {
      setTimeout(() => {
        removeNotification(id)
      }, newNotification.duration)
    }

    return id
  }

  // 移除通知
  const removeNotification = (id) => {
    const index = notifications.value.findIndex(n => n.id === id)
    if (index > -1) {
      notifications.value.splice(index, 1)
    }
  }

  // 标记已读
  const markAsRead = (id) => {
    const notification = notifications.value.find(n => n.id === id)
    if (notification) {
      notification.read = true
    }
  }

  // 全部标记已读
  const markAllAsRead = () => {
    notifications.value.forEach(n => {
      n.read = true
    })
  }

  // 清除所有通知
  const clearAll = () => {
    notifications.value = []
  }

  // 添加客服会话通知
  const addCustomerServiceNotification = (conversation) => {
    return addNotification({
      type: 'success',
      title: '新会话分配',
      message: `用户 ${conversation.userNickname || '用户'} 的会话已分配给您`,
      duration: 10000,
      data: conversation
    })
  }

  // 添加会话自动转接通知（强提醒）
  const addConversationTransferNotification = (conversation) => {
    return addNotification({
      type: 'warning',
      title: '会话自动转接',
      message: `用户 ${conversation.userNickname || '用户'} 的会话已自动转接给您，请及时处理`,
      duration: 15000, // 更长的显示时间
      data: conversation,
      route: '/customer-service' // 点击跳转到客服管理页面
    })
  }

  return {
    notifications,
    unreadCount,
    addNotification,
    removeNotification,
    markAsRead,
    markAllAsRead,
    clearAll,
    addCustomerServiceNotification,
    addConversationTransferNotification
  }
})
