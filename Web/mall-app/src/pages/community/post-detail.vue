<template>
  <view class="detail-container" v-if="detail">
    <!-- Post Card -->
    <view class="post-card">
      <view class="card-header">
        <view class="avatar-section">
          <view class="avatar-ring">
            <image :src="detail.post.userAvatar || defaultAvatar" class="avatar" mode="aspectFill"></image>
          </view>
          <view class="meta">
            <text class="name">{{ detail.post.userNickname || '创作者' }}</text>
            <text class="time">{{ detail.post.createTime || '' }}</text>
          </view>
        </view>
        <view class="category-badge">
          <text>{{ detail.post.categoryName || '未分类' }}</text>
        </view>
      </view>

      <view class="content-section">
        <text class="title">{{ detail.post.title }}</text>
        <text class="content">{{ detail.content }}</text>
      </view>

      <!-- Media Section -->
      <view class="media-section" v-if="videoList.length || imageList.length">
        <!-- Video List -->
        <view class="video-list" v-if="videoList.length">
          <view v-for="(media, index) in videoList" :key="'v'+index" class="video-item" :style="{ animationDelay: `${index * 0.1}s` }">
            <video
              :src="media.mediaUrl"
              :poster="getVideoCoverUrl(media.mediaUrl)"
              controls
              :page-gesture="true"
              :vslide-gesture="false"
              object-fit="cover"
            ></video>
          </view>
        </view>

        <!-- Image Gallery -->
        <view class="image-grid" v-if="imageList.length">
          <view v-for="(media, index) in imageList" :key="'i'+index" class="image-item" :style="{ animationDelay: `${index * 0.1}s` }">
            <image
              :src="media.mediaUrl"
              mode="aspectFill"
              @click="previewImage(media.mediaUrl)"
            ></image>
          </view>
        </view>
      </view>

      <view class="action-bar">
        <view class="action-item" :class="{ active: detail.post.liked }" @click="toggleInteraction(1)">
          <view class="icon-wrap">
            <uni-icons :type="detail.post.liked ? 'heart-filled' : 'heart'" size="22" :color="detail.post.liked ? '#ff4d6d' : '#8a9aaa'"></uni-icons>
          </view>
          <text>{{ detail.post.likeCount || 0 }}</text>
        </view>
        <view class="action-item" :class="{ active: detail.post.collected }" @click="toggleInteraction(2)">
          <view class="icon-wrap">
            <uni-icons :type="detail.post.collected ? 'star-filled' : 'star'" size="22" :color="detail.post.collected ? '#00bfff' : '#8a9aaa'"></uni-icons>
          </view>
          <text>{{ detail.post.collectCount || 0 }}</text>
        </view>
        <view class="action-item edit-action" @click="goEdit" v-if="isPostOwner">
          <view class="icon-wrap">
            <uni-icons type="compose" size="22" color="#0099cc"></uni-icons>
          </view>
          <text>编辑</text>
        </view>
      </view>
    </view>

    <!-- Replies Section -->
    <view class="replies-section">
      <view class="section-header">
        <text class="section-title">全部回复</text>
        <view class="reply-count-badge">{{ detail.replies?.length || 0 }}</view>
      </view>

      <view v-if="detail.replies && detail.replies.length" class="replies-list">
        <view
          class="reply-item"
          v-for="(reply, index) in detail.replies"
          :key="reply.id"
          :style="{ animationDelay: `${index * 0.05}s` }"
        >
          <view class="reply-header">
            <image :src="reply.userAvatar || defaultAvatar" class="reply-avatar" mode="aspectFill"></image>
            <view class="reply-meta">
              <text class="reply-name">{{ reply.userNickname || '用户' }}</text>
              <text class="reply-time">{{ reply.createTime || '' }}</text>
            </view>
            <view class="reply-badges">
              <view class="badge adopted" v-if="reply.isAdopted === 1">
                <uni-icons type="checkmarkempty" size="14" color="#10b981"></uni-icons>
                <text>已采纳</text>
              </view>
              <view class="badge excellent" v-if="reply.isExcellent === 1">
                <uni-icons type="star-filled" size="14" color="#0099cc"></uni-icons>
                <text>优质</text>
              </view>
            </view>
          </view>
          <view class="reply-content-wrap">
            <text class="reply-to-tag" v-if="reply.parentId > 0 && reply.parentUserNickname">回复 @{{ reply.parentUserNickname }}</text>
            <text class="reply-content">{{ reply.content }}</text>
          </view>
          <view class="reply-actions">
            <view class="like-btn" :class="{ liked: reply.liked }" @click="toggleReplyLike(reply)">
              <uni-icons :type="reply.liked ? 'heart-filled' : 'heart'" size="18" :color="reply.liked ? '#ff4d6d' : '#8a9aaa'"></uni-icons>
              <text>{{ reply.likeCount || 0 }}</text>
            </view>
            <text class="action-link reply-action" @click="startReply(reply)">回复</text>
            <text class="action-link" v-if="canAdopt(reply)" @click="adoptReply(reply)">采纳</text>
            <text class="action-link danger" v-if="canDeleteReply(reply)" @click="removeReply(reply)">删除</text>
          </view>
        </view>
      </view>

      <view class="empty-replies" v-else>
        <view class="empty-icon">
          <uni-icons type="chatbubble" size="40" color="#e7e5e4"></uni-icons>
        </view>
        <text>暂无回复，来抢沙发吧</text>
      </view>
    </view>

    <!-- Bottom Reply Input -->
    <view class="reply-input-bar">
      <view class="reply-indicator" v-if="replyingTo" @click="cancelReply">
        <text class="indicator-text">回复 @{{ replyingTo.userNickname }}</text>
        <uni-icons type="closeempty" size="16" color="#8a9aaa"></uni-icons>
      </view>
      <view class="input-row">
        <view class="input-wrapper">
          <input
            v-model="replyContent"
            class="reply-input"
            type="text"
            maxlength="500"
            confirm-type="send"
            :placeholder="replyPlaceholder"
            placeholder-class="input-placeholder"
            :focus="inputFocus"
            @confirm="submitReply"
          />
        </view>
        <button class="send-btn" :class="{ active: replyContent.trim() }" @click="submitReply">
          <uni-icons type="paperplane" size="20" color="#fff"></uni-icons>
        </button>
      </view>
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
const replyingTo = ref(null)
const inputFocus = ref(false)

const replyPlaceholder = computed(() => {
  return replyingTo.value ? `回复 @${replyingTo.value.userNickname}` : '写下你的想法...'
})

const toIdString = (value) => (value === null || value === undefined ? '' : String(value))

const currentUserId = computed(() => {
  const profile = uni.getStorageSync('user_profile') || {}
  return toIdString(profile.id)
})

const isPostOwner = computed(() => toIdString(detail.value?.post?.userId) === currentUserId.value)

const mediaList = computed(() => Array.isArray(detail.value?.post?.mediaList) ? detail.value.post.mediaList : [])
const videoList = computed(() => mediaList.value.filter(m => m.mediaType === 2))
const imageList = computed(() => mediaList.value.filter(m => m.mediaType === 1))

const getVideoCoverUrl = (videoUrl) => {
  return videoUrl ? videoUrl.replace(/\.\w+$/, '-cover.jpg') : ''
}

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

const startReply = (reply) => {
  replyingTo.value = reply
  inputFocus.value = false
  setTimeout(() => { inputFocus.value = true }, 50)
}

const cancelReply = () => {
  replyingTo.value = null
}

const submitReply = async () => {
  if (!replyContent.value.trim()) {
    uni.showToast({ title: '请输入回复内容', icon: 'none' })
    return
  }
  try {
    // 显示AI审核提示
    uni.showLoading({ title: '智能审核中...', mask: true })
    await createReplyApi({ postId: postId.value, parentId: replyingTo.value?.id || 0, content: replyContent.value.trim() })
    uni.hideLoading()
    replyContent.value = ''
    replyingTo.value = null
    uni.showToast({ title: '回复成功', icon: 'success' })
    await fetchDetail()
  } catch (error) {
    uni.hideLoading()
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
      title: '确认删除',
      content: '删除后无法恢复，确定要删除吗？',
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
/* Design System Tokens */
$primary: #00bfff;
$deep: #0099cc;
$light: #5ce1ff;
$success: #10b981;
$danger: #ff4d6d;

$bg: #f8f8f8;
$card: #ffffff;
$text-primary: #1a2030;
$text-secondary: #5a6a7a;
$text-muted: #8a9aaa;

$gradient: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
$shadow-card: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
$radius-card: 24rpx;
$radius-capsule: 999rpx;

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(24rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes jellyPop {
  0% { transform: scale(1); }
  30% { transform: scale(1.25); }
  50% { transform: scale(0.9); }
  70% { transform: scale(1.08); }
  100% { transform: scale(1); }
}

@keyframes imageFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.detail-container {
  min-height: 100vh;
  background: $bg;
  padding: 28rpx;
  padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
}

/* Post Card */
.post-card {
  background: $card;
  border-radius: $radius-card;
  padding: 32rpx;
  margin-bottom: 28rpx;
  box-shadow: $shadow-card;
  animation: fadeInUp 0.4s ease;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 28rpx;
}

.avatar-section {
  display: flex;
  align-items: center;
}

.avatar-ring {
  width: 88rpx;
  height: 88rpx;
  padding: 4rpx;
  background: $gradient;
  border-radius: 50%;
}

.avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  animation: imageFadeIn 0.4s ease;
}

.meta {
  margin-left: 20rpx;
}

.name {
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
  display: block;
}

.time {
  font-size: 24rpx;
  color: $text-muted;
  display: block;
  margin-top: 4rpx;
}

.category-badge {
  font-size: 22rpx;
  color: $deep;
  background: rgba(0, 191, 255, 0.08);
  padding: 10rpx 20rpx;
  border-radius: $radius-capsule;
  font-weight: 500;
}

.content-section {
  margin-bottom: 28rpx;
}

.title {
  display: block;
  font-size: 36rpx;
  color: $text-primary;
  font-weight: 700;
  line-height: 1.4;
  margin-bottom: 16rpx;
}

.content {
  display: block;
  font-size: 28rpx;
  color: $text-secondary;
  line-height: 1.8;
  white-space: pre-wrap;
}

/* Media Section */
.media-section {
  margin: 24rpx 0;
}

/* Video List */
.video-list {
  padding: 20rpx 0 40rpx;
}

.video-item {
  animation: fadeInUp 0.4s ease forwards;
  opacity: 0;
  border-radius: 16rpx;
  overflow: hidden;
  background: #000;

  &:not(:last-child) {
    margin-bottom: 20rpx;
  }

  video {
    width: 100%;
    height: 300rpx;
    display: block;
  }
}

/* Image Grid */
.image-grid {
  display: grid;
  gap: 16rpx;
  grid-template-columns: repeat(2, 1fr);
  padding: 20rpx 0;
}

.image-item {
  animation: fadeInUp 0.4s ease forwards;
  opacity: 0;
  border-radius: 16rpx;
  overflow: hidden;

  image {
    width: 100%;
    height: 280rpx;
    display: block;
    background: $bg;
  }
}

/* Action Bar */
.action-bar {
  display: flex;
  gap: 16rpx;
  padding-top: 24rpx;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 14rpx 24rpx;
  border-radius: $radius-capsule;
  background: $bg;
  transition: all 0.25s ease;

  .icon-wrap {
    display: flex;
    align-items: center;
  }

  text {
    font-size: 26rpx;
    color: $text-secondary;
    font-weight: 500;
  }

  &.active {
    background: rgba(0, 191, 255, 0.08);
    text { color: $primary; }
  }

  &:active {
    transform: scale(0.96);

    .icon-wrap {
      animation: jellyPop 0.4s ease;
    }
  }
}

.edit-action {
  background: rgba(0, 191, 255, 0.08);
  text { color: $deep; }
}

/* Replies Section */
.replies-section {
  background: $card;
  border-radius: $radius-card;
  padding: 32rpx;
  box-shadow: $shadow-card;
  animation: fadeInUp 0.5s ease;
}

.section-header {
  display: flex;
  align-items: center;
  margin-bottom: 28rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
}

.reply-count-badge {
  margin-left: 12rpx;
  font-size: 20rpx;
  color: $deep;
  background: rgba(0, 191, 255, 0.08);
  padding: 4rpx 16rpx;
  border-radius: $radius-capsule;
  font-weight: 500;
}

/* Reply Items */
.replies-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.reply-item {
  padding: 28rpx;
  background: $bg;
  border-radius: $radius-card;
  animation: fadeInUp 0.4s ease forwards;
  opacity: 0;
}

.reply-header {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.reply-avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  animation: imageFadeIn 0.4s ease;
}

.reply-meta {
  flex: 1;
  margin-left: 16rpx;
}

.reply-name {
  display: block;
  font-size: 26rpx;
  color: $text-primary;
  font-weight: 500;
}

.reply-time {
  display: block;
  margin-top: 4rpx;
  font-size: 20rpx;
  color: $text-muted;
}

.reply-badges {
  display: flex;
  gap: 10rpx;
}

.badge {
  display: flex;
  align-items: center;
  gap: 6rpx;
  font-size: 20rpx;
  border-radius: $radius-capsule;
  padding: 6rpx 14rpx;

  &.adopted {
    color: $success;
    background: rgba(16, 185, 129, 0.1);
  }

  &.excellent {
    color: $deep;
    background: rgba(0, 191, 255, 0.08);
  }
}

.reply-content-wrap {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
}

.reply-to-tag {
  font-size: 24rpx;
  color: $deep;
  background: rgba(0, 191, 255, 0.08);
  padding: 4rpx 12rpx;
  border-radius: $radius-capsule;
  margin-right: 8rpx;
  font-weight: 500;
  flex-shrink: 0;
}

.reply-content {
  display: block;
  font-size: 28rpx;
  color: $text-secondary;
  line-height: 1.7;
  white-space: pre-wrap;
}

.reply-actions {
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-top: 20rpx;
  padding-top: 16rpx;
}

.like-btn {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 18rpx;
  border-radius: $radius-capsule;
  transition: all 0.2s ease;

  text {
    font-size: 24rpx;
    color: $text-muted;
  }

  &.liked text {
    color: $danger;
  }

  &:active {
    transform: scale(0.96);
  }
}

.action-link {
  font-size: 24rpx;
  color: $deep;
  font-weight: 500;
  padding: 8rpx 18rpx;
  border-radius: $radius-capsule;
  transition: all 0.2s ease;

  &:active {
    background: rgba(0, 191, 255, 0.08);
    transform: scale(0.96);
  }

  &.danger {
    color: $danger;

    &:active {
      background: rgba(255, 77, 109, 0.06);
    }
  }

  &.reply-action {
    color: $text-secondary;
  }
}

/* Empty Replies */
.empty-replies {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 24rpx;
  animation: fadeInUp 0.5s ease;

  .empty-icon {
    width: 100rpx;
    height: 100rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: $bg;
    border-radius: 50%;
    margin-bottom: 16rpx;
  }

  text {
    font-size: 28rpx;
    color: $text-muted;
  }
}

/* Reply Input Bar */
.reply-input-bar {
  position: fixed;
  left: 28rpx;
  right: 28rpx;
  bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px);
  border-radius: $radius-card;
  padding: 16rpx;
  box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.08);
  z-index: 100;
}

.reply-indicator {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8rpx 16rpx 12rpx;
}

.indicator-text {
  font-size: 24rpx;
  color: $deep;
  font-weight: 500;
}

.input-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.input-wrapper {
  flex: 1;
  background: $bg;
  border-radius: $radius-capsule;
  padding: 0 24rpx;
}

.reply-input {
  height: 72rpx;
  line-height: 72rpx;
  font-size: 28rpx;
  color: $text-primary;
}

.input-placeholder {
  color: $text-muted;
}

.send-btn {
  width: 72rpx;
  height: 72rpx;
  margin: 0;
  padding: 0;
  background: $bg;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;

  &.active {
    background: $gradient;
    box-shadow: 0 4rpx 20rpx rgba(0, 191, 255, 0.35);
  }

  &:active {
    transform: scale(0.96);
  }
}
</style>
