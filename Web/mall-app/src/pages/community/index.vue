<template>
  <view class="community-container">
    <view class="header-sticky">
      <view class="search-row">
        <view class="search-bar">
          <uni-icons type="search" size="18" color="#94a3b8"></uni-icons>
          <input
            v-model="query.keyword"
            type="text"
            placeholder="搜索帖子标题或内容"
            @confirm="refreshList"
          />
        </view>
        <view class="manage-btn" @click="goMyPosts">我的帖子</view>
      </view>

      <view class="top-actions">
        <view class="order-tabs">
          <view class="tab" :class="{ active: query.orderBy === 'latest' }" @click="changeOrder('latest')">最新</view>
          <view class="tab" :class="{ active: query.orderBy === 'hot' }" @click="changeOrder('hot')">热门</view>
        </view>
        <view class="my-interaction" @click="goMyInteractions">我的互动</view>
      </view>

      <scroll-view scroll-x class="category-scroll">
        <view
          v-for="item in categoryTabs"
          :key="item.id"
          class="category-item"
          :class="{ active: query.categoryId === item.id }"
          @click="switchCategory(item.id)"
        >
          {{ item.name }}
        </view>
      </scroll-view>
    </view>

    <scroll-view
      scroll-y
      class="post-scroll"
      enhanced
      :show-scrollbar="false"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onPullDown"
      @scrolltolower="loadMore"
    >
      <view class="post-card" v-for="post in records" :key="post.id" @click="goDetail(post.id)">
        <view class="post-head">
          <image :src="post.userAvatar || defaultAvatar" class="avatar" mode="aspectFill"></image>
          <view class="meta">
            <text class="nickname">{{ post.userNickname || '用户' }}</text>
            <text class="time">{{ post.createTime || '' }}</text>
          </view>
          <view class="category-label">{{ post.categoryName || '未分类' }}</view>
        </view>

        <view class="post-title">{{ post.title }}</view>
        <view class="post-summary">{{ post.summary || '' }}</view>

        <view class="media-preview" v-if="post.mediaList && post.mediaList.length" @click.stop="previewPostMedia(post)">
          <image v-if="post.mediaList[0].mediaType === 1" :src="post.mediaList[0].mediaUrl" mode="aspectFill"></image>
          <view v-else class="video-box">
            <uni-icons type="videocam" size="34" color="#ffffff"></uni-icons>
            <text>视频内容，点击查看</text>
          </view>
          <view class="media-count" v-if="post.mediaList.length > 1">+{{ post.mediaList.length - 1 }}</view>
        </view>

        <view class="post-footer">
          <view class="action-item" @click.stop="toggleInteraction(post, 1)">
            <uni-icons :type="post.liked ? 'heart-filled' : 'heart'" size="20" :color="post.liked ? '#ef4444' : '#64748b'"></uni-icons>
            <text :class="{ active: post.liked }">{{ post.likeCount || 0 }}</text>
          </view>
          <view class="action-item" @click.stop="toggleInteraction(post, 2)">
            <uni-icons type="star" size="20" :color="post.collected ? '#f59e0b' : '#64748b'"></uni-icons>
            <text :class="{ active: post.collected }">{{ post.collectCount || 0 }}</text>
          </view>
          <view class="action-item" @click.stop="goDetail(post.id)">
            <uni-icons type="chat" size="20" color="#64748b"></uni-icons>
            <text>{{ post.replyCount || 0 }}</text>
          </view>
        </view>
      </view>

      <view class="empty" v-if="!loading && !records.length">暂无帖子</view>
      <view class="loading" v-if="loading">加载中...</view>
      <view class="load-more" v-if="!loading && records.length">{{ hasMore ? '上拉加载更多' : '没有更多了' }}</view>
    </scroll-view>

    <view class="publish-btn" @click="goPublish">
      <uni-icons type="plusempty" size="28" color="#fff"></uni-icons>
    </view>

    <!-- #ifdef APP-PLUS -->
    <AppTabbar />
    <!-- #endif -->
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import {
  getPostCategoryListApi,
  getPostPageApi,
  togglePostInteractionApi
} from '../../api/community'
// #ifdef APP-PLUS
import AppTabbar from '../../components/AppTabbar.vue'
// #endif

const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=community'

const categoryOptions = ref([])
const records = ref([])
const loading = ref(false)
const refreshing = ref(false)
const hasMore = ref(true)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  categoryId: 0,
  orderBy: 'latest'
})

const categoryTabs = computed(() => [{ id: 0, name: '全部' }, ...categoryOptions.value])

const loadCategories = async () => {
  const list = await getPostCategoryListApi()
  categoryOptions.value = Array.isArray(list) ? list : []
}

const buildPayload = () => {
  const payload = {
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    orderBy: query.orderBy
  }
  if (query.keyword && query.keyword.trim()) payload.keyword = query.keyword.trim()
  if (query.categoryId && query.categoryId > 0) payload.categoryId = query.categoryId
  return payload
}

const fetchList = async (append = false) => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await getPostPageApi(buildPayload())
    const list = Array.isArray(res?.records) ? res.records : []
    records.value = append ? [...records.value, ...list] : list
    const total = Number(res?.total || 0)
    hasMore.value = records.value.length < total
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

const switchCategory = async (categoryId) => {
  query.categoryId = categoryId
  await refreshList()
}

const changeOrder = async (orderBy) => {
  if (query.orderBy === orderBy) return
  query.orderBy = orderBy
  await refreshList()
}

const toggleInteraction = async (post, interactType) => {
  try {
    const res = await togglePostInteractionApi({ postId: post.id, interactType })
    const active = !!res?.active
    if (interactType === 1) {
      post.liked = active
      post.likeCount = Math.max(0, Number(post.likeCount || 0) + (active ? 1 : -1))
    } else {
      post.collected = active
      post.collectCount = Math.max(0, Number(post.collectCount || 0) + (active ? 1 : -1))
    }
  } catch (error) {
    uni.showToast({ title: error.message || '操作失败', icon: 'none' })
  }
}

const previewPostMedia = (post) => {
  const mediaList = Array.isArray(post?.mediaList) ? post.mediaList : []
  const imageUrls = mediaList.filter(item => item.mediaType === 1).map(item => item.mediaUrl)
  if (imageUrls.length) {
    uni.previewImage({ urls: imageUrls, current: imageUrls[0] })
    return
  }
  if (mediaList.length && mediaList[0].mediaType === 2) {
    uni.navigateTo({ url: `/pages/community/post-detail?id=${post.id}` })
  }
}

const goDetail = (id) => {
  uni.navigateTo({ url: `/pages/community/post-detail?id=${id}` })
}

const goPublish = () => {
  uni.navigateTo({ url: '/pages/community/post-edit' })
}

const goMyInteractions = () => {
  uni.navigateTo({ url: '/pages/community/my-interactions' })
}

const goMyPosts = () => {
  uni.navigateTo({ url: '/pages/community/my-posts' })
}

onLoad(async () => {
  if (!ensureLoginOrRedirect()) return
  await loadCategories()
  await refreshList()
})

onShow(async () => {
  if (!ensureLoginOrRedirect()) return
  await refreshList()
})
</script>

<style scoped lang="scss">
.community-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f1f5f9;
  overflow: hidden;
}
.header-sticky {
  background-color: #fff;
  padding-top: calc(env(safe-area-inset-top) + 12rpx);
}
.search-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 0 24rpx;
}
.search-bar {
  flex: 1;
  height: 70rpx;
  background: #f8fafc;
  border-radius: 35rpx;
  display: flex;
  align-items: center;
  padding: 0 20rpx;
  input { flex: 1; margin-left: 12rpx; font-size: 26rpx; }
}
.manage-btn {
  font-size: 24rpx;
  color: #4f46e5;
}
.top-actions {
  margin-top: 16rpx;
  padding: 0 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.order-tabs { display: flex; gap: 24rpx; }
.tab { font-size: 26rpx; color: #64748b; }
.tab.active { color: #4f46e5; font-weight: 700; }
.my-interaction { font-size: 24rpx; color: #4f46e5; }
.category-scroll {
  white-space: nowrap;
  padding: 16rpx 24rpx 20rpx;
}
.category-item {
  display: inline-block;
  margin-right: 28rpx;
  font-size: 24rpx;
  color: #64748b;
}
.category-item.active { color: #4f46e5; font-weight: 700; }
.post-scroll {
  flex: 1;
  min-height: 0;
}
.post-card {
  background: #fff;
  margin-top: 16rpx;
  padding: 24rpx;
}
.post-head {
  display: flex;
  align-items: center;
}
.avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
}
.meta { flex: 1; margin-left: 16rpx; }
.nickname { font-size: 26rpx; color: #1e293b; font-weight: 700; display: block; }
.time { font-size: 22rpx; color: #94a3b8; display: block; margin-top: 4rpx; }
.category-label {
  font-size: 22rpx;
  color: #4f46e5;
  background: #eef2ff;
  padding: 4rpx 14rpx;
  border-radius: 18rpx;
}
.post-title { margin-top: 14rpx; font-size: 30rpx; color: #1e293b; font-weight: 700; }
.post-summary { margin-top: 8rpx; font-size: 26rpx; color: #475569; line-height: 1.5; }
.media-preview {
  margin-top: 16rpx;
  position: relative;
  image { width: 100%; height: 340rpx; border-radius: 12rpx; }
}
.video-box {
  width: 100%;
  height: 340rpx;
  border-radius: 12rpx;
  background: linear-gradient(180deg, #64748b 0%, #334155 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  text {
    color: #fff;
    font-size: 24rpx;
  }
}
.media-count {
  position: absolute;
  right: 12rpx;
  bottom: 12rpx;
  color: #fff;
  background: rgba(0, 0, 0, 0.55);
  border-radius: 20rpx;
  padding: 4rpx 14rpx;
  font-size: 22rpx;
}
.post-footer {
  margin-top: 18rpx;
  display: flex;
  gap: 36rpx;
}
.action-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
  text { font-size: 24rpx; color: #64748b; }
  text.active { color: #ef4444; }
}
.empty,
.loading,
.load-more {
  text-align: center;
  color: #94a3b8;
  font-size: 24rpx;
  padding: 28rpx 0;
}
.publish-btn {
  position: fixed;
  right: 36rpx;
  bottom: calc(120rpx + env(safe-area-inset-bottom));
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12rpx 24rpx rgba(79, 70, 229, 0.35);
}
</style>
