<template>
	<view class="page-container">
		<view class="card status-card">
			<view class="status-row">
				<text class="status-label">当前状态</text>
				<text class="status-value">{{ designerApplyStatusText }}</text>
			</view>
			<view class="status-tip" v-if="latestDesignerApply?.retryAfter">
				可重提时间：{{ formatDateTime(latestDesignerApply.retryAfter) }}
			</view>
			<view class="status-tip" v-if="isDesigner">您已通过审核，可前往上传模型与参与悬赏。</view>
		</view>

		<view class="card form-card" v-if="!isDesigner">
			<view class="form-title">申请成为设计者</view>
			<view class="form-tip">提交后将进入管理员审核，审核通过后可上传模型与参与悬赏任务。</view>
			<view class="form-item textarea-item">
				<textarea
					class="textarea"
					v-model="designerApplyForm.applyReason"
					maxlength="1000"
					placeholder="请填写申请理由、擅长方向、案例说明等"
				/>
			</view>
			<view class="attachment-panel">
				<view class="attachment-header">
					<text class="attachment-title">附件（模型/图片/视频）</text>
					<button class="mini-btn" :disabled="attachmentUploading" @click="addDesignerAttachment">
						{{ attachmentUploading ? '上传中' : '添加附件' }}
					</button>
				</view>
				<view v-if="designerAttachmentUrls.length === 0" class="attachment-empty">暂无附件，可添加作品链接证明能力</view>
				<view v-for="(url, idx) in designerAttachmentUrls" :key="url + idx" class="attachment-item">
					<text class="attachment-url">{{ url }}</text>
					<text class="attachment-remove" @click="removeDesignerAttachment(idx)">删除</text>
				</view>
			</view>
			<button class="submit-btn" :disabled="designerApplySubmitting" @click="submitDesignerApply">
				{{ designerApplySubmitting ? '提交中...' : '提交申请' }}
			</button>
		</view>
	</view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
	getMyDesignerApplyStatusApi,
	submitDesignerApplyApi,
	uploadDesignerApplyAttachmentApi
} from '../../api/user'
import { ensureLoginOrRedirect } from '../../utils/auth'
import { getStoredUserRole, isDesignerRole } from '../../utils/role'

const designerApplySubmitting = ref(false)
const attachmentUploading = ref(false)
const ENABLE_PICKER_DEBUG = typeof process !== 'undefined' && process.env && process.env.NODE_ENV !== 'production'
const userRole = ref('')
const latestDesignerApply = ref(null)
const designerApplyForm = ref({
	applyReason: '',
	attachmentUrls: ''
})

const debugPicker = (...args) => {
	if (!ENABLE_PICKER_DEBUG) return
	console.log('[designer-apply-picker]', ...args)
}

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

const chooseByApi = (apiName, options) => {
	debugPicker('call', apiName, options)
	return new Promise((resolve, reject) => {
		uni[apiName]({
			...options,
			success: (res) => {
				debugPicker('success', apiName, res)
				resolve(res)
			},
			fail: (err) => {
				debugPicker('fail', apiName, err)
				reject(err)
			}
		})
	})
}

const withRetryWithoutExtension = async (apiName, baseOptions) => {
	try {
		return await chooseByApi(apiName, baseOptions)
	} catch (firstError) {
		debugPicker('retry-without-extension', apiName, firstError)
		if (String(firstError?.errMsg || '').includes('cancel')) {
			throw firstError
		}
		const retryOptions = { ...baseOptions }
		delete retryOptions.extension
		return chooseByApi(apiName, retryOptions)
	}
}

const pickModelFileByUniApi = async () => {
	const extensions = ['stl', 'obj', '3mf', 'step', 'stp', 'iges', 'igs']
	if (typeof uni.chooseMessageFile === 'function') {
		debugPicker('use-api', 'chooseMessageFile')
		const chooseRes = await withRetryWithoutExtension('chooseMessageFile', {
			count: 1,
			type: 'file',
			extension: extensions
		})
		return normalizePickedModelFile(chooseRes)
	}

	if (typeof uni.chooseFile === 'function') {
		debugPicker('use-api', 'chooseFile')
		const chooseRes = await withRetryWithoutExtension('chooseFile', {
			count: 1,
			extension: extensions
		})
		return normalizePickedModelFile(chooseRes)
	}

	// #ifdef APP-PLUS
	debugPicker('use-api', 'android-intent-picker')
	return pickModelFileByAndroidIntent()
	// #endif

	throw new Error('当前环境不支持模型文件选择')
}

const pickModelFileByWebInput = () => {
	debugPicker('use-fallback', 'web-input')
	if (!canUseWebInput()) {
		return Promise.reject(new Error('当前环境不支持模型文件选择'))
	}
	return new Promise((resolve, reject) => {
		const input = document.createElement('input')
		input.type = 'file'
		input.accept = '.stl,.obj,.3mf,.step,.stp,.iges,.igs'
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

const designerAttachmentUrls = computed(() => {
	const text = String(designerApplyForm.value.attachmentUrls || '').trim()
	if (!text) return []
	return text.split(/\r?\n|,|，/).map(item => item.trim()).filter(Boolean)
})

const isDesigner = computed(() => isDesignerRole(userRole.value))

const designerApplyStatusText = computed(() => {
	if (isDesigner.value) return '已通过'
	const status = latestDesignerApply.value?.status
	if (!status) return '未申请'
	return renderDesignerStatus(status)
})

const renderDesignerStatus = (status) => {
	const map = {
		pending: '审核中',
		approved: '已通过',
		rejected: '已拒绝'
	}
	return map[String(status || '').toLowerCase()] || '未知状态'
}

const formatDateTime = (value) => {
	if (!value) return '-'
	return String(value).replace('T', ' ').slice(0, 19)
}

const loadDesignerApplyStatus = async () => {
	try {
		const data = await getMyDesignerApplyStatusApi()
		latestDesignerApply.value = data?.latestApply || null
		if (data?.alreadyDesigner) {
			const profile = uni.getStorageSync('user_profile') || {}
			uni.setStorageSync('user_role', 'designer')
			uni.setStorageSync('user_profile', {
				...profile,
				role: 'designer'
			})
			userRole.value = 'designer'
		}
	} catch (_) {
		latestDesignerApply.value = null
	}
}

onShow(() => {
	if (!ensureLoginOrRedirect()) return
	userRole.value = getStoredUserRole()
	loadDesignerApplyStatus()
})

const addDesignerAttachment = () => {
	if (attachmentUploading.value) return
	uni.showActionSheet({
		itemList: ['上传图片', '上传视频', '上传模型文件', '手动填写链接'],
		success: async (res) => {
			if (res.tapIndex === 3) {
				uni.showModal({
					title: '提示',
					content: '请在申请理由中粘贴作品链接（当前端暂不支持直接输入链接列表）。',
					showCancel: false
				})
				return
			}

			const selectAndUpload = async (picker, type, loadingTitle = '上传中...') => {
				attachmentUploading.value = true
				uni.showLoading({ title: loadingTitle })
				try {
					const filePath = await picker()
					if (!filePath) {
						uni.hideLoading()
						attachmentUploading.value = false
						return
					}
					const url = await uploadDesignerApplyAttachmentApi(filePath, type)
					const merged = [...designerAttachmentUrls.value, url]
					designerApplyForm.value.attachmentUrls = merged.join('\n')
					uni.hideLoading()
					uni.showToast({ title: '附件上传成功', icon: 'none' })
				} catch (error) {
					uni.hideLoading()
					if (String(error?.errMsg || '').includes('cancel')) {
						return
					}
					if (String(error?.message || '').includes('取消')) {
						return
					}
					if (type === 'modelFile') {
						const reason = getReadablePickerError(error)
						const platform = getRuntimePlatform()
						const supportHint = reason.includes('not function') || reason.includes('not support') || reason.includes('不支持')
							? `当前运行平台(${platform})不支持模型文件选择接口，请在APP端或微信小程序中上传，或先上传图片/视频。`
							: `模型文件选择失败：${reason}`
						uni.showModal({
							title: '上传失败',
							content: supportHint,
							showCancel: false
						})
						return
					}
					uni.showToast({ title: error.message || '附件上传失败', icon: 'none' })
				} finally {
					attachmentUploading.value = false
				}
			}

			if (res.tapIndex === 0) {
				await selectAndUpload(() => new Promise((resolve, reject) => {
					uni.chooseImage({
						count: 1,
						sizeType: ['compressed'],
						success: (chooseRes) => resolve(chooseRes?.tempFilePaths?.[0] || ''),
						fail: reject
					})
				}), 'others', '上传图片中...')
			} else if (res.tapIndex === 1) {
				await selectAndUpload(() => new Promise((resolve, reject) => {
					uni.chooseVideo({
						sourceType: ['album', 'camera'],
						success: (videoRes) => resolve(videoRes?.tempFilePath || ''),
						fail: reject
					})
				}), 'others', '上传视频中...')
			} else {
				await selectAndUpload(async () => {
					let filePath = ''
					try {
						const pickedByUni = await pickModelFileByUniApi()
						filePath = pickedByUni.path || ''
						debugPicker('picked-by-uni', { filePath, fileName: pickedByUni.name || '' })
					} catch (uniPickError) {
						debugPicker('uni-pick-failed', uniPickError)
						if (String(uniPickError?.errMsg || '').includes('cancel')) {
							throw uniPickError
						}
						if (!canUseWebInput()) {
							throw uniPickError
						}
						const picked = await pickModelFileByWebInput()
						if (!picked?.file) {
							throw new Error('未选择模型文件')
						}
						throw new Error('当前平台请通过APP端或小程序端上传模型文件')
					}

					if (!filePath) {
						throw new Error('未选择模型文件')
					}
					return filePath
				}, 'modelFile', '上传模型中...')
			}
		}
	})
}

const removeDesignerAttachment = (index) => {
	const merged = [...designerAttachmentUrls.value]
	merged.splice(index, 1)
	designerApplyForm.value.attachmentUrls = merged.join('\n')
}

const submitDesignerApply = async () => {
	const applyReason = String(designerApplyForm.value.applyReason || '').trim()
	if (!applyReason) {
		uni.showToast({ title: '请填写申请理由', icon: 'none' })
		return
	}

	designerApplySubmitting.value = true
	uni.showLoading({ title: '提交中...' })
	try {
		await submitDesignerApplyApi({
			applyReason,
			attachmentUrls: designerAttachmentUrls.value.join(',')
		})
		uni.hideLoading()
		uni.showToast({ title: '申请已提交', icon: 'none' })
		designerApplyForm.value = {
			applyReason: '',
			attachmentUrls: ''
		}
		await loadDesignerApplyStatus()
	} catch (error) {
		uni.hideLoading()
		uni.showToast({ title: error.message || '提交失败', icon: 'none' })
	} finally {
		designerApplySubmitting.value = false
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

.page-container {
	min-height: 100vh;
	background: $bg;
	padding: 28rpx;
}

.card {
	background-color: $card;
	border-radius: 24rpx;
	padding: 32rpx;
	margin-bottom: 28rpx;
	box-shadow: $shadow;
}

.status-card {
	.status-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}
	.status-label { font-size: 28rpx; color: $text2; }
	.status-value {
		font-size: 28rpx;
		font-weight: 600;
		color: $primary;
		background: rgba(0, 191, 255, 0.08);
		padding: 6rpx 20rpx;
		border-radius: 999rpx;
	}
	.status-tip { font-size: 24rpx; color: $text2; margin-top: 16rpx; line-height: 1.6; }
}

.form-card {
	.form-title { font-size: 36rpx; font-weight: 700; color: $text1; margin-bottom: 12rpx; }
	.form-tip { font-size: 24rpx; color: $text2; margin-bottom: 20rpx; line-height: 1.6; }
	.form-item {
		&.textarea-item { border-bottom: none; }
		.textarea {
			width: 100%;
			min-height: 200rpx;
			font-size: 28rpx;
			color: $text1;
			padding: 20rpx;
			background-color: $bg;
			border-radius: 16rpx;
		}
	}
	.attachment-panel {
		margin-top: 20rpx;
		margin-bottom: 28rpx;
		padding: 20rpx;
		background-color: $bg;
		border-radius: 16rpx;
		.attachment-header {
			display: flex;
			justify-content: space-between;
			align-items: center;
			margin-bottom: 12rpx;
		}
		.attachment-title { font-size: 28rpx; color: $text1; font-weight: 600; }
		.attachment-empty { font-size: 24rpx; color: $text3; }
		.attachment-item {
			display: flex;
			justify-content: space-between;
			gap: 12rpx;
			margin-top: 10rpx;
			padding: 12rpx 0;
			& + .attachment-item { border-top: 1rpx solid rgba(0,0,0,0.04); }
		}
		.attachment-url { font-size: 22rpx; color: $text2; flex: 1; word-break: break-all; }
		.attachment-remove { font-size: 22rpx; color: $danger; font-weight: 500; }
	}
	.mini-btn {
		width: 180rpx;
		height: 64rpx;
		line-height: 64rpx;
		font-size: 24rpx;
		border-radius: 999rpx;
		background: $gradient;
		color: #ffffff;
		padding: 0;
		margin: 0;
		&:active { transform: scale(0.96); }
	}
	.mini-btn[disabled] { background-color: $bg; color: $text3; }
	.submit-btn {
		height: 88rpx;
		line-height: 88rpx;
		background: $gradient;
		color: #ffffff;
		border-radius: 999rpx;
		font-size: 30rpx;
		font-weight: 600;
		box-shadow: 0 8rpx 30rpx rgba(0, 191, 255, 0.25);
		&:active { transform: scale(0.96); }
	}
	.submit-btn[disabled] { background: $bg; color: $text3; box-shadow: none; }
}
</style>