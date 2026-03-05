<template>
  <view class="edit-container">
    <view class="page-hero card">
      <view class="hero-title">{{ isEdit ? '编辑帖子' : '发布帖子' }}</view>
      <view class="hero-subtitle">分享你的 3D 打印经验、模型技巧和问题讨论</view>
    </view>

    <view class="form-card card">
      <view class="label-row">
        <view class="label">分类</view>
      </view>
      <picker :range="categoryNames" @change="onCategoryChange" :value="categoryIndex">
        <view class="picker-value">{{ selectedCategoryName || '请选择分类' }}</view>
      </picker>

      <view class="label-row">
        <view class="label">标题</view>
        <view class="counter">{{ titleCount }}/200</view>
      </view>
      <input v-model="form.title" maxlength="200" placeholder="请输入帖子标题" class="input" />

      <view class="label-row">
        <view class="label">内容</view>
        <view class="counter">{{ contentCount }}/5000</view>
      </view>
      <textarea v-model="form.content" maxlength="5000" placeholder="请输入帖子内容" class="textarea" />

      <view class="label-row">
        <view class="label">媒体（图片/视频）</view>
        <view class="counter">{{ form.mediaList.length }} 个</view>
      </view>
      <view class="media-actions">
        <button class="small-btn" @click="chooseImage">+ 上传图片</button>
        <button class="small-btn" @click="chooseVideo">+ 上传视频</button>
      </view>
      <view class="media-grid" v-if="form.mediaList.length">
        <view class="media-item" v-for="(item, index) in form.mediaList" :key="index">
          <image v-if="item.mediaType === 1" :src="item.mediaUrl" mode="aspectFill"></image>
          <video
            v-else
            :src="item.mediaUrl"
            controls
            :page-gesture="true"
            :vslide-gesture="false"
            object-fit="cover"
          ></video>
          <view class="delete-media" @click="removeMedia(index)">×</view>
          <view class="media-tag">{{ item.mediaType === 1 ? '图片' : '视频' }}</view>
        </view>
      </view>
      <view class="media-empty" v-else>
        <uni-icons type="image" size="26" color="#94a3b8"></uni-icons>
        <text>可上传图片或视频，让内容更直观</text>
      </view>
    </view>

    <view class="bottom-actions">
      <button class="draft" @click="submit(0)">保存草稿</button>
      <button class="publish" @click="submit(1)">发布</button>
    </view>

    <view class="delete-bar" v-if="isEdit">
      <button class="delete-btn" @click="removePost">删除帖子</button>
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
    if (isEdit.value) {
      await updatePostApi({ id: postId.value, ...payload })
      uni.showToast({ title: '更新成功', icon: 'success' })
    } else {
      await createPostApi(payload)
      uni.showToast({ title: status === 1 ? '发布成功' : '草稿已保存', icon: 'success' })
    }
    setTimeout(() => {
      uni.navigateBack()
    }, 500)
  } catch (error) {
    uni.showToast({ title: error.message || '提交失败', icon: 'none' })
  }
}

const removePost = async () => {
  const confirm = await new Promise((resolve) => {
    uni.showModal({
      title: '提示',
      content: '确认删除该帖子吗？',
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
.edit-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #eef2ff 0%, #f8fafc 200rpx);
  padding: 20rpx;
  padding-bottom: 220rpx;
}
.page-hero {
  padding: 24rpx;
  margin-bottom: 16rpx;
  background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
  .hero-title {
    font-size: 34rpx;
    font-weight: 700;
    color: #fff;
  }
  .hero-subtitle {
    margin-top: 8rpx;
    font-size: 24rpx;
    color: rgba(255, 255, 255, 0.88);
  }
}
.form-card {
  padding: 26rpx;
}
.label-row {
  margin-top: 18rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.label {
  font-size: 26rpx;
  color: #1e293b;
  font-weight: 700;
}
.counter {
  font-size: 22rpx;
  color: #94a3b8;
}
.picker-value, .input {
  margin-top: 10rpx;
  background: #f8fafc;
  border: 1rpx solid #e2e8f0;
  border-radius: 12rpx;
  min-height: 78rpx;
  line-height: 78rpx;
  padding: 0 20rpx;
  font-size: 26rpx;
}
.textarea {
  margin-top: 10rpx;
  width: 100%;
  min-height: 240rpx;
  background: #f8fafc;
  border: 1rpx solid #e2e8f0;
  border-radius: 12rpx;
  padding: 20rpx;
  box-sizing: border-box;
  font-size: 26rpx;
}
.media-actions { margin-top: 12rpx; display: flex; gap: 12rpx; }
.small-btn {
  flex: 1;
  background: #eef2ff;
  color: #4f46e5;
  border-radius: 30rpx;
  font-size: 24rpx;
  padding: 0 24rpx;
  height: 64rpx;
  line-height: 64rpx;
  border: 1rpx solid #c7d2fe;
}
.media-grid { margin-top: 14rpx; display: grid; grid-template-columns: repeat(2, 1fr); gap: 12rpx; }
.media-item { position: relative; }
.media-item image, .media-item video { width: 100%; height: 220rpx; border-radius: 10rpx; }
.delete-media {
  position: absolute;
  right: 8rpx;
  top: 8rpx;
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  color: #fff;
  background: rgba(15, 23, 42, 0.6);
  text-align: center;
  line-height: 36rpx;
}
.media-tag {
  position: absolute;
  left: 8rpx;
  bottom: 8rpx;
  padding: 2rpx 10rpx;
  border-radius: 20rpx;
  color: #fff;
  font-size: 20rpx;
  background: rgba(15, 23, 42, 0.6);
}
.media-empty {
  margin-top: 14rpx;
  min-height: 140rpx;
  border: 1rpx dashed #cbd5e1;
  border-radius: 12rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  text {
    font-size: 22rpx;
    color: #94a3b8;
  }
}
.bottom-actions {
  position: fixed;
  left: 20rpx;
  right: 20rpx;
  bottom: calc(24rpx + env(safe-area-inset-bottom));
  display: flex;
  gap: 16rpx;
  background: rgba(248, 250, 252, 0.9);
  backdrop-filter: blur(8rpx);
  padding: 12rpx;
  border-radius: 44rpx;
}
.bottom-actions button {
  flex: 1;
  border-radius: 36rpx;
  font-size: 28rpx;
  height: 74rpx;
  line-height: 74rpx;
}
.draft {
  background: #e2e8f0;
  color: #334155;
}
.publish {
  background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
  color: #fff;
}
.delete-bar {
  margin-top: 24rpx;
  padding: 0 8rpx;
}
.delete-btn {
  background: #fee2e2;
  color: #dc2626;
  border-radius: 40rpx;
  font-size: 26rpx;
}
</style>
