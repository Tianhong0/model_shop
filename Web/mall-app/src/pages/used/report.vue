<template>
  <view class="page">
    <view class="hero fadeInUp">
      <text class="hero-title">举报</text>
      <text class="hero-sub">如果你发现违规行为，请填写以下信息</text>
    </view>
    <view class="card fadeInUp">
      <picker :range="targetTypes" range-key="label" @change="onTargetTypeChange">
        <view class="picker">举报对象：{{ currentTargetLabel }}</view>
      </picker>
      <picker :range="reasonTypes" range-key="label" @change="onReasonTypeChange">
        <view class="picker">举报原因：{{ currentReasonLabel }}</view>
      </picker>
      <textarea v-model="form.reasonText" class="textarea" placeholder="请详细说明问题经过" />
      <button class="submit-btn" @click="submit">提交举报</button>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { createUsedReportApi } from '../../api/used'

const targetTypes = [
  { label: '商品', value: 'LISTING' },
  { label: '订单', value: 'ORDER' },
  { label: '消息', value: 'MESSAGE' }
]
const reasonTypes = [
  { label: '虚假描述', value: 'FAKE_DESC' },
  { label: '疑似诈骗', value: 'FRAUD' },
  { label: '辱骂骚扰', value: 'HARASS' },
  { label: '其他', value: 'OTHER' }
]

const form = reactive({
  targetType: 'LISTING',
  targetId: '',
  reasonType: 'OTHER',
  reasonText: ''
})

const currentTargetLabel = computed(() => targetTypes.find(item => item.value === form.targetType)?.label || '请选择')
const currentReasonLabel = computed(() => reasonTypes.find(item => item.value === form.reasonType)?.label || '请选择')

const onTargetTypeChange = (e) => { form.targetType = targetTypes[e.detail.value]?.value || form.targetType }
const onReasonTypeChange = (e) => { form.reasonType = reasonTypes[e.detail.value]?.value || form.reasonType }

const submit = async () => {
  if (!form.targetId || !form.reasonText) {
    uni.showToast({ title: '请完整填写举报信息', icon: 'none' })
    return
  }
  try {
    await createUsedReportApi({ ...form, targetId: form.targetId })
    uni.showToast({ title: '举报已提交', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 400)
  } catch (error) {
    uni.showToast({ title: error.message || '提交失败', icon: 'none' })
  }
}

onLoad((options) => {
  form.targetType = options?.targetType || 'LISTING'
  form.targetId = options?.targetId || ''
})
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
  padding: 28rpx;
  background: $bg;
}

.hero {
  padding: 16rpx 8rpx 24rpx;
}

.hero-title {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  color: $text1;
}

.hero-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: $text2;
}

.card {
  background: $card;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: $shadow;
}

.picker, .textarea {
  width: 100%;
  background: #fafbfc;
  border-radius: 20rpx;
  padding: 22rpx 24rpx;
  font-size: 28rpx;
  margin-bottom: 16rpx;
  color: $text1;
  box-sizing: border-box;
}

.textarea { min-height: 220rpx; }

.submit-btn {
  margin-top: 28rpx;
  background: $gradient;
  color: #fff;
  border-radius: 999rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 191, 255, 0.2);
  &:active { transform: scale(0.96); }
}
</style>
