<template>
  <view class="page-container">
    <view class="tabs card">
      <view class="tab" :class="{ active: interactType === 1 }" @click="switchType(1)">我的点赞</view>
      <view class="tab" :class="{ active: interactType === 2 }" @click="switchType(2)">我的收藏</view>
    </view>

    <view class="list-wrap">
      <view class="post-card card" v-for="post in records" :key="post.id" @click="goDetail(post.id)">
        <text class="title">{{ post.title }}</text>
        <text class="summary">{{ post.summary || '' }}</text>
        <view class="meta">
          <text>{{ post.categoryName || '未分类' }}</text>
          <text>点赞 {{ post.likeCount || 0 }}</text>
          <text>收藏 {{ post.collectCount || 0 }}</text>
        </view>
      </view>
      <view class="empty-tip" v-if="!loading && !records.length">暂无数据</view>
      <view class="loading" v-if="loading">加载中...</view>
      <view class="load-more" v-if="!loading && records.length">{{ hasMore ? '上拉加载更多' : '没有更多了' }}</view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import { getMyInteractionPageApi } from '../../api/community'

const interactType = ref(1)
const loading = ref(false)
const hasMore = ref(true)
const records = ref([])
const query = reactive({ pageNum: 1, pageSize: 10 })

const fetchList = async (append = false) => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await getMyInteractionPageApi({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      interactType: interactType.value
    })
    const list = Array.isArray(res?.records) ? res.records : []
    records.value = append ? [...records.value, ...list] : list
    const total = Number(res?.total || 0)
    hasMore.value = records.value.length < total
  } catch (error) {
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const refresh = async () => {
  query.pageNum = 1
  hasMore.value = true
  await fetchList(false)
}

const switchType = async (type) => {
  if (interactType.value === type) return
  interactType.value = type
  await refresh()
}

const goDetail = (id) => {
  uni.navigateTo({ url: `/pages/community/post-detail?id=${id}` })
}

onLoad(async () => {
  if (!ensureLoginOrRedirect()) return
  await refresh()
})

onReachBottom(async () => {
  if (!hasMore.value || loading.value) return
  query.pageNum += 1
  await fetchList(true)
})
</script>

<style scoped lang="scss">
.page-container { min-height: 100vh; background: #f8fafc; padding: 20rpx; }
.tabs { display: flex; padding: 10rpx; margin-bottom: 20rpx; }
.tab {
  flex: 1;
  text-align: center;
  height: 64rpx;
  line-height: 64rpx;
  color: #64748b;
  font-size: 26rpx;
}
.tab.active { color: #4f46e5; font-weight: 700; background: #eef2ff; border-radius: 10rpx; }
.post-card { padding: 20rpx; margin-bottom: 16rpx; }
.title { font-size: 30rpx; color: #1e293b; font-weight: 700; display: block; }
.summary { margin-top: 10rpx; display: block; font-size: 24rpx; color: #475569; }
.meta { margin-top: 12rpx; display: flex; gap: 20rpx; color: #94a3b8; font-size: 22rpx; }
.empty-tip, .loading, .load-more { text-align: center; color: #94a3b8; font-size: 24rpx; padding: 24rpx 0; }
</style>
