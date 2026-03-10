<template>
  <view class="page">
    <view class="hero-shell">
      <view class="hero-panel">
        <view class="hero-copy">
          <text class="eyebrow">SECOND LIFE CURATION</text>
          <view class="title">二手好物</view>
          <view class="sub">把闲置打印成品、展示摆件和创意配件重新流转，让真正喜欢的人接手。</view>
        </view>
        <view class="hero-stat">
          <text class="hero-stat-value">{{ records.length }}</text>
          <text class="hero-stat-label">当前展示</text>
        </view>
      </view>

      <view class="action-ribbon action-ribbon-top">
        <button class="ghost-btn" @click="goMySales">我的出售</button>
        <button class="ghost-btn" @click="goMyPurchases">我的购买</button>
        <button class="primary-btn" @click="goPublish">发布闲置</button>
      </view>
    </view>

    <view class="manage-strip">
      <view class="manage-card" @click="goMySales">
        <view>
          <text class="manage-title">管理我的出售</text>
          <text class="manage-desc">查看在售、已成交和订单动态</text>
        </view>
        <text class="manage-arrow">→</text>
      </view>
      <view class="manage-card warm" @click="goMyPurchases">
        <view>
          <text class="manage-title">管理我的购买</text>
          <text class="manage-desc">继续支付、查看物流和售后记录</text>
        </view>
        <text class="manage-arrow">→</text>
      </view>
    </view>

    <view class="search-panel">
      <view class="search-box">
        <text class="search-icon">⌕</text>
        <input v-model="query.keyword" class="search-input" placeholder="搜索标题、描述、地区" confirm-type="search" @confirm="fetchList" />
      </view>
      <button class="mini-btn" @click="fetchList">搜索</button>
    </view>

    <scroll-view scroll-y class="scroll" @scrolltolower="loadMore">
      <view v-if="records.length === 0" class="empty-state">
        <view class="empty-orb"></view>
        <text class="empty-title">还没有二手好物上架</text>
        <text class="empty-sub">不妨先发布第一件，让你的作品进入下一段旅程。</text>
      </view>

      <view v-for="item in records" :key="item.id" class="listing-card" @click="goDetail(item.id)">
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
.page {
  min-height: 100vh;
  padding: 24rpx;
  background:
    radial-gradient(circle at top left, rgba(255, 219, 172, 0.65), transparent 26%),
    radial-gradient(circle at 85% 10%, rgba(255, 243, 214, 0.88), transparent 20%),
    linear-gradient(180deg, #fff8ef 0%, #fffdf8 34%, #f7f1e9 100%);
}

.hero-shell {
  margin-bottom: 22rpx;
}

.hero-panel {
  display: flex;
  justify-content: space-between;
  gap: 18rpx;
  padding: 28rpx;
  border-radius: 32rpx;
  background: linear-gradient(145deg, rgba(50, 32, 20, 0.92) 0%, rgba(98, 63, 39, 0.9) 100%);
  box-shadow: 0 18rpx 40rpx rgba(59, 31, 13, 0.18);
}

.hero-copy {
  flex: 1;
}

.eyebrow {
  display: block;
  color: rgba(255, 232, 208, 0.7);
  font-size: 20rpx;
  letter-spacing: 4rpx;
}

.title {
  margin-top: 12rpx;
  font-size: 48rpx;
  font-weight: 700;
  color: #fff7ef;
}

.sub {
  margin-top: 14rpx;
  font-size: 24rpx;
  color: rgba(255, 237, 222, 0.82);
  line-height: 1.7;
}

.hero-stat {
  width: 170rpx;
  padding: 22rpx 16rpx;
  border-radius: 26rpx;
  background: rgba(255, 244, 229, 0.14);
  border: 1px solid rgba(255, 241, 222, 0.16);
}

.hero-stat-value {
  display: block;
  color: #fff2df;
  font-size: 42rpx;
  font-weight: 700;
}

.hero-stat-label {
  display: block;
  margin-top: 10rpx;
  color: rgba(255, 235, 214, 0.76);
  font-size: 22rpx;
}

.action-ribbon {
  display: flex;
  gap: 16rpx;
  margin-top: 18rpx;
}

.action-ribbon-top .ghost-btn,
.action-ribbon-top .primary-btn {
  flex: 1;
  min-width: 0;
}

.primary-btn,
.ghost-btn,
.mini-btn {
  border-radius: 999rpx;
  font-size: 24rpx;
}

.primary-btn {
  flex: 1;
  background: linear-gradient(135deg, #ffb55e 0%, #ff8d39 100%);
  color: #fff;
  box-shadow: 0 12rpx 24rpx rgba(255, 141, 57, 0.22);
}

.ghost-btn {
  flex: 1;
  background: rgba(255, 255, 255, 0.74);
  color: #7c4a26;
  border: 1px solid rgba(171, 99, 44, 0.12);
}

.search-panel {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 22rpx;
}

.manage-strip {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-bottom: 22rpx;
}

.manage-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16rpx;
  padding: 24rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(171, 99, 44, 0.08);
  box-shadow: 0 14rpx 30rpx rgba(80, 41, 16, 0.05);
}

.manage-card.warm {
  background: linear-gradient(180deg, rgba(255, 247, 235, 0.96) 0%, rgba(255, 239, 220, 0.96) 100%);
}

.manage-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #2f1d14;
}

.manage-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: #8a6b54;
}

.manage-arrow {
  flex-shrink: 0;
  width: 52rpx;
  height: 52rpx;
  line-height: 52rpx;
  text-align: center;
  border-radius: 50%;
  background: #fff1db;
  color: #a16207;
  font-size: 28rpx;
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12rpx;
  height: 84rpx;
  padding: 0 22rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(163, 107, 67, 0.08);
  box-shadow: 0 12rpx 28rpx rgba(80, 41, 16, 0.05);
}

.search-icon {
  color: #a16207;
  font-size: 28rpx;
}

.search-input {
  flex: 1;
  height: 100%;
  font-size: 26rpx;
  color: #332117;
}

.mini-btn {
  min-width: 144rpx;
  background: #2f1d14;
  color: #fff5ea;
  padding: 0 28rpx;
}

.scroll {
  height: calc(100vh - 330rpx);
}

.listing-card {
  display: flex;
  gap: 20rpx;
  margin-bottom: 20rpx;
  padding: 18rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(178, 123, 81, 0.08);
  box-shadow: 0 14rpx 34rpx rgba(89, 47, 18, 0.06);
}

.cover-wrap {
  position: relative;
  flex-shrink: 0;
}

.cover {
  width: 228rpx;
  height: 228rpx;
  border-radius: 22rpx;
  background: #e2e8f0;
}

.status-badge {
  position: absolute;
  left: 12rpx;
  bottom: 12rpx;
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(47, 29, 20, 0.72);
  color: #fff5ea;
  font-size: 20rpx;
}

.info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.name {
  font-size: 30rpx;
  font-weight: 700;
  color: #2f1d14;
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
  background: #fff4de;
  color: #a16207;
  font-size: 22rpx;
}

.meta-text,
.seller,
.hint {
  font-size: 23rpx;
  color: #8a6b54;
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
  color: #c2410c;
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
  background: #fff1db;
  color: #a16207;
  font-size: 30rpx;
}

.empty-state {
  position: relative;
  overflow: hidden;
  padding: 120rpx 40rpx;
  text-align: center;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(186, 145, 105, 0.12);
}

.empty-orb {
  width: 120rpx;
  height: 120rpx;
  margin: 0 auto 24rpx;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #fff6d9 0%, #ffd59f 45%, #ffb35d 100%);
  box-shadow: 0 20rpx 38rpx rgba(255, 179, 93, 0.24);
}

.empty-title {
  display: block;
  color: #3f2b1f;
  font-size: 30rpx;
  font-weight: 700;
}

.empty-sub {
  display: block;
  margin-top: 12rpx;
  color: #8a6b54;
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
