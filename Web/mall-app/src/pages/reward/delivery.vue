<template>
	<view class="delivery-container">
		<view class="section card">
			<view class="sec-title">交付说明</view>
			<textarea
				v-model="form.description"
				placeholder="请描述本次交付内容、完成情况等"
				:maxlength="2000"
				class="desc-input"
			/>
			<text class="char-count">{{ form.description.length }}/2000</text>
		</view>

		<view class="section card">
			<view class="sec-title">模型文件 <text class="required">*</text></view>
			<text class="sec-hint">请上传交付的3D模型文件（支持 STL、OBJ、FBX、GLTF、3MF、STEP 等格式）</text>
			<view class="file-list">
				<view v-for="(file, idx) in form.files" :key="idx" class="file-item">
					<text class="file-icon">🧊</text>
					<view class="file-info">
						<text class="file-name">{{ file.name }}</text>
						<text class="file-ext">{{ file.ext }}</text>
					</view>
					<view class="remove-btn" @click="removeFile(idx)">×</view>
				</view>
				<view v-if="form.files.length < 10" class="add-file" @click="chooseModel">
					<text class="add-icon">+</text>
					<text class="add-text">上传模型文件</text>
				</view>
			</view>
		</view>

		<view class="section card">
			<view class="sec-title">参考图片（可选）</view>
			<view class="image-list">
				<view v-for="(img, idx) in form.imageUrls" :key="idx" class="image-item">
					<image :src="img" mode="aspectFill" @click="previewImage(idx)" />
					<view class="remove-btn" @click="removeImage(idx)">×</view>
				</view>
				<view v-if="form.imageUrls.length < 9" class="add-image" @click="chooseImage">
					<text>+</text>
				</view>
			</view>
		</view>

		<view class="section card">
			<view class="sec-title">交付类型</view>
			<view class="toggle-row" @click="form.isFinal = form.isFinal === 1 ? 0 : 1">
				<view class="checkbox" :class="{ checked: form.isFinal === 1 }">
					<text v-if="form.isFinal === 1">✓</text>
				</view>
				<text>标记为最终交付（发布者可直接验收）</text>
			</view>

			<view v-if="form.isFinal === 1" class="license-section">
				<view class="toggle-row" @click="form.allowCommercialUse = form.allowCommercialUse === 1 ? 0 : 1">
					<view class="checkbox" :class="{ checked: form.allowCommercialUse === 1 }">
						<text v-if="form.allowCommercialUse === 1">✓</text>
					</view>
					<text>允许商业使用</text>
				</view>
				<view class="toggle-row" @click="form.allowModification = form.allowModification === 1 ? 0 : 1">
					<view class="checkbox" :class="{ checked: form.allowModification === 1 }">
						<text v-if="form.allowModification === 1">✓</text>
					</view>
					<text>允许修改作品</text>
				</view>
				<view class="form-row">
					<text class="label">许可类型</text>
					<picker :range="licenseTypes" @change="onLicenseChange">
						<view class="picker-val">{{ form.licenseType }}</view>
					</picker>
				</view>
			</view>
		</view>

		<view class="bottom-bar">
			<button class="submit-btn" :disabled="!canSubmit || submitting" @click="submit">
				{{ submitting ? '提交中...' : '提交交付' }}
			</button>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
	submitBountyDeliveryApi,
	uploadBountyAttachmentApi,
	uploadBountyBidAssetApi
} from '../../api/reward'

const taskId = ref(null)
const submitting = ref(false)
const licenseTypes = ['Personal', 'Commercial', 'Custom']

const form = ref({
	description: '',
	files: [],       // { url, name, ext }
	imageUrls: [],
	isFinal: 1,
	allowCommercialUse: 0,
	allowModification: 1,
	licenseType: 'Personal'
})

const canSubmit = computed(() => form.value.description.trim() && form.value.files.length > 0)

const onLicenseChange = (e) => {
	form.value.licenseType = licenseTypes[e.detail.value]
}

// ==================== 模型文件选择（复用竞标页逻辑） ====================

const MODEL_EXTENSIONS = ['stl', 'obj', 'fbx', 'gltf', 'glb', '3mf', 'step', 'stp', 'iges', 'igs', 'blend', 'max', 'ma', 'mb', 'c4d', 'ply', 'amf']

const canUseWebInput = () => typeof document !== 'undefined' && typeof window !== 'undefined'

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

const getRuntimePlatform = () => {
	const info = uni.getSystemInfoSync ? uni.getSystemInfoSync() : null
	const uniPlatform = info?.uniPlatform || 'unknown'
	const platform = info?.platform || 'unknown'
	return `${uniPlatform}/${platform}`
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

const getExtFromName = (name) => {
	const clean = String(name || '').split('?')[0].split('#')[0]
	return clean.includes('.') ? clean.split('.').pop().toLowerCase() : ''
}

const normalizePickedModelFile = (pickedRes) => {
	const tempFiles = pickedRes?.tempFiles || []
	const first = tempFiles[0] || {}
	const path = first.path || first.tempFilePath || (pickedRes?.tempFilePaths || [])[0] || ''
	const name = first.name || (path ? path.split('/').pop() : '')
	return { path, name }
}

const chooseByApi = (apiName, options) => {
	return new Promise((resolve, reject) => {
		uni[apiName]({
			...options,
			success: (res) => resolve(res),
			fail: (err) => reject(err)
		})
	})
}

const withRetryWithoutExtension = async (apiName, baseOptions) => {
	try {
		return await chooseByApi(apiName, baseOptions)
	} catch (firstError) {
		if (String(firstError?.errMsg || '').includes('cancel')) {
			throw firstError
		}
		const retryOptions = { ...baseOptions }
		delete retryOptions.extension
		return chooseByApi(apiName, retryOptions)
	}
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
	const extensions = MODEL_EXTENSIONS.slice(0, 7) // stl, obj, fbx, gltf, glb, 3mf, step
	if (typeof uni.chooseMessageFile === 'function') {
		const chooseRes = await withRetryWithoutExtension('chooseMessageFile', {
			count: 1,
			type: 'file',
			extension: extensions
		})
		return normalizePickedModelFile(chooseRes)
	}

	if (typeof uni.chooseFile === 'function') {
		const chooseRes = await withRetryWithoutExtension('chooseFile', {
			count: 1,
			extension: extensions
		})
		return normalizePickedModelFile(chooseRes)
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
		input.accept = MODEL_EXTENSIONS.map(e => `.${e}`).join(',')
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

const chooseModel = async () => {
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

		const ext = getExtFromName(fileName)
		if (ext && !MODEL_EXTENSIONS.includes(ext)) {
			uni.showToast({ title: `不支持的格式: .${ext}`, icon: 'none' })
			return
		}

		uni.showLoading({ title: '上传模型中...' })
		let url
		if (fileObject) {
			url = await uploadBountyBidAssetApi(fileObject, 'model', fileName)
		} else {
			url = await uploadBountyBidAssetApi(filePath, 'model', fileName)
		}
		form.value.files.push({ url, name: fileName || '模型文件', ext: (ext || 'stl').toUpperCase() })
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

const removeFile = (idx) => {
	form.value.files.splice(idx, 1)
}

// ==================== 参考图片 ====================

const chooseImage = () => {
	uni.chooseImage({
		count: 9 - form.value.imageUrls.length,
		sizeType: ['compressed'],
		sourceType: ['album', 'camera'],
		success: async (res) => {
			uni.showLoading({ title: '上传中...' })
			for (const path of res.tempFilePaths) {
				try {
					const url = await uploadBountyAttachmentApi(path, 'postImg')
					form.value.imageUrls.push(url)
				} catch (_) {
					uni.showToast({ title: '图片上传失败', icon: 'none' })
				}
			}
			uni.hideLoading()
		}
	})
}

const removeImage = (idx) => {
	form.value.imageUrls.splice(idx, 1)
}

const previewImage = (idx) => {
	uni.previewImage({ current: idx, urls: form.value.imageUrls })
}

// ==================== 提交 ====================

const submit = async () => {
	if (!form.value.description.trim()) {
		uni.showToast({ title: '请填写交付说明', icon: 'none' })
		return
	}
	if (form.value.files.length === 0) {
		uni.showToast({ title: '请上传至少一个模型文件', icon: 'none' })
		return
	}
	if (submitting.value) return
	submitting.value = true

	try {
		const allFileUrls = [
			...form.value.files.map(f => f.url),
			...form.value.imageUrls
		]
		await submitBountyDeliveryApi({
			taskId: taskId.value,
			description: form.value.description,
			isFinal: form.value.isFinal,
			fileUrls: allFileUrls,
			allowCommercialUse: form.value.isFinal === 1 ? form.value.allowCommercialUse : 0,
			allowModification: form.value.isFinal === 1 ? form.value.allowModification : 1,
			licenseType: form.value.isFinal === 1 ? form.value.licenseType : 'Personal'
		})
		uni.showToast({ title: '交付提交成功', icon: 'success' })
		setTimeout(() => { uni.navigateBack() }, 1500)
	} catch (error) {
		uni.showToast({ title: error?.message || '提交失败', icon: 'none' })
	} finally {
		submitting.value = false
	}
}

onLoad((options) => {
	if (options?.taskId) {
		taskId.value = options.taskId
	}
})
</script>

<style scoped lang="scss">
$primary: #00bfff;
$light: #5ce1ff;
$deep: #0099cc;
$danger: #ff4d6d;
$bg: #f8f8f8;
$card: #ffffff;
$text-primary: #1a2030;
$text-secondary: #5a6a7a;
$text-muted: #8a9aaa;
$gradient: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
$shadow-card: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);

@keyframes fadeInUp {
	from { opacity: 0; transform: translateY(24rpx); }
	to { opacity: 1; transform: translateY(0); }
}

.delivery-container {
	min-height: 100vh;
	background-color: $bg;
	padding-bottom: 140rpx;
}

.section {
	margin: 28rpx 32rpx;
	padding: 32rpx;
	background-color: $card;
	border-radius: 24rpx;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.4s ease-out both;
	.sec-title {
		font-size: 30rpx; font-weight: 700; color: $text-primary; margin-bottom: 16rpx;
		.required { color: $danger; font-weight: 700; }
	}
	.sec-hint { font-size: 24rpx; color: $text-muted; margin-bottom: 20rpx; display: block; line-height: 1.5; }
}

.desc-input {
	width: 100%;
	min-height: 220rpx;
	padding: 24rpx;
	font-size: 28rpx;
	color: $text-primary;
	background-color: $bg;
	border-radius: 20rpx;
	box-sizing: border-box;
}

.char-count {
	font-size: 22rpx;
	color: $text-muted;
	text-align: right;
	display: block;
	margin-top: 10rpx;
}

.file-list {
	display: flex;
	flex-direction: column;
	gap: 16rpx;
	.file-item {
		display: flex;
		align-items: center;
		gap: 16rpx;
		padding: 24rpx;
		background: rgba(0, 191, 255, 0.04);
		border-radius: 20rpx;
		.file-icon { font-size: 36rpx; }
		.file-info { flex: 1; overflow: hidden; }
		.file-name {
			font-size: 28rpx; color: $text-primary; display: block;
			overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
		}
		.file-ext {
			font-size: 20rpx; color: $primary;
			background: rgba(0, 191, 255, 0.1);
			padding: 4rpx 16rpx; border-radius: 999rpx;
			display: inline-block; margin-top: 8rpx;
		}
		.remove-btn {
			width: 44rpx; height: 44rpx; background-color: $danger; color: #fff;
			border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 28rpx;
			flex-shrink: 0;
		}
	}
	.add-file {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 48rpx;
		background-color: $bg;
		border: 2rpx dashed rgba(0,191,255,0.3);
		border-radius: 20rpx;
		.add-icon { font-size: 48rpx; color: $primary; }
		.add-text { font-size: 26rpx; color: $primary; margin-top: 8rpx; font-weight: 500; }
		&:active { background: rgba(0,191,255,0.04); }
	}
}

.image-list {
	display: flex; flex-wrap: wrap; gap: 16rpx;
	.image-item {
		position: relative; width: 140rpx; height: 140rpx;
		image {
			width: 100%; height: 100%; border-radius: 16rpx;
			opacity: 0; animation: fadeInUp 0.3s ease-out forwards;
		}
		.remove-btn {
			position: absolute; top: -10rpx; right: -10rpx;
			width: 40rpx; height: 40rpx; background-color: $danger; color: #fff;
			border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 24rpx;
		}
	}
	.add-image {
		width: 140rpx; height: 140rpx; background-color: $bg;
		border: 2rpx dashed rgba(0,191,255,0.25); border-radius: 16rpx;
		display: flex; align-items: center; justify-content: center;
		text { font-size: 52rpx; color: $text-muted; }
		&:active { background: rgba(0,191,255,0.04); }
	}
}

.toggle-row {
	display: flex; align-items: center; gap: 16rpx; margin-bottom: 24rpx;
	.checkbox {
		width: 40rpx; height: 40rpx; border: 2rpx solid rgba(0,0,0,0.12); border-radius: 10rpx;
		display: flex; align-items: center; justify-content: center;
		transition: all 0.2s;
		&.checked {
			background-color: $primary; border-color: $primary;
			text { color: #fff; font-size: 24rpx; }
		}
	}
	text { font-size: 28rpx; color: $text-secondary; }
}

.license-section {
	margin-top: 12rpx;
	padding-top: 24rpx;
}

.form-row {
	display: flex; align-items: center; justify-content: space-between; margin-bottom: 20rpx;
	.label { font-size: 28rpx; color: $text-secondary; }
	.picker-val { font-size: 28rpx; color: $text-primary; font-weight: 500; }
}

.bottom-bar {
	position: fixed; bottom: 0; left: 0; right: 0;
	background: rgba(255,255,255,0.72);
	backdrop-filter: blur(24px);
	padding: 20rpx 32rpx;
	padding-bottom: calc(env(safe-area-inset-bottom) + 20rpx);
	box-shadow: 0 -4rpx 24rpx rgba(0, 0, 0, 0.06);
	.submit-btn {
		width: 100%; height: 84rpx; border-radius: 999rpx; font-size: 30rpx;
		font-weight: 600; color: #fff; background: $gradient; border: none;
		&:active { transform: scale(0.96); }
		&[disabled] { opacity: 0.4; }
	}
}
</style>
