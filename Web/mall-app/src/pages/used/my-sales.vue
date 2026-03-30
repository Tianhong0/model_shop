<template>
  <view class="page">
    <!-- Hero -->
    <view class="hero-panel fadeInUp">
      <view>
        <text class="hero-kicker">MY SECOND-HAND STUDIO</text>
        <text class="hero-title">我的出售</text>
        <text class="hero-sub">统一查看已发布商品和出售订单，保留原有操作但让信息更清晰。</text>
      </view>
      <view class="hero-side">
        <view class="hero-metric">
          <text class="hero-metric-value">{{ listingRecords.length }}</text>
          <text class="hero-metric-label">商品数</text>
        </view>
        <view class="hero-metric hero-metric-soft">
          <text class="hero-metric-value">{{ orderRecords.length }}</text>
          <text class="hero-metric-label">订单数</text>
        </view>
      </view>
    </view>

    <!-- Tabs -->
    <view class="tabs-shell">
      <view class="tabs">
        <view class="tab" :class="{ active: activeTab === 'listing' }" @click="switchTab('listing')">我的商品</view>
        <view class="tab" :class="{ active: activeTab === 'order' }" @click="switchTab('order')">出售订单</view>
      </view>
    </view>

    <!-- List -->
    <scroll-view scroll-y class="scroll">
      <template v-if="activeTab === 'listing'">
        <view v-for="item in listingRecords" :key="item.id" class="card item-card fadeInUp" @click="goDetail(item.id)">
          <view class="thumb-wrap">
            <image :src="item.coverUrl" class="thumb" mode="aspectFill"></image>
            <view class="status-pill">{{ statusText(item.status) }}</view>
          </view>
          <view class="content">
            <view class="name u-line-2">{{ item.title }}</view>
            <view class="meta-row">
              <text class="meta-chip">{{ item.conditionLevel }}</text>
              <text class="meta-text">商品编号 {{ item.id }}</text>
            </view>
            <view class="bottom">
              <view>
                <text class="price">￥{{ Number(item.price || 0).toFixed(2) }}</text>
                <text class="helper">点击查看详情</text>
              </view>
              <button class="mini-btn" @click.stop="toggleListing(item)">{{ item.status === 1 ? '下架' : '上架' }}</button>
            </view>
          </view>
        </view>
      </template>
      <template v-else>
        <view v-for="item in orderRecords" :key="item.id" class="card order-card fadeInUp" @click="goOrderDetail(item.id, 'sell')">
          <view class="row-between top-row">
            <text class="sn">{{ item.orderSn }}</text>
            <text class="order-status">{{ orderStatusText(item.status) }}</text>
          </view>
          <view class="title u-line-1">{{ item.listingTitle }}</view>
          <view class="meta-row order-meta-row">
            <text class="meta-chip meta-chip-warm">买家</text>
            <text class="meta-text">{{ item.buyerNickname || '平台用户' }}</text>
          </view>
          <view class="row-between action-row">
            <view>
              <text class="price">￥{{ Number(item.orderAmount || 0).toFixed(2) }}</text>
              <text class="helper">点击查看订单详情</text>
            </view>
            <button v-if="item.status === 1" class="mini-btn" @click.stop="goOrderDetail(item.id, 'sell')">去发货</button>
          </view>
        </view>
      </template>
      <view v-if="activeTab === 'listing' && listingRecords.length === 0" class="empty-state">
        <view class="empty-icon">◌</view>
        <text class="empty-title">暂无发布记录</text>
        <text class="empty-sub">发布过的闲置商品会集中展示在这里。</text>
      </view>
      <view v-if="activeTab === 'order' && orderRecords.length === 0" class="empty-state">
        <view class="empty-icon empty-icon-warm">◎</view>
        <text class="empty-title">暂无出售订单</text>
        <text class="empty-sub">买家下单后，订单信息会自动出现在这里。</text>
      </view>
    </scroll-view>

    <button class="fab" @click="goPublish">发布新商品</button>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMySellUsedOrderPageApi, getMyUsedListingPageApi, updateUsedListingStatusApi } from '../../api/used'

const activeTab = ref('listing')
const listingRecords = ref([])
const orderRecords = ref([])

const statusText = (status) => ({ 0: '草稿', 1: '在售', 2: '已下架', 3: '已成交' }[Number(status)] || '未知')
const orderStatusText = (status) => ({ 0: '待支付', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消', 5: '售后中' }[Number(status)] || '未知')

const loadData = async () => {
  try {
    const [listingRes, orderRes] = await Promise.all([
      getMyUsedListingPageApi({ pageNum: 1, pageSize: 100 }),
      getMySellUsedOrderPageApi({ pageNum: 1, pageSize: 100 })
    ])
    listingRecords.value = listingRes.records || []
    orderRecords.value = orderRes.records || []
  } catch (error) {
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  }
}

const switchTab = (tab) => {
  activeTab.value = tab
}

const toggleListing = async (item) => {
  const status = Number(item.status) === 1 ? 2 : 1
  try {
    await updateUsedListingStatusApi({ listingId: item.id, status })
    uni.showToast({ title: '状态已更新', icon: 'success' })
    loadData()
  } catch (error) {
    uni.showToast({ title: error.message || '操作失败', icon: 'none' })
  }
}

const goDetail = (id) => uni.navigateTo({ url: `/pages/used/detail?id=${id}` })
const goOrderDetail = (id, role) => uni.navigateTo({ url: `/pages/used/order-detail?id=${id}&role=${role}` })
const goPublish = () => uni.navigateTo({ url: '/pages/used/publish' })

onShow(loadData)
</script>

<style scoped lang="scss">
$primary: #00bfff;
$deep: #0099cc;
$light: #5ce1ff;
$success: #10b981;
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
  padding: 28rpx 0 140rpx;
  background: $bg;
}

.hero-panel {
  margin: 0 28rpx 28rpx;
  padding: 32rpx;
  border-radius: 24rpx;
  background: linear-gradient(145deg, $deep 0%, $primary 100%);
  box-shadow: 0 12rpx 40rpx rgba(0, 153, 204, 0.18);
}

.hero-kicker {
  display: block;
  color: rgba(255, 255, 255, 0.6);
  font-size: 20rpx;
  letter-spacing: 4rpx;
}

.hero-title {
  display: block;
  margin-top: 12rpx;
  color: #ffffff;
  font-size: 36rpx;
  font-weight: 700;
}

.hero-sub {
  display: block;
  margin-top: 14rpx;
  color: rgba(255, 255, 255, 0.78);
  font-size: 24rpx;
  line-height: 1.7;
}

.hero-side {
  display: flex;
  gap: 14rpx;
  margin-top: 24rpx;
}

.hero-metric {
  flex: 1;
  padding: 20rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.15);
}

.hero-metric-soft {
  background: rgba(255, 255, 255, 0.1);
}

.hero-metric-value {
  display: block;
  color: #ffffff;
  font-size: 36rpx;
  font-weight: 700;
}

.hero-metric-label {
  display: block;
  margin-top: 8rpx;
  color: rgba(255, 255, 255, 0.72);
  font-size: 22rpx;
}

.tabs-shell {
  margin: 0 28rpx 20rpx;
}

.tabs {
  display: flex;
  background: $card;
  border-radius: 999rpx;
  padding: 8rpx;
  box-shadow: $shadow;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 18rpx 0;
  font-size: 28rpx;
  color: $text2;
  border-radius: 999rpx;
}

.tab.active {
  background: $gradient;
  color: #fff;
  font-weight: 600;
}

.scroll { height: calc(100vh - 310rpx); }

.card {
  background: $card;
  border-radius: 24rpx;
  padding: 24rpx;
  margin: 0 28rpx 20rpx;
  box-shadow: $shadow;
}

.item-card { display: flex; gap: 18rpx; }

.thumb-wrap {
  position: relative;
  flex-shrink: 0;
}

.thumb {
  width: 188rpx;
  height: 188rpx;
  border-radius: 20rpx;
  background: #e0f2fe;
  opacity: 0;
  animation: fadeInUp 0.45s ease both;
}

.status-pill {
  position: absolute;
  left: 10rpx;
  bottom: 10rpx;
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: $deep;
  color: #fff;
  font-size: 20rpx;
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.name, .title {
  font-size: 30rpx;
  font-weight: 600;
  color: $text1;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-wrap: wrap;
  margin-top: 14rpx;
}

.meta-chip {
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(0, 191, 255, 0.1);
  color: $deep;
  font-size: 22rpx;
}

.meta-chip-warm {
  background: rgba(0, 153, 204, 0.12);
  color: $deep;
}

.meta-text, .sn, .helper {
  font-size: 24rpx;
  color: $text2;
}

.top-row { align-items: flex-start; }

.order-status {
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(0, 191, 255, 0.08);
  color: $deep;
  font-size: 24rpx;
}

.bottom, .row-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bottom { margin-top: auto; }

.action-row { margin-top: 18rpx; align-items: flex-end; }

.price {
  display: block;
  color: $danger;
  font-weight: 700;
  font-size: 34rpx;
}

.helper {
  display: block;
  margin-top: 6rpx;
}

.mini-btn {
  padding: 0 28rpx;
  height: 66rpx;
  line-height: 66rpx;
  background: $gradient;
  color: #fff;
  border-radius: 999rpx;
  font-size: 24rpx;
  &:active { transform: scale(0.96); }
}

.empty-state {
  margin: 0 28rpx;
  padding: 120rpx 36rpx;
  text-align: center;
  border-radius: 24rpx;
  background: $card;
  box-shadow: $shadow;
}

.empty-icon {
  display: block;
  color: $primary;
  font-size: 72rpx;
  line-height: 1;
}

.empty-icon-warm { color: $deep; }

.empty-title {
  display: block;
  margin-top: 20rpx;
  color: $text1;
  font-size: 30rpx;
  font-weight: 700;
}

.empty-sub {
  display: block;
  margin-top: 12rpx;
  color: $text3;
  font-size: 24rpx;
  line-height: 1.7;
}

.fab {
  position: fixed;
  left: 36rpx; right: 36rpx; bottom: 32rpx;
  background: $gradient;
  color: #fff;
  border-radius: 999rpx;
  box-shadow: 0 12rpx 36rpx rgba(0, 191, 255, 0.22);
  &:active { transform: scale(0.96); }
}
</style>
