<template>
  <view class="detail-container">
    <!-- Loading -->
    <view class="loading-state" v-if="loading && !detail">
      <view class="loading-spinner"></view>
      <text>加载中...</text>
    </view>

    <scroll-view
      v-if="detail"
      scroll-y
      class="detail-scroll"
      enhanced
      :show-scrollbar="false"
    >
      <!-- Cover -->
      <view class="cover-section" @click="isOwner && chooseCoverImage()">
        <image
          v-if="detail.listInfo.coverImage"
          :src="detail.listInfo.coverImage"
          class="cover-img"
          mode="aspectFill"
        ></image>
        <view class="cover-img cover-empty" v-else>
          <uni-icons type="image" size="48" color="#8a9aaa"></uni-icons>
          <text v-if="isOwner" class="cover-empty-hint">点击设置封面</text>
        </view>
        <view class="cover-edit-overlay" v-if="isOwner">
          <uni-icons type="camera-filled" size="20" color="#ffffff"></uni-icons>
          <text>{{ detail.listInfo.coverImage ? '更换封面' : '设置封面' }}</text>
        </view>
      </view>

      <!-- Info Card -->
      <view class="info-card">
        <view class="info-head">
          <image :src="detail.listInfo.userAvatar || defaultAvatar" class="avatar" mode="aspectFill"></image>
          <view class="meta">
            <text class="nickname">{{ detail.listInfo.userNickname || '用户' }}</text>
            <text class="time">{{ detail.listInfo.createTime || '' }}</text>
          </view>
          <view class="owner-badge" v-if="isOwner">
            <text>我的清单</text>
          </view>
        </view>

        <view class="info-title">{{ detail.listInfo.title }}</view>
        <view class="info-desc" v-if="detail.listInfo.description">{{ detail.listInfo.description }}</view>

        <view class="info-stats">
          <view class="stat">
            <text class="val">{{ detail.listInfo.modelCount || 0 }}</text>
            <text class="lab">模型</text>
          </view>
          <view class="stat">
            <text class="val">{{ detail.listInfo.viewCount || 0 }}</text>
            <text class="lab">浏览</text>
          </view>
          <view class="stat">
            <text class="val">{{ detail.listInfo.likeCount || 0 }}</text>
            <text class="lab">点赞</text>
          </view>
          <view class="stat">
            <text class="val">{{ detail.listInfo.collectCount || 0 }}</text>
            <text class="lab">收藏</text>
          </view>
        </view>
      </view>

      <!-- Owner: Add Model Button -->
      <view class="add-model-row" v-if="isOwner">
        <button class="add-model-btn" @click="openModelPicker">
          <uni-icons type="plusempty" size="18" color="#ffffff"></uni-icons>
          <text>从收藏添加模型</text>
        </button>
      </view>

      <!-- Model Items -->
      <view class="section-title" v-if="detail.items && detail.items.length">
        <text>清单模型（{{ detail.items.length }}）</text>
      </view>

      <view class="model-list">
        <view
          class="model-card"
          v-for="(item, index) in detail.items"
          :key="item.id"
          @click="goModelDetail(item.modelId)"
          :style="{ animationDelay: `${index * 0.05}s` }"
        >
          <image
            :src="item.mainImageUrl || fallbackImage"
            class="model-cover"
            mode="aspectFill"
          ></image>
          <view class="model-info">
            <text class="model-name">{{ item.modelName || '未知模型' }}</text>
            <view class="model-meta">
              <text class="category" v-if="item.categoryName">{{ item.categoryName }}</text>
              <text class="designer" v-if="item.designerName">{{ item.designerName }}</text>
            </view>
            <view class="model-remark" v-if="item.remark">
              <uni-icons type="chat" size="14" color="#0099cc"></uni-icons>
              <text>{{ item.remark }}</text>
            </view>
            <view class="price-row">
              <text class="price" v-if="item.basePrice">￥{{ Number(item.basePrice).toFixed(2) }}</text>
              <text class="price-tip">基础价</text>
            </view>
          </view>
          <!-- Owner: Remove Button -->
          <view class="remove-btn" v-if="isOwner" @click.stop="removeModel(item)">
            <uni-icons type="close" size="16" color="#ff4d6d"></uni-icons>
          </view>
        </view>
      </view>

      <view class="empty-items" v-if="detail.items && !detail.items.length">
        <uni-icons type="info" size="40" color="#8a9aaa"></uni-icons>
        <text>清单暂无模型</text>
        <button v-if="isOwner" class="add-hint-btn" @click="openModelPicker">点击添加模型</button>
      </view>

      <!-- Bottom Padding -->
      <view style="height: 160rpx;"></view>
    </scroll-view>

    <!-- Bottom Action Bar -->
    <view class="action-bar" v-if="detail">
      <view class="action-btn" :class="{ active: detail.listInfo.liked }" @click="toggleInteraction(1)">
        <uni-icons
          :type="detail.listInfo.liked ? 'heart-filled' : 'heart'"
          size="22"
          :color="detail.listInfo.liked ? '#ff4d6d' : '#5a6a7a'"
        ></uni-icons>
        <text>{{ detail.listInfo.liked ? '已点赞' : '点赞' }}</text>
      </view>
      <view class="action-btn" :class="{ active: detail.listInfo.collected }" @click="toggleInteraction(2)">
        <uni-icons
          :type="detail.listInfo.collected ? 'star-filled' : 'star'"
          size="22"
          :color="detail.listInfo.collected ? '#00bfff' : '#5a6a7a'"
        ></uni-icons>
        <text>{{ detail.listInfo.collected ? '已收藏' : '收藏' }}</text>
      </view>
    </view>

    <!-- Model Picker Modal -->
    <view class="picker-mask" v-if="showPicker" @click.self="closePicker">
      <view class="picker-panel">
        <view class="picker-header">
          <text class="picker-title">从收藏中选择模型</text>
          <view class="picker-close" @click="closePicker">
            <uni-icons type="close" size="20" color="#5a6a7a"></uni-icons>
          </view>
        </view>

        <view class="picker-loading" v-if="pickerLoading">
          <view class="loading-spinner"></view>
          <text>加载中...</text>
        </view>

        <scroll-view v-else scroll-y class="picker-scroll" :show-scrollbar="false">
          <view class="picker-empty" v-if="!favoriteModels.length">
            <text>暂无收藏模型</text>
            <text class="picker-empty-hint">先去商城收藏一些模型吧</text>
          </view>
          <view
            class="picker-item"
            v-for="model in favoriteModels"
            :key="model.id"
            :class="{ selected: selectedModelIds.has(model.id), exists: existingModelIds.has(model.id) }"
            @click="toggleSelectModel(model)"
          >
            <image :src="model.mainImageUrl || fallbackImage" class="picker-cover" mode="aspectFill"></image>
            <view class="picker-info">
              <text class="picker-name">{{ model.modelName }}</text>
              <text class="picker-price" v-if="model.basePrice">￥{{ Number(model.basePrice).toFixed(2) }}</text>
            </view>
            <view class="picker-check">
              <uni-icons
                v-if="existingModelIds.has(model.id)"
                type="checkmarkempty" size="18" color="#8a9aaa"
              ></uni-icons>
              <uni-icons
                v-else-if="selectedModelIds.has(model.id)"
                type="checkbox-filled" size="22" color="#00bfff"
              ></uni-icons>
              <uni-icons
                v-else
                type="circle" size="22" color="#cbd5e1"
              ></uni-icons>
            </view>
          </view>
        </scroll-view>

        <!-- Remark Input -->
        <view class="remark-row" v-if="selectedModelIds.size > 0">
          <input class="remark-input" v-model="addRemark" placeholder="推荐理由（可选）" maxlength="200" />
        </view>

        <view class="picker-footer">
          <button class="btn ghost" @click="closePicker">取消</button>
          <button
            class="btn primary"
            :disabled="selectedModelIds.size === 0 || addingItems"
            @click="confirmAddModels"
          >
            {{ addingItems ? '添加中...' : `添加 ${selectedModelIds.size} 个模型` }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import {
  getModelListDetailApi,
  toggleModelListInteractionApi,
  addModelListItemsApi,
  removeModelListItemApi,
  updateModelListApi
} from '../../api/modelList'
import { getMyFavoriteModelsApi } from '../../api/model'
import { uploadCommunityMediaApi } from '../../api/community'

const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=list'
const fallbackImage = 'https://images.unsplash.com/photo-1519710164239-da123dc03ef4?w=300'

const detail = ref(null)
const loading = ref(false)
const listId = ref(null)

const currentUserId = computed(() => {
  try {
    const profile = uni.getStorageSync('user_profile')
    return profile?.id || null
  } catch { return null }
})

const isOwner = computed(() => {
  if (!detail.value || !currentUserId.value) return false
  return String(detail.value.listInfo.userId) === String(currentUserId.value)
})

const existingModelIds = computed(() => {
  if (!detail.value?.items) return new Set()
  return new Set(detail.value.items.map(i => i.modelId))
})

// Picker state
const showPicker = ref(false)
const pickerLoading = ref(false)
const favoriteModels = ref([])
const selectedModelIds = ref(new Set())
const addRemark = ref('')
const addingItems = ref(false)

const loadDetail = async () => {
  if (!listId.value) return
  loading.value = true
  try {
    const res = await getModelListDetailApi(listId.value)
    detail.value = res
  } catch (error) {
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const toggleInteraction = async (interactType) => {
  if (!detail.value) return
  try {
    const res = await toggleModelListInteractionApi({ listId: listId.value, interactType })
    const active = !!res?.active
    if (interactType === 1) {
      detail.value.listInfo.liked = active
      detail.value.listInfo.likeCount = Math.max(0, Number(detail.value.listInfo.likeCount || 0) + (active ? 1 : -1))
    } else {
      detail.value.listInfo.collected = active
      detail.value.listInfo.collectCount = Math.max(0, Number(detail.value.listInfo.collectCount || 0) + (active ? 1 : -1))
    }
  } catch (error) {
    uni.showToast({ title: error.message || '操作失败', icon: 'none' })
  }
}

const goModelDetail = (modelId) => {
  if (!modelId) return
  uni.navigateTo({ url: `/pages/custom/detail?id=${modelId}` })
}

// ========== Cover Image ==========

const chooseCoverImage = () => {
  uni.chooseImage({
    count: 1,
    success: async (res) => {
      const filePath = res?.tempFilePaths?.[0]
      if (!filePath) return
      uni.showLoading({ title: '上传中...' })
      try {
        const coverUrl = await uploadCommunityMediaApi(filePath, 'postImg')
        await updateModelListApi({ id: listId.value, coverImage: coverUrl })
        detail.value.listInfo.coverImage = coverUrl
        uni.showToast({ title: '封面已更新', icon: 'success' })
      } catch (error) {
        uni.showToast({ title: error.message || '上传失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    }
  })
}

// ========== Model Picker ==========

const openModelPicker = async () => {
  showPicker.value = true
  selectedModelIds.value = new Set()
  addRemark.value = ''
  pickerLoading.value = true
  try {
    const data = await getMyFavoriteModelsApi({ pageNum: 1, pageSize: 200 })
    favoriteModels.value = Array.isArray(data?.records) ? data.records : []
  } catch (error) {
    uni.showToast({ title: error.message || '加载收藏失败', icon: 'none' })
  } finally {
    pickerLoading.value = false
  }
}

const closePicker = () => {
  showPicker.value = false
  selectedModelIds.value = new Set()
  addRemark.value = ''
}

const toggleSelectModel = (model) => {
  if (existingModelIds.value.has(model.id)) return
  const next = new Set(selectedModelIds.value)
  if (next.has(model.id)) {
    next.delete(model.id)
  } else {
    next.add(model.id)
  }
  selectedModelIds.value = next
}

const confirmAddModels = async () => {
  if (selectedModelIds.value.size === 0) return
  addingItems.value = true
  try {
    const items = Array.from(selectedModelIds.value).map(modelId => ({
      modelId,
      remark: addRemark.value.trim() || null,
      sortNo: 0
    }))
    await addModelListItemsApi({ listId: listId.value, items })
    uni.showToast({ title: `已添加 ${items.length} 个模型`, icon: 'success' })
    closePicker()
    await loadDetail()
  } catch (error) {
    uni.showToast({ title: error.message || '添加失败', icon: 'none' })
  } finally {
    addingItems.value = false
  }
}

// ========== Remove Model ==========

const removeModel = (item) => {
  uni.showModal({
    title: '移除模型',
    content: `确定从清单中移除「${item.modelName || '该模型'}」吗？`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await removeModelListItemApi({ listId: listId.value, modelId: item.modelId })
        uni.showToast({ title: '已移除', icon: 'success' })
        await loadDetail()
      } catch (error) {
        uni.showToast({ title: error.message || '移除失败', icon: 'none' })
      }
    }
  })
}

onLoad(async (options) => {
  if (!ensureLoginOrRedirect()) return
  listId.value = options?.id
  await loadDetail()
})

onShow(async () => {
  if (listId.value) await loadDetail()
})
</script>

<style scoped lang="scss">
$primary: #00bfff;
$deep: #0099cc;
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
  from { opacity: 0; transform: translateY(24rpx); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.detail-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: $bg;
  overflow: hidden;
}

.detail-scroll {
  flex: 1;
  min-height: 0;
}

.cover-section {
  position: relative;

  .cover-img {
    width: 100%;
    height: 400rpx;
  }
  .cover-empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12rpx;
    background: linear-gradient(135deg, #e8f4fd 0%, #dbeafe 100%);
  }
  .cover-empty-hint {
    font-size: 24rpx;
    color: $text-muted;
  }
}

.cover-edit-overlay {
  position: absolute;
  right: 24rpx;
  bottom: 24rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 12rpx 24rpx;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(8px);
  border-radius: $radius-capsule;

  text {
    font-size: 24rpx;
    color: #ffffff;
    font-weight: 500;
  }

  &:active { transform: scale(0.96); }
}

.info-card {
  margin: -40rpx 28rpx 0;
  position: relative;
  z-index: 2;
  padding: 32rpx;
  background: $card;
  border-radius: $radius-card;
  box-shadow: $shadow-card;
  animation: fadeInUp 0.4s ease;
}

.info-head {
  display: flex;
  align-items: center;

  .avatar {
    width: 72rpx;
    height: 72rpx;
    border-radius: 50%;
    border: 3rpx solid rgba(0, 191, 255, 0.3);
  }

  .meta {
    flex: 1;
    margin-left: 20rpx;
  }

  .nickname {
    font-size: 28rpx;
    color: $text-primary;
    font-weight: 600;
    display: block;
  }

  .time {
    font-size: 24rpx;
    color: $text-muted;
    display: block;
    margin-top: 4rpx;
  }
}

.owner-badge {
  font-size: 22rpx;
  color: $deep;
  background: rgba(0, 191, 255, 0.08);
  padding: 8rpx 18rpx;
  border-radius: $radius-capsule;
  font-weight: 600;
}

.info-title {
  margin-top: 24rpx;
  font-size: 36rpx;
  color: $text-primary;
  font-weight: 700;
  line-height: 1.4;
}

.info-desc {
  margin-top: 12rpx;
  font-size: 28rpx;
  color: $text-secondary;
  line-height: 1.6;
}

.info-stats {
  margin-top: 28rpx;
  display: flex;

  .stat { flex: 1; text-align: center; }
  .val { font-size: 30rpx; font-weight: 700; color: $primary; display: block; }
  .lab { font-size: 22rpx; color: $text-muted; margin-top: 6rpx; display: block; }
}

.add-model-row {
  padding: 24rpx 28rpx 0;
}

.add-model-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  height: 80rpx;
  background: $gradient;
  color: #ffffff;
  border-radius: $radius-capsule;
  font-size: 28rpx;
  font-weight: 600;
  box-shadow: 0 6rpx 20rpx rgba(0, 191, 255, 0.2);
  &:active { transform: scale(0.96); }
}

.section-title {
  padding: 28rpx 32rpx 16rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: $text-primary;
}

.model-list {
  padding: 0 28rpx;
}

.model-card {
  display: flex;
  padding: 24rpx;
  margin-bottom: 20rpx;
  background: $card;
  border-radius: $radius-card;
  box-shadow: $shadow-card;
  animation: fadeInUp 0.4s ease forwards;
  opacity: 0;
  position: relative;

  &:active { transform: scale(0.99); }

  .model-cover {
    width: 180rpx;
    height: 180rpx;
    border-radius: 16rpx;
    background: $bg;
  }

  .model-info {
    flex: 1;
    margin-left: 20rpx;
    display: flex;
    flex-direction: column;
  }

  .model-name { font-size: 28rpx; font-weight: 700; color: $text-primary; }

  .model-meta {
    display: flex;
    gap: 12rpx;
    margin-top: 8rpx;

    .category, .designer {
      font-size: 22rpx;
      padding: 4rpx 14rpx;
      border-radius: $radius-capsule;
      background: $bg;
      color: $text-secondary;
    }
  }

  .model-remark {
    margin-top: 10rpx;
    display: flex;
    align-items: center;
    gap: 8rpx;
    font-size: 24rpx;
    color: $deep;
    line-height: 1.4;
  }

  .price-row {
    margin-top: auto;
    padding-top: 10rpx;
    display: flex;
    align-items: baseline;
    gap: 8rpx;

    .price { font-size: 28rpx; font-weight: 700; color: $danger; }
    .price-tip { font-size: 20rpx; color: $text-muted; }
  }
}

.remove-btn {
  position: absolute;
  top: 16rpx;
  right: 16rpx;
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 77, 109, 0.08);
  border-radius: 50%;
  &:active { transform: scale(0.9); background: rgba(255, 77, 109, 0.15); }
}

.empty-items {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx;
  gap: 16rpx;

  text { font-size: 28rpx; color: $text-muted; }
}

.add-hint-btn {
  margin-top: 12rpx;
  height: 64rpx;
  padding: 0 36rpx;
  font-size: 26rpx;
  border-radius: $radius-capsule;
  background: $gradient;
  color: #ffffff;
  font-weight: 600;
  &:active { transform: scale(0.96); }
}

.action-bar {
  display: flex;
  gap: 24rpx;
  padding: 20rpx 40rpx calc(env(safe-area-inset-bottom) + 20rpx);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px);
}

.action-btn {
  flex: 1;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  border-radius: $radius-capsule;
  background: $bg;
  box-shadow: $shadow-card;
  transition: all 0.2s ease;

  text { font-size: 28rpx; color: $text-secondary; font-weight: 600; }

  &.active {
    background: rgba(0, 191, 255, 0.08);
    text { color: $deep; }
  }

  &:active { transform: scale(0.96); }
}

/* ========== Model Picker Modal ========== */

.picker-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: flex-end;
  z-index: 20;
}

.picker-panel {
  width: 100%;
  max-height: 80vh;
  background: $card;
  border-radius: 32rpx 32rpx 0 0;
  display: flex;
  flex-direction: column;
  animation: fadeInUp 0.3s ease;
}

.picker-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx 32rpx 20rpx;
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.04);
}

.picker-title {
  font-size: 32rpx;
  font-weight: 700;
  color: $text-primary;
}

.picker-close {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: $bg;
  &:active { transform: scale(0.9); }
}

.picker-scroll {
  flex: 1;
  min-height: 0;
  max-height: 50vh;
  padding: 0 28rpx;
}

.picker-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx;
  gap: 16rpx;
  text { font-size: 26rpx; color: $text-muted; }
}

.picker-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx;
  gap: 8rpx;
  text { font-size: 28rpx; color: $text-muted; }
  .picker-empty-hint { font-size: 24rpx; }
}

.picker-item {
  display: flex;
  align-items: center;
  padding: 20rpx 16rpx;
  margin-bottom: 8rpx;
  border-radius: 16rpx;
  transition: all 0.2s ease;

  &.selected { background: rgba(0, 191, 255, 0.06); }
  &.exists { opacity: 0.5; }
  &:active { transform: scale(0.99); }
}

.picker-cover {
  width: 100rpx;
  height: 100rpx;
  border-radius: 12rpx;
  background: $bg;
}

.picker-info {
  flex: 1;
  margin-left: 20rpx;
}

.picker-name {
  font-size: 28rpx;
  font-weight: 600;
  color: $text-primary;
  display: block;
}

.picker-price {
  font-size: 24rpx;
  color: $danger;
  font-weight: 600;
  margin-top: 6rpx;
  display: block;
}

.picker-check {
  display: flex;
  align-items: center;
  margin-left: 16rpx;
}

.remark-row {
  padding: 16rpx 28rpx;
  border-top: 1rpx solid rgba(0, 0, 0, 0.04);
}

.remark-input {
  height: 72rpx;
  background: $bg;
  border-radius: 16rpx;
  padding: 0 24rpx;
  font-size: 26rpx;
  color: $text-primary;
}

.picker-footer {
  display: flex;
  gap: 16rpx;
  padding: 16rpx 28rpx calc(env(safe-area-inset-bottom) + 16rpx);
  border-top: 1rpx solid rgba(0, 0, 0, 0.04);

  .btn {
    flex: 1;
    height: 80rpx;
    border-radius: $radius-capsule;
    font-size: 28rpx;
    font-weight: 600;
    &:active { transform: scale(0.96); }
  }
  .btn.primary {
    background: $gradient;
    color: #ffffff;
    box-shadow: 0 6rpx 20rpx rgba(0, 191, 255, 0.2);
    &:disabled { opacity: 0.5; }
  }
  .btn.ghost { background: $bg; color: $text-secondary; }
}

.loading-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  text { font-size: 26rpx; color: $text-muted; }
}

.loading-spinner {
  width: 48rpx;
  height: 48rpx;
  border: 4rpx solid $bg;
  border-top-color: $primary;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
</style>
