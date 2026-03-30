<template>
  <view class="page">
    <!-- Hero Section -->
    <view class="hero-panel">
      <view class="hero-copy">
        <text class="eyebrow">SECOND LIFE CURATION</text>
        <view class="hero-title">二手好物</view>
        <view class="hero-sub">把闲置打印成品、展示摆件和创意配件重新流转，让真正喜欢的人接手。</view>
      </view>
      <view class="hero-stat">
        <text class="hero-stat-value">{{ records.length }}</text>
        <text class="hero-stat-label">当前展示</text>
      </view>
    </view>

    <!-- Action Buttons -->
    <view class="action-ribbon">
      <button class="ghost-btn" @click="goMySales">我的出售</button>
      <button class="ghost-btn" @click="goMyPurchases">我的购买</button>
      <button class="primary-btn" @click="goPublish">发布闲置</button>
    </view>

    <!-- Manage Cards -->
    <view class="manage-strip">
      <view class="manage-card" @click="goMySales">
        <view class="manage-info">
          <text class="manage-title">管理我的出售</text>
          <text class="manage-desc">查看在售、已成交和订单动态</text>
        </view>
        <text class="manage-arrow">→</text>
      </view>
      <view class="manage-card" @click="goMyPurchases">
        <view class="manage-info">
          <text class="manage-title">管理我的购买</text>
          <text class="manage-desc">继续支付、查看物流和售后记录</text>
        </view>
        <text class="manage-arrow">→</text>
      </view>
    </view>

    <!-- Search -->
    <view class="search-panel">
      <view class="search-box">
        <text class="search-icon">⌕</text>
        <input v-model="query.keyword" class="search-input" placeholder="搜索标题、描述、地区" confirm-type="search" @confirm="fetchList" />
      </view>
      <button class="mini-btn" @click="fetchList">搜索</button>
    </view>

    <!-- Listing Scroll -->
    <scroll-view scroll-y class="scroll" @scrolltolower="loadMore">
      <view v-if="records.length === 0" class="empty-state">
        <view class="empty-orb"></view>
        <text class="empty-title">还没有二手好物上架</text>
        <text class="empty-sub">不妨先发布第一件，让你的作品进入下一段旅程。</text>
      </view>

      <view v-for="item in records" :key="item.id" class="listing-card fadeInUp" @click="goDetail(item.id)">
        <view class="cover-wrap">
          <image :src="item.coverUrl" class="cover" mode="aspectFill"></image>
          <view class="status-badge">{{ statusText(item.status) }}</view>
        </view>
        <view class="info">
          <view class="name u-line-2">{{ item.title }}</view>
          <view class="meta-row">
            <text class="meta-pill">{{ item.conditionLevel || '成色未知' }}</text>
            <text class="meta-text">{{ item.location || '未知地区' }}</text>
          </view>
          <view class="seller">卖家：{{ item.sellerNickname || '平台用户' }}</view>
          <view class="bottom">
            <view>
              <view class="price">￥{{ Number(item.price || 0).toFixed(2) }}</view>
              <view class="hint">点击查看商品详情</view>
            </view>
            <view class="enter-mark">→</view>
          </view>
        </view>
      </view>
      <view class="safe-bottom"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import { getUsedListingPageApi } from '../../api/used'

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const records = ref([])
const loading = ref(false)
const finished = ref(false)

const statusText = (status) => ({ 0: '草稿', 1: '在售', 2: '已下架', 3: '已成交' }[Number(status)] || '未知')

const fetchList = async (reset = true) => {
  if (loading.value) return
  loading.value = true
  try {
    if (reset) {
      query.pageNum = 1
      finished.value = false
    }
    const res = await getUsedListingPageApi({ ...query })
    const list = Array.isArray(res?.records) ? res.records : []
    records.value = reset ? list : [...records.value, ...list]
    const pages = Number(res?.pages || 1)
    finished.value = query.pageNum >= pages
  } catch (error) {
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  if (finished.value || loading.value) return
  query.pageNum += 1
  fetchList(false)
}

const goDetail = (id) => uni.navigateTo({ url: `/pages/used/detail?id=${id}` })
const goPublish = () => uni.navigateTo({ url: '/pages/used/publish' })
const goMySales = () => uni.navigateTo({ url: '/pages/used/my-sales' })
const goMyPurchases = () => uni.navigateTo({ url: '/pages/used/my-purchases' })

onShow(() => {
  if (!ensureLoginOrRedirect()) return
  fetchList(true)
})
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

.fadeInUp {
  animation: fadeInUp 0.4s ease both;
}

.page {
  min-height: 100vh;
  padding: 28rpx;
  background: $bg;
}

.hero-panel {
  display: flex;
  justify-content: space-between;
  align-items: stretch;
  gap: 20rpx;
  padding: 32rpx;
  border-radius: 28rpx;
  background: linear-gradient(145deg, $deep 0%, $primary 100%);
  box-shadow: 0 16rpx 40rpx rgba(0, 153, 204, 0.18);
  margin-bottom: 28rpx;
}

.hero-copy {
  flex: 1;
}

.eyebrow {
  display: block;
  color: rgba(255, 255, 255, 0.65);
  font-size: 20rpx;
  letter-spacing: 4rpx;
}

.hero-title {
  margin-top: 12rpx;
  font-size: 36rpx;
  font-weight: 700;
  color: #ffffff;
}

.hero-sub {
  margin-top: 14rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.7;
}

.hero-stat {
  width: 160rpx;
  padding: 22rpx 16rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.14);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.hero-stat-value {
  display: block;
  color: #ffffff;
  font-size: 42rpx;
  font-weight: 700;
}

.hero-stat-label {
  display: block;
  margin-top: 10rpx;
  color: rgba(255, 255, 255, 0.72);
  font-size: 22rpx;
}

.action-ribbon {
  display: flex;
  gap: 16rpx;
  margin-bottom: 28rpx;
}

.primary-btn,
.ghost-btn,
.mini-btn {
  border-radius: 999rpx;
  font-size: 24rpx;
  &:active { transform: scale(0.96); }
}

.primary-btn {
  flex: 1;
  background: $gradient;
  color: #fff;
  box-shadow: 0 8rpx 24rpx rgba(0, 191, 255, 0.22);
}

.ghost-btn {
  flex: 1;
  background: $card;
  color: $deep;
  box-shadow: $shadow;
}

.manage-strip {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-bottom: 28rpx;
}

.manage-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
  padding: 32rpx;
  border-radius: 24rpx;
  background: $card;
  box-shadow: $shadow;
}

.manage-info {
  flex: 1;
  min-width: 0;
}

.manage-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: $text1;
}

.manage-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: $text2;
}

.manage-arrow {
  flex-shrink: 0;
  width: 52rpx;
  height: 52rpx;
  line-height: 52rpx;
  text-align: center;
  border-radius: 50%;
  background: rgba(0, 191, 255, 0.1);
  color: $deep;
  font-size: 28rpx;
}

.search-panel {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 28rpx;
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12rpx;
  height: 84rpx;
  padding: 0 24rpx;
  border-radius: 24rpx;
  background: $card;
  box-shadow: $shadow;
}

.search-icon {
  color: $deep;
  font-size: 28rpx;
}

.search-input {
  flex: 1;
  height: 100%;
  font-size: 28rpx;
  color: $text1;
}

.mini-btn {
  min-width: 144rpx;
  background: $deep;
  color: #ffffff;
  padding: 0 28rpx;
}

.scroll {
  height: calc(100vh - 330rpx);
}

.listing-card {
  display: flex;
  gap: 20rpx;
  margin-bottom: 20rpx;
  padding: 20rpx;
  border-radius: 24rpx;
  background: $card;
  box-shadow: $shadow;
}

.cover-wrap {
  position: relative;
  flex-shrink: 0;
}

.cover {
  width: 228rpx;
  height: 228rpx;
  border-radius: 20rpx;
  background: #e2e8f0;
  opacity: 0;
  animation: fadeInUp 0.45s ease both;
}

.status-badge {
  position: absolute;
  left: 12rpx;
  bottom: 12rpx;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(0, 153, 204, 0.78);
  color: #ffffff;
  font-size: 20rpx;
  backdrop-filter: blur(8rpx);
}

.info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.name {
  font-size: 30rpx;
  font-weight: 600;
  color: $text1;
  line-height: 1.5;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 14rpx;
  flex-wrap: wrap;
}

.meta-pill {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(0, 191, 255, 0.1);
  color: $deep;
  font-size: 22rpx;
}

.meta-text,
.seller,
.hint {
  font-size: 24rpx;
  color: $text2;
}

.seller {
  margin-top: 12rpx;
}

.bottom {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.price {
  color: $deep;
  font-size: 36rpx;
  font-weight: 700;
}

.hint {
  margin-top: 6rpx;
}

.enter-mark {
  width: 54rpx;
  height: 54rpx;
  line-height: 54rpx;
  text-align: center;
  border-radius: 50%;
  background: rgba(0, 191, 255, 0.1);
  color: $deep;
  font-size: 30rpx;
}

.empty-state {
  padding: 120rpx 40rpx;
  text-align: center;
  border-radius: 24rpx;
  background: $card;
  box-shadow: $shadow;
}

.empty-orb {
  width: 120rpx;
  height: 120rpx;
  margin: 0 auto 28rpx;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #b8e4f0 0%, #87ceeb 45%, #00bfff 100%);
  box-shadow: 0 16rpx 36rpx rgba(0, 191, 255, 0.22);
}

.empty-title {
  display: block;
  color: $text1;
  font-size: 30rpx;
  font-weight: 700;
}

.empty-sub {
  display: block;
  margin-top: 12rpx;
  color: $text2;
  font-size: 24rpx;
  line-height: 1.7;
}

.safe-bottom { height: 40rpx; }

@media (max-width: 720rpx) {
  .manage-strip {
    grid-template-columns: 1fr;
  }
}
</style>
