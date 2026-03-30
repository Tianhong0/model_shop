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
$primary: #00bfff;
$deep: #0099cc;
$light: #5ce1ff;
$bg: #f8f8f8;
$card: #ffffff;
$text1: #1a2030;
$text2: #5a6a7a;
$text3: #8a9aaa;
$gradient: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
$shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(24rpx); }
  to { opacity: 1; transform: translateY(0); }
}

.page {
  min-height: 100vh;
  background: $bg;
  padding: 28rpx;
}

.card {
  background: $card;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: $shadow;
  animation: fadeInUp 0.4s ease both;
}

.express-top { display: flex; justify-content: space-between; align-items: flex-start; }
.company { display: block; color: $text1; font-size: 32rpx; font-weight: 700; }
.sn { display: block; color: $text3; font-size: 24rpx; margin-top: 8rpx; }
.status { color: $primary; font-size: 24rpx; background: rgba(0, 191, 255, 0.08); border-radius: 999rpx; padding: 8rpx 20rpx; font-weight: 600; }
.receiver { margin-top: 20rpx; padding-top: 20rpx; border-top: 1rpx solid rgba(0,0,0,0.04); }
.name { display: block; color: $text1; font-size: 28rpx; font-weight: 600; }
.addr { display: block; color: $text2; font-size: 24rpx; margin-top: 8rpx; line-height: 1.5; }
.time-row { margin-top: 16rpx; display: flex; flex-direction: column; gap: 8rpx; color: $text3; font-size: 22rpx; }
.title { color: $text1; font-size: 30rpx; font-weight: 700; margin-bottom: 20rpx; }
.timeline-item { display: flex; }
.dot-wrap { width: 44rpx; display: flex; flex-direction: column; align-items: center; }
.dot { width: 16rpx; height: 16rpx; border-radius: 50%; background: #ddd; margin-top: 8rpx; }
.dot.active { background: $primary; box-shadow: 0 0 0 6rpx rgba(0, 191, 255, 0.15); }
.line { width: 2rpx; flex: 1; background: rgba(0,0,0,0.06); margin-top: 8rpx; }
.timeline-content { flex: 1; padding-bottom: 24rpx; }
.content { display: block; color: $text1; font-size: 26rpx; line-height: 1.5; }
.content.active { color: $text1; font-weight: 600; }
.time { display: block; color: $text3; font-size: 22rpx; margin-top: 8rpx; }
.empty-card { text-align: center; color: $text3; font-size: 28rpx; padding: 60rpx 0; }
</style>
