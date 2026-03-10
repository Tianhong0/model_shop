<template>
  <view class="page">
    <scroll-view scroll-y class="scroll">
      <view v-for="item in records" :key="item.id" class="card order-card" @click="goDetail(item.id)">
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
.page { min-height: 100vh; background: #f8fafc; }
.scroll { height: 100vh; }
.card { background: #fff; border-radius: 24rpx; padding: 24rpx; margin: 20rpx; }
.row-between { display: flex; justify-content: space-between; align-items: center; }
.sn, .status, .meta { font-size: 24rpx; color: #64748b; }
.title { margin-top: 12rpx; font-size: 30rpx; color: #0f172a; font-weight: 600; }
.action-row { margin-top: 18rpx; }
.price { color: #dc2626; font-size: 34rpx; font-weight: 700; }
.mini-btn { height: 64rpx; line-height: 64rpx; padding: 0 26rpx; background: #111827; color: #fff; border-radius: 999rpx; font-size: 24rpx; }
.empty { text-align: center; color: #94a3b8; font-size: 26rpx; padding-top: 180rpx; }
</style>
