<template>
  <view class="page">
    <view class="card express-card" v-if="delivery">
      <view class="express-top">
        <view>
          <text class="company">{{ delivery.deliveryCompany || '物流公司' }}</text>
          <text class="sn">运单号：{{ delivery.deliverySn || '-' }}</text>
        </view>
        <text class="status">{{ deliveryStatusText(delivery.status) }}</text>
      </view>
      <view class="receiver">
        <text class="name">{{ delivery.receiverName || '收件人' }} {{ delivery.receiverPhone || '' }}</text>
        <text class="addr">{{ delivery.receiverAddress || '-' }}</text>
      </view>
      <view class="time-row">
        <text>发货时间：{{ formatTime(delivery.deliveryTime) }}</text>
        <text>签收时间：{{ formatTime(delivery.receiveTime) }}</text>
      </view>
    </view>

    <view class="card timeline-card" v-if="tracks.length">
      <view class="title">物流轨迹</view>
      <view class="timeline-item" v-for="(item, index) in tracks" :key="item.id || index">
        <view class="dot-wrap">
          <view class="dot" :class="{ active: index === 0 }" />
          <view class="line" v-if="index !== tracks.length - 1" />
        </view>
        <view class="timeline-content">
          <text class="content" :class="{ active: index === 0 }">{{ item.trackContent }}</text>
          <text class="time">{{ formatTime(item.trackTime) }}</text>
        </view>
      </view>
    </view>

    <view class="card empty-card" v-else>
      <text>暂无物流轨迹</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getMyDeliveryDetailApi } from '../../api/order'

const delivery = ref(null)
const tracks = ref([])

const formatTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

const deliveryStatusText = (status) => ({
  0: '待发货',
  1: '已发货',
  2: '运输中',
  3: '已签收',
  4: '异常'
}[Number(status)] || '未知')

const loadDetail = async (orderSn) => {
  if (!orderSn) {
    uni.showToast({ title: '订单号无效', icon: 'none' })
    return
  }
  try {
    const data = await getMyDeliveryDetailApi(orderSn)
    delivery.value = data || null
    const trackRecords = Array.isArray(data?.tracks) ? data.tracks.slice() : []
    tracks.value = trackRecords.sort((a, b) => {
      const t1 = new Date(a?.trackTime || 0).getTime()
      const t2 = new Date(b?.trackTime || 0).getTime()
      return t2 - t1
    })
  } catch (error) {
    uni.showToast({ title: error.message || '物流信息加载失败', icon: 'none' })
  }
}

onLoad((options) => {
  loadDetail(options?.orderSn)
})
</script>

<style scoped lang="scss">
.page { min-height: 100vh; background: #f6f7fb; padding: 20rpx; }
.card { background: #fff; border-radius: 24rpx; padding: 24rpx; margin-bottom: 18rpx; box-shadow: 0 8rpx 22rpx rgba(15, 23, 42, 0.04); }
.express-top { display: flex; justify-content: space-between; align-items: flex-start; }
.company { display: block; color: #111827; font-size: 32rpx; font-weight: 700; }
.sn { display: block; color: #94a3b8; font-size: 24rpx; margin-top: 8rpx; }
.status { color: #4f46e5; font-size: 24rpx; background: #eef2ff; border-radius: 22rpx; padding: 8rpx 16rpx; }
.receiver { margin-top: 18rpx; border-top: 1px solid #f1f5f9; padding-top: 18rpx; }
.name { display: block; color: #1e293b; font-size: 26rpx; font-weight: 600; }
.addr { display: block; color: #64748b; font-size: 24rpx; margin-top: 8rpx; line-height: 1.5; }
.time-row { margin-top: 14rpx; display: flex; flex-direction: column; gap: 8rpx; color: #94a3b8; font-size: 22rpx; }
.title { color: #0f172a; font-size: 30rpx; font-weight: 700; margin-bottom: 16rpx; }
.timeline-item { display: flex; }
.dot-wrap { width: 40rpx; display: flex; flex-direction: column; align-items: center; }
.dot { width: 14rpx; height: 14rpx; border-radius: 50%; background: #cbd5e1; margin-top: 8rpx; }
.dot.active { background: #4f46e5; }
.line { width: 2rpx; flex: 1; background: #e2e8f0; margin-top: 6rpx; }
.timeline-content { flex: 1; padding-bottom: 22rpx; }
.content { display: block; color: #334155; font-size: 26rpx; line-height: 1.5; }
.content.active { color: #0f172a; font-weight: 600; }
.time { display: block; color: #94a3b8; font-size: 22rpx; margin-top: 8rpx; }
.empty-card { text-align: center; color: #94a3b8; font-size: 24rpx; }
</style>
