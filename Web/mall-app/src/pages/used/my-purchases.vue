<template>
  <view class="page">
    <scroll-view scroll-y class="scroll">
      <view v-for="item in records" :key="item.id" class="card fadeInUp" @click="goDetail(item.id)">
        <view class="row-between">
          <text class="sn">{{ item.orderSn }}</text>
          <text class="status">{{ statusText(item.status) }}</text>
        </view>
        <view class="title u-line-1">{{ item.listingTitle }}</view>
        <view class="meta">卖家：{{ item.sellerNickname || '平台用户' }}</view>
        <view class="row-between action-row">
          <text class="price">￥{{ Number(item.orderAmount || 0).toFixed(2) }}</text>
          <button class="mini-btn" @click.stop="goDetail(item.id)">查看订单</button>
        </view>
      </view>
      <view v-if="records.length === 0" class="empty">暂无购买记录</view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyBuyUsedOrderPageApi } from '../../api/used'

const records = ref([])
const statusText = (status) => ({ 0: '待支付', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消', 5: '售后中' }[Number(status)] || '未知')

const loadData = async () => {
  try {
    const res = await getMyBuyUsedOrderPageApi({ pageNum: 1, pageSize: 100 })
    records.value = res.records || []
  } catch (error) {
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  }
}

const goDetail = (id) => uni.navigateTo({ url: `/pages/used/order-detail?id=${id}&role=buy` })

onShow(loadData)
</script>

<style scoped lang="scss">
$primary: #00bfff;
$deep: #0099cc;
$light: #5ce1ff;
$danger: #ff4d6d;
$gradient: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
$bg: #f8f8f8;
$card: #ffffff;
$text1: #1a2030;
$text2: #5a6a7a;
$text3: #8a9aaa;
$shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(24rpx); }
  to { opacity: 1; transform: translateY(0); }
}

.fadeInUp { animation: fadeInUp 0.4s ease both; }

.page {
  min-height: 100vh;
  background: $bg;
  padding: 28rpx;
}

.scroll { height: 100vh; }

.card {
  background: $card;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: $shadow;
}

.row-between { display: flex; justify-content: space-between; align-items: center; }

.sn { font-size: 24rpx; color: $text2; }

.status {
  font-size: 24rpx;
  color: $deep;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(0, 191, 255, 0.08);
}

.meta { font-size: 24rpx; color: $text2; margin-top: 12rpx; line-height: 1.6; }

.title { margin-top: 14rpx; font-size: 30rpx; color: $text1; font-weight: 600; }

.action-row { margin-top: 20rpx; }

.price { color: $danger; font-size: 34rpx; font-weight: 700; }

.mini-btn {
  height: 66rpx;
  line-height: 66rpx;
  padding: 0 28rpx;
  background: $gradient;
  color: #fff;
  border-radius: 999rpx;
  font-size: 24rpx;
  &:active { transform: scale(0.96); }
}

.empty {
  text-align: center;
  color: $text3;
  font-size: 28rpx;
  padding-top: 200rpx;
}
</style>
