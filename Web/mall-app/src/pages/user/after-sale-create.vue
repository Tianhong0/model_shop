<template>
  <view class="page">
    <view class="hero">
      <text class="hero-title">申请售后</text>
      <text class="hero-sub">请按真实情况提交，我们会尽快处理</text>
    </view>

    <view class="card">
      <view class="row">
        <text class="label">订单号</text>
        <text class="value">{{ orderSn || '-' }}</text>
      </view>
      <view class="row">
        <text class="label">售后类型</text>
        <picker :range="typeOptions" range-key="label" @change="onTypeChange">
          <view class="picker-value">{{ selectedTypeLabel }}</view>
        </picker>
      </view>
      <view class="row column">
        <text class="section-title">订单信息</text>
        <view class="order-brief">
          <view class="goods-item">
            <image :src="orderBrief.image" mode="aspectFill" class="brief-img" />
            <view class="goods-info">
              <text class="brief-name">{{ orderBrief.name }}</text>
              <text class="brief-params">{{ orderBrief.params }}</text>
              <view class="price-line">
                <text class="brief-price">￥{{ orderBrief.price }}</text>
                <text class="brief-count">x1</text>
              </view>
            </view>
          </view>

          <view class="sum-row"><text>订单金额</text><text>￥{{ orderBrief.price }}</text></view>

          <view class="brief-row" v-if="isRefundType">
            <text class="brief-label">申请金额</text>
            <input class="input" type="digit" v-model="form.requestedAmount" :disabled="lockPrefilledFields" :placeholder="lockPrefilledFields ? '已自动带出不可修改' : '请输入退款金额'" />
          </view>
        </view>
      </view>
      <view class="row column">
        <text class="section-title">申请原因</text>
        <input class="input" v-model="form.reason" placeholder="请输入申请原因" />
      </view>
      <view class="row column">
        <text class="section-title">问题描述</text>
        <textarea class="textarea" v-model="form.description" placeholder="请补充问题详情" />
      </view>
      <view class="row column">
        <text class="section-title">上传凭证</text>
        <view class="upload-actions">
          <button class="mini-btn" @click="chooseEvidence">选择图片/视频</button>
          <text class="upload-tip">最多9个，可混合上传</text>
        </view>
        <view class="media-list" v-if="mediaList.length">
          <view class="media-item" v-for="(media, idx) in mediaList" :key="media.url + idx">
            <image v-if="media.kind === 'image'" :src="media.url" mode="aspectFill" class="media-thumb" @click="previewImage(media.url)" />
            <video
              v-else
              :id="`evidence-video-${idx}`"
              class="video-player"
              :src="media.url"
              :controls="true"
              :show-center-play-btn="true"
              :show-fullscreen-btn="true"
              :page-gesture="true"
              :vslide-gesture-in-fullscreen="true"
              @play="onVideoPlay(idx)"
              @fullscreenchange="onVideoFullscreenChange($event, idx)"
              object-fit="cover"
            />
            <text class="remove" @click="removeMedia(idx)">删除</text>
          </view>
        </view>
      </view>
    </view>

    <button class="submit-btn" @click="submit">提交申请</button>
  </view>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { createAfterSaleApi, getMyAfterSaleListApi, getOrderDetailApi, getOrderDetailBySnApi, uploadAfterSaleMediaApi } from '../../api/order'

const orderId = ref(null)
const orderSn = ref('')
const orderBrief = ref({
  name: '模型订单',
  params: '-',
  price: '0.00',
  image: 'https://images.unsplash.com/photo-1581092160562-40aa08e78837?w=200'
})

const typeOptions = [
  { label: '仅退款', value: 1 },
  { label: '退货退款', value: 2 },
  { label: '补打', value: 3 },
  { label: '换货', value: 4 }
]

const form = ref({
  type: 1,
  reason: '',
  description: '',
  requestedAmount: '',
  evidenceUrls: ''
})
const mediaList = ref([])
const MAX_IMAGE_SIZE = 10 * 1024 * 1024
const MAX_VIDEO_SIZE = 100 * 1024 * 1024
const videoFullscreenState = ref({})
const videoFullscreenExitAt = ref({})
const lockPrefilledFields = ref(false)

const selectedTypeLabel = computed(() => typeOptions.find(item => item.value === form.value.type)?.label || '请选择')
const isRefundType = computed(() => form.value.type === 1 || form.value.type === 2)

const onTypeChange = (event) => {
  const index = Number(event.detail.value || 0)
  form.value.type = typeOptions[index].value
  autoFillRequestedAmount()
}

onLoad((options) => {
  orderId.value = options?.orderId ? String(options.orderId) : null
  orderSn.value = options?.orderSn || ''
  loadOrderBrief()
  loadCanceledAfterSalePrefill()
})

const loadOrderBrief = async () => {
  try {
    const detail = orderSn.value
      ? await getOrderDetailBySnApi(orderSn.value)
      : await getOrderDetailApi(orderId.value)
    if (!detail) return

    let paramsText = detail?.orderSn || '-'
    if (detail?.customParams) {
      try {
        const paramsObj = JSON.parse(detail.customParams)
        const pieces = []
        if (detail?.materialName) pieces.push(detail.materialName)
        if (detail?.materialColor || paramsObj?.color) pieces.push(detail.materialColor || paramsObj?.color)
        if (paramsObj?.scale) pieces.push(`倍率${paramsObj.scale}`)
        if (paramsObj?.fillPercent) pieces.push(`填充${paramsObj.fillPercent}%`)
        if (pieces.length) paramsText = pieces.join(' / ')
      } catch (_) {
        paramsText = detail?.orderSn || '-'
      }
    }

    orderBrief.value = {
      name: detail?.modelName || '模型订单',
      params: paramsText,
      price: Number(detail?.orderPrice || 0).toFixed(2),
      image: detail?.mainImageUrl || orderBrief.value.image
    }

    autoFillRequestedAmount()
  } catch (_) {
    // ignore summary load error
  }
}

const loadCanceledAfterSalePrefill = async () => {
  if (!orderSn.value) return
  try {
    const data = await getMyAfterSaleListApi({ orderSn: orderSn.value, pageNum: 1, pageSize: 20 })
    const records = Array.isArray(data?.records) ? data.records : []
    const canceledRecord = records.find(item => Number(item?.status) === 6)
    if (!canceledRecord) {
      lockPrefilledFields.value = false
      return
    }

    if (canceledRecord?.requestedAmount != null && canceledRecord?.requestedAmount !== '') {
      form.value.requestedAmount = String(canceledRecord.requestedAmount)
    }
    if (typeof canceledRecord?.evidenceUrls === 'string') {
      form.value.evidenceUrls = canceledRecord.evidenceUrls
    }
    lockPrefilledFields.value = true
  } catch (_) {
    lockPrefilledFields.value = false
  }
}

const autoFillRequestedAmount = () => {
  if (!isRefundType.value) return
  if (lockPrefilledFields.value) return
  if (String(form.value.requestedAmount || '').trim()) return

  const orderPrice = Number(orderBrief.value?.price || 0)
  if (!Number.isFinite(orderPrice) || orderPrice <= 0) return
  form.value.requestedAmount = orderPrice.toFixed(2)
}

const chooseEvidence = async () => {
  const remain = 9 - mediaList.value.length
  if (remain <= 0) {
    uni.showToast({ title: '最多上传9个凭证', icon: 'none' })
    return
  }

  try {
    const result = await new Promise((resolve, reject) => {
      uni.chooseMedia({
        count: remain,
        mediaType: ['image', 'video'],
        sourceType: ['album', 'camera'],
        success: resolve,
        fail: reject
      })
    })

    const files = Array.isArray(result?.tempFiles) ? result.tempFiles : []
    if (!files.length) return

    uni.showLoading({ title: '上传中...' })
    let successCount = 0
    let failCount = 0

    for (const file of files) {
      const filePath = file?.tempFilePath || file?.path
      if (!filePath) {
        failCount += 1
        continue
      }

      const mediaKind = file?.fileType === 'video' ? 'video' : 'image'
      const fileSize = Number(file?.size || 0)
      const sizeLimit = mediaKind === 'video' ? MAX_VIDEO_SIZE : MAX_IMAGE_SIZE
      if (fileSize > sizeLimit) {
        failCount += 1
        continue
      }

      try {
        const mediaUrl = await uploadAfterSaleMediaApi(filePath, mediaKind)
        mediaList.value.push({ url: mediaUrl, kind: mediaKind })
        successCount += 1
      } catch (_) {
        failCount += 1
      }

      if (mediaList.value.length >= 9) break
    }

    uni.hideLoading()
    if (successCount > 0 && failCount === 0) {
      uni.showToast({ title: '凭证上传成功', icon: 'success' })
      return
    }
    if (successCount > 0) {
      uni.showToast({ title: `已上传${successCount}个，失败${failCount}个`, icon: 'none' })
      return
    }
    uni.showToast({ title: '凭证上传失败，请检查大小或网络', icon: 'none' })
  } catch (error) {
    uni.hideLoading()
    if (String(error?.errMsg || '').includes('cancel')) return
    uni.showToast({ title: error.message || '凭证上传失败', icon: 'none' })
  }
}

const removeMedia = (index) => {
  mediaList.value.splice(index, 1)
}

const previewImage = (url) => {
  const imageUrls = mediaList.value.filter(item => item.kind === 'image').map(item => item.url)
  if (!imageUrls.length) return
  uni.previewImage({ urls: imageUrls, current: url })
}

const onVideoPlay = async (index) => {
  const lastExitAt = Number(videoFullscreenExitAt.value[index] || 0)
  if (Date.now() - lastExitAt < 1200) return
  if (videoFullscreenState.value[index]) return

  const videoId = `evidence-video-${index}`
  await nextTick()
  const videoContext = uni.createVideoContext(videoId)
  if (!videoContext || typeof videoContext.requestFullScreen !== 'function') return
  setTimeout(() => {
    try {
      videoContext.requestFullScreen({ direction: 0 })
    } catch (_) {
      videoContext.requestFullScreen()
    }
  }, 50)
}

const onVideoFullscreenChange = (event, index) => {
  const isFullscreen = Boolean(event?.detail?.fullScreen)
  videoFullscreenState.value[index] = isFullscreen
  if (!isFullscreen) {
    videoFullscreenExitAt.value[index] = Date.now()
  }
}

const buildEvidenceUrls = () => {
  const uploaded = mediaList.value.map(item => item.url).filter(Boolean)
  const manual = String(form.value.evidenceUrls || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
  return [...new Set([...uploaded, ...manual])].join(',')
}

const submit = async () => {
  if (!orderId.value) {
    uni.showToast({ title: '订单参数无效', icon: 'none' })
    return
  }
  if (!form.value.reason) {
    uni.showToast({ title: '请填写申请原因', icon: 'none' })
    return
  }

  try {
    await createAfterSaleApi({
      orderId: orderId.value,
      type: form.value.type,
      reason: form.value.reason,
      description: form.value.description,
      evidenceUrls: buildEvidenceUrls(),
      requestedAmount: isRefundType.value ? Number(form.value.requestedAmount || 0) : null
    })
    uni.showToast({ title: '申请已提交', icon: 'success' })
    setTimeout(() => {
      uni.redirectTo({ url: '/pages/user/after-sale-list' })
    }, 300)
  } catch (error) {
    uni.showToast({ title: error.message || '提交失败', icon: 'none' })
  }
}
</script>

<style scoped lang="scss">
$primary: #00bfff;
$deep: #0099cc;
$light: #5ce1ff;
$danger: #ff4d6d;
$bg: #f8f8f8;
$card: #ffffff;
$text1: #1a2030;
$text2: #5a6a7a;
$text3: #8a9aaa;
$gradient: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
$shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);

.page {
  min-height: 100vh;
  background: $bg;
  padding: 28rpx;
}

.hero { padding: 12rpx 8rpx 24rpx; }

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

.row {
  display: flex;
  align-items: center;
  min-height: 96rpx;
  & + .row { border-top: 1rpx solid rgba(0,0,0,0.04); }
}

.row.column {
  display: block;
  padding: 24rpx 0;
}

.label {
  width: 170rpx;
  color: $text2;
  font-size: 28rpx;
}

.section-title {
  display: block;
  color: $text1;
  font-size: 28rpx;
  font-weight: 600;
  margin-bottom: 12rpx;
}

.value,
.picker-value {
  color: $text1;
  font-size: 28rpx;
  font-weight: 500;
}

.input {
  flex: 1;
  color: $text1;
  font-size: 28rpx;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 16rpx;
  background: $bg;
  padding: 0 20rpx;
  box-sizing: border-box;
}

.textarea {
  width: 100%;
  min-height: 200rpx;
  margin-top: 12rpx;
  font-size: 28rpx;
  color: $text1;
  border-radius: 16rpx;
  background: $bg;
  padding: 20rpx;
  box-sizing: border-box;
}

.order-brief {
  margin-top: 12rpx;
  border-radius: 20rpx;
  background: $bg;
  padding: 20rpx;
}

.goods-item { display: flex; margin-bottom: 18rpx; }

.brief-img {
  width: 132rpx;
  height: 132rpx;
  border-radius: 16rpx;
  background: $card;
}

.goods-info { margin-left: 16rpx; flex: 1; }
.brief-name { display: block; color: $text1; font-size: 28rpx; font-weight: 700; }
.brief-params { display: block; color: $text2; font-size: 22rpx; margin-top: 8rpx; }
.price-line { display: flex; justify-content: space-between; margin-top: 12rpx; }
.brief-price { color: $deep; font-size: 30rpx; font-weight: 700; }
.brief-count { color: $text3; font-size: 24rpx; }
.sum-row { display: flex; justify-content: space-between; color: $text2; font-size: 24rpx; margin-top: 10rpx; }

.brief-row {
  display: flex;
  align-items: center;
  min-height: 76rpx;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx dashed rgba(0,0,0,0.08);
}

.brief-row.column { display: block; }
.brief-label { width: 150rpx; color: $text2; font-size: 24rpx; }

.upload-actions {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 12rpx;
}

.mini-btn {
  margin: 0;
  height: 68rpx;
  line-height: 68rpx;
  padding: 0 28rpx;
  border-radius: 999rpx;
  background: $gradient;
  color: #fff;
  font-size: 24rpx;
  border: none;
  &:active { transform: scale(0.96); }
}

.upload-tip { color: $text3; font-size: 22rpx; }

.media-list {
  display: flex;
  flex-wrap: wrap;
  gap: 18rpx;
  margin-top: 18rpx;
}

.media-item { width: 180rpx; }
.media-thumb { width: 180rpx; height: 180rpx; border-radius: 16rpx; background: $bg; }
.video-player { width: 180rpx; height: 180rpx; border-radius: 16rpx; background: $text1; overflow: hidden; }
.remove { margin-top: 10rpx; display: block; text-align: center; color: $danger; font-size: 22rpx; font-weight: 500; }

.submit-btn {
  margin-top: 36rpx;
  border-radius: 999rpx;
  background: $gradient;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  box-shadow: 0 8rpx 30rpx rgba(0, 191, 255, 0.25);
  &:active { transform: scale(0.96); }
}
</style>
