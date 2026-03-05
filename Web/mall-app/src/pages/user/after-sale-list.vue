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
.page { min-height: 100vh; background: #f8fafc; padding: 20rpx; }
.item { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 18rpx; }
.top { display: flex; justify-content: space-between; margin-bottom: 10rpx; }
.sn { color: #1e293b; font-size: 26rpx; font-weight: 600; }
.status { color: #4f46e5; font-size: 24rpx; }
.row { font-size: 24rpx; color: #475569; line-height: 1.6; }
.empty { padding-top: 200rpx; text-align: center; color: #94a3b8; font-size: 28rpx; }
</style>
