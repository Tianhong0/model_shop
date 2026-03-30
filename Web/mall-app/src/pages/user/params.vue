<template>
  <view class="my-list-container">
    <!-- Header Card -->
    <view class="header-card">
      <view class="title-row">
        <view>
          <view class="title">我的清单</view>
          <view class="desc">管理你的模型清单并分享给其他人</view>
        </view>
        <button class="create-btn" @click="openCreateForm">
          <uni-icons type="plusempty" size="16" color="#ffffff"></uni-icons>
          <text>新建</text>
        </button>
      </view>
      <view class="summary">
        <view class="item" @click="goPlaza">
          <text class="val-icon"><uni-icons type="search" size="20" color="#00bfff"></uni-icons></text>
          <text class="lab">清单广场</text>
        </view>
        <view class="item">
          <text class="val">{{ totalCount }}</text>
          <text class="lab">我的清单</text>
        </view>
      </view>

      <!-- Status Filter -->
      <view class="status-tabs">
        <view
          v-for="tab in statusTabs"
          :key="tab.value"
          class="tab"
          :class="{ active: query.status === tab.value }"
          @click="switchStatus(tab.value)"
        >
          <text>{{ tab.label }}</text>
        </view>
      </view>
    </view>

    <!-- List -->
    <scroll-view
      scroll-y
      class="list-scroll"
      enhanced
      :show-scrollbar="false"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onPullDown"
      @scrolltolower="loadMore"
    >
      <view class="list-wrapper">
        <view
          class="list-card"
          v-for="(item, index) in records"
          :key="item.id"
          @click="goDetail(item.id)"
          :style="{ animationDelay: `${index * 0.05}s` }"
        >
          <view class="card-row">
            <image
              v-if="item.coverImage"
              :src="item.coverImage"
              class="card-cover"
              mode="aspectFill"
            ></image>
            <view class="card-cover card-cover-empty" v-else>
              <uni-icons type="image" size="28" color="#8a9aaa"></uni-icons>
            </view>
            <view class="card-info">
              <view class="card-title-row">
                <text class="card-title">{{ item.title }}</text>
                <view class="status-tag" :class="statusClass(item.status)">
                  <text>{{ statusText(item.status) }}</text>
                </view>
              </view>
              <text class="card-desc" v-if="item.description">{{ item.description }}</text>
              <view class="card-stats">
                <text>{{ item.modelCount || 0 }}件模型</text>
                <text>{{ item.viewCount || 0 }}浏览</text>
                <text>{{ item.likeCount || 0 }}赞</text>
              </view>
            </view>
          </view>
          <view class="card-actions">
            <button
              v-if="item.status === 0"
              class="btn primary"
              @click.stop="publishList(item)"
            >发布</button>
            <button
              v-if="item.status === 0"
              class="btn"
              @click.stop="editList(item)"
            >编辑</button>
            <button class="btn ghost" @click.stop="deleteList(item)">删除</button>
          </view>
        </view>
      </view>

      <view class="empty-state" v-if="!loading && !records.length">
        <view class="empty-icon">
          <uni-icons type="compose" size="48" color="#8a9aaa"></uni-icons>
        </view>
        <text class="empty-text">暂无清单</text>
        <text class="empty-hint">点击右上角新建你的第一个清单</text>
      </view>

      <view class="loading-state" v-if="loading">
        <view class="loading-spinner"></view>
        <text>加载中...</text>
      </view>

      <view class="load-more-hint" v-if="!loading && records.length">
        <view class="hint-line"></view>
        <text>{{ hasMore ? '上拉加载更多' : '已经到底了' }}</text>
        <view class="hint-line"></view>
      </view>
    </scroll-view>

    <!-- Create / Edit Modal -->
    <view class="create-mask" v-if="showForm" @click.self="cancelForm">
      <view class="create-panel">
        <view class="panel-title">{{ editingId ? '编辑清单' : '新建清单' }}</view>
        <input class="panel-input" v-model="form.title" placeholder="请输入清单标题" maxlength="100" />
        <textarea
          class="panel-textarea"
          v-model="form.description"
          placeholder="请输入清单描述（可选）"
          maxlength="500"
        ></textarea>
        <view class="panel-actions">
          <button class="btn ghost" @click="cancelForm">取消</button>
          <button class="btn primary" @click="submitForm" :disabled="submitting">
            {{ submitting ? '提交中...' : (editingId ? '保存' : '创建') }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import {
  createModelListApi,
  updateModelListApi,
  deleteModelListApi,
  publishModelListApi,
  getMyModelListPageApi
} from '../../api/modelList'

const records = ref([])
const loading = ref(false)
const refreshing = ref(false)
const hasMore = ref(true)
const totalCount = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: null
})

const statusTabs = [
  { label: '全部', value: null },
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 }
]

const showForm = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const form = reactive({
  title: '',
  description: ''
})

const statusText = (status) => {
  if (status === 0) return '草稿'
  if (status === 1) return '已发布'
  if (status === 2) return '已下架'
  return '未知'
}

const statusClass = (status) => {
  if (status === 0) return 'draft'
  if (status === 1) return 'published'
  return 'offline'
}

const buildPayload = () => {
  const payload = { pageNum: query.pageNum, pageSize: query.pageSize }
  if (query.status !== null) payload.status = query.status
  return payload
}

const fetchList = async (append = false) => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await getMyModelListPageApi(buildPayload())
    const list = Array.isArray(res?.records) ? res.records : []
    records.value = append ? [...records.value, ...list] : list
    totalCount.value = Number(res?.total || 0)
    hasMore.value = records.value.length < totalCount.value
  } catch (error) {
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const refreshList = async () => {
  query.pageNum = 1
  hasMore.value = true
  await fetchList(false)
}

const loadMore = async () => {
  if (!hasMore.value || loading.value) return
  query.pageNum += 1
  await fetchList(true)
}

const onPullDown = async () => {
  refreshing.value = true
  await refreshList()
}

const switchStatus = async (status) => {
  query.status = status
  await refreshList()
}

const openCreateForm = () => {
  editingId.value = null
  form.title = ''
  form.description = ''
  showForm.value = true
}

const editList = (item) => {
  editingId.value = item.id
  form.title = item.title || ''
  form.description = item.description || ''
  showForm.value = true
}

const cancelForm = () => {
  showForm.value = false
  editingId.value = null
  form.title = ''
  form.description = ''
}

const submitForm = async () => {
  if (!form.title.trim()) {
    uni.showToast({ title: '请输入清单标题', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateModelListApi({
        id: editingId.value,
        title: form.title.trim(),
        description: form.description.trim() || null
      })
      uni.showToast({ title: '更新成功', icon: 'success' })
    } else {
      await createModelListApi({
        title: form.title.trim(),
        description: form.description.trim() || null,
        status: 0
      })
      uni.showToast({ title: '创建成功', icon: 'success' })
    }
    cancelForm()
    await refreshList()
  } catch (error) {
    uni.showToast({ title: error.message || '操作失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

const publishList = async (item) => {
  try {
    await publishModelListApi(item.id)
    item.status = 1
    uni.showToast({ title: '发布成功', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error.message || '发布失败', icon: 'none' })
  }
}

const deleteList = (item) => {
  uni.showModal({
    title: '确认删除',
    content: '删除后清单及其中的模型关联将无法恢复',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await deleteModelListApi(item.id)
        await refreshList()
        uni.showToast({ title: '已删除', icon: 'success' })
      } catch (error) {
        uni.showToast({ title: error.message || '删除失败', icon: 'none' })
      }
    }
  })
}

const goDetail = (id) => {
  uni.navigateTo({ url: `/pages/model-list/detail?id=${id}` })
}

const goPlaza = () => {
  uni.navigateTo({ url: '/pages/model-list/index' })
}

onLoad(async () => {
  if (!ensureLoginOrRedirect()) return
  await refreshList()
})

onShow(async () => {
  if (!ensureLoginOrRedirect()) return
  await refreshList()
})
</script>

<style scoped lang="scss">
$primary: #00bfff;
$deep: #0099cc;
$light: #5ce1ff;
$danger: #ff4d6d;
$bg: #f8f8f8;
$card: #ffffff;
$text1: #1a2030;
$text2: #5a6a7a;
$text3: #8a9aaa;
$gradient: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
$shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
$radius-card: 24rpx;
$radius-capsule: 999rpx;

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(24rpx); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.my-list-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $bg;
  overflow: hidden;
}

.header-card {
  margin: 24rpx 28rpx 0;
  padding: 32rpx;
  background: $card;
  border-radius: $radius-card;
  box-shadow: $shadow;
  animation: fadeInUp 0.4s ease both;

  .title { font-size: 36rpx; font-weight: 700; color: $text1; }
  .desc { font-size: 24rpx; color: $text3; margin-top: 8rpx; }
  .title-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 8rpx;
  height: 64rpx;
  padding: 0 28rpx;
  font-size: 24rpx;
  border-radius: $radius-capsule;
  background: $gradient;
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 6rpx 20rpx rgba(0, 191, 255, 0.2);
  &:active { transform: scale(0.96); }
}

.summary {
  margin-top: 28rpx;
  display: flex;

  .item {
    flex: 1;
    text-align: center;
    &:active { opacity: 0.7; }
  }

  .val { font-size: 30rpx; font-weight: 700; color: $primary; display: block; }
  .val-icon { display: block; }
  .lab { font-size: 22rpx; color: $text3; margin-top: 8rpx; display: block; }
}

.status-tabs {
  margin-top: 24rpx;
  display: flex;
  gap: 12rpx;

  .tab {
    flex: 1;
    text-align: center;
    padding: 14rpx 0;
    font-size: 24rpx;
    color: $text2;
    background: $bg;
    border-radius: $radius-capsule;
    font-weight: 500;
    transition: all 0.25s ease;

    &.active {
      color: #ffffff;
      font-weight: 700;
      background: $gradient;
    }

    &:active { transform: scale(0.96); }
  }
}

.list-scroll {
  flex: 1;
  min-height: 0;
  margin-top: 16rpx;
}

.list-wrapper {
  padding: 0 28rpx 24rpx;
}

.list-card {
  background: $card;
  margin-bottom: 20rpx;
  padding: 24rpx;
  border-radius: $radius-card;
  box-shadow: $shadow;
  animation: fadeInUp 0.4s ease forwards;
  opacity: 0;

  &:active { transform: scale(0.99); }
}

.card-row {
  display: flex;
  gap: 20rpx;
}

.card-cover {
  width: 160rpx;
  height: 160rpx;
  border-radius: 16rpx;
  background: $bg;
}

.card-cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f4fd 0%, #dbeafe 100%);
}

.card-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 28rpx;
  font-weight: 700;
  color: $text1;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-tag {
  font-size: 20rpx;
  padding: 4rpx 14rpx;
  border-radius: $radius-capsule;
  font-weight: 600;
  margin-left: 12rpx;
  flex-shrink: 0;

  &.draft { background: #fef3c7; color: #d97706; }
  &.published { background: #dcfce7; color: #16a34a; }
  &.offline { background: #fee2e2; color: #dc2626; }
}

.card-desc {
  font-size: 24rpx;
  color: $text3;
  margin-top: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-stats {
  margin-top: auto;
  padding-top: 10rpx;
  display: flex;
  gap: 16rpx;
  font-size: 22rpx;
  color: $text3;
}

.card-actions {
  margin-top: 16rpx;
  display: flex;
  gap: 12rpx;
  justify-content: flex-end;

  .btn {
    height: 56rpx;
    padding: 0 24rpx;
    font-size: 22rpx;
    border-radius: $radius-capsule;
    background: $bg;
    color: $text2;
    &:active { transform: scale(0.96); }
  }

  .btn.primary {
    background: $gradient;
    color: #ffffff;
    box-shadow: 0 4rpx 16rpx rgba(0, 191, 255, 0.2);
  }

  .btn.ghost {
    background: $card;
    box-shadow: $shadow;
    color: $danger;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 40rpx;
  animation: fadeInUp 0.5s ease;

  .empty-icon {
    width: 120rpx;
    height: 120rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: $card;
    border-radius: 50%;
    box-shadow: $shadow;
    margin-bottom: 28rpx;
  }

  .empty-text { font-size: 36rpx; color: $text1; font-weight: 700; margin-bottom: 8rpx; }
  .empty-hint { font-size: 28rpx; color: $text3; }
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx;
  gap: 16rpx;

  text { font-size: 26rpx; color: $text3; }
}

.loading-spinner {
  width: 48rpx;
  height: 48rpx;
  border: 4rpx solid $bg;
  border-top-color: $primary;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.load-more-hint {
  display: flex;
  align-items: center;
  padding: 36rpx 40rpx;
  gap: 20rpx;

  text { font-size: 24rpx; color: $text3; white-space: nowrap; }
  .hint-line {
    flex: 1;
    height: 2rpx;
    background: linear-gradient(90deg, transparent 0%, rgba(0, 0, 0, 0.06) 50%, transparent 100%);
  }
}

.create-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 20;
}

.create-panel {
  width: 620rpx;
  padding: 36rpx;
  background: $card;
  border-radius: $radius-card;
  box-shadow: 0 16rpx 60rpx rgba(0, 0, 0, 0.12);

  .panel-title {
    font-size: 36rpx;
    font-weight: 700;
    color: $text1;
  }

  .panel-input {
    margin-top: 24rpx;
    height: 84rpx;
    background: $bg;
    border-radius: 16rpx;
    padding: 0 24rpx;
    font-size: 28rpx;
    color: $text1;
  }

  .panel-textarea {
    margin-top: 20rpx;
    min-height: 180rpx;
    background: $bg;
    border-radius: 16rpx;
    padding: 20rpx 24rpx;
    font-size: 26rpx;
    color: $text1;
  }

  .panel-actions {
    margin-top: 28rpx;
    display: flex;
    gap: 16rpx;

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
    }

    .btn.ghost {
      background: $bg;
      color: $text2;
    }
  }
}
</style>
