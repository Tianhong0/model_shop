<template>
  <view class="page" v-if="detail">
    <view class="hero-stage">
      <swiper class="swiper" circular autoplay>
        <swiper-item v-for="(item, index) in detail.imageUrls || []" :key="index">
          <image :src="item" class="banner" mode="aspectFill"></image>
        </swiper-item>
      </swiper>
      <view class="hero-overlay">
        <view class="hero-badges">
          <text class="hero-chip warm">{{ detail.conditionLevel || '成色未知' }}</text>
          <text class="hero-chip">{{ detail.location || '未知地区' }}</text>
        </view>
        <view class="hero-count">{{ (detail.imageUrls || []).length }} 张实拍</view>
      </view>
    </view>

    <view class="headline-card">
      <text class="eyebrow">SECOND LIFE DETAIL</text>
      <view class="title-row">
        <view class="title-block">
          <view class="title">{{ detail.title }}</view>
          <view class="seller-line">卖家：{{ detail.sellerNickname || '平台用户' }}</view>
        </view>
        <view class="title-actions">
          <view class="side-status">{{ detail.owner ? '我的商品' : '在售商品' }}</view>
          <view v-if="!detail.owner" class="report-icon-btn" @click="goReport">
            <text class="report-icon-mark">!</text>
          </view>
        </view>
      </view>

      <view class="price-panel">
        <view>
          <view class="price">￥{{ Number(detail.price || 0).toFixed(2) }}</view>
          <view class="origin" v-if="detail.originalPrice">原价 ￥{{ Number(detail.originalPrice).toFixed(2) }}</view>
        </view>
        <view class="insight-box">
          <text class="insight-label">浏览</text>
          <text class="insight-value">{{ detail.viewCount || 0 }}</text>
          <text class="insight-sub">人看过</text>
        </view>
      </view>

      <view class="meta-strip">
        <view class="meta-pill meta-pill-code">
          <text class="meta-label">商品编号</text>
          <text class="meta-value meta-value-code">{{ detail.id }}</text>
        </view>
        <view class="meta-pill">
          <text class="meta-label">想要人数</text>
          <text class="meta-value">{{ detail.wantCount || 0 }}</text>
        </view>
        <view class="meta-pill">
          <text class="meta-label">状态</text>
          <text class="meta-value">{{ detail.status === 1 ? '在售中' : detail.status === 2 ? '已下架' : detail.status === 3 ? '已成交' : '草稿' }}</text>
        </view>
      </view>
    </view>

    <view class="content-card story-card">
      <view class="section-head">
        <text class="section-title">商品描述</text>
        <text class="section-kicker">REAL SELLER NOTES</text>
      </view>
      <view class="desc">{{ detail.description || '卖家暂未补充描述' }}</view>
    </view>

    <view class="content-card seller-card">
      <view class="section-head">
        <text class="section-title">卖家信息</text>
        <text class="section-kicker">TRUST SNAPSHOT</text>
      </view>
      <view class="seller-panel">
        <image v-if="detail.sellerAvatar" :src="detail.sellerAvatar" class="seller-avatar" mode="aspectFill" />
        <view v-else class="seller-avatar placeholder">{{ (detail.sellerNickname || '卖').slice(0, 1) }}</view>
        <view class="seller-main">
          <text class="seller-name">{{ detail.sellerNickname || '平台用户' }}</text>
          <text class="seller-tip">议价、聊天、收到的报价，现在都集中放在商品沟通页里处理。</text>
        </view>
      </view>
    </view>

    <view class="content-card bargain-card">
      <view class="section-head">
        <text class="section-title">商品沟通</text>
        <text class="section-kicker">CHAT & NEGOTIATE</text>
      </view>
      <view class="comm-grid">
        <view class="comm-card warm">
          <text class="comm-label">{{ detail.owner ? '收到的议价' : '我的议价' }}</text>
          <text class="comm-value">{{ (detail.offers || []).length }} 条</text>
        </view>
        <view class="comm-card">
          <text class="comm-label">沟通方式</text>
          <text class="comm-value">实时聊天</text>
        </view>
      </view>
      <view class="comm-tip">{{ detail.owner ? '点进沟通页可按买家切换会话，并直接处理收到的议价。' : '点进沟通页可以边聊天边出价，接受后的报价还能直接下单。' }}</view>
      <button class="submit-btn" @click="goChat">进入商品沟通</button>
    </view>

    <view class="bottom-bar">
      <button class="ghost-btn" @click="goChat">{{ detail.owner ? '查看沟通' : '商品沟通' }}</button>
      <button v-if="detail.owner" class="primary-btn" @click="toggleStatus">{{ detail.status === 1 ? '下架商品' : '重新上架' }}</button>
      <button v-else class="primary-btn" @click="createOrder">立即下单</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { createUsedOrderApi, getUsedListingDetailApi, updateUsedListingStatusApi } from '../../api/used'

const listingId = ref('')
const detail = ref(null)

const loadDetail = async () => {
  detail.value = await getUsedListingDetailApi(listingId.value)
}

const createOrder = async () => {
  try {
    await createUsedOrderApi({
      listingId: listingId.value,
      receiverName: '默认收货人',
      receiverPhone: '13800000000',
      receiverAddress: '请在订单详情中确认地址'
    })
    uni.showToast({ title: '订单已创建', icon: 'success' })
    uni.navigateTo({ url: '/pages/used/my-purchases' })
  } catch (error) {
    uni.showToast({ title: error.message || '下单失败', icon: 'none' })
  }
}

const toggleStatus = async () => {
  const targetStatus = detail.value?.status === 1 ? 2 : 1
  try {
    await updateUsedListingStatusApi({ listingId: listingId.value, status: targetStatus })
    uni.showToast({ title: '状态已更新', icon: 'success' })
    loadDetail()
  } catch (error) {
    uni.showToast({ title: error.message || '状态更新失败', icon: 'none' })
  }
}

const goChat = () => {
  if (detail.value?.owner) {
    uni.navigateTo({ url: `/pages/used/chat?listingId=${listingId.value}` })
    return
  }
  if (!detail.value?.sellerId) return
  uni.navigateTo({ url: `/pages/used/chat?listingId=${listingId.value}&counterpartId=${detail.value.sellerId}` })
}

const goReport = () => uni.navigateTo({ url: `/pages/used/report?targetType=LISTING&targetId=${listingId.value}` })

onLoad(async (options) => {
  listingId.value = options?.id || ''
  if (!listingId.value) {
    uni.showToast({ title: '参数错误', icon: 'none' })
    return
  }
  try {
    await loadDetail()
  } catch (error) {
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  }
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding-bottom: 150rpx;
  background:
    radial-gradient(circle at top left, rgba(255, 224, 181, 0.62), transparent 26%),
    radial-gradient(circle at 90% 10%, rgba(255, 243, 223, 0.74), transparent 18%),
    linear-gradient(180deg, #fff7ef 0%, #fffdfa 36%, #f5efe8 100%);
}

.hero-stage {
  position: relative;
  margin: 20rpx 20rpx 0;
  border-radius: 34rpx;
  overflow: hidden;
  box-shadow: 0 24rpx 46rpx rgba(68, 37, 16, 0.16);
}

.swiper { width: 100%; height: 700rpx; }
.banner { width: 100%; height: 100%; }

.hero-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16rpx;
  padding: 24rpx;
  background: linear-gradient(180deg, transparent 0%, rgba(28, 17, 11, 0.72) 100%);
}

.hero-badges {
  display: flex;
  gap: 10rpx;
  flex-wrap: wrap;
}

.hero-chip,
.hero-count {
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  color: #fff7ef;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8rpx);
}

.hero-chip.warm {
  background: rgba(255, 171, 90, 0.26);
}

.headline-card,
.content-card {
  margin: 20rpx;
  padding: 26rpx;
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(178, 123, 81, 0.08);
  box-shadow: 0 14rpx 34rpx rgba(89, 47, 18, 0.06);
}

.eyebrow {
  display: block;
  color: #a16207;
  font-size: 20rpx;
  letter-spacing: 4rpx;
}

.title-row,
.price-panel,
.section-head,
.seller-panel,
.offer-top,
.suggest-price-row {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
}

.title-row {
  align-items: flex-start;
  margin-top: 14rpx;
}

.title-actions {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-shrink: 0;
}

.title-block {
  flex: 1;
  min-width: 0;
}

.title {
  font-size: 38rpx;
  font-weight: 700;
  color: #2f1d14;
  line-height: 1.4;
}

.seller-line {
  display: block;
  margin-top: 12rpx;
  color: #8a6b54;
  font-size: 24rpx;
}

.side-status {
  flex-shrink: 0;
  padding: 12rpx 18rpx;
  border-radius: 999rpx;
  background: #fff2dc;
  color: #b45309;
  font-size: 22rpx;
}

.report-icon-btn {
  width: 66rpx;
  height: 66rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #fff7ef 0%, #ffe9d9 100%);
  border: 1px solid #f2d3bc;
  box-shadow: 0 10rpx 20rpx rgba(194, 65, 12, 0.1);
}

.report-icon-mark {
  color: #c2410c;
  font-size: 32rpx;
  font-weight: 700;
  line-height: 1;
}

.price-panel {
  align-items: flex-end;
  margin-top: 20rpx;
}

.price { font-size: 50rpx; font-weight: 700; color: #c2410c; }

.origin {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #a8a29e;
  text-decoration: line-through;
}

.insight-box {
  min-width: 150rpx;
  padding: 18rpx;
  border-radius: 24rpx;
  background: linear-gradient(180deg, #fff8ee 0%, #fff2de 100%);
  text-align: right;
}

.insight-label,
.insight-sub,
.meta-label,
.section-kicker,
.offer-time,
.seller-tip,
.desc,
.offer-remark,
.helper {
  color: #8a6b54;
}

.insight-label,
.insight-sub,
.meta-label,
.section-kicker,
.offer-time,
.helper {
  font-size: 21rpx;
}

.insight-value,
.meta-value,
.seller-name,
.offer-buyer {
  color: #2f1d14;
}

.insight-value {
  display: block;
  margin-top: 8rpx;
  font-size: 34rpx;
  font-weight: 700;
}

.meta-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14rpx;
  margin-top: 20rpx;
}

.meta-pill {
  min-width: 0;
  padding: 18rpx;
  border-radius: 22rpx;
  background: #fff9f1;
  border: 1px solid #f3e2cf;
}

.meta-pill-code {
  grid-column: span 2;
}

.meta-label {
  display: block;
}

.meta-value {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  font-weight: 700;
}

.meta-value-code {
  font-size: 22rpx;
  line-height: 1.5;
  word-break: break-all;
}

.section-head {
  align-items: center;
  margin-bottom: 18rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #2f1d14;
}

.desc {
  font-size: 26rpx;
  line-height: 1.8;
}

.seller-panel {
  align-items: center;
}

.seller-avatar {
  width: 94rpx;
  height: 94rpx;
  border-radius: 50%;
  flex-shrink: 0;
  background: #eadfd3;
}

.seller-avatar.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7c4a26;
  font-size: 34rpx;
  font-weight: 700;
}

.seller-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.seller-name {
  font-size: 28rpx;
  font-weight: 700;
}

.seller-tip {
  margin-top: 10rpx;
  font-size: 23rpx;
  line-height: 1.6;
}

.comm-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx;
}

.comm-card {
  padding: 22rpx 20rpx;
  border-radius: 22rpx;
  background: #fffaf4;
  border: 1px solid #f1e1cf;
}

.comm-card.warm {
  background: linear-gradient(135deg, #fff2df 0%, #ffe5c7 100%);
}

.comm-label {
  display: block;
  color: #8a6b54;
  font-size: 22rpx;
}

.comm-value {
  display: block;
  margin-top: 10rpx;
  color: #2f1d14;
  font-size: 32rpx;
  font-weight: 700;
}

.comm-tip {
  margin-top: 18rpx;
  color: #8a6b54;
  font-size: 24rpx;
  line-height: 1.7;
}

.suggest-price-card {
  flex: 1;
  padding: 18rpx;
  border-radius: 22rpx;
  background: #fff7ee;
  border: 1px solid #f0dcc7;
}

.suggest-label {
  display: block;
  color: #8a6b54;
  font-size: 22rpx;
}

.suggest-value {
  display: block;
  margin-top: 10rpx;
  color: #c2410c;
  font-size: 30rpx;
  font-weight: 700;
}

.input,
.textarea {
  width: 100%;
  box-sizing: border-box;
  background: #fffdf9;
  border-radius: 20rpx;
  border: 1px solid #eadac8;
  padding: 20rpx;
  font-size: 26rpx;
  color: #2f1d14;
}

.input { margin-top: 18rpx; }

.textarea {
  min-height: 180rpx;
  margin-top: 16rpx;
}

.submit-btn,
.ghost-btn,
.primary-btn,
.mini {
  border-radius: 999rpx;
}

.submit-btn {
  margin-top: 20rpx;
  background: linear-gradient(135deg, #ffb55e 0%, #ff8d39 100%);
  color: #fff;
}

.offer-item {
  background: #fff9f1;
  border-radius: 24rpx;
  padding: 20rpx;
  margin-bottom: 16rpx;
  border: 1px solid #f2e2cf;
}

.offer-item.mine {
  background: #fffdf8;
}

.offer-top {
  align-items: flex-start;
}

.offer-buyer {
  display: block;
  font-size: 26rpx;
  font-weight: 700;
}

.offer-time {
  display: block;
  margin-top: 6rpx;
}

.offer-price {
  color: #c2410c;
  font-weight: 700;
  font-size: 30rpx;
}

.offer-remark {
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.7;
}

.offer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12rpx;
  margin-top: 16rpx;
}

.mini {
  height: 64rpx;
  line-height: 64rpx;
  padding: 0 28rpx;
  font-size: 24rpx;
}

.mini.primary {
  background: #2f1d14;
  color: #fff5ea;
}

.mini.ghost {
  background: #f3e6d8;
  color: #6b4d38;
}

.offer-status {
  margin-top: 14rpx;
  display: inline-block;
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  color: #a16207;
  background: #fff3dd;
  font-size: 22rpx;
}

.offer-status.inline {
  margin-top: 16rpx;
}

.bottom-bar {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: 22rpx;
  padding: 12rpx;
  display: flex;
  gap: 12rpx;
  background: rgba(47, 29, 20, 0.94);
  border-radius: 999rpx;
  box-shadow: 0 18rpx 36rpx rgba(47, 29, 20, 0.18);
}

.ghost-btn {
  flex: 1;
  margin: 0;
  height: 76rpx;
  line-height: 76rpx;
  padding: 0 20rpx;
  background: rgba(255, 255, 255, 0.1);
  color: #fff1df;
  font-size: 25rpx;
}

.primary-btn {
  flex: 1.2;
  margin: 0;
  height: 76rpx;
  line-height: 76rpx;
  padding: 0 22rpx;
  background: linear-gradient(135deg, #ffb55e 0%, #ff8d39 100%);
  color: #fff;
  font-size: 25rpx;
}
</style>
