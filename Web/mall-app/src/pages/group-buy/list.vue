<template>
  <view class="group-buy-list">
    <view class="header">
      <text class="title">拼团专区</text>
    </view>

    <scroll-view scroll-y class="scroll-area" @scrolltolower="loadMore">
      <view v-if="loading" class="loading-wrap">
        <uni-load-more status="loading" />
      </view>

      <view v-else-if="list.length === 0" class="empty-wrap">
        <uni-icons type="shop" size="64" color="#ccc" />
        <text class="empty-text">暂无拼团活动</text>
      </view>

      <view v-else class="activity-list">
        <view class="activity-card" v-for="item in list" :key="item.id" @click="goDetail(item)">
          <image :src="item.modelImage || item.coverImage" class="cover" mode="aspectFill" />
          <view class="info">
            <text class="name">{{ item.activityName }}</text>
            <view class="price-row">
              <text class="group-price">¥{{ item.groupPrice }}</text>
              <text class="original-price">¥{{ item.originalPrice }}</text>
            </view>
            <view class="meta">
              <text class="people">{{ item.minPeople }}人团</text>
              <text class="sold">已售{{ item.soldCount || 0 }}件</text>
            </view>
          </view>
          <view class="action-btn" @click.stop="goCreate(item)">发起拼团</view>
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
import { onLoad } from '@dcloudio/uni-app'
import { getGroupBuyActivitiesApi } from '@/api/groupBuy'

const list = ref([])
const loading = ref(false)
const noMore = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const modelId = ref(null)

const loadList = async (isRefresh = false) => {
  if (loading.value) return
  if (!isRefresh && noMore.value) return

  loading.value = true
  try {
    if (isRefresh) {
      pageNum.value = 1
      noMore.value = false
    }

    const res = await getGroupBuyActivitiesApi({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      modelId: modelId.value || undefined
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
    console.error('加载拼团活动失败', e)
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  loadList()
}

const goDetail = (item) => {
  uni.navigateTo({
    url: `/pages/group-buy/activity-detail?id=${item.id}`
  })
}

const goCreate = (item) => {
  uni.navigateTo({
    url: `/pages/group-buy/create?activityId=${item.id}`
  })
}

onLoad((options) => {
  modelId.value = options?.modelId || null
  loadList(true)
})
</script>

<style lang="scss" scoped>
.group-buy-list {
  min-height: 100vh;
  background: #f5f6f8;
}

.header {
  padding: 24rpx 32rpx;
  background: linear-gradient(135deg, #ff6b6b 0%, #ff8e53 100%);
  color: #fff;
}

.title {
  font-size: 36rpx;
  font-weight: bold;
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

.activity-list {
  padding: 20rpx;
}

.activity-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  padding: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.cover {
  width: 180rpx;
  height: 180rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
}

.info {
  flex: 1;
  margin-left: 20rpx;
  overflow: hidden;
}

.name {
  font-size: 30rpx;
  font-weight: 500;
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.price-row {
  display: flex;
  align-items: baseline;
  margin-top: 12rpx;
}

.group-price {
  font-size: 36rpx;
  font-weight: bold;
  color: #ff6b6b;
}

.original-price {
  font-size: 24rpx;
  color: #999;
  text-decoration: line-through;
  margin-left: 12rpx;
}

.meta {
  display: flex;
  align-items: center;
  margin-top: 12rpx;
}

.people {
  font-size: 24rpx;
  color: #ff6b6b;
  background: rgba(255, 107, 107, 0.1);
  padding: 4rpx 12rpx;
  border-radius: 4rpx;
  margin-right: 16rpx;
}

.sold {
  font-size: 24rpx;
  color: #999;
}

.action-btn {
  background: linear-gradient(135deg, #ff6b6b 0%, #ff8e53 100%);
  color: #fff;
  font-size: 26rpx;
  padding: 16rpx 24rpx;
  border-radius: 30rpx;
  flex-shrink: 0;
}

.no-more {
  text-align: center;
  padding: 30rpx;
  color: #999;
  font-size: 26rpx;
}
</style>
