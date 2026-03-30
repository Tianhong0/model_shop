<template>
	<view class="my-submission-page">
		<!-- 活动状态提示 -->
		<view v-if="eventStatus !== null && eventStatus !== 2" class="status-notice">
			<view class="notice-icon">
				<uni-icons type="info" size="18" color="#f59e0b"></uni-icons>
			</view>
			<text>{{ getEventStatusHint() }}</text>
		</view>

		<!-- 未提交状态：显示提交表单 -->
		<view v-if="!hasSubmitted" class="submit-form">
			<view class="form-header">
				<text class="form-title">提交参赛作品</text>
				<text class="form-tip">请填写您的作品信息</text>
			</view>

			<view class="form-section">
				<view class="form-item">
					<text class="label">作品标题 <text class="required">*</text></text>
					<input v-model="form.title" class="input" placeholder="请输入作品标题" maxlength="50" />
				</view>

				<view class="form-item">
					<text class="label">作品描述</text>
					<textarea v-model="form.description" class="textarea" placeholder="请描述您的作品设计理念、特点等..." maxlength="500"></textarea>
				</view>

				<view class="form-item">
					<text class="label">模型文件</text>
					<view class="upload-area">
						<view class="file-item" v-for="(file, idx) in form.modelFiles" :key="'file-'+idx">
							<view class="file-icon">
								<uni-icons type="paperclip" size="20" color="#00bfff"></uni-icons>
							</view>
							<text class="file-name">{{getFileName(file)}}</text>
							<view class="remove-btn" @tap="removeModelFile(idx)">
								<uni-icons type="closeempty" size="12" color="#fff"></uni-icons>
							</view>
						</view>
						<view class="upload-btn" @tap="chooseModelFile" v-if="form.modelFiles.length < 5">
							<uni-icons type="plusempty" size="24" color="#ccc"></uni-icons>
							<text>添加模型</text>
						</view>
					</view>
					<text class="upload-tip">支持 STL、OBJ、3MF 等格式，最多5个文件</text>
				</view>

				<view class="form-item">
					<text class="label">作品图片</text>
					<view class="upload-area">
						<view class="img-item" v-for="(img, idx) in form.images" :key="'img-'+idx">
							<image :src="img" mode="aspectFill" class="preview-img"></image>
							<view class="remove-btn" @tap="removeImage(idx)">
								<uni-icons type="closeempty" size="12" color="#fff"></uni-icons>
							</view>
						</view>
						<view class="upload-btn" @tap="chooseImage" v-if="form.images.length < 9">
							<uni-icons type="plusempty" size="24" color="#ccc"></uni-icons>
							<text>添加图片</text>
						</view>
					</view>
				</view>
			</view>

			<view class="form-footer">
				<button class="submit-btn" :loading="submitting" @tap="handleSubmit">提交作品</button>
			</view>
		</view>

		<!-- 已提交状态：显示作品详情 -->
		<view v-else class="submission-detail">
			<view class="detail-header">
				<view class="status-badge" :class="getStatusClass(submission.status)">
					{{submission.statusName}}
				</view>
				<text class="submission-title">{{submission.title}}</text>
				<text class="submit-time">提交于 {{formatTime(submission.createTime)}}</text>
			</view>

			<view class="detail-section" v-if="submission.description">
				<text class="section-title">作品描述</text>
				<text class="section-content">{{submission.description}}</text>
			</view>

			<view class="detail-section" v-if="submission.fileUrls && submission.fileUrls.length">
				<text class="section-title">模型文件</text>
				<view class="file-list">
					<view class="file-item" v-for="(file, idx) in submission.fileUrls" :key="idx">
						<view class="file-icon">
							<uni-icons type="paperclip" size="18" color="#00bfff"></uni-icons>
						</view>
						<text class="file-name">{{getFileName(file)}}</text>
					</view>
				</view>
			</view>

			<view class="detail-section" v-if="submission.imageUrls && submission.imageUrls.length">
				<text class="section-title">作品图片</text>
				<view class="image-grid">
					<image
						v-for="(img, idx) in submission.imageUrls"
						:key="idx"
						:src="img"
						mode="aspectFill"
						class="preview-img"
						@tap="previewImage(img, submission.imageUrls)"
					></image>
				</view>
			</view>

			<!-- 编辑按钮 -->
			<view class="edit-section" v-if="canEdit">
				<button class="edit-btn" @tap="startEdit">修改作品</button>
			</view>
		</view>

		<!-- 编辑模式 -->
		<view v-if="isEditing" class="edit-modal">
			<view class="modal-content">
				<view class="modal-header">
					<text class="modal-title">修改作品</text>
					<view class="close-btn" @tap="cancelEdit">
						<uni-icons type="close" size="22" color="#999"></uni-icons>
					</view>
				</view>

				<view class="modal-body">
					<view class="form-item">
						<text class="label">作品标题</text>
						<input v-model="editForm.title" class="input" placeholder="请输入作品标题" />
					</view>
					<view class="form-item">
						<text class="label">作品描述</text>
						<textarea v-model="editForm.description" class="textarea" placeholder="请描述您的作品..."></textarea>
					</view>
					<view class="form-item">
						<text class="label">模型文件</text>
						<view class="upload-area">
							<view class="file-item" v-for="(file, idx) in editForm.modelFiles" :key="idx">
								<view class="file-icon">
									<uni-icons type="paperclip" size="20" color="#00bfff"></uni-icons>
								</view>
								<text class="file-name">{{getFileName(file)}}</text>
								<view class="remove-btn" @tap="removeEditModelFile(idx)">
									<uni-icons type="closeempty" size="12" color="#fff"></uni-icons>
								</view>
							</view>
							<view class="upload-btn" @tap="chooseEditModelFile" v-if="editForm.modelFiles.length < 5">
								<uni-icons type="plusempty" size="24" color="#ccc"></uni-icons>
								<text>添加</text>
							</view>
						</view>
					</view>
					<view class="form-item">
						<text class="label">作品图片</text>
						<view class="upload-area">
							<view class="img-item" v-for="(img, idx) in editForm.images" :key="idx">
								<image :src="img" mode="aspectFill" class="preview-img"></image>
								<view class="remove-btn" @tap="removeEditImage(idx)">
									<uni-icons type="closeempty" size="12" color="#fff"></uni-icons>
								</view>
							</view>
							<view class="upload-btn" @tap="chooseEditImage" v-if="editForm.images.length < 9">
								<uni-icons type="plusempty" size="24" color="#ccc"></uni-icons>
								<text>添加</text>
							</view>
						</view>
					</view>
				</view>

				<view class="modal-footer">
					<button class="cancel-btn" @tap="cancelEdit">取消</button>
					<button class="confirm-btn" :loading="updating" @tap="handleUpdate">保存</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getMySubmissionApi, createSubmissionApi, updateSubmissionApi, getEventDetailApi } from '../../api/event'
import { uploadCommunityMediaApi } from '../../api/community'
import { getApiBaseUrl } from '../../utils/apiBase'

// 平台检测和模型文件选择工具函数
const canUseWebInput = () => typeof document !== 'undefined' && typeof window !== 'undefined'

const getRuntimePlatform = () => {
	const info = uni.getSystemInfoSync ? uni.getSystemInfoSync() : null
	const uniPlatform = info?.uniPlatform || 'unknown'
	const platform = info?.platform || 'unknown'
	return `${uniPlatform}/${platform}`
}

const isAndroidRuntime = () => {
	// #ifdef APP-PLUS
	try {
		const osName = String(plus?.os?.name || '').toLowerCase()
		if (osName === 'android') return true
	} catch (_) {}
	// #endif
	const info = uni.getSystemInfoSync ? uni.getSystemInfoSync() : null
	const uniPlatform = String(info?.uniPlatform || '').toLowerCase()
	const platform = String(info?.platform || '').toLowerCase()
	return uniPlatform.includes('android') || platform.includes('android')
}

const getReadablePickerError = (error) => {
	const errMsg = String(error?.errMsg || '')
	const msg = String(error?.message || '')
	const raw = msg || errMsg
	if (!raw) return '文件选择失败'
	return raw
		.replace(/^choose\w*:fail\s*/i, '')
		.replace(/^fail\s*/i, '')
		.trim() || '文件选择失败'
}

const eventId = ref(null)
const eventStatus = ref(null)
const eventStatusName = ref('')
const hasSubmitted = ref(false)
const submitting = ref(false)
const updating = ref(false)
const isEditing = ref(false)

// 活动状态：0-未开始, 1-报名中, 2-进行中, 3-评审中, 4-已结束
const canSubmitOrEdit = computed(() => {
	return eventStatus.value === 2 // 只有进行中才能提交/修改作品
})

const getEventStatusHint = () => {
	const hints = {
		0: '活动尚未开始，暂不能提交作品',
		1: '活动正在报名中，请等待活动开始后再提交作品',
		3: '活动已进入评审阶段，不能再修改作品',
		4: '活动已结束，不能再提交或修改作品'
	}
	return hints[eventStatus.value] || '当前活动状态不允许提交作品'
}

const submission = ref({
	id: null,
	title: '',
	description: '',
	fileUrls: [],
	imageUrls: [],
	status: 1,
	statusName: '',
	createTime: ''
})

const form = ref({
	title: '',
	description: '',
	images: [],
	modelFiles: []
})

const editForm = ref({
	title: '',
	description: '',
	images: [],
	modelFiles: []
})

const canEdit = computed(() => {
	// 只有活动进行中且作品未被拒绝时可以编辑
	return eventStatus.value === 2 && submission.value.status !== 3
})

const getStatusClass = (status) => {
	const map = {
		1: 'pending',
		2: 'approved',
		3: 'rejected'
	}
	return map[status] || 'pending'
}

const formatTime = (time) => {
	if (!time) return ''
	return time.substring(0, 16).replace('T', ' ')
}

const getFileName = (url) => {
	if (!url) return '文件'
	const parts = url.split('/')
	return decodeURIComponent(parts[parts.length - 1]) || '模型文件'
}

// 获取我的作品
const fetchMySubmission = async () => {
	try {
		const res = await getMySubmissionApi(eventId.value)
		if (res) {
			submission.value = res
			hasSubmitted.value = true
		}
	} catch (error) {
		// 未提交过作品
		hasSubmitted.value = false
	}
}

// 获取活动详情
const fetchEventDetail = async () => {
	try {
		const res = await getEventDetailApi(eventId.value)
		if (res) {
			eventStatus.value = res.status
			eventStatusName.value = res.statusName
		}
	} catch (error) {
		console.error('获取活动详情失败:', error)
	}
}

// 图片上传
const chooseImage = () => {
	uni.chooseImage({
		count: 9 - form.value.images.length,
		sizeType: ['compressed'],
		sourceType: ['album', 'camera'],
		success: async (res) => {
			uni.showLoading({ title: '上传中...' })
			try {
				for (const file of res.tempFilePaths) {
					const url = await uploadCommunityMediaApi(file, 'postImg')
					if (url) form.value.images.push(url)
				}
			} catch (error) {
				uni.showToast({ title: error.message || '上传失败', icon: 'none' })
			} finally {
				uni.hideLoading()
			}
		}
	})
}

const removeImage = (index) => {
	form.value.images.splice(index, 1)
}

// 模型文件上传 - 使用与悬赏模块相同的健壮选择方式
const normalizePickedModelFile = (pickedRes) => {
	const tempFiles = pickedRes?.tempFiles || []
	const first = tempFiles[0] || {}
	const path = first.path || first.tempFilePath || (pickedRes?.tempFilePaths || [])[0] || ''
	const name = first.name || (path ? path.split('/').pop() : '')
	return { path, name }
}

const pickModelFileByAndroidIntent = () => {
	return new Promise((resolve, reject) => {
		// #ifdef APP-PLUS
		try {
			if (!isAndroidRuntime()) {
				reject(new Error('当前设备不是Android，暂不支持模型文件选择'))
				return
			}
			if (typeof plus === 'undefined' || !plus.android) {
				reject(new Error('当前环境不支持系统文件选择'))
				return
			}

			const main = plus.android.runtimeMainActivity()
			const Intent = plus.android.importClass('android.content.Intent')
			const Activity = plus.android.importClass('android.app.Activity')
			const OpenableColumns = plus.android.importClass('android.provider.OpenableColumns')

			const intent = new Intent(Intent.ACTION_GET_CONTENT)
			intent.setType('*/*')
			intent.addCategory(Intent.CATEGORY_OPENABLE)

			const chooser = Intent.createChooser(intent, '选择模型文件')
			const requestCode = 30002
			const oldOnActivityResult = main.onActivityResult

			main.onActivityResult = function(reqCode, resultCode, data) {
				if (reqCode === requestCode) {
					main.onActivityResult = oldOnActivityResult
					if (resultCode !== Activity.RESULT_OK || !data) {
						reject(new Error('用户取消选择文件'))
						return
					}
					try {
						const uri = data.getData()
						if (!uri) {
							reject(new Error('未获取到文件'))
							return
						}
						plus.android.importClass(uri)
						const uriString = uri.toString()
						let fileName = ''

						try {
							const resolver = main.getContentResolver()
							const cursor = resolver.query(uri, null, null, null, null)
							if (cursor && cursor.moveToFirst()) {
								const nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
								if (nameIndex >= 0) {
									fileName = cursor.getString(nameIndex) || ''
								}
							}
							if (cursor) cursor.close()
						} catch (_) {}

						resolve({ path: uriString, name: fileName })
					} catch (err) {
						reject(err)
					}
					return
				}
				if (typeof oldOnActivityResult === 'function') {
					oldOnActivityResult(reqCode, resultCode, data)
				}
			}

			main.startActivityForResult(chooser, requestCode)
		} catch (error) {
			reject(error)
		}
		// #endif

		// #ifndef APP-PLUS
		reject(new Error('当前环境不支持系统文件选择'))
		// #endif
	})
}

const pickModelFileByUniApi = async () => {
	const extensions = ['stl', 'obj', '3mf', 'step', 'stp', 'iges', 'igs', 'ply']
	if (typeof uni.chooseMessageFile === 'function') {
		const chooseRes = await new Promise((resolve, reject) => {
			uni.chooseMessageFile({
				count: 1,
				type: 'file',
				extension: extensions,
				success: resolve,
				fail: reject
			})
		})
		return normalizePickedModelFile(chooseRes)
	}

	if (typeof uni.chooseFile === 'function') {
		try {
			const chooseRes = await new Promise((resolve, reject) => {
				uni.chooseFile({
					count: 1,
					extension: extensions,
					success: resolve,
					fail: reject
				})
			})
			return normalizePickedModelFile(chooseRes)
		} catch (firstError) {
			// 尝试不带extension参数重试
			const chooseRes = await new Promise((resolve, reject) => {
				uni.chooseFile({
					count: 1,
					success: resolve,
					fail: reject
				})
			})
			return normalizePickedModelFile(chooseRes)
		}
	}

	// #ifdef APP-PLUS
	return pickModelFileByAndroidIntent()
	// #endif

	throw new Error('当前环境不支持模型文件选择')
}

const pickModelFileByWebInput = () => {
	if (!canUseWebInput()) {
		return Promise.reject(new Error('当前环境不支持模型文件选择'))
	}
	return new Promise((resolve, reject) => {
		const input = document.createElement('input')
		input.type = 'file'
		input.accept = '.stl,.obj,.3mf,.step,.stp,.iges,.igs,.ply'
		input.onchange = () => {
			const file = (input.files && input.files[0]) || null
			if (!file) {
				reject(new Error('未选择模型文件'))
				return
			}
			resolve({ file, name: file.name })
		}
		input.click()
	})
}

const chooseModelFile = async () => {
	try {
		let filePath = ''
		let fileName = ''
		let fileObject = null

		try {
			const pickedByUni = await pickModelFileByUniApi()
			filePath = pickedByUni.path || ''
			fileName = pickedByUni.name || ''
		} catch (uniPickError) {
			if (String(uniPickError?.errMsg || '').includes('cancel')) {
				return
			}
			if (!canUseWebInput()) {
				throw uniPickError
			}
			const picked = await pickModelFileByWebInput()
			fileObject = picked.file
			fileName = picked.name || ''
		}

		if (!filePath && !fileObject) {
			uni.showToast({ title: '未选择模型文件', icon: 'none' })
			return
		}

		uni.showLoading({ title: '上传模型中...' })
		if (fileObject) {
			const url = await uploadModelFile(fileObject)
			if (url) form.value.modelFiles.push(url)
		} else {
			const url = await uploadCommunityMediaApi(filePath, 'model')
			if (url) form.value.modelFiles.push(url)
		}
		uni.hideLoading()
		uni.showToast({ title: '模型上传成功', icon: 'success' })
	} catch (error) {
		uni.hideLoading()
		if (String(error?.errMsg || '').includes('cancel')) return
		if (String(error?.message || '').includes('取消')) return
		if (String(error?.message || '').includes('未选择模型文件')) return
		const platform = getRuntimePlatform()
		const reason = getReadablePickerError(error)
		const supportHint = reason.includes('not function') || reason.includes('not support') || reason.includes('不支持')
			? `当前运行平台(${platform})不支持模型文件选择接口，请在H5或微信小程序中上传。`
			: `模型文件选择失败：${reason}`
		uni.showModal({
			title: '上传失败',
			content: supportHint,
			showCancel: false
		})
	}
}

const uploadModelFile = (file) => {
	return new Promise((resolve, reject) => {
		const formData = new FormData()
		formData.append('file', file)
		formData.append('type', 'model')

		const token = uni.getStorageSync('token')
		const baseUrl = getApiBaseUrl()
		fetch(`${baseUrl}/api/community/file/upload`, {
			method: 'POST',
			headers: { 'Authorization': token ? `Bearer ${token}` : '' },
			body: formData
		})
		.then(res => res.json())
		.then(data => {
			if (data.code === 200 && data.data) resolve(data.data)
			else reject(new Error(data.message || '上传失败'))
		})
		.catch(err => reject(err))
	})
}

const removeModelFile = (index) => {
	form.value.modelFiles.splice(index, 1)
}

// 提交作品
const handleSubmit = async () => {
	if (!canSubmitOrEdit.value) {
		uni.showToast({ title: getEventStatusHint(), icon: 'none' })
		return
	}

	if (!form.value.title.trim()) {
		uni.showToast({ title: '请输入作品标题', icon: 'none' })
		return
	}

	uni.showModal({
		title: '确认提交',
		content: '确定提交作品吗？',
		success: async (res) => {
			if (!res.confirm) return

			submitting.value = true
			try {
				await createSubmissionApi({
					eventId: eventId.value,
					title: form.value.title,
					description: form.value.description,
					imageUrls: form.value.images,
					fileUrls: form.value.modelFiles
				})
				uni.showToast({ title: '提交成功', icon: 'success' })
				await fetchMySubmission()
			} catch (error) {
				uni.showToast({ title: error.message || '提交失败', icon: 'none' })
			} finally {
				submitting.value = false
			}
		}
	})
}

// 开始编辑
const startEdit = () => {
	if (!canSubmitOrEdit.value) {
		uni.showToast({ title: getEventStatusHint(), icon: 'none' })
		return
	}

	editForm.value = {
		title: submission.value.title,
		description: submission.value.description || '',
		images: [...(submission.value.imageUrls || [])],
		modelFiles: [...(submission.value.fileUrls || [])]
	}
	isEditing.value = true
}

const cancelEdit = () => {
	isEditing.value = false
}

// 编辑模式的图片和文件操作
const chooseEditImage = () => {
	uni.chooseImage({
		count: 9 - editForm.value.images.length,
		sizeType: ['compressed'],
		sourceType: ['album', 'camera'],
		success: async (res) => {
			uni.showLoading({ title: '上传中...' })
			try {
				for (const file of res.tempFilePaths) {
					const url = await uploadCommunityMediaApi(file, 'postImg')
					if (url) editForm.value.images.push(url)
				}
			} catch (error) {
				uni.showToast({ title: error.message || '上传失败', icon: 'none' })
			} finally {
				uni.hideLoading()
			}
		}
	})
}

const removeEditImage = (index) => {
	editForm.value.images.splice(index, 1)
}

const chooseEditModelFile = async () => {
	try {
		let filePath = ''
		let fileName = ''
		let fileObject = null

		try {
			const pickedByUni = await pickModelFileByUniApi()
			filePath = pickedByUni.path || ''
			fileName = pickedByUni.name || ''
		} catch (uniPickError) {
			if (String(uniPickError?.errMsg || '').includes('cancel')) {
				return
			}
			if (!canUseWebInput()) {
				throw uniPickError
			}
			const picked = await pickModelFileByWebInput()
			fileObject = picked.file
			fileName = picked.name || ''
		}

		if (!filePath && !fileObject) {
			uni.showToast({ title: '未选择模型文件', icon: 'none' })
			return
		}

		uni.showLoading({ title: '上传模型中...' })
		if (fileObject) {
			const url = await uploadModelFile(fileObject)
			if (url) editForm.value.modelFiles.push(url)
		} else {
			const url = await uploadCommunityMediaApi(filePath, 'model')
			if (url) editForm.value.modelFiles.push(url)
		}
		uni.hideLoading()
		uni.showToast({ title: '模型上传成功', icon: 'success' })
	} catch (error) {
		uni.hideLoading()
		if (String(error?.errMsg || '').includes('cancel')) return
		if (String(error?.message || '').includes('取消')) return
		const platform = getRuntimePlatform()
		const reason = getReadablePickerError(error)
		uni.showModal({
			title: '上传失败',
			content: `模型文件选择失败：${reason}`,
			showCancel: false
		})
	}
}

const removeEditModelFile = (index) => {
	editForm.value.modelFiles.splice(index, 1)
}

// 更新作品
const handleUpdate = async () => {
	if (!canSubmitOrEdit.value) {
		uni.showToast({ title: getEventStatusHint(), icon: 'none' })
		return
	}

	if (!editForm.value.title.trim()) {
		uni.showToast({ title: '请输入作品标题', icon: 'none' })
		return
	}

	updating.value = true
	try {
		await updateSubmissionApi(submission.value.id, {
			title: editForm.value.title,
			description: editForm.value.description,
			imageUrls: editForm.value.images,
			fileUrls: editForm.value.modelFiles
		})
		uni.showToast({ title: '修改成功', icon: 'success' })
		isEditing.value = false
		await fetchMySubmission()
	} catch (error) {
		uni.showToast({ title: error.message || '修改失败', icon: 'none' })
	} finally {
		updating.value = false
	}
}

const previewImage = (current, urls) => {
	uni.previewImage({ current, urls })
}

onLoad((options) => {
	if (options.eventId) {
		eventId.value = options.eventId
		fetchEventDetail()
		fetchMySubmission()
	}
})
</script>

<style scoped lang="scss">
.my-submission-page {
	min-height: 100vh;
	background: #f8f8f8;
}

/* 状态提示 */
.status-notice {
	margin: 24rpx 28rpx 0;
	padding: 24rpx 28rpx;
	background: rgba(245, 158, 11, 0.08);
	border-radius: 24rpx;
	display: flex;
	align-items: center;
	gap: 14rpx;
	animation: fadeInUp 0.4s ease forwards;

	.notice-icon {
		flex-shrink: 0;
	}

	text {
		font-size: 26rpx;
		color: #d97706;
		line-height: 1.5;
	}
}

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

/* 提交表单样式 */
.submit-form {
	padding: 28rpx;

	.form-header {
		margin-bottom: 32rpx;
		animation: fadeInUp 0.4s ease forwards;

		.form-title {
			font-size: 36rpx;
			font-weight: 700;
			color: #1a1a1a;
			display: block;
		}

		.form-tip {
			font-size: 26rpx;
			color: #999;
			margin-top: 10rpx;
			display: block;
		}
	}

	.form-section {
		background: #ffffff;
		border-radius: 24rpx;
		padding: 32rpx;
		box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
		animation: fadeInUp 0.4s ease 0.1s forwards;
		opacity: 0;
	}

	.form-item {
		margin-bottom: 32rpx;

		&:last-child {
			margin-bottom: 0;
		}

		.label {
			display: block;
			font-size: 28rpx;
			font-weight: 600;
			color: #1a1a1a;
			margin-bottom: 16rpx;

			.required {
				color: #ff4d6d;
			}
		}

		.input {
			width: 100%;
			height: 88rpx;
			padding: 0 28rpx;
			background: #f8f8f8;
			border-radius: 16rpx;
			font-size: 28rpx;
			color: #1a1a1a;
		}

		.textarea {
			width: 100%;
			height: 220rpx;
			padding: 24rpx 28rpx;
			background: #f8f8f8;
			border-radius: 16rpx;
			font-size: 28rpx;
			color: #1a1a1a;
			line-height: 1.6;
		}
	}

	.upload-area {
		display: flex;
		flex-wrap: wrap;
		gap: 16rpx;

		.img-item {
			position: relative;
			width: 160rpx;
			height: 160rpx;

			.preview-img {
				width: 100%;
				height: 100%;
				border-radius: 16rpx;
			}

			.remove-btn {
				position: absolute;
				top: -10rpx;
				right: -10rpx;
				width: 36rpx;
				height: 36rpx;
				background: #ff4d6d;
				border-radius: 50%;
				display: flex;
				align-items: center;
				justify-content: center;
				box-shadow: 0 4rpx 12rpx rgba(255, 77, 109, 0.3);
			}
		}

		.file-item {
			width: 100%;
			display: flex;
			align-items: center;
			padding: 20rpx 24rpx;
			background: #f8f8f8;
			border-radius: 16rpx;
			gap: 14rpx;

			.file-icon {
				width: 52rpx;
				height: 52rpx;
				background: rgba(0, 191, 255, 0.08);
				border-radius: 12rpx;
				display: flex;
				align-items: center;
				justify-content: center;
			}

			.file-name {
				flex: 1;
				font-size: 24rpx;
				color: #1a1a1a;
				overflow: hidden;
				text-overflow: ellipsis;
				white-space: nowrap;
			}

			.remove-btn {
				width: 32rpx;
				height: 32rpx;
				background: #ff4d6d;
				border-radius: 50%;
				display: flex;
				align-items: center;
				justify-content: center;
			}
		}

		.upload-btn {
			width: 160rpx;
			height: 160rpx;
			background: #f8f8f8;
			border-radius: 16rpx;
			display: flex;
			flex-direction: column;
			align-items: center;
			justify-content: center;
			gap: 8rpx;
			transition: all 0.2s;

			&:active {
				transform: scale(0.96);
				background: #f0f0f0;
			}

			text {
				font-size: 22rpx;
				color: #999;
			}
		}
	}

	.upload-tip {
		font-size: 22rpx;
		color: #bbb;
		margin-top: 14rpx;
		display: block;
	}

	.form-footer {
		margin-top: 40rpx;
		animation: fadeInUp 0.4s ease 0.2s forwards;
		opacity: 0;

		.submit-btn {
			width: 100%;
			height: 92rpx;
			background: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
			color: #fff;
			border-radius: 999rpx;
			font-size: 30rpx;
			font-weight: 700;
			box-shadow: 0 8rpx 24rpx rgba(0, 191, 255, 0.3);

			&:active {
				transform: scale(0.96);
			}
		}
	}
}

/* 作品详情样式 */
.submission-detail {
	padding: 28rpx;
	display: flex;
	flex-direction: column;
	gap: 24rpx;

	.detail-header {
		background: #ffffff;
		border-radius: 24rpx;
		padding: 32rpx;
		box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
		animation: fadeInUp 0.4s ease forwards;

		.status-badge {
			display: inline-block;
			padding: 8rpx 24rpx;
			border-radius: 999rpx;
			font-size: 22rpx;
			font-weight: 600;
			margin-bottom: 18rpx;
			animation: breatheGlow 2.5s ease-in-out infinite;

			&.pending {
				background: rgba(245, 158, 11, 0.1);
				color: #f59e0b;
			}

			&.approved {
				background: rgba(16, 185, 129, 0.1);
				color: #10b981;
			}

			&.rejected {
				background: rgba(255, 77, 109, 0.1);
				color: #ff4d6d;
				animation: none;
			}
		}

		.submission-title {
			font-size: 36rpx;
			font-weight: 700;
			color: #1a1a1a;
			display: block;
			line-height: 1.4;
		}

		.submit-time {
			font-size: 24rpx;
			color: #999;
			margin-top: 14rpx;
			display: block;
		}
	}

	.detail-section {
		background: #ffffff;
		border-radius: 24rpx;
		padding: 32rpx;
		box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
		animation: fadeInUp 0.4s ease 0.1s forwards;
		opacity: 0;

		.section-title {
			font-size: 30rpx;
			font-weight: 600;
			color: #1a1a1a;
			display: block;
			margin-bottom: 20rpx;
		}

		.section-content {
			font-size: 28rpx;
			color: #666;
			line-height: 1.7;
		}

		.file-list {
			display: flex;
			flex-direction: column;
			gap: 12rpx;

			.file-item {
				display: flex;
				align-items: center;
				gap: 14rpx;
				padding: 18rpx 24rpx;
				background: #f8f8f8;
				border-radius: 16rpx;

				.file-icon {
					width: 44rpx;
					height: 44rpx;
					background: rgba(0, 191, 255, 0.08);
					border-radius: 10rpx;
					display: flex;
					align-items: center;
					justify-content: center;
				}

				.file-name {
					flex: 1;
					font-size: 26rpx;
					color: #666;
					overflow: hidden;
					text-overflow: ellipsis;
					white-space: nowrap;
				}
			}
		}

		.image-grid {
			display: flex;
			flex-wrap: wrap;
			gap: 12rpx;

			.preview-img {
				width: 200rpx;
				height: 200rpx;
				border-radius: 16rpx;
				transition: opacity 0.4s ease;
			}
		}
	}

	.edit-section {
		margin-top: 16rpx;
		animation: fadeInUp 0.4s ease 0.2s forwards;
		opacity: 0;

		.edit-btn {
			width: 100%;
			height: 92rpx;
			background: #ffffff;
			color: #00bfff;
			border-radius: 999rpx;
			font-size: 30rpx;
			font-weight: 600;
			box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);

			&:active {
				transform: scale(0.96);
			}
		}
	}
}

@keyframes breatheGlow {
	0%, 100% {
		box-shadow: 0 0 12rpx rgba(0, 191, 255, 0.15);
	}
	50% {
		box-shadow: 0 0 24rpx rgba(0, 191, 255, 0.35);
	}
}

/* 编辑弹窗 */
.edit-modal {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(0, 0, 0, 0.45);
	display: flex;
	align-items: flex-end;
	z-index: 999;

	.modal-content {
		width: 100%;
		max-height: 90vh;
		background: #ffffff;
		border-radius: 32rpx 32rpx 0 0;
		display: flex;
		flex-direction: column;
	}

	.modal-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 32rpx;

		.modal-title {
			font-size: 34rpx;
			font-weight: 700;
			color: #1a1a1a;
		}

		.close-btn {
			width: 56rpx;
			height: 56rpx;
			display: flex;
			align-items: center;
			justify-content: center;
			border-radius: 50%;
			background: #f8f8f8;

			&:active {
				background: #f0f0f0;
			}
		}
	}

	.modal-body {
		flex: 1;
		overflow-y: auto;
		padding: 0 32rpx 32rpx;

		.form-item {
			margin-bottom: 28rpx;

			.label {
				display: block;
				font-size: 28rpx;
				font-weight: 600;
				color: #1a1a1a;
				margin-bottom: 14rpx;
			}

			.input {
				width: 100%;
				height: 88rpx;
				padding: 0 28rpx;
				background: #f8f8f8;
				border-radius: 16rpx;
				font-size: 28rpx;
				color: #1a1a1a;
			}

			.textarea {
				width: 100%;
				height: 180rpx;
				padding: 20rpx 28rpx;
				background: #f8f8f8;
				border-radius: 16rpx;
				font-size: 28rpx;
				color: #1a1a1a;
				line-height: 1.6;
			}
		}

		.upload-area {
			display: flex;
			flex-wrap: wrap;
			gap: 12rpx;

			.img-item {
				position: relative;
				width: 140rpx;
				height: 140rpx;

				.preview-img {
					width: 100%;
					height: 100%;
					border-radius: 12rpx;
				}

				.remove-btn {
					position: absolute;
					top: -8rpx;
					right: -8rpx;
					width: 32rpx;
					height: 32rpx;
					background: #ff4d6d;
					border-radius: 50%;
					display: flex;
					align-items: center;
					justify-content: center;
					box-shadow: 0 4rpx 12rpx rgba(255, 77, 109, 0.3);
				}
			}

			.file-item {
				width: 100%;
				display: flex;
				align-items: center;
				padding: 16rpx 20rpx;
				background: #f8f8f8;
				border-radius: 12rpx;
				gap: 12rpx;

				.file-icon {
					width: 44rpx;
					height: 44rpx;
					background: rgba(0, 191, 255, 0.08);
					border-radius: 10rpx;
					display: flex;
					align-items: center;
					justify-content: center;
				}

				.file-name {
					flex: 1;
					font-size: 24rpx;
					color: #1a1a1a;
					overflow: hidden;
					text-overflow: ellipsis;
					white-space: nowrap;
				}

				.remove-btn {
					width: 28rpx;
					height: 28rpx;
					background: #ff4d6d;
					border-radius: 50%;
					display: flex;
					align-items: center;
					justify-content: center;
				}
			}

			.upload-btn {
				width: 140rpx;
				height: 140rpx;
				background: #f8f8f8;
				border-radius: 12rpx;
				display: flex;
				flex-direction: column;
				align-items: center;
				justify-content: center;
				gap: 6rpx;

				&:active {
					transform: scale(0.96);
				}

				text {
					font-size: 20rpx;
					color: #999;
				}
			}
		}
	}

	.modal-footer {
		display: flex;
		gap: 20rpx;
		padding: 24rpx 32rpx 40rpx;

		.cancel-btn {
			flex: 1;
			height: 84rpx;
			background: #f8f8f8;
			color: #666;
			border-radius: 999rpx;
			font-size: 28rpx;

			&:active {
				transform: scale(0.96);
			}
		}

		.confirm-btn {
			flex: 1;
			height: 84rpx;
			background: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
			color: #fff;
			border-radius: 999rpx;
			font-size: 28rpx;
			font-weight: 600;
			box-shadow: 0 8rpx 24rpx rgba(0, 191, 255, 0.3);

			&:active {
				transform: scale(0.96);
			}
		}
	}
}
</style>
