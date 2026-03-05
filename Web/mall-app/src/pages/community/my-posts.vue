<template>
  <view class="page-container">
    <view class="filter card">
      <picker :range="statusOptions" @change="onStatusChange" :value="statusIndex">
        <view class="status-picker">{{ statusOptions[statusIndex] }}</view>
      </picker>
      <input v-model="query.keyword" placeholder="搜索我的帖子" class="search-input" @confirm="refresh" />
      <view class="search-btn" @click="refresh">搜索</view>
    </view>

    <view class="list-wrap">
      <view class="post-card card" v-for="post in records" :key="post.id">
        <view class="head">
          <text class="title" @click="goDetail(post.id)">{{ post.title }}</text>
          <text class="status" :class="statusClass(post.status)">{{ statusText(post.status) }}</text>
        </view>
        <text class="summary">{{ post.summary || '' }}</text>
        <view class="meta">
          <text>{{ post.createTime || '' }}</text>
          <text>回复 {{ post.replyCount || 0 }}</text>
        </view>
        <view class="actions">
          <view class="btn" @click="goEdit(post.id)">编辑</view>
          <view class="btn danger" @click="removePost(post)">删除</view>
        </view>
      </view>

      <view class="empty-tip" v-if="!loading && !records.length">暂无帖子</view>
      <view class="loading" v-if="loading">加载中...</view>
      <view class="load-more" v-if="!loading && records.length">{{ hasMore ? '上拉加载更多' : '没有更多了' }}</view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import { deletePostApi, getMyPostPageApi } from '../../api/community'

const loading = ref(false)
const hasMore = ref(true)
const records = ref([])
const statusOptions = ['全部状态', '草稿', '已发布', '已下架']
const statusIndex = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: null,
  keyword: ''
})

const statusText = (status) => {
  if (status === 0) return '草稿'
  if (status === 1) return '已发布'
  if (status === 2) return '已下架'
  return '未知'
}

const statusClass = (status) => {
  if (status === 1) return 'published'
  if (status === 2) return 'offline'
  return 'draft'
}

const fetchList = async (append = false) => {
  if (loading.value) return
  loading.value = true
  try {
    const payload = {
      pageNum: query.pageNum,
      pageSize: query.pageSize
    }
    if (query.status !== null) payload.status = query.status
    if (query.keyword && query.keyword.trim()) payload.keyword = query.keyword.trim()

    const res = await getMyPostPageApi(payload)
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

const onStatusChange = async (e) => {
  const idx = Number(e?.detail?.value || 0)
  statusIndex.value = idx
  query.status = idx === 0 ? null : idx - 1
  await refresh()
}

const goDetail = (id) => {
  uni.navigateTo({ url: `/pages/community/post-detail?id=${id}` })
}

const goEdit = (id) => {
  uni.navigateTo({ url: `/pages/community/post-edit?id=${id}` })
}

const removePost = async (post) => {
  const confirm = await new Promise((resolve) => {
    uni.showModal({ title: '提示', content: `确认删除帖子【${post.title}】吗？`, success: (res) => resolve(!!res.confirm) })
  })
  if (!confirm) return

  try {
    await deletePostApi(post.id)
    uni.showToast({ title: '删除成功', icon: 'success' })
    await refresh()
  } catch (error) {
    uni.showToast({ title: error.message || '删除失败', icon: 'none' })
  }
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
.filter { display: flex; align-items: center; gap: 12rpx; padding: 16rpx; margin-bottom: 16rpx; }
.status-picker {
  width: 180rpx;
  height: 64rpx;
  line-height: 64rpx;
  border-radius: 10rpx;
  background: #f8fafc;
  text-align: center;
  color: #334155;
  font-size: 24rpx;
}
.search-input {
  flex: 1;
  height: 64rpx;
  line-height: 64rpx;
  background: #f8fafc;
  border-radius: 10rpx;
  padding: 0 16rpx;
  font-size: 24rpx;
}
.search-btn {
  width: 100rpx;
  text-align: center;
  color: #4f46e5;
  font-size: 24rpx;
}
.post-card { padding: 20rpx; margin-bottom: 16rpx; }
.head { display: flex; align-items: flex-start; gap: 10rpx; }
.title { flex: 1; font-size: 30rpx; color: #1e293b; font-weight: 700; }
.status { font-size: 20rpx; padding: 4rpx 12rpx; border-radius: 8rpx; }
.status.draft { background: #f1f5f9; color: #64748b; }
.status.published { background: #dcfce7; color: #16a34a; }
.status.offline { background: #fee2e2; color: #dc2626; }
.summary { margin-top: 10rpx; display: block; font-size: 24rpx; color: #475569; }
.meta { margin-top: 10rpx; display: flex; gap: 20rpx; color: #94a3b8; font-size: 22rpx; }
.actions { margin-top: 14rpx; display: flex; gap: 14rpx; }
.btn {
  width: 120rpx;
  text-align: center;
  line-height: 56rpx;
  height: 56rpx;
  border-radius: 30rpx;
  background: #eef2ff;
  color: #4f46e5;
  font-size: 24rpx;
}
.btn.danger { background: #fee2e2; color: #dc2626; }
.empty-tip, .loading, .load-more { text-align: center; color: #94a3b8; font-size: 24rpx; padding: 24rpx 0; }
</style>
