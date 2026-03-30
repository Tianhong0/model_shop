<template>
  <view class="page">
    <view class="list" v-if="list.length">
      <view class="item card" v-for="item in list" :key="item.id" @click="goDetail(item)">
        <view class="top">
          <text class="sn">{{ item.afterSaleSn }}</text>
          <text class="status">{{ statusText(item.status) }}</text>
        </view>
        <view class="row">订单：{{ item.orderSn }}</view>
        <view class="row">类型：{{ typeText(item.type) }}</view>
        <view class="row">原因：{{ item.reason }}</view>
        <view class="row">申请金额：￥{{ Number(item.requestedAmount || 0).toFixed(2) }}</view>
      </view>
    </view>
    <view class="empty" v-else>暂无售后记录</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyAfterSaleListApi } from '../../api/order'

const list = ref([])

const typeText = (type) => ({ 1: '仅退款', 2: '退货退款', 3: '补打', 4: '换货' }[Number(type)] || '未知')
const statusText = (status) => ({ 0: '已申请', 1: '审核中', 2: '处理中', 3: '退款中', 4: '已完成', 5: '已拒绝', 6: '已取消' }[Number(status)] || '未知')

const loadData = async () => {
  try {
    const data = await getMyAfterSaleListApi({ pageNum: 1, pageSize: 50 })
    list.value = data?.records || []
  } catch (error) {
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  }
}

const goDetail = (item) => {
  uni.navigateTo({ url: `/pages/user/after-sale-detail?afterSaleSn=${encodeURIComponent(item.afterSaleSn || '')}&id=${encodeURIComponent(item.id || '')}` })
}

onShow(loadData)
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

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(24rpx); }
  to { opacity: 1; transform: translateY(0); }
}

.page {
  min-height: 100vh;
  background: $bg;
  padding: 28rpx;
}

.item {
  background: $card;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: $shadow;
  animation: fadeInUp 0.4s ease both;
}

.top { display: flex; justify-content: space-between; margin-bottom: 14rpx; }
.sn { color: $text1; font-size: 28rpx; font-weight: 600; }
.status {
  color: $primary;
  font-size: 24rpx;
  font-weight: 600;
  background: rgba(0, 191, 255, 0.08);
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
}
.row { font-size: 24rpx; color: $text2; line-height: 1.8; }
.empty { padding-top: 240rpx; text-align: center; color: $text3; font-size: 28rpx; }
</style>
