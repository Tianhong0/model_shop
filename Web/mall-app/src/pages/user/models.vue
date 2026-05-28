<template>
	<view class="upload-container" v-if="canAccess">
		<view class="tab-bar">
			<view class="tab-item" :class="{ active: currentTab === 0 }" @click="switchTab(0)">我的模型</view>
			<view class="tab-item" :class="{ active: currentTab === 1 }" @click="switchTab(1)">上传模型</view>
		</view>

		<view v-if="currentTab === 0" class="tab-content">
			<view class="model-list" v-if="myModels.length > 0">
				<view class="model-card" v-for="model in myModels" :key="model.id" @click="goDetail(model.id)">
					<image :src="model.mainImageUrl || '/static/icons/tools.png'" class="preview" mode="aspectFill"></image>
					<view class="info">
						<view class="name-row">
							<text class="name">{{ model.modelName }}</text>
							<text class="status" :class="getStatusClass(model.status)">{{ getStatusText(model.status) }}</text>
						</view>
						<text class="meta">分类: {{ model.categoryName || '-' }}</text>
						<text class="meta">价格: ￥{{ model.basePrice || '0.00' }}</text>
						<view class="stats">
							<text>下载: {{ model.downloadCount || 0 }}</text>
							<text class="source-badge" :class="model.sourceType === 1 ? 'official' : 'designer'">
								{{ model.sourceTypeDesc || '官方' }}
							</text>
						</view>
					</view>
				</view>
			</view>

			<view class="empty-state" v-else>
				<uni-icons type="cloud-upload" size="64" color="#cbd5e1"></uni-icons>
				<text>还没有上传过模型</text>
				<text class="empty-hint">切换到「上传模型」标签开始上传</text>
			</view>

			<view class="load-more" v-if="myModels.length > 0">
				<text v-if="loadingMore" class="loading-text">加载中...</text>
				<text v-else-if="!hasMore" class="no-more">没有更多了</text>
			</view>
		</view>

		<view v-if="currentTab === 1" class="tab-content">
			<view class="upload-form">
				<view class="form-item">
					<text class="label">模型名称 <text class="required">*</text></text>
					<input class="input" v-model="form.modelName" placeholder="请输入模型名称" />
				</view>

				<view class="form-item">
					<text class="label">所属分类 <text class="required">*</text></text>
					<picker :value="form.categoryIndex" :range="categoryNames" @change="onCategoryChange">
						<view class="picker-box">
							<text :class="{ placeholder: !form.categoryName }">{{ form.categoryName || '请选择分类' }}</text>
							<uni-icons type="right" size="16" color="#94a3b8"></uni-icons>
						</view>
					</picker>
				</view>

				<view class="form-item">
					<text class="label">描述</text>
					<textarea class="textarea" v-model="form.description" placeholder="请输入模型描述（选填）" />
				</view>

				<view class="form-item">
					<text class="label">基础价格(元) <text class="required">*</text></text>
					<input class="input" v-model="form.basePrice" type="digit" placeholder="如: 29.99" />
				</view>

				<view class="form-item">
					<text class="label">原始体积(mm³) <text class="required">*</text></text>
					<input class="input" v-model="form.baseVolume" type="digit" placeholder="如: 50000" />
				</view>

				<view class="form-item">
					<text class="label">三维尺寸(L*W*H) <text class="required">*</text></text>
					<input class="input" v-model="form.baseSize" placeholder="如: 100x80x60" />
				</view>

				<view class="form-item">
					<text class="label">授权说明</text>
					<picker :value="licenseIndex" :range="licenseOptions" @change="onLicenseChange">
						<view class="picker-box">
							<text>{{ licenseOptions[licenseIndex] || '请选择' }}</text>
							<uni-icons type="right" size="16" color="#94a3b8"></uni-icons>
						</view>
					</picker>
				</view>

				<view class="form-item">
					<text class="label">模型文件 <text class="required">*</text></text>
					<view class="file-upload">
						<view v-if="form.filePath" class="file-info">
							<text class="file-name">{{ getFileName(form.filePath) }}</text>
							<text class="file-remove" @click="form.filePath = ''">删除</text>
						</view>
						<button v-else class="upload-btn" @click="pickAndUploadFile" :loading="uploadingFile">
							{{ uploadingFile ? '上传中...' : '选择模型文件 (.stl/.obj/.3mf)' }}
						</button>
					</view>
				</view>

				<view class="form-item">
					<text class="label">主图上传 <text class="required">*</text></text>
					<view class="image-upload">
						<view v-if="form.images.length > 0" class="image-grid">
							<view v-for="(img, idx) in form.images" :key="idx" class="img-item">
								<image :src="img.imageUrl" mode="aspectFill" class="img-preview"></image>
								<view v-if="img.isMain === 1" class="main-tag">主图</view>
								<view class="img-remove" @click="removeImage(idx)">
									<uni-icons type="closeempty" size="16" color="#fff"></uni-icons>
								</view>
							</view>
						</view>
						<button class="upload-btn" @click="uploadImages" :loading="uploadingImg">上传图片</button>
						<text class="upload-hint">第一张上传的图片自动设为主图</text>
					</view>
				</view>

				<button class="submit-btn" @click="submitModel" :loading="submitting" :disabled="submitting">
					{{ submitting ? '提交中...' : '提交审核' }}
				</button>
			</view>
		</view>
	</view>

	<view class="blocked" v-else>
		<text class="blocked-title">当前角色无权限查看模型上传记录</text>
		<text class="blocked-desc">该功能仅对设计者开放。</text>
		<button class="back-btn" @click="uni.navigateBack()">返回</button>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onReachBottom } from '@dcloudio/uni-app'
import { isDesignerRole } from '../../utils/role'
import { createModelApi, getMyModelsApi, getCategoryTreeApi } from '../../api/model'

const userRole = ref(uni.getStorageSync('user_role') || 'user')
const canAccess = computed(() => isDesignerRole(userRole.value))

const currentTab = ref(0)
const myModels = ref([])
const loadingMore = ref(false)
const hasMore = ref(true)
const pageNum = ref(1)
const pageSize = ref(10)

const uploadingFile = ref(false)
const uploadingImg = ref(false)
const submitting = ref(false)

const categoryOptions = ref([])
const categoryNames = computed(() => categoryOptions.value.map(c => c.categoryName))
const licenseOptions = ['个人使用', '商业使用', '扩展商业许可']
const licenseIndex = ref(0)

const form = ref({
	modelName: '',
	description: '',
	categoryId: null,
	categoryName: '',
	categoryIndex: -1,
	basePrice: '',
	baseVolume: '',
	baseSize: '',
	licenseType: '个人使用',
	filePath: '',
	images: []
})

const switchTab = (tab) => {
	currentTab.value = tab
	if (tab === 0 && myModels.value.length === 0) {
		fetchMyModels(true)
	}
}

const getStatusText = (status) => {
	const map = { 0: '审核中', 1: '已上架', 2: '已驳回' }
	return map[status] || '未知'
}

const getStatusClass = (status) => {
	const map = { 0: 'pending', 1: 'approved', 2: 'rejected' }
	return map[status] || ''
}

const getFileName = (path) => {
	if (!path) return ''
	const parts = String(path).split('/')
	return parts[parts.length - 1] || path
}

const onCategoryChange = (e) => {
	const idx = e.detail.value
	form.value.categoryIndex = idx
	form.value.categoryId = categoryOptions.value[idx]?.id || null
	form.value.categoryName = categoryNames.value[idx] || ''
}

const onLicenseChange = (e) => {
	licenseIndex.value = e.detail.value
	form.value.licenseType = licenseOptions[e.detail.value]
}

const flattenCategories = (cats) => {
	const result = []
	const walk = (items) => {
		for (const item of items) {
			result.push({ id: item.id, categoryName: item.categoryName })
			if (item.children && item.children.length > 0) walk(item.children)
		}
	}
	walk(cats)
	return result
}

const fetchCategories = async () => {
	try {
		const tree = await getCategoryTreeApi()
		if (Array.isArray(tree)) {
			categoryOptions.value = tree.length > 0 && tree[0]?.children ? flattenCategories(tree) : tree.map(c => ({ id: c.id, categoryName: c.categoryName }))
		}
	} catch (e) {
		console.error('获取分类失败:', e)
	}
}

const fetchMyModels = async (reset = false) => {
	if (reset) {
		pageNum.value = 1
		hasMore.value = true
	}
	if (!hasMore.value || loadingMore.value) return

	loadingMore.value = true
	try {
		const res = await getMyModelsApi({ pageNum: pageNum.value, pageSize: pageSize.value })
		const records = res?.records || []
		if (reset) {
			myModels.value = records
		} else {
			myModels.value = [...myModels.value, ...records]
		}
		hasMore.value = records.length >= pageSize.value
		pageNum.value++
	} catch (e) {
		console.error('获取我的模型失败:', e)
	} finally {
		loadingMore.value = false
	}
}

// ==================== 文件上传核心函数 ====================

// 从响应 body 中提取上传后的 URL（与 bounty/community 模块一致的逻辑）
const resolveUploadUrl = (body) => {
	if (!body || typeof body !== 'object') return ''
	if (typeof body.data === 'string' && body.data.trim()) return body.data.trim()
	if (typeof body.message === 'string' && /^https?:\/\//.test(body.message.trim()))
		return body.message.trim()
	return ''
}

// 通用的文件上传方法
const uploadFileToServer = (filePath, type) => {
	return new Promise((resolve, reject) => {
		const token = uni.getStorageSync('token')
		const baseUrl = uni.getStorageSync('api_base_url') || 'http://120.48.50.30:9999'

		const url = type === 'modelImg'
			? `${baseUrl}/api/file/upload`
			: `${baseUrl}/api/file/upload`

		uni.uploadFile({
			url,
			filePath,
			name: 'file',
			formData: { type },
			header: token ? { Authorization: `Bearer ${token}` } : {},
			success: (res) => {
				try {
					const raw = res.data
					const body = typeof raw === 'string' ? JSON.parse(raw) : raw
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

// ==================== 模型文件选择与上传（与 bounty 交付模块一致） ====================

// 从 uni 选择结果中提取文件路径和名称
const normalizePickedFile = (pickedRes) => {
	const tempFiles = pickedRes?.tempFiles || []
	const first = tempFiles[0] || {}
	const path = first.path || first.tempFilePath || (pickedRes?.tempFilePaths || [])[0] || ''
	const name = first.name || (path ? path.split('/').pop() : '')
	return { path, name }
}

const pickAndUploadFile = () => {
	// 1. 优先: uni.chooseMessageFile（微信小程序）
	if (typeof uni.chooseMessageFile === 'function') {
		uni.chooseMessageFile({
			count: 1,
			type: 'file',
			extension: ['stl', 'obj', '3mf'],
			success: (res) => {
				const { path } = normalizePickedFile(res)
				if (path) doModelFileUpload(path)
			},
			fail: (err) => {
				if (!String(err?.errMsg || '').includes('cancel')) {
					// 某些平台 extension 参数不支持，去掉重试
					uni.chooseMessageFile({
						count: 1,
						type: 'file',
						success: (res2) => {
							const { path } = normalizePickedFile(res2)
							if (path) doModelFileUpload(path)
						},
						fail: () => {}
					})
				}
			}
		})
		return
	}

	// 2. 其次: uni.chooseFile（H5/部分 APP）
	if (typeof uni.chooseFile === 'function') {
		uni.chooseFile({
			count: 1,
			extension: ['stl', 'obj', '3mf'],
			success: (res) => {
				const { path } = normalizePickedFile(res)
				if (path) doModelFileUpload(path)
			},
			fail: (err) => {
				if (String(err?.errMsg || '').includes('cancel')) return
				// 去掉 extension 重试
				uni.chooseFile({
					count: 1,
					success: (res2) => {
						const { path } = normalizePickedFile(res2)
						if (path) doModelFileUpload(path)
					},
					fail: () => {
						uni.showToast({ title: '选择文件失败', icon: 'none' })
					}
				})
			}
		})
		return
	}

	// 3. APP-PLUS: Android Intent 原生选择器
	// #ifdef APP-PLUS
	try {
		const main = plus.android.runtimeMainActivity()
		const Intent = plus.android.importClass('android.content.Intent')
		const intent = new Intent(Intent.ACTION_GET_CONTENT)
		intent.setType('*/*')
		intent.addCategory(Intent.CATEGORY_OPENABLE)
		main.startActivityForResult(intent, 10001)
		const origOnActivityResult = main.onActivityResult
		main.onActivityResult = (requestCode, resultCode, data) => {
			if (requestCode === 10001) {
				if (data && data.getData()) {
					const uri = data.getData()
					const uriStr = uri.toString()
					const path = uriStr.startsWith('file://') ? uriStr.replace('file://', '') : uriStr
					if (path) doModelFileUpload(path)
				}
			}
			if (origOnActivityResult) {
				main.onActivityResult = origOnActivityResult
			}
		}
		return
	} catch (_) {}
	// #endif

	uni.showToast({ title: '当前环境不支持模型文件选择', icon: 'none' })
}

const doModelFileUpload = async (filePath) => {
	uploadingFile.value = true
	try {
		const url = await uploadFileToServer(filePath, 'modelFile')
		form.value.filePath = url
		uni.showToast({ title: '模型文件上传成功', icon: 'success' })
	} catch (e) {
		console.error('模型文件上传失败:', e)
		uni.showToast({ title: (e && e.message) || '上传失败', icon: 'error' })
	} finally {
		uploadingFile.value = false
	}
}

// ==================== 图片上传 ====================

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
						id: 'temp_' + Date.now() + '_' + i,
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
				uni.showToast({ title: '图片上传失败，请重试', icon: 'error' })
			}
		},
		fail: (err) => {
			console.error('选择图片失败:', err)
		}
	})
}

const removeImage = (idx) => {
	const removed = form.value.images[idx]
	form.value.images.splice(idx, 1)
	if (removed.isMain === 1 && form.value.images.length > 0) {
		form.value.images[0].isMain = 1
	}
}

// ==================== 提交模型 ====================

const submitModel = async () => {
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
	if (!form.value.filePath) {
		uni.showToast({ title: '请上传模型文件', icon: 'none' })
		return
	}

	submitting.value = true
	try {
		const mainImg = form.value.images.find(img => img.isMain === 1)
		await createModelApi({
			modelName: form.value.modelName,
			description: form.value.description || '',
			categoryId: form.value.categoryId,
			basePrice: parseFloat(form.value.basePrice),
			baseVolume: parseFloat(form.value.baseVolume),
			baseSize: form.value.baseSize,
			filePath: form.value.filePath,
			mainImageUrl: mainImg?.imageUrl || '',
			licenseType: form.value.licenseType
		})
		uni.showToast({ title: '提交成功，等待审核', icon: 'success' })
		form.value = {
			modelName: '', description: '', categoryId: null, categoryName: '', categoryIndex: -1,
			basePrice: '', baseVolume: '', baseSize: '', licenseType: '个人使用', filePath: '', images: []
		}
		licenseIndex.value = 0
		switchTab(0)
		fetchMyModels(true)
	} catch (e) {
		console.error('提交模型失败:', e)
		uni.showToast({ title: '提交失败，请重试', icon: 'error' })
	} finally {
		submitting.value = false
	}
}

const goDetail = (id) => {
	uni.navigateTo({ url: `/pages/custom/detail?id=${id}` })
}

onReachBottom(() => {
	if (currentTab.value === 0) {
		fetchMyModels(false)
	}
})

onMounted(() => {
	fetchCategories()
	fetchMyModels(true)
})
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

@keyframes fadeInUp {
	from { opacity: 0; transform: translateY(24rpx); }
	to { opacity: 1; transform: translateY(0); }
}

.upload-container {
	min-height: 100vh;
	background: $bg;
	padding-bottom: env(safe-area-inset-bottom);
}

.tab-bar {
	display: flex;
	background: $card;
	padding: 0 28rpx;
	border-bottom: 1rpx solid #e2e8f0;
	.tab-item {
		flex: 1;
		text-align: center;
		padding: 24rpx 0;
		font-size: 28rpx;
		color: $text2;
		font-weight: 500;
		&.active {
			color: $primary;
			border-bottom: 4rpx solid $primary;
			font-weight: 600;
		}
	}
}

.tab-content {
	padding: 28rpx;
}

.model-card {
	display: flex;
	padding: 24rpx;
	margin-bottom: 20rpx;
	align-items: center;
	background: $card;
	border-radius: 24rpx;
	box-shadow: $shadow;
	animation: fadeInUp 0.4s ease both;
	.preview {
		width: 140rpx;
		height: 140rpx;
		border-radius: 16rpx;
		background-color: $bg;
	}
	.info {
		flex: 1;
		margin-left: 24rpx;
		.name-row {
			display: flex;
			align-items: center;
			justify-content: space-between;
			.name { font-size: 28rpx; font-weight: 700; color: $text1; }
			.status {
				font-size: 20rpx;
				padding: 4rpx 16rpx;
				border-radius: 999rpx;
				font-weight: 600;
				&.pending { background-color: #fef3c7; color: #d97706; }
				&.approved { background-color: #dcfce7; color: #16a34a; }
				&.rejected { background-color: #fee2e2; color: #dc2626; }
			}
		}
		.meta { font-size: 22rpx; color: $text2; margin-top: 4rpx; display: block; }
		.stats {
			display: flex;
			gap: 20rpx;
			margin-top: 10rpx;
			font-size: 22rpx;
			color: $text2;
			align-items: center;
			.source-badge {
				padding: 2rpx 12rpx;
				border-radius: 999rpx;
				font-size: 18rpx;
				font-weight: 600;
				&.official { background-color: #dbeafe; color: #2563eb; }
				&.designer { background-color: #dcfce7; color: #16a34a; }
			}
		}
	}
}

.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding-top: 200rpx;
	color: $text3;
	text { margin-top: 24rpx; font-size: 28rpx; }
	.empty-hint { font-size: 22rpx; margin-top: 10rpx; }
}

.load-more {
	text-align: center;
	padding: 20rpx;
	.loading-text, .no-more { font-size: 24rpx; color: $text3; }
}

.upload-form {
	padding-bottom: 80rpx;
}

.form-item {
	margin-bottom: 28rpx;
	.label { font-size: 26rpx; color: $text1; font-weight: 600; margin-bottom: 10rpx; display: block; }
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
		.file-name { font-size: 24rpx; color: $text1; max-width: 400rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
		.file-remove { font-size: 24rpx; color: $danger; }
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

.blocked {
	margin: 40rpx 28rpx;
	padding: 48rpx 32rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	text-align: center;
	background: $card;
	border-radius: 24rpx;
	box-shadow: $shadow;
	.blocked-title { font-size: 30rpx; font-weight: 700; color: $text1; }
	.blocked-desc { font-size: 24rpx; color: $text2; margin-top: 16rpx; line-height: 1.6; }
	.back-btn {
		margin-top: 32rpx;
		height: 80rpx;
		padding: 0 48rpx;
		background: $gradient;
		color: #ffffff;
		border-radius: 999rpx;
		font-size: 28rpx;
		&:active { transform: scale(0.96); }
	}
}
</style>
