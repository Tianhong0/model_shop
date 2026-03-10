<template>
  <view class="page">
    <view class="card">
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
.page { min-height: 100vh; background: #f8fafc; padding: 20rpx; }
.card { background: #fff; border-radius: 24rpx; padding: 24rpx; }
.picker, .textarea { width: 100%; background: #f8fafc; border-radius: 18rpx; padding: 20rpx; font-size: 26rpx; margin-bottom: 16rpx; }
.textarea { min-height: 220rpx; }
.submit-btn { margin-top: 24rpx; background: #111827; color: #fff; border-radius: 999rpx; }
</style>
