<template>
  <view class="edit-container">
    <!-- Hero Header -->
    <view class="page-hero">
      <view class="hero-content">
        <text class="hero-title">{{ isEdit ? '编辑帖子' : '发布帖子' }}</text>
        <text class="hero-subtitle">分享你的 3D 打印经验与创意</text>
      </view>
    </view>

    <!-- Form Card -->
    <view class="form-card">
      <!-- Category Section -->
      <view class="form-section">
        <view class="section-label">
          <view class="label-icon">
            <uni-icons type="bars" size="18" color="#0099cc"></uni-icons>
          </view>
          <text class="label-text">选择分类</text>
        </view>
        <picker :range="categoryNames" @change="onCategoryChange" :value="categoryIndex">
          <view class="picker-trigger">
            <text class="picker-value">{{ selectedCategoryName || '请选择分类' }}</text>
            <uni-icons type="arrowright" size="18" color="#a8a29e"></uni-icons>
          </view>
        </picker>
      </view>

      <!-- Title Section -->
      <view class="form-section">
        <view class="section-label">
          <view class="label-icon">
            <uni-icons type="compose" size="18" color="#00bfff"></uni-icons>
          </view>
          <text class="label-text">帖子标题</text>
          <text class="char-count">{{ titleCount }}/200</text>
        </view>
        <input
          v-model="form.title"
          maxlength="200"
          placeholder="输入一个吸引人的标题..."
          placeholder-class="input-placeholder"
          class="title-input"
        />
      </view>

      <!-- Content Section -->
      <view class="form-section">
        <view class="section-label">
          <view class="label-icon">
            <uni-icons type="list" size="18" color="#0099cc"></uni-icons>
          </view>
          <text class="label-text">帖子内容</text>
          <text class="char-count">{{ contentCount }}/5000</text>
        </view>
        <textarea
          v-model="form.content"
          maxlength="5000"
          placeholder="详细描述你的想法、经验或问题..."
          placeholder-class="input-placeholder"
          class="content-textarea"
        />
      </view>

      <!-- Media Section -->
      <view class="form-section">
        <view class="section-label">
          <view class="label-icon">
            <uni-icons type="image" size="18" color="#00bfff"></uni-icons>
          </view>
          <text class="label-text">添加媒体</text>
          <text class="media-count-badge">{{ form.mediaList.length }}/9</text>
        </view>

        <view class="media-actions">
          <view class="media-btn" @click="chooseImage">
            <view class="btn-icon image-icon">
              <uni-icons type="image" size="24" color="#0099cc"></uni-icons>
            </view>
            <text>添加图片</text>
          </view>
          <view class="media-btn" @click="chooseVideo">
            <view class="btn-icon video-icon">
              <uni-icons type="videocam" size="24" color="#00bfff"></uni-icons>
            </view>
            <text>添加视频</text>
          </view>
        </view>

        <view class="media-grid" v-if="form.mediaList.length">
          <view
            class="media-item"
            v-for="(item, index) in form.mediaList"
            :key="index"
            :style="{ animationDelay: `${index * 0.05}s` }"
          >
            <image v-if="item.mediaType === 1" :src="item.mediaUrl" mode="aspectFill"></image>
            <video
              v-else
              :src="item.mediaUrl"
              controls
              :page-gesture="true"
              :vslide-gesture="false"
              object-fit="cover"
            ></video>
            <view class="media-overlay">
              <view class="media-type-tag">{{ item.mediaType === 1 ? '图片' : '视频' }}</view>
              <view class="delete-btn" @click="removeMedia(index)">
                <uni-icons type="close" size="16" color="#fff"></uni-icons>
              </view>
            </view>
          </view>
        </view>

        <view class="media-empty" v-else>
          <view class="empty-visual">
            <view class="empty-icon-wrap">
              <uni-icons type="image" size="32" color="#e7e5e4"></uni-icons>
            </view>
          </view>
          <text class="empty-hint">添加图片或视频让内容更生动</text>
        </view>
      </view>
    </view>

    <!-- Bottom Actions -->
    <view class="bottom-bar">
      <button class="btn draft-btn" @click="submit(0)">
        <uni-icons type="paperclip" size="18" color="#57534e"></uni-icons>
        <text>存草稿</text>
      </button>
      <button class="btn publish-btn" @click="submit(1)">
        <uni-icons type="paperplane" size="18" color="#fff"></uni-icons>
        <text>发布</text>
      </button>
    </view>

    <!-- Delete Bar for Edit Mode -->
    <view class="delete-section" v-if="isEdit">
      <button class="delete-btn-full" @click="removePost">
        <uni-icons type="trash" size="18" color="#dc2626"></uni-icons>
        <text>删除帖子</text>
      </button>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import {
  createPostApi,
  deletePostApi,
  getPostCategoryListApi,
  getPostDetailApi,
  updatePostApi,
  uploadCommunityMediaApi
} from '../../api/community'

const isEdit = ref(false)
const postId = ref('')
const categoryOptions = ref([])
const categoryIndex = ref(-1)

const form = reactive({
  categoryId: null,
  title: '',
  content: '',
  mediaList: []
})

const categoryNames = computed(() => categoryOptions.value.map(item => item.name))
const titleCount = computed(() => (form.title || '').length)
const contentCount = computed(() => (form.content || '').length)
const selectedCategoryName = computed(() => {
  const found = categoryOptions.value.find(item => item.id === form.categoryId)
  return found?.name || ''
})

const loadCategories = async () => {
  const list = await getPostCategoryListApi()
  categoryOptions.value = Array.isArray(list) ? list : []
}

const loadEditData = async () => {
  const detail = await getPostDetailApi(postId.value)
  form.categoryId = detail?.post?.categoryId || null
  form.title = detail?.post?.title || ''
  form.content = detail?.content || ''
  form.mediaList = (detail?.post?.mediaList || []).map((item, index) => ({
    mediaUrl: item.mediaUrl,
    mediaType: item.mediaType,
    sortOrder: index
  }))
  categoryIndex.value = categoryOptions.value.findIndex(item => item.id === form.categoryId)
}

const onCategoryChange = (e) => {
  const idx = Number(e?.detail?.value || 0)
  categoryIndex.value = idx
  const target = categoryOptions.value[idx]
  form.categoryId = target?.id || null
}

const chooseImage = () => {
  uni.chooseImage({
    count: 6,
    success: async (res) => {
      const paths = res?.tempFilePaths || []
      if (!paths.length) return
      uni.showLoading({ title: '上传中...' })
      try {
        for (const path of paths) {
          const mediaUrl = await uploadCommunityMediaApi(path, 'postImg')
          form.mediaList.push({ mediaUrl, mediaType: 1, sortOrder: form.mediaList.length })
        }
      } catch (error) {
        uni.showToast({ title: error.message || '图片上传失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    }
  })
}

const chooseVideo = () => {
  uni.chooseVideo({
    compressed: true,
    success: async (res) => {
      const filePath = res?.tempFilePath
      if (!filePath) return
      uni.showLoading({ title: '上传中...' })
      try {
        const mediaUrl = await uploadCommunityMediaApi(filePath, 'postVideo')
        form.mediaList.push({ mediaUrl, mediaType: 2, sortOrder: form.mediaList.length })
      } catch (error) {
        uni.showToast({ title: error.message || '视频上传失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    }
  })
}

const removeMedia = (index) => {
  form.mediaList.splice(index, 1)
  form.mediaList = form.mediaList.map((item, idx) => ({ ...item, sortOrder: idx }))
}

const submit = async (status) => {
  if (!form.categoryId) {
    uni.showToast({ title: '请选择分类', icon: 'none' })
    return
  }
  if (!form.title.trim()) {
    uni.showToast({ title: '请输入标题', icon: 'none' })
    return
  }
  if (!form.content.trim()) {
    uni.showToast({ title: '请输入内容', icon: 'none' })
    return
  }

  const payload = {
    categoryId: form.categoryId,
    title: form.title.trim(),
    content: form.content.trim(),
    status,
    mediaList: form.mediaList
  }

  try {
    // 显示AI审核提示（发布时才审核）
    if (status === 1) {
      uni.showLoading({ title: '智能审核中...', mask: true })
    }

    if (isEdit.value) {
      await updatePostApi({ id: postId.value, ...payload })
      uni.hideLoading()
      uni.showToast({ title: '更新成功', icon: 'success' })
    } else {
      await createPostApi(payload)
      uni.hideLoading()
      uni.showToast({ title: status === 1 ? '发布成功' : '草稿已保存', icon: 'success' })
    }
    setTimeout(() => {
      uni.navigateBack()
    }, 500)
  } catch (error) {
    uni.hideLoading()
    uni.showToast({ title: error.message || '提交失败', icon: 'none' })
  }
}

const removePost = async () => {
  const confirm = await new Promise((resolve) => {
    uni.showModal({
      title: '确认删除',
      content: '删除后无法恢复，确定要删除吗？',
      success: (res) => resolve(!!res.confirm)
    })
  })
  if (!confirm) return

  try {
    await deletePostApi(postId.value)
    uni.showToast({ title: '删除成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 500)
  } catch (error) {
    uni.showToast({ title: error.message || '删除失败', icon: 'none' })
  }
}

onLoad(async (options) => {
  if (!ensureLoginOrRedirect()) return
  postId.value = options?.id ? String(options.id) : ''
  isEdit.value = !!postId.value
  try {
    await loadCategories()
    if (isEdit.value) {
      await loadEditData()
    }
  } catch (error) {
    uni.showToast({ title: error.message || '页面初始化失败', icon: 'none' })
  }
})
</script>

<style scoped lang="scss">
/* Design System Tokens */
$primary: #00bfff;
$deep: #0099cc;
$light: #5ce1ff;
$success: #10b981;
$danger: #ff4d6d;

$bg: #f8f8f8;
$card: #ffffff;
$text-primary: #1a2030;
$text-secondary: #5a6a7a;
$text-muted: #8a9aaa;

$gradient: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
$shadow-card: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
$radius-card: 24rpx;
$radius-capsule: 999rpx;

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(24rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes imageFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.edit-container {
  min-height: 100vh;
  background: $bg;
  padding-bottom: 200rpx;
}

/* Hero Header */
.page-hero {
  position: relative;
  padding: 40rpx 36rpx;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px);
}

.hero-content {
  position: relative;
  z-index: 1;
}

.hero-title {
  font-size: 36rpx;
  font-weight: 700;
  color: $text-primary;
  display: block;
  margin-bottom: 8rpx;
}

.hero-subtitle {
  font-size: 28rpx;
  color: $text-secondary;
  font-weight: 400;
}

/* Form Card */
.form-card {
  margin: 28rpx;
  background: $card;
  border-radius: $radius-card;
  padding: 32rpx;
  box-shadow: $shadow-card;
  animation: fadeInUp 0.4s ease;
}

.form-section {
  margin-bottom: 36rpx;

  &:last-child {
    margin-bottom: 0;
  }
}

.section-label {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.label-icon {
  width: 40rpx;
  height: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 191, 255, 0.08);
  border-radius: 12rpx;
  margin-right: 12rpx;
}

.label-text {
  font-size: 28rpx;
  font-weight: 600;
  color: $text-primary;
  flex: 1;
}

.char-count {
  font-size: 24rpx;
  color: $text-muted;
}

.media-count-badge {
  font-size: 20rpx;
  color: $deep;
  background: rgba(0, 191, 255, 0.08);
  padding: 4rpx 16rpx;
  border-radius: $radius-capsule;
  font-weight: 500;
}

/* Picker */
.picker-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: $bg;
  border-radius: $radius-card;
  padding: 24rpx;
  box-shadow: $shadow-card;
  transition: all 0.2s ease;

  &:active {
    transform: scale(0.99);
  }
}

.picker-value {
  font-size: 28rpx;
  color: $text-primary;
}

/* Inputs */
.title-input {
  width: 100%;
  min-height: 88rpx;
  background: $bg;
  border-radius: $radius-card;
  padding: 24rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
  box-sizing: border-box;
  box-shadow: $shadow-card;
}

.content-textarea {
  width: 100%;
  min-height: 280rpx;
  background: $bg;
  border-radius: $radius-card;
  padding: 24rpx;
  font-size: 28rpx;
  color: $text-primary;
  line-height: 1.7;
  box-sizing: border-box;
  box-shadow: $shadow-card;
}

.input-placeholder {
  color: $text-muted;
}

/* Media Actions */
.media-actions {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.media-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  padding: 28rpx;
  background: $bg;
  border-radius: $radius-card;
  box-shadow: $shadow-card;
  transition: all 0.25s ease;

  .btn-icon {
    width: 56rpx;
    height: 56rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;

    &.image-icon {
      background: rgba(0, 191, 255, 0.08);
    }

    &.video-icon {
      background: rgba(0, 191, 255, 0.08);
    }
  }

  text {
    font-size: 26rpx;
    color: $text-secondary;
    font-weight: 500;
  }

  &:active {
    transform: scale(0.96);
  }
}

/* Media Grid */
.media-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
}

.media-item {
  position: relative;
  border-radius: $radius-card;
  overflow: hidden;
  animation: fadeInUp 0.3s ease forwards;
  opacity: 0;
}

.media-item image,
.media-item video {
  width: 100%;
  height: 200rpx;
  background: $bg;
  animation: imageFadeIn 0.5s ease;
}

.media-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 12rpx;
  background: linear-gradient(180deg, rgba(0,0,0,0.3) 0%, transparent 30%, transparent 70%, rgba(0,0,0,0.4) 100%);
}

.media-type-tag {
  font-size: 20rpx;
  color: $card;
  background: rgba(0, 0, 0, 0.45);
  padding: 4rpx 14rpx;
  border-radius: $radius-capsule;
  align-self: flex-start;
}

.delete-btn {
  position: absolute;
  top: 8rpx;
  right: 8rpx;
  width: 44rpx;
  height: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(220, 38, 38, 0.8);
  border-radius: 50%;

  &:active {
    transform: scale(0.96);
  }
}

/* Empty Media */
.media-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 24rpx;
  background: $bg;
  border-radius: $radius-card;
  box-shadow: $shadow-card;

  .empty-visual {
    margin-bottom: 16rpx;
  }

  .empty-icon-wrap {
    width: 80rpx;
    height: 80rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.03);
    border-radius: 50%;
  }

  .empty-hint {
    font-size: 28rpx;
    color: $text-muted;
    font-weight: 400;
  }
}

/* Bottom Bar */
.bottom-bar {
  position: fixed;
  left: 28rpx;
  right: 28rpx;
  bottom: calc(24rpx + env(safe-area-inset-bottom));
  display: flex;
  gap: 16rpx;
  padding: 16rpx;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px);
  border-radius: $radius-card;
  box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.08);
  z-index: 100;
}

.btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  height: 88rpx;
  border-radius: $radius-capsule;
  font-size: 30rpx;
  font-weight: 600;
  transition: all 0.25s ease;
  margin: 0;
  padding: 0;

  &:active {
    transform: scale(0.96);
  }
}

.draft-btn {
  background: $bg;
  color: $text-secondary;
  box-shadow: $shadow-card;
}

.publish-btn {
  background: $gradient;
  color: $card;
  box-shadow: 0 4rpx 20rpx rgba(0, 191, 255, 0.35);
}

/* Delete Section */
.delete-section {
  margin: 28rpx;
  padding-top: 24rpx;
}

.delete-btn-full {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  height: 88rpx;
  background: rgba(255, 77, 109, 0.06);
  border-radius: $radius-capsule;
  margin: 0;
  padding: 0;

  text {
    font-size: 28rpx;
    color: $danger;
    font-weight: 600;
  }

  &:active {
    transform: scale(0.96);
    background: rgba(255, 77, 109, 0.12);
  }
}
</style>
