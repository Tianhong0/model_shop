<template>
  <view class="page">
    <view class="card" v-if="order">
      <text class="order-sn">订单号：{{ order.orderSn || '-' }}</text>
      <text class="model">模型：{{ order.modelName || '模型订单' }}</text>
    </view>

    <view class="card form-card">
      <view class="form-item">
        <text class="label">模型质量</text>
        <uni-rate :value="form.modelScore" :max="5" @change="(e) => form.modelScore = Number(e.value || 0)" />
      </view>
      <view class="form-item">
        <text class="label">打印效果</text>
        <uni-rate :value="form.printScore" :max="5" @change="(e) => form.printScore = Number(e.value || 0)" />
      </view>
      <view class="form-item">
        <text class="label">服务体验</text>
        <uni-rate :value="form.serviceScore" :max="5" @change="(e) => form.serviceScore = Number(e.value || 0)" />
      </view>
      <view class="form-item vertical">
        <text class="label">评价内容</text>
        <textarea v-model="form.commentText" class="textarea" placeholder="说说你的真实体验，帮助更多人做选择" maxlength="300" />
      </view>
      <view class="form-item vertical">
        <text class="label">上传图片/视频</text>
        <view class="upload-actions">
          <button class="mini-btn" @click="chooseCommentMedia">选择图片/视频</button>
          <text class="upload-tip">最多9个，可混合上传</text>
        </view>
        <view class="media-list" v-if="mediaList.length">
          <view class="media-item" v-for="(media, idx) in mediaList" :key="media.url + idx">
            <image v-if="media.kind === 'image'" :src="media.url" mode="aspectFill" class="media-thumb" @click="previewImage(media.url)" />
            <video v-else class="video-player" :src="media.url" controls preload="metadata" object-fit="cover" />
            <text class="remove" @click="removeMedia(idx)">删除</text>
          </view>
        </view>
      </view>
      <view class="switch-row">
        <text>匿名评价</text>
        <switch :checked="form.isAnonymous === 1" @change="(e) => form.isAnonymous = e.detail.value ? 1 : 0" />
      </view>
      <button class="submit" @click="submitComment" :disabled="submitting">提交评价</button>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { createOrderCommentApi, getOrderDetailApi, getMyOrderCommentsApi, uploadOrderCommentMediaApi } from '../../api/order'

const orderId = ref(null)
const order = ref(null)
const submitting = ref(false)
const alreadyCommented = ref(false)
const mediaList = ref([])
const MAX_IMAGE_SIZE = 10 * 1024 * 1024
const MAX_VIDEO_SIZE = 100 * 1024 * 1024

const form = reactive({
  modelScore: 5,
  printScore: 5,
  serviceScore: 5,
  commentText: '',
  commentImages: '',
  isAnonymous: 0
})

const chooseCommentMedia = async () => {
  const remain = 9 - mediaList.value.length
  if (remain <= 0) {
    uni.showToast({ title: '最多上传9个媒体', icon: 'none' })
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
        const mediaUrl = await uploadOrderCommentMediaApi(filePath, mediaKind)
        mediaList.value.push({ url: mediaUrl, kind: mediaKind })
        successCount += 1
      } catch (_) {
        failCount += 1
      }
      if (mediaList.value.length >= 9) break
    }
    uni.hideLoading()
    if (successCount > 0 && failCount === 0) {
      uni.showToast({ title: '媒体上传成功', icon: 'success' })
      return
    }
    if (successCount > 0) {
      uni.showToast({ title: `已上传${successCount}个，失败${failCount}个`, icon: 'none' })
      return
    }
    uni.showToast({ title: '媒体上传失败', icon: 'none' })
  } catch (error) {
    uni.hideLoading()
    if (String(error?.errMsg || '').includes('cancel')) return
    uni.showToast({ title: error?.message || '媒体上传失败', icon: 'none' })
  }
}

const removeMedia = (index) => {
  mediaList.value.splice(index, 1)
}

const previewImage = (url) => {
  const urls = mediaList.value.filter(item => item.kind === 'image').map(item => item.url)
  if (!urls.length) return
  uni.previewImage({ urls, current: url })
}

const buildCommentImages = () => {
  return mediaList.value.map(item => item.url).filter(Boolean).join(',')
}

const loadOrder = async () => {
  if (!orderId.value) return
  try {
    order.value = await getOrderDetailApi(orderId.value)
  } catch (error) {
    uni.showToast({ title: error.message || '订单信息加载失败', icon: 'none' })
  }
}

const checkAlreadyCommented = async () => {
  if (!orderId.value) return
  try {
    const data = await getMyOrderCommentsApi({ orderId: orderId.value, pageNum: 1, pageSize: 1 })
    const records = Array.isArray(data?.records) ? data.records : []
    alreadyCommented.value = records.length > 0
    if (alreadyCommented.value) {
      uni.showToast({ title: '该订单已评价', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 500)
    }
  } catch (_) {
    alreadyCommented.value = false
  }
}

const submitComment = async () => {
  if (submitting.value) return
  if (alreadyCommented.value) {
    uni.showToast({ title: '该订单已评价', icon: 'none' })
    return
  }
  if (!orderId.value) {
    uni.showToast({ title: '订单参数无效', icon: 'none' })
    return
  }
  if (!form.modelScore || !form.printScore || !form.serviceScore) {
    uni.showToast({ title: '请完成评分', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    await createOrderCommentApi({
      orderId: orderId.value,
      modelScore: Number(form.modelScore),
      printScore: Number(form.printScore),
      serviceScore: Number(form.serviceScore),
      commentText: String(form.commentText || '').trim(),
      commentImages: buildCommentImages(),
      isAnonymous: Number(form.isAnonymous || 0)
    })
    uni.showToast({ title: '评价成功', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 600)
  } catch (error) {
    uni.showToast({ title: error.message || '评价失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onLoad((options) => {
  orderId.value = options?.orderId ? String(options.orderId) : null
  if (!orderId.value) {
    uni.showToast({ title: '参数无效', icon: 'none' })
    return
  }
  loadOrder()
  checkAlreadyCommented()
})
</script>

<style scoped lang="scss">
.page { min-height: 100vh; background: #f6f7fb; padding: 20rpx; }
.card { background: #fff; border-radius: 24rpx; padding: 24rpx; margin-bottom: 18rpx; box-shadow: 0 8rpx 22rpx rgba(15, 23, 42, 0.04); }
.order-sn { display: block; color: #0f172a; font-size: 28rpx; font-weight: 700; }
.model { display: block; color: #64748b; font-size: 24rpx; margin-top: 10rpx; }
.form-item { display: flex; justify-content: space-between; align-items: center; padding: 18rpx 0; border-bottom: 1px solid #f1f5f9; }
.form-item.vertical { display: block; }
.label { color: #1e293b; font-size: 28rpx; font-weight: 600; }
.textarea { width: 100%; min-height: 180rpx; background: #f8fafc; border-radius: 14rpx; margin-top: 14rpx; padding: 16rpx; font-size: 24rpx; color: #334155; }
.upload-actions { display: flex; align-items: center; gap: 16rpx; margin-top: 10rpx; }
.mini-btn { margin: 0; height: 64rpx; line-height: 64rpx; padding: 0 24rpx; border-radius: 32rpx; background: #4f46e5; color: #fff; font-size: 24rpx; }
.upload-tip { color: #94a3b8; font-size: 22rpx; }
.media-list { display: flex; flex-wrap: wrap; gap: 16rpx; margin-top: 14rpx; }
.media-item { width: 180rpx; }
.media-thumb { width: 180rpx; height: 180rpx; border-radius: 12rpx; background: #f1f5f9; }
.video-player { width: 180rpx; height: 180rpx; border-radius: 12rpx; background: #0f172a; }
.remove { margin-top: 8rpx; display: block; text-align: center; color: #ef4444; font-size: 22rpx; }
.switch-row { display: flex; justify-content: space-between; align-items: center; padding: 20rpx 0; color: #334155; font-size: 26rpx; }
.submit { margin-top: 12rpx; border-radius: 44rpx; background: #4f46e5; color: #fff; font-size: 28rpx; }
.submit[disabled] { opacity: 0.6; }
</style>
