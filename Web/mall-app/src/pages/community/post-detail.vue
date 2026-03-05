<template>
  <view class="detail-container" v-if="detail">
    <view class="post-card card">
      <view class="head-row">
        <image :src="detail.post.userAvatar || defaultAvatar" class="avatar" mode="aspectFill"></image>
        <view class="meta">
          <text class="name">{{ detail.post.userNickname || '用户' }}</text>
          <text class="time">{{ detail.post.createTime || '' }}</text>
        </view>
        <view class="category">{{ detail.post.categoryName || '未分类' }}</view>
      </view>
      <text class="title">{{ detail.post.title }}</text>
      <text class="content">{{ detail.content }}</text>

      <view class="media-list" v-if="detail.post.mediaList && detail.post.mediaList.length">
        <view v-for="media in detail.post.mediaList" :key="media.id" class="media-item">
          <image
            v-if="media.mediaType === 1"
            :src="media.mediaUrl"
            mode="aspectFill"
            @click="previewImage(media.mediaUrl)"
          ></image>
          <video
            v-else
            :src="media.mediaUrl"
            controls
            :page-gesture="true"
            :vslide-gesture="false"
            object-fit="cover"
          ></video>
        </view>
      </view>

      <view class="action-row">
        <view class="action" @click="toggleInteraction(1)">
          <uni-icons :type="detail.post.liked ? 'heart-filled' : 'heart'" size="20" :color="detail.post.liked ? '#ef4444' : '#64748b'"></uni-icons>
          <text>{{ detail.post.likeCount || 0 }}</text>
        </view>
        <view class="action" @click="toggleInteraction(2)">
          <uni-icons type="star" size="20" :color="detail.post.collected ? '#f59e0b' : '#64748b'"></uni-icons>
          <text>{{ detail.post.collectCount || 0 }}</text>
        </view>
        <view class="action" @click="goEdit" v-if="isPostOwner">
          <uni-icons type="compose" size="20" color="#4f46e5"></uni-icons>
          <text>编辑</text>
        </view>
      </view>
    </view>

    <view class="reply-card card">
      <view class="reply-title">全部回复（{{ detail.replies?.length || 0 }}）</view>
      <view v-if="detail.replies && detail.replies.length">
        <view class="reply-item" v-for="reply in detail.replies" :key="reply.id">
          <view class="reply-head">
            <image :src="reply.userAvatar || defaultAvatar" class="reply-avatar" mode="aspectFill"></image>
            <view class="reply-meta">
              <text class="reply-name">{{ reply.userNickname || '用户' }}</text>
              <text class="reply-time">{{ reply.createTime || '' }}</text>
            </view>
            <view class="reply-tags">
              <text class="tag adopted" v-if="reply.isAdopted === 1">已采纳</text>
              <text class="tag excellent" v-if="reply.isExcellent === 1">优质</text>
            </view>
          </view>
          <text class="reply-content">{{ reply.content }}</text>
          <view class="reply-actions">
            <view class="reply-like" @click="toggleReplyLike(reply)">
              <uni-icons :type="reply.liked ? 'heart-filled' : 'heart'" size="18" :color="reply.liked ? '#ef4444' : '#64748b'"></uni-icons>
              <text :class="{ active: reply.liked }">{{ reply.likeCount || 0 }}</text>
            </view>
            <text v-if="canAdopt(reply)" @click="adoptReply(reply)">采纳</text>
            <text v-if="canDeleteReply(reply)" @click="removeReply(reply)">删除</text>
          </view>
        </view>
      </view>
      <view v-else class="empty-tip">暂无回复</view>
    </view>

    <view class="reply-input-box">
      <input
        v-model="replyContent"
        class="reply-input"
        type="text"
        maxlength="500"
        confirm-type="send"
        placeholder="写下你的回复..."
        @confirm="submitReply"
      />
      <button class="send-btn" @click="submitReply">发送</button>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import {
  adoptReplyApi,
  createReplyApi,
  deleteReplyApi,
  getPostDetailApi,
  toggleReplyLikeApi,
  togglePostInteractionApi
} from '../../api/community'

const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=community'
const detail = ref(null)
const replyContent = ref('')
const postId = ref('')

const toIdString = (value) => (value === null || value === undefined ? '' : String(value))

const currentUserId = computed(() => {
  const profile = uni.getStorageSync('user_profile') || {}
  return toIdString(profile.id)
})

const isPostOwner = computed(() => toIdString(detail.value?.post?.userId) === currentUserId.value)

const fetchDetail = async () => {
  detail.value = await getPostDetailApi(postId.value)
}

const canDeleteReply = (reply) => {
  return toIdString(reply.userId) === currentUserId.value || isPostOwner.value
}

const canAdopt = (reply) => {
  return isPostOwner.value && reply.isAdopted !== 1
}

const previewImage = (url) => {
  uni.previewImage({ urls: [url], current: url })
}

const toggleInteraction = async (interactType) => {
  try {
    const res = await togglePostInteractionApi({ postId: postId.value, interactType })
    const active = !!res?.active
    if (interactType === 1) {
      detail.value.post.liked = active
      detail.value.post.likeCount = Math.max(0, Number(detail.value.post.likeCount || 0) + (active ? 1 : -1))
    } else {
      detail.value.post.collected = active
      detail.value.post.collectCount = Math.max(0, Number(detail.value.post.collectCount || 0) + (active ? 1 : -1))
    }
  } catch (error) {
    uni.showToast({ title: error.message || '操作失败', icon: 'none' })
  }
}

const toggleReplyLike = async (reply) => {
  try {
    const res = await toggleReplyLikeApi({ replyId: reply.id })
    const active = !!res?.active
    reply.liked = active
    reply.likeCount = Math.max(0, Number(reply.likeCount || 0) + (active ? 1 : -1))
  } catch (error) {
    uni.showToast({ title: error.message || '点赞失败', icon: 'none' })
  }
}

const submitReply = async () => {
  if (!replyContent.value.trim()) {
    uni.showToast({ title: '请输入回复内容', icon: 'none' })
    return
  }
  try {
    await createReplyApi({ postId: postId.value, parentId: 0, content: replyContent.value.trim() })
    replyContent.value = ''
    uni.showToast({ title: '回复成功', icon: 'success' })
    await fetchDetail()
  } catch (error) {
    uni.showToast({ title: error.message || '回复失败', icon: 'none' })
  }
}

const adoptReply = async (reply) => {
  try {
    await adoptReplyApi({ replyId: reply.id })
    uni.showToast({ title: '采纳成功', icon: 'success' })
    await fetchDetail()
  } catch (error) {
    uni.showToast({ title: error.message || '采纳失败', icon: 'none' })
  }
}

const removeReply = async (reply) => {
  const confirm = await new Promise((resolve) => {
    uni.showModal({
      title: '提示',
      content: '确认删除该回复吗？',
      success: (res) => resolve(!!res.confirm)
    })
  })
  if (!confirm) return

  try {
    await deleteReplyApi(reply.id)
    uni.showToast({ title: '已删除', icon: 'success' })
    await fetchDetail()
  } catch (error) {
    uni.showToast({ title: error.message || '删除失败', icon: 'none' })
  }
}

const goEdit = () => {
  uni.navigateTo({ url: `/pages/community/post-edit?id=${postId.value}` })
}

onLoad(async (options) => {
  if (!ensureLoginOrRedirect()) return
  postId.value = toIdString(options?.id)
  if (!postId.value) {
    uni.showToast({ title: '帖子参数错误', icon: 'none' })
    return
  }
  try {
    await fetchDetail()
  } catch (error) {
    uni.showToast({ title: error.message || '加载详情失败', icon: 'none' })
  }
})
</script>

<style scoped lang="scss">
.detail-container { min-height: 100vh; background: #f8fafc; padding: 20rpx; padding-bottom: calc(140rpx + env(safe-area-inset-bottom)); }
.post-card, .reply-card { padding: 24rpx; margin-bottom: 20rpx; }
.head-row { display: flex; align-items: center; }
.avatar { width: 72rpx; height: 72rpx; border-radius: 50%; }
.meta { flex: 1; margin-left: 14rpx; }
.name { font-size: 26rpx; font-weight: 700; color: #1e293b; display: block; }
.time { font-size: 22rpx; color: #94a3b8; display: block; margin-top: 4rpx; }
.category { font-size: 22rpx; color: #4f46e5; }
.title { margin-top: 16rpx; display: block; font-size: 32rpx; color: #1e293b; font-weight: 700; }
.content { margin-top: 10rpx; display: block; font-size: 28rpx; color: #334155; line-height: 1.6; white-space: pre-wrap; }
.media-list { margin-top: 16rpx; display: grid; gap: 12rpx; grid-template-columns: repeat(2, 1fr); }
.media-item image, .media-item video { width: 100%; height: 240rpx; border-radius: 12rpx; }
.action-row { margin-top: 18rpx; display: flex; gap: 30rpx; }
.action { display: flex; align-items: center; gap: 8rpx; font-size: 24rpx; color: #64748b; }
.reply-title { font-size: 28rpx; font-weight: 700; color: #1e293b; }
.reply-item { margin-top: 20rpx; padding-top: 20rpx; border-top: 1rpx solid #f1f5f9; }
.reply-head { display: flex; align-items: center; }
.reply-avatar { width: 56rpx; height: 56rpx; border-radius: 50%; }
.reply-meta { flex: 1; margin-left: 12rpx; }
.reply-name { display: block; font-size: 24rpx; color: #1e293b; }
.reply-time { display: block; margin-top: 2rpx; font-size: 20rpx; color: #94a3b8; }
.reply-tags { display: flex; gap: 8rpx; }
.tag { font-size: 20rpx; border-radius: 6rpx; padding: 2rpx 10rpx; }
.tag.adopted { color: #16a34a; background: #dcfce7; }
.tag.excellent { color: #4f46e5; background: #e0e7ff; }
.reply-content { margin-top: 10rpx; display: block; font-size: 26rpx; color: #334155; line-height: 1.5; white-space: pre-wrap; }
.reply-actions { margin-top: 10rpx; display: flex; gap: 24rpx; }
.reply-actions text { font-size: 24rpx; color: #4f46e5; }
.reply-like {
  display: flex;
  align-items: center;
  gap: 6rpx;
  text {
    font-size: 24rpx;
    color: #64748b;
  }
  text.active {
    color: #ef4444;
  }
}
.empty-tip { margin-top: 20rpx; text-align: center; color: #94a3b8; font-size: 24rpx; }
.reply-input-box {
  position: fixed;
  left: 20rpx;
  right: 20rpx;
  bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-radius: 44rpx;
  padding: 10rpx 12rpx 10rpx 20rpx;
  box-shadow: 0 8rpx 20rpx rgba(15, 23, 42, 0.08);
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.reply-input {
  flex: 1;
  height: 64rpx;
  line-height: 64rpx;
  font-size: 26rpx;
}
.send-btn {
  width: 130rpx;
  height: 64rpx;
  line-height: 64rpx;
  margin: 0;
  background: #4f46e5;
  color: #fff;
  border-radius: 34rpx;
  font-size: 26rpx;
}
</style>
