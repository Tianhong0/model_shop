<template>
  <view class="my-groups">
    <view class="header">
      <text class="title">我的拼团</text>
    </view>

    <scroll-view scroll-y class="scroll-area" @scrolltolower="loadMore">
      <view v-if="loading" class="loading-wrap">
        <uni-load-more status="loading" />
      </view>

      <view v-else-if="list.length === 0" class="empty-wrap">
        <uni-icons type="shop" size="64" color="#ccc" />
        <text class="empty-text">暂无拼团记录</text>
        <button class="go-btn" @click="goList">去拼团</button>
      </view>

      <view v-else class="group-list">
        <view class="group-card" v-for="item in list" :key="item.id" @click="goDetail(item)">
          <view class="status-tag" :class="'status-' + item.status">
            {{ statusText(item.status) }}
          </view>
          <image :src="item.modelImage" class="cover" mode="aspectFill" />
          <view class="info">
            <text class="name">{{ item.activityName }}</text>
            <view class="price-row">
              <text class="price">¥{{ item.groupPrice }}</text>
            </view>
            <view class="meta">
              <text class="people">{{ item.currentPeople }}/{{ item.targetPeople }}人</text>
              <text class="time" v-if="item.status === 0 && item.remainingSeconds > 0">
                剩余 {{ formatTime(item.remainingSeconds) }}
              </text>
            </view>
            <view class="leader" v-if="item.leaderNickname">
              <image :src="item.leaderAvatar || '/static/default-avatar.png'" class="avatar" />
              <text class="leader-name">{{ item.leaderNickname }}</text>
              <text class="leader-tag" v-if="item.leaderUserId === currentUserId">团长</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="!loading && list.length > 0 && noMore" class="no-more">
        <text>没有更多了</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyGroupsApi } from '@/api/groupBuy'

const list = ref([])
const loading = ref(false)
const noMore = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const currentUserId = ref(null)

const statusText = (status) => {
  const map = { 0: '拼团中', 1: '拼团成功', 2: '拼团失败', 3: '已取消' }
  return map[status] || '未知'
}

const formatTime = (seconds) => {
  if (seconds <= 0) return ''
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) {
    return `${hours}小时${minutes}分`
  }
  return `${minutes}分钟`
}

const loadList = async (isRefresh = false) => {
  if (loading.value) return
  if (!isRefresh && noMore.value) return

  loading.value = true
  try {
    if (isRefresh) {
      pageNum.value = 1
      noMore.value = false
    }

    const res = await getMyGroupsApi({
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })

    const records = res?.records || []
    if (isRefresh) {
      list.value = records
    } else {
      list.value = [...list.value, ...records]
    }

    if (records.length < pageSize.value) {
      noMore.value = true
    } else {
      pageNum.value++
    }
  } catch (e) {
    console.error('加载我的拼团失败', e)
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  loadList()
}

const goDetail = (item) => {
  uni.navigateTo({
    url: `/pages/group-buy/group-detail?id=${item.id}`
  })
}

const goList = () => {
  uni.navigateTo({
    url: '/pages/group-buy/list'
  })
}

onMounted(() => {
  // 获取当前用户ID
  const userInfo = uni.getStorageSync('userInfo')
  currentUserId.value = userInfo?.id || null
  loadList(true)
})
</script>

<style lang="scss" scoped>
.my-groups {
  min-height: 100vh;
  background: #f5f6f8;
}

.header {
  padding: 24rpx 32rpx;
  background: #fff;
  border-bottom: 1rpx solid #eee;
}

.title {
  font-size: 34rpx;
  font-weight: bold;
  color: #333;
}

.scroll-area {
  height: calc(100vh - 100rpx);
}

.loading-wrap,
.empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 0;
}

.empty-text {
  margin-top: 20rpx;
  color: #999;
  font-size: 28rpx;
}

.go-btn {
  margin-top: 30rpx;
  background: linear-gradient(135deg, #ff6b6b 0%, #ff8e53 100%);
  color: #fff;
  font-size: 28rpx;
  padding: 16rpx 48rpx;
  border-radius: 30rpx;
}

.group-list {
  padding: 20rpx;
}

.group-card {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  padding: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
  position: relative;
  overflow: hidden;
}

.status-tag {
  position: absolute;
  top: 0;
  right: 0;
  padding: 8rpx 20rpx;
  font-size: 22rpx;
  border-radius: 0 16rpx 0 16rpx;
}

.status-0 {
  background: rgba(255, 107, 107, 0.1);
  color: #ff6b6b;
}

.status-1 {
  background: rgba(46, 204, 113, 0.1);
  color: #2ecc71;
}

.status-2,
.status-3 {
  background: rgba(149, 165, 166, 0.1);
  color: #95a5a6;
}

.cover {
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
}

.info {
  flex: 1;
  margin-left: 20rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.name {
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  padding-right: 100rpx;
}

.price-row {
  display: flex;
  align-items: baseline;
  margin-top: 8rpx;
}

.price {
  font-size: 32rpx;
  font-weight: bold;
  color: #ff6b6b;
}

.meta {
  display: flex;
  align-items: center;
  margin-top: 8rpx;
}

.people {
  font-size: 24rpx;
  color: #ff6b6b;
  background: rgba(255, 107, 107, 0.1);
  padding: 4rpx 12rpx;
  border-radius: 4rpx;
  margin-right: 16rpx;
}

.time {
  font-size: 24rpx;
  color: #faad14;
}

.leader {
  display: flex;
  align-items: center;
  margin-top: 12rpx;
}

.avatar {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  margin-right: 12rpx;
}

.leader-name {
  font-size: 24rpx;
  color: #666;
}

.leader-tag {
  font-size: 20rpx;
  color: #ff6b6b;
  background: rgba(255, 107, 107, 0.1);
  padding: 2rpx 8rpx;
  border-radius: 4rpx;
  margin-left: 12rpx;
}

.no-more {
  text-align: center;
  padding: 30rpx;
  color: #999;
  font-size: 26rpx;
}
</style>
