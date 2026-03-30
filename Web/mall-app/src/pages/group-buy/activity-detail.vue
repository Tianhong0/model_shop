<template>
  <view class="activity-detail">
    <scroll-view scroll-y class="scroll-area">
      <!-- 活动封面 -->
      <view class="cover-section">
        <image :src="activity.modelImage || activity.coverImage" class="cover" mode="aspectFill" />
      </view>

      <!-- 活动信息 -->
      <view class="info-card">
        <text class="name">{{ activity.activityName }}</text>
        <view class="price-row">
          <text class="group-price">¥{{ activity.groupPrice }}</text>
          <text class="original-price">¥{{ activity.originalPrice }}</text>
        </view>
        <view class="meta-row">
          <view class="meta-item">
            <text class="meta-value">{{ activity.minPeople }}</text>
            <text class="meta-label">人成团</text>
          </view>
          <view class="meta-item">
            <text class="meta-value">{{ activity.soldCount || 0 }}</text>
            <text class="meta-label">已售</text>
          </view>
          <view class="meta-item">
            <text class="meta-value">{{ activity.timeoutHours }}h</text>
            <text class="meta-label">拼团限时</text>
          </view>
        </view>
      </view>

      <!-- 进行中的拼团 -->
      <view class="section-card" v-if="ongoingGroups.length > 0">
        <view class="section-header">
          <text class="section-title">进行中的拼团</text>
          <text class="section-tip">参与可更快成团</text>
        </view>
        <view class="group-list">
          <view class="group-item" v-for="group in ongoingGroups" :key="group.id" @click="joinGroup(group)">
            <view class="group-info">
              <view class="group-progress">
                <text class="progress-text">还差{{ group.targetPeople - group.currentPeople }}人成团</text>
                <view class="progress-bar">
                  <view class="progress-fill" :style="{ width: (group.currentPeople / group.targetPeople * 100) + '%' }"></view>
                </view>
              </view>
              <view class="group-meta">
                <text class="remaining-time" v-if="group.remainingSeconds > 0">
                  剩余 {{ formatTime(group.remainingSeconds) }}
                </text>
                <text class="current-people">{{ group.currentPeople }}/{{ group.targetPeople }}人</text>
              </view>
            </view>
            <view class="join-btn">去参团</view>
          </view>
        </view>
        <view class="more-groups" v-if="hasMoreGroups" @click="loadMoreGroups">
          <text>查看更多拼团</text>
          <uni-icons type="down" size="14" color="#ff6b6b" />
        </view>
      </view>

      <!-- 模型信息 -->
      <view class="section-card">
        <view class="section-title">商品信息</view>
        <view class="model-info">
          <text class="model-name">{{ activity.modelName }}</text>
          <text class="model-desc">{{ activity.modelDescription || activity.description }}</text>
        </view>
      </view>

      <!-- 活动规则 -->
      <view class="section-card">
        <view class="section-title">活动规则</view>
        <view class="rules">
          <view class="rule-item">
            <text class="rule-num">1</text>
            <text class="rule-text">选择商品规格，发起或参与拼团</text>
          </view>
          <view class="rule-item">
            <text class="rule-num">2</text>
            <text class="rule-text">邀请好友参团，{{ activity.minPeople }}人即可成团</text>
          </view>
          <view class="rule-item">
            <text class="rule-num">3</text>
            <text class="rule-text">拼团成功后自动进入生产队列</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar">
      <button class="create-btn" @click="goCreate">发起拼团</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getActivityDetailApi, getOngoingGroupsApi } from '@/api/groupBuy'

const activityId = ref(null)
const activity = ref({})
const ongoingGroups = ref([])
const hasMoreGroups = ref(false)
const groupsLimit = ref(5)

const formatTime = (seconds) => {
  if (seconds <= 0) return ''
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) {
    return `${hours}小时${minutes}分`
  }
  return `${minutes}分钟`
}

const loadDetail = async () => {
  if (!activityId.value) return

  try {
    const res = await getActivityDetailApi(activityId.value)
    activity.value = res || {}
  } catch (e) {
    console.error('加载活动详情失败', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

const loadOngoingGroups = async () => {
  if (!activityId.value) return

  try {
    const res = await getOngoingGroupsApi(activityId.value, groupsLimit.value + 1)
    const groups = res || []
    if (groups.length > groupsLimit.value) {
      ongoingGroups.value = groups.slice(0, groupsLimit.value)
      hasMoreGroups.value = true
    } else {
      ongoingGroups.value = groups
      hasMoreGroups.value = false
    }
  } catch (e) {
    console.error('加载拼团列表失败', e)
  }
}

const loadMoreGroups = () => {
  groupsLimit.value += 10
  loadOngoingGroups()
}

const goCreate = () => {
  uni.navigateTo({
    url: `/pages/group-buy/create?activityId=${activityId.value}`
  })
}

const joinGroup = (group) => {
  uni.navigateTo({
    url: `/pages/group-buy/group-detail?id=${group.id}`
  })
}

onLoad((options) => {
  activityId.value = options?.id
  loadDetail()
  loadOngoingGroups()
})
</script>

<style lang="scss" scoped>
.activity-detail {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 120rpx;
}

.scroll-area {
  height: calc(100vh - 120rpx);
}

.cover-section {
  width: 100%;
  height: 500rpx;
}

.cover {
  width: 100%;
  height: 100%;
}

.info-card {
  background: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.name {
  font-size: 34rpx;
  font-weight: bold;
  color: #333;
}

.price-row {
  display: flex;
  align-items: baseline;
  margin-top: 20rpx;
}

.group-price {
  font-size: 48rpx;
  font-weight: bold;
  color: #ff6b6b;
}

.original-price {
  font-size: 28rpx;
  color: #999;
  text-decoration: line-through;
  margin-left: 16rpx;
}

.meta-row {
  display: flex;
  justify-content: space-around;
  margin-top: 30rpx;
  padding-top: 30rpx;
  border-top: 1rpx solid #eee;
}

.meta-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.meta-value {
  font-size: 36rpx;
  font-weight: bold;
  color: #ff6b6b;
}

.meta-label {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
}

.section-card {
  background: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 20rpx;
}

.section-header .section-title {
  margin-bottom: 0;
}

.section-tip {
  font-size: 24rpx;
  color: #ff6b6b;
}

.group-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.group-item {
  display: flex;
  align-items: center;
  padding: 20rpx;
  background: #f5f6f8;
  border-radius: 12rpx;
}

.group-info {
  flex: 1;
}

.group-progress {
  margin-bottom: 12rpx;
}

.progress-text {
  font-size: 26rpx;
  color: #333;
  font-weight: 500;
}

.progress-bar {
  height: 8rpx;
  background: #e5e5e5;
  border-radius: 4rpx;
  margin-top: 8rpx;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(135deg, #ff6b6b 0%, #ff8e53 100%);
  border-radius: 4rpx;
  transition: width 0.3s;
}

.group-meta {
  display: flex;
  justify-content: space-between;
}

.remaining-time {
  font-size: 22rpx;
  color: #faad14;
}

.current-people {
  font-size: 22rpx;
  color: #999;
}

.join-btn {
  padding: 12rpx 24rpx;
  background: linear-gradient(135deg, #ff6b6b 0%, #ff8e53 100%);
  color: #fff;
  font-size: 24rpx;
  border-radius: 30rpx;
  margin-left: 20rpx;
}

.more-groups {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20rpx;
  color: #ff6b6b;
  font-size: 26rpx;
  gap: 8rpx;
}

.model-info {
  display: flex;
  flex-direction: column;
}

.model-name {
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
  margin-bottom: 12rpx;
}

.model-desc {
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
}

.rules {
  display: flex;
  flex-direction: column;
}

.rule-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 20rpx;
}

.rule-num {
  width: 40rpx;
  height: 40rpx;
  background: #ff6b6b;
  color: #fff;
  font-size: 24rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rule-text {
  font-size: 26rpx;
  color: #666;
  margin-left: 16rpx;
  line-height: 40rpx;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 20rpx 30rpx;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
}

.create-btn {
  flex: 1;
  background: linear-gradient(135deg, #ff6b6b 0%, #ff8e53 100%);
  color: #fff;
  font-size: 32rpx;
  font-weight: bold;
  border-radius: 44rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
