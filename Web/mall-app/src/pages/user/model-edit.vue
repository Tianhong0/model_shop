<template>
	<view class="edit-container">
		<view class="edit-form" v-if="!loading">
			<!-- 模型名称 -->
			<view class="form-item">
				<text class="label">模型名称 <text class="required">*</text></text>
				<input class="input" v-model="form.modelName" placeholder="请输入模型名称" />
			</view>

			<!-- 所属分类 -->
			<view class="form-item">
				<text class="label">所属分类 <text class="required">*</text></text>
				<picker :value="categoryIndex" :range="categoryNames" @change="onCategoryChange">
					<view class="picker-box">
						<text :class="{ placeholder: !form.categoryName }">{{ form.categoryName || '请选择分类' }}</text>
						<uni-icons type="right" size="16" color="#94a3b8"></uni-icons>
					</view>
				</picker>
			</view>

			<!-- 描述 -->
			<view class="form-item">
				<text class="label">描述</text>
				<textarea class="textarea" v-model="form.description" placeholder="请输入模型描述（选填）" />
			</view>

			<!-- 基础价格 -->
			<view class="form-item">
				<text class="label">基础价格(元) <text class="required">*</text></text>
				<input class="input" v-model="form.basePrice" type="digit" placeholder="如: 29.99" />
			</view>

			<!-- 原始体积 -->
			<view class="form-item">
				<text class="label">原始体积(mm³) <text class="required">*</text></text>
				<input class="input" v-model="form.baseVolume" type="digit" placeholder="如: 50000" />
			</view>

			<!-- 三维尺寸 -->
			<view class="form-item">
				<text class="label">三维尺寸(L*W*H) <text class="required">*</text></text>
				<input class="input" v-model="form.baseSize" placeholder="如: 100x80x60" />
			</view>

			<!-- 模型文件 -->
			<view class="form-item">
				<text class="label">模型文件 <text class="required">*</text></text>
				<view class="file-upload">
					<view v-if="form.filePath" class="file-info">
						<text class="file-name">{{ getFileName(form.filePath) }}</text>
						<text class="file-remove" @click="form.filePath = ''">替换</text>
					</view>
					<button v-else class="upload-btn" @click="pickAndUploadFile" :loading="uploadingFile">
						{{ uploadingFile ? '上传中...' : '选择模型文件 (.stl/.obj/.3mf)' }}
					</button>
				</view>
			</view>

			<!-- 主图管理 -->
			<view class="form-item">
				<text class="label">模型图片</text>
				<view class="image-upload">
					<view v-if="form.images.length > 0" class="image-grid">
						<view v-for="(img, idx) in form.images" :key="img.id || idx" class="img-item" @click="setMainImage(idx)">
							<image :src="img.imageUrl" mode="aspectFill" class="img-preview"></image>
							<view v-if="img.isMain === 1" class="main-tag">主图</view>
							<view class="img-remove" @click.stop="removeImage(idx)">
								<uni-icons type="closeempty" size="16" color="#fff"></uni-icons>
							</view>
						</view>
					</view>
					<button class="upload-btn" @click="uploadImages" :loading="uploadingImg">上传图片</button>
					<text class="upload-hint">点击图片可设为主图，第一张自动设为主图</text>
				</view>
			</view>

			<!-- 保存按钮 -->
			<button class="submit-btn" @click="submitEdit" :loading="submitting" :disabled="submitting">
				{{ submitting ? '保存中...' : '保存修改' }}
			</button>

			<!-- 下架按钮 -->
			<button v-if="modelStatus === 1" class="delist-btn" @click="confirmDelist">下架模型</button>
			<button v-if="modelStatus === 2" class="relist-btn" @click="confirmRelist">重新上架</button>
		</view>

		<view class="loading-state" v-else>
			<text>加载中...</text>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
	getModelDetailApi,
	getCategoryTreeApi,
	updateModelApi,
	addModelImageApi,
	setModelMainImageApi,
	deleteModelImageApi
} from '../../api/model'

const modelId = ref(null)
const modelStatus = ref(0)
const loading = ref(true)
const submitting = ref(false)
const uploadingFile = ref(false)
const uploadingImg = ref(false)

const categoryOptions = ref([])
const categoryNames = ref([])
const categoryIndex = ref(-1)
const deletedImageIds = ref([])

const form = ref({
	modelName: '',
	description: '',
	categoryId: null,
	categoryName: '',
	basePrice: '',
	baseVolume: '',
	baseSize: '',
	licenseType: '',
	filePath: '',
	images: []
})

onLoad((options) => {
	const id = options?.id
	if (id) {
		modelId.value = id
		initPage()
	} else {
		loading.value = false
	}
})

const initPage = async () => {
	try {
		await Promise.all([fetchCategories(), fetchModelDetail()])
	} catch (e) {
		uni.showToast({ title: '加载失败', icon: 'none' })
	} finally {
		loading.value = false
	}
}

const fetchCategories = async () => {
	try {
		const tree = await getCategoryTreeApi(true)
		if (Array.isArray(tree)) {
			const flat = []
			const walk = (items) => {
				for (const item of items) {
					flat.push({ id: item.id, categoryName: item.categoryName })
					if (item.children && item.children.length > 0) walk(item.children)
				}
			}
			walk(tree)
			categoryOptions.value = flat
			categoryNames.value = flat.map(c => c.categoryName)
		}
	} catch (e) {
		console.error('获取分类失败:', e)
	}
}

const fetchModelDetail = async () => {
	const detail = await getModelDetailApi(modelId.value, true)
	if (!detail) return

	modelStatus.value = detail.status || 0
	form.value.modelName = detail.modelName || ''
	form.value.description = detail.description || ''
	form.value.categoryId = detail.categoryId
	form.value.categoryName = detail.categoryName || ''
	form.value.basePrice = detail.basePrice != null ? String(detail.basePrice) : ''
	form.value.baseVolume = detail.baseVolume != null ? String(detail.baseVolume) : ''
	form.value.baseSize = detail.baseSize || ''
	form.value.licenseType = detail.licenseType || ''
	form.value.filePath = detail.filePath || ''
	form.value.images = (detail.images || []).map(img => ({
		id: img.id,
		imageUrl: img.imageUrl,
		isMain: img.isMain || 0,
		imgType: img.imgType || 1,
		sortOrder: img.sortOrder || 0
	}))

	// Set category picker index
	const idx = categoryOptions.value.findIndex(c => c.id === detail.categoryId)
	if (idx >= 0) categoryIndex.value = idx
}

const getFileName = (path) => {
	if (!path) return ''
	const parts = String(path).split('/')
	return parts[parts.length - 1] || path
}

const onCategoryChange = (e) => {
	const idx = e.detail.value
	categoryIndex.value = idx
	form.value.categoryId = categoryOptions.value[idx]?.id || null
	form.value.categoryName = categoryNames.value[idx] || ''
}

// ---- Image upload ----
const resolveUploadUrl = (body) => {
	if (!body || typeof body !== 'object') return ''
	if (typeof body.data === 'string' && body.data.trim()) return body.data.trim()
	if (typeof body.message === 'string' && /^https?:\/\//.test(body.message.trim()))
		return body.message.trim()
	return ''
}

const uploadFileToServer = (filePath, type) => {
	return new Promise((resolve, reject) => {
		const token = uni.getStorageSync('token')
		const baseUrl = uni.getStorageSync('api_base_url') || 'http://120.48.50.30:9999'
		uni.uploadFile({
			url: `${baseUrl}/api/file/upload`,
			filePath,
			name: 'file',
			formData: { type },
			header: token ? { Authorization: `Bearer ${token}` } : {},
			success: (res) => {
				try {
					const body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
					const uploadedUrl = resolveUploadUrl(body)
					if (body?.code === 200 && uploadedUrl) {
						resolve(uploadedUrl)
					} else {
						reject(new Error(body?.message || '上传失败'))
					}
				} catch (e) {
					reject(new Error('响应解析失败'))
				}
			},
			fail: (err) => {
				reject(new Error(err?.errMsg || '网络异常'))
			}
		})
	})
}

const uploadImages = () => {
	uni.chooseImage({
		count: 9,
		sizeType: ['compressed'],
		sourceType: ['album', 'camera'],
		success: async (res) => {
			uploadingImg.value = true
			let uploaded = 0
			for (let i = 0; i < res.tempFilePaths.length; i++) {
				try {
					const url = await uploadFileToServer(res.tempFilePaths[i], 'modelImg')
					if (!url) continue
					const isMain = form.value.images.length === 0 ? 1 : 0
					form.value.images.push({
						id: null, // new image, no server ID yet
						imageUrl: url,
						isMain,
						imgType: 1,
						sortOrder: form.value.images.length
					})
					uploaded++
				} catch (e) {
					console.error('图片上传失败:', e)
				}
			}
			uploadingImg.value = false
			if (uploaded > 0) {
				uni.showToast({ title: `成功上传${uploaded}张图片`, icon: 'success' })
			} else {
				uni.showToast({ title: '图片上传失败', icon: 'error' })
			}
		}
	})
}

const setMainImage = async (idx) => {
	const img = form.value.images[idx]
	if (!img || img.isMain === 1) return

	// If image has a server ID, call the API
	if (img.id) {
		try {
			await setModelMainImageApi({ modelId: modelId.value, imageId: img.id })
		} catch (e) {
			console.error('设置主图失败:', e)
		}
	}

	// Update local state
	form.value.images.forEach((item, i) => {
		item.isMain = i === idx ? 1 : 0
	})
}

const removeImage = (idx) => {
	const img = form.value.images[idx]
	if (img.id) {
		deletedImageIds.value.push(img.id)
	}
	form.value.images.splice(idx, 1)
	// If removed image was main, set first remaining as main
	if (img.isMain === 1 && form.value.images.length > 0) {
		form.value.images[0].isMain = 1
		if (form.value.images[0].id) {
			setModelMainImageApi({ modelId: modelId.value, imageId: form.value.images[0].id }).catch(() => {})
		}
	}
}

// ---- Model file upload ----
const normalizePickedFile = (pickedRes) => {
	const tempFiles = pickedRes?.tempFiles || []
	const first = tempFiles[0] || {}
	const path = first.path || first.tempFilePath || (pickedRes?.tempFilePaths || [])[0] || ''
	return { path }
}

const pickAndUploadFile = () => {
	if (typeof uni.chooseMessageFile === 'function') {
		uni.chooseMessageFile({
			count: 1,
			type: 'file',
			success: (res) => {
				const { path } = normalizePickedFile(res)
				if (path) doModelFileUpload(path)
			},
			fail: () => {}
		})
		return
	}
	if (typeof uni.chooseFile === 'function') {
		uni.chooseFile({
			count: 1,
			success: (res) => {
				const { path } = normalizePickedFile(res)
				if (path) doModelFileUpload(path)
			},
			fail: () => {}
		})
		return
	}
	// #ifdef APP-PLUS
	try {
		const main = plus.android.runtimeMainActivity()
		const Intent = plus.android.importClass('android.content.Intent')
		const intent = new Intent(Intent.ACTION_GET_CONTENT)
		intent.setType('*/*')
		intent.addCategory(Intent.CATEGORY_OPENABLE)
		main.startActivityForResult(intent, 10002)
		const orig = main.onActivityResult
		main.onActivityResult = (requestCode, resultCode, data) => {
			if (requestCode === 10002 && data && data.getData()) {
				const uri = data.getData()
				const path = uri.toString().startsWith('file://') ? uri.toString().replace('file://', '') : uri.toString()
				if (path) doModelFileUpload(path)
			}
			if (orig) main.onActivityResult = orig
		}
		return
	} catch (_) {}
	// #endif
	uni.showToast({ title: '当前环境不支持文件选择', icon: 'none' })
}

const doModelFileUpload = async (filePath) => {
	uploadingFile.value = true
	try {
		const url = await uploadFileToServer(filePath, 'modelFile')
		form.value.filePath = url
		uni.showToast({ title: '文件上传成功', icon: 'success' })
	} catch (e) {
		uni.showToast({ title: (e && e.message) || '上传失败', icon: 'error' })
	} finally {
		uploadingFile.value = false
	}
}

// ---- Submit edit ----
const submitEdit = async () => {
	if (!form.value.modelName) {
		uni.showToast({ title: '请输入模型名称', icon: 'none' })
		return
	}
	if (!form.value.categoryId) {
		uni.showToast({ title: '请选择分类', icon: 'none' })
		return
	}
	if (!form.value.basePrice || parseFloat(form.value.basePrice) <= 0) {
		uni.showToast({ title: '请输入有效的价格', icon: 'none' })
		return
	}
	if (!form.value.baseVolume) {
		uni.showToast({ title: '请输入原始体积', icon: 'none' })
		return
	}
	if (!form.value.baseSize) {
		uni.showToast({ title: '请输入三维尺寸', icon: 'none' })
		return
	}

	submitting.value = true
	try {
		// 1. Update model basic info
		const mainImg = form.value.images.find(img => img.isMain === 1)
		await updateModelApi({
			id: modelId.value,
			modelName: form.value.modelName,
			description: form.value.description || '',
			categoryId: form.value.categoryId,
			basePrice: parseFloat(form.value.basePrice),
			baseVolume: parseFloat(form.value.baseVolume),
			baseSize: form.value.baseSize,
			filePath: form.value.filePath || undefined,
			mainImageUrl: mainImg?.imageUrl || undefined,
			licenseType: form.value.licenseType || undefined
		})

		// 2. Add new images (those without server ID)
		const newImages = form.value.images.filter(img => !img.id)
		for (const img of newImages) {
			try {
				await addModelImageApi({
					modelId: modelId.value,
					imageUrl: img.imageUrl,
					isMain: img.isMain || 0,
					imgType: img.imgType || 1,
					sortOrder: img.sortOrder || 0
				})
			} catch (e) {
				console.error('添加图片失败:', e)
			}
		}

		// 3. Delete removed images
		for (const imageId of deletedImageIds.value) {
			try {
				await deleteModelImageApi(imageId)
			} catch (e) {
				console.error('删除图片失败:', e)
			}
		}

		uni.showToast({ title: '保存成功', icon: 'success' })
		setTimeout(() => {
			uni.navigateBack()
		}, 800)
	} catch (e) {
		uni.showToast({ title: e.message || '保存失败', icon: 'error' })
	} finally {
		submitting.value = false
	}
}

// ---- Delist / Relist ----
const confirmDelist = () => {
	uni.showModal({
		title: '确认下架',
		content: '确定要将该模型下架吗？下架后用户将无法搜索和购买。',
		confirmText: '确认下架',
		confirmColor: '#ff4d6d',
		success: async (res) => {
			if (res.confirm) {
				try {
					await updateModelApi({ id: modelId.value, status: 2 })
					modelStatus.value = 2
					uni.showToast({ title: '已下架', icon: 'success' })
				} catch (e) {
					uni.showToast({ title: e.message || '操作失败', icon: 'none' })
				}
			}
		}
	})
}

const confirmRelist = () => {
	uni.showModal({
		title: '确认上架',
		content: '确定要将该模型重新上架吗？',
		confirmText: '确认上架',
		confirmColor: '#00bfff',
		success: async (res) => {
			if (res.confirm) {
				try {
					await updateModelApi({ id: modelId.value, status: 1 })
					modelStatus.value = 1
					uni.showToast({ title: '已上架', icon: 'success' })
				} catch (e) {
					uni.showToast({ title: e.message || '操作失败', icon: 'none' })
				}
			}
		}
	})
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

.edit-container {
	min-height: 100vh;
	background: $bg;
	padding-bottom: env(safe-area-inset-bottom);
}

.loading-state {
	display: flex;
	justify-content: center;
	padding-top: 300rpx;
	font-size: 28rpx;
	color: $text3;
}

.edit-form {
	padding: 28rpx;
}

.form-item {
	margin-bottom: 28rpx;
	.label {
		font-size: 26rpx;
		color: $text1;
		font-weight: 600;
		margin-bottom: 10rpx;
		display: block;
	}
	.required { color: $danger; }
	.input {
		height: 80rpx;
		padding: 0 24rpx;
		border: 1rpx solid #e2e8f0;
		border-radius: 12rpx;
		font-size: 28rpx;
		background: $card;
	}
	.textarea {
		width: 100%;
		height: 160rpx;
		padding: 16rpx 24rpx;
		border: 1rpx solid #e2e8f0;
		border-radius: 12rpx;
		font-size: 28rpx;
		background: $card;
		box-sizing: border-box;
	}
	.picker-box {
		display: flex;
		justify-content: space-between;
		align-items: center;
		height: 80rpx;
		padding: 0 24rpx;
		border: 1rpx solid #e2e8f0;
		border-radius: 12rpx;
		background: $card;
		font-size: 28rpx;
		.placeholder { color: $text3; }
	}
}

.file-upload, .image-upload {
	.upload-btn {
		height: 80rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border: 2rpx dashed #cbd5e1;
		border-radius: 12rpx;
		background: $card;
		font-size: 26rpx;
		color: $text2;
	}
	.file-info {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 16rpx 24rpx;
		background: #f1f5f9;
		border-radius: 12rpx;
		.file-name {
			font-size: 24rpx;
			color: $text1;
			max-width: 400rpx;
			overflow: hidden;
			text-overflow: ellipsis;
			white-space: nowrap;
		}
		.file-remove {
			font-size: 24rpx;
			color: $danger;
		}
	}
	.upload-hint { font-size: 20rpx; color: $text3; margin-top: 8rpx; display: block; }
}

.image-grid {
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx;
	margin-bottom: 16rpx;
	.img-item {
		position: relative;
		width: 160rpx;
		height: 160rpx;
		border-radius: 12rpx;
		overflow: hidden;
		.img-preview { width: 100%; height: 100%; }
		.main-tag {
			position: absolute;
			top: 0; left: 0;
			background: $primary;
			color: #fff;
			font-size: 18rpx;
			padding: 4rpx 12rpx;
			border-radius: 0 0 8rpx 0;
		}
		.img-remove {
			position: absolute;
			top: 4rpx; right: 4rpx;
			width: 36rpx; height: 36rpx;
			background: rgba(0,0,0,0.5);
			border-radius: 50%;
			display: flex;
			align-items: center;
			justify-content: center;
		}
	}
}

.submit-btn {
	margin-top: 40rpx;
	background: $gradient;
	color: #ffffff;
	border-radius: 999rpx;
	font-size: 30rpx;
	font-weight: 600;
	height: 88rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 8rpx 30rpx rgba(0, 191, 255, 0.25);
	&:active { transform: scale(0.96); }
}

.delist-btn {
	margin-top: 24rpx;
	height: 80rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background: #fee2e2;
	color: $danger;
	border-radius: 999rpx;
	font-size: 28rpx;
	font-weight: 600;
	border: 2rpx solid $danger;
	&:active { opacity: 0.7; }
}

.relist-btn {
	margin-top: 24rpx;
	height: 80rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background: #dcfce7;
	color: #16a34a;
	border-radius: 999rpx;
	font-size: 28rpx;
	font-weight: 600;
	border: 2rpx solid #16a34a;
	&:active { opacity: 0.7; }
}
</style>
