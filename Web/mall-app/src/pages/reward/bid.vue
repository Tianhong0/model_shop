<template>
	<view class="bid-page">
		<view class="card header-card">
			<view class="title">{{ isEditMode ? '修改竞标方案' : '提交竞标方案' }}</view>
			<view class="meta">任务：{{ taskTitle || ('#' + taskId) }}</view>
			<view class="meta">参考预算：￥{{ taskPrice }}</view>
			<view class="status-row" v-if="bidStatusText">
				<text class="status-label">当前竞标状态</text>
				<text class="status-pill" :class="bidStatusClass">{{ bidStatusText }}</text>
			</view>
			<view class="status-tip" v-if="bidStatusTip">{{ bidStatusTip }}</view>
		</view>

		<view class="card form-card" v-if="canOperate">
			<view class="row">
				<text class="label">报价金额</text>
				<input class="input" type="digit" v-model="form.quoteAmount" placeholder="请输入报价" />
			</view>
			<view class="row">
				<text class="label">交付天数</text>
				<input class="input" type="number" v-model="form.deliveryDays" placeholder="如 7" />
			</view>
			<view class="desc-wrap">
				<textarea class="textarea" v-model="form.proposal" placeholder="请输入竞标说明、技术方案、交付内容"></textarea>
			</view>

			<view class="upload-actions">
				<button class="btn" @click="chooseImage">上传图片</button>
				<button class="btn" @click="chooseVideo">上传视频</button>
				<button class="btn" @click="chooseModel">上传模型文件</button>
			</view>

			<view class="asset-list" v-if="form.assets.length">
				<view class="asset-item" v-for="(asset, idx) in form.assets" :key="asset.url + idx" @click="previewAsset(asset, form.assets, idx)">
					<image v-if="asset.type === 'image'" class="asset-image" :src="asset.url" mode="aspectFill" />
					<view v-else class="asset-file">
						<text class="asset-type">{{ asset.type === 'video' ? '视频' : '模型' }}</text>
						<text class="asset-name">{{ asset.name || '点击查看' }}</text>
					</view>
					<view class="asset-remove" @click.stop="removeAsset(idx)">×</view>
				</view>
			</view>
		</view>

		<view class="card blocked" v-else>
			<text>{{ blockedText }}</text>
		</view>

		<view class="submit-bar" v-if="canOperate">
			<button class="submit-btn" @click="submitBid">{{ isEditMode ? '保存修改' : '提交竞标申请' }}</button>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getStoredUserRole, isDesignerRole } from '../../utils/role'
import {
	createBountyBidApi,
	getBountyTaskDetailApi,
	updateBountyBidApi,
	uploadBountyBidAssetApi
} from '../../api/reward'

const BID_ASSET_MARK = '[BID_ASSETS]'
const BID_STATUS_SUBMITTED = 0
const BID_STATUS_WINNER = 1
const BID_STATUS_LOST = 2
const BID_STATUS_WITHDRAWN = 3
const ENABLE_PICKER_DEBUG = typeof process !== 'undefined' && process.env && process.env.NODE_ENV !== 'production'

const debugPicker = (...args) => {
	if (!ENABLE_PICKER_DEBUG) return
	console.log('[reward-bid-picker]', ...args)
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

const taskId = ref('')
const bidId = ref('')
const taskTitle = ref('')
const taskPrice = ref(0)
const taskStatus = ref(0)
const designerBid = ref(null)

const isDesigner = computed(() => isDesignerRole(getStoredUserRole()))
const isEditMode = computed(() => Boolean(bidId.value))
const hasActiveBid = computed(() => {
	if (!designerBid.value) return false
	return Number(designerBid.value.status) !== BID_STATUS_WITHDRAWN
})
const canOperate = computed(() => {
	if (!isDesigner.value) return false
	if (taskStatus.value !== 1) return false
	if (isEditMode.value) {
		return Boolean(designerBid.value && Number(designerBid.value.status) === BID_STATUS_SUBMITTED)
	}
	return !hasActiveBid.value
})
const blockedText = computed(() => {
	if (!isDesigner.value) return '仅设计者可提交竞标方案'
	if (taskStatus.value !== 1) return '当前任务状态不可提交竞标'
	if (isEditMode.value && (!designerBid.value || Number(designerBid.value.status) !== BID_STATUS_SUBMITTED)) {
		return '当前竞标不可修改'
	}
	if (!isEditMode.value && hasActiveBid.value) return '你已提交竞标，请在详情页修改或撤回'
	return '当前不可操作'
})
const bidStatusText = computed(() => {
	const status = Number(designerBid.value?.status)
	if (!Number.isFinite(status)) return ''
	if (status === BID_STATUS_WINNER) return '已中标'
	if (status === BID_STATUS_LOST) return '未中标'
	if (status === BID_STATUS_WITHDRAWN) return '已撤回'
	return '已提交'
})
const bidStatusClass = computed(() => {
	const status = Number(designerBid.value?.status)
	if (status === BID_STATUS_WINNER) return 'winner'
	if (status === BID_STATUS_LOST) return 'lost'
	if (status === BID_STATUS_WITHDRAWN) return 'withdrawn'
	return 'submitted'
})
const bidStatusTip = computed(() => {
	const status = Number(designerBid.value?.status)
	if (!Number.isFinite(status)) return ''
	if (status === BID_STATUS_SUBMITTED) return '已提交，可在招募中状态下继续修改或撤回。'
	if (status === BID_STATUS_WINNER) return '该方案已被选为中标方案。'
	if (status === BID_STATUS_LOST) return '该方案未中标，可参与新的悬赏任务。'
	if (status === BID_STATUS_WITHDRAWN) return '该方案已撤回，你可以重新提交新的竞标方案。'
	return ''
})

const form = ref({
	quoteAmount: '',
	deliveryDays: 7,
	proposal: '',
	assets: []
})

const parseBidProposal = (rawProposal) => {
	const text = String(rawProposal || '')
	const index = text.indexOf(BID_ASSET_MARK)
	if (index < 0) {
		return { plan: text, assets: [] }
	}
	const plan = text.slice(0, index).trim()
	const payload = text.slice(index + BID_ASSET_MARK.length).trim()
	try {
		const assets = JSON.parse(payload)
		if (!Array.isArray(assets)) {
			return { plan: plan || text, assets: [] }
		}
		return {
			plan: plan || '（含附件）',
			assets: assets
				.filter(item => item && typeof item.url === 'string' && item.url)
				.map(item => ({
					url: item.url,
					type: item.type === 'video' || item.type === 'model' ? item.type : 'image',
					name: item.name || ''
				}))
		}
	} catch (_) {
		return { plan: text, assets: [] }
	}
}

const buildBidProposalPayload = (proposalText, assets) => {
	const normalizedAssets = (assets || []).map(item => ({
		url: item.url,
		type: item.type,
		name: item.name || ''
	}))
	return `${proposalText}\n${BID_ASSET_MARK}${JSON.stringify(normalizedAssets)}`
}

const loadTaskDetail = async () => {
	const data = await getBountyTaskDetailApi(taskId.value)
	taskTitle.value = data?.title || ''
	taskPrice.value = Number(data?.finalAmount || data?.budgetAmount || 0)
	taskStatus.value = Number(data?.status ?? 0)
	const userId = String((uni.getStorageSync('user_profile') || {}).id || '')
	const ownBid = (data?.bids || []).find(item => String(item?.designerId || '') === userId) || null
	designerBid.value = ownBid

	if (ownBid && (isEditMode.value || !form.value.proposal)) {
		const parsed = parseBidProposal(ownBid.proposal)
		form.value = {
			quoteAmount: ownBid.quoteAmount,
			deliveryDays: ownBid.deliveryDays || 7,
			proposal: parsed.plan,
			assets: parsed.assets
		}
	}
}

onLoad(async (options) => {
	taskId.value = String(options?.taskId || '')
	bidId.value = String(options?.bidId || '')
	if (!taskId.value) {
		uni.showToast({ title: '参数错误', icon: 'none' })
		setTimeout(() => uni.navigateBack(), 300)
		return
	}
	try {
		await loadTaskDetail()
	} catch (error) {
		uni.showToast({ title: error?.message || '加载失败', icon: 'none' })
	}
})

const uploadAsset = async (filePath, type, name = '') => {
	const url = await uploadBountyBidAssetApi(filePath, type)
	form.value.assets.push({ url, type, name })
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
			const requestCode = 30001
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

const chooseImage = async () => {
	try {
		const chooseRes = await new Promise((resolve, reject) => {
			uni.chooseImage({
				count: 6,
				sizeType: ['compressed'],
				sourceType: ['album', 'camera'],
				success: resolve,
				fail: reject
			})
		})
		const filePaths = chooseRes?.tempFilePaths || []
		if (!filePaths.length) return
		uni.showLoading({ title: '上传图片中...' })
		for (const filePath of filePaths) {
			await uploadAsset(filePath, 'image')
		}
		uni.hideLoading()
		uni.showToast({ title: '图片上传成功', icon: 'success' })
	} catch (error) {
		uni.hideLoading()
		if (String(error?.errMsg || '').includes('cancel')) return
		uni.showToast({ title: error?.message || '图片上传失败', icon: 'none' })
	}
}

const chooseVideo = async () => {
	try {
		const chooseRes = await new Promise((resolve, reject) => {
			uni.chooseVideo({
				sourceType: ['album', 'camera'],
				compressed: true,
				maxDuration: 60,
				success: resolve,
				fail: reject
			})
		})
		const filePath = chooseRes?.tempFilePath
		if (!filePath) return
		uni.showLoading({ title: '上传视频中...' })
		await uploadAsset(filePath, 'video')
		uni.hideLoading()
		uni.showToast({ title: '视频上传成功', icon: 'success' })
	} catch (error) {
		uni.hideLoading()
		if (String(error?.errMsg || '').includes('cancel')) return
		uni.showToast({ title: error?.message || '视频上传失败', icon: 'none' })
	}
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
			debugPicker('picked-by-uni', { filePath, fileName })
		} catch (uniPickError) {
			debugPicker('uni-pick-failed', uniPickError)
			if (String(uniPickError?.errMsg || '').includes('cancel')) {
				return
			}
			if (!canUseWebInput()) {
				throw uniPickError
			}
			const picked = await pickModelFileByWebInput()
			fileObject = picked.file
			fileName = picked.name || ''
			debugPicker('picked-by-web', { fileName })
		}

		if (!filePath && !fileObject) {
			uni.showToast({ title: '未选择模型文件', icon: 'none' })
			return
		}
		uni.showLoading({ title: '上传模型中...' })
		if (fileObject) {
			const url = await uploadBountyBidAssetApi(fileObject, 'model', fileName)
			form.value.assets.push({ url, type: 'model', name: fileName })
		} else {
			await uploadAsset(filePath, 'model', fileName)
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
			? `当前运行平台(${platform})不支持模型文件选择接口，请在H5或微信小程序中上传，或先上传图片/视频。`
			: `模型文件选择失败：${reason}`
		uni.showModal({
			title: '上传失败',
			content: supportHint,
			showCancel: false
		})
	}
}

const removeAsset = (index) => {
	form.value.assets.splice(index, 1)
}

const parseModelType = (asset = {}) => {
	const rawUrl = String(asset.url || '')
	const cleanUrl = rawUrl.split('?')[0].split('#')[0]
	const ext = cleanUrl.includes('.') ? cleanUrl.split('.').pop().toLowerCase() : ''
	if (ext) return ext
	const name = String(asset.name || '').toLowerCase()
	if (name.includes('.')) {
		return name.split('.').pop() || 'stl'
	}
	return 'stl'
}

const openModelPreviewPage = (asset) => {
	const modelUrl = encodeURIComponent(String(asset.url || ''))
	const modelName = encodeURIComponent(String(asset.name || '模型文件'))
	const modelType = encodeURIComponent(parseModelType(asset))
	uni.navigateTo({
		url: `/pages/reward/model-preview?url=${modelUrl}&name=${modelName}&type=${modelType}`
	})
}

const openVideoPreviewPage = (asset) => {
	const videoUrl = encodeURIComponent(String(asset.url || ''))
	const videoName = encodeURIComponent(String(asset.name || '视频附件'))
	uni.navigateTo({
		url: `/pages/reward/video-preview?url=${videoUrl}&name=${videoName}`
	})
}

const previewAsset = (asset, list, index) => {
	if (asset.type === 'image') {
		uni.previewImage({
			current: index,
			urls: list.filter(item => item.type === 'image').map(item => item.url)
		})
		return
	}
	if (asset.type === 'model') {
		openModelPreviewPage(asset)
		return
	}
	if (asset.type === 'video') {
		openVideoPreviewPage(asset)
		return
	}
	uni.showModal({
		title: asset.type === 'video' ? '视频链接' : '模型文件链接',
		content: asset.url,
		confirmText: '复制链接',
		success: (res) => {
			if (res.confirm) {
				uni.setClipboardData({ data: asset.url })
			}
		}
	})
}

const submitBid = async () => {
	if (!canOperate.value) {
		uni.showToast({ title: blockedText.value, icon: 'none' })
		return
	}
	const quoteAmount = Number(form.value.quoteAmount || taskPrice.value)
	const deliveryDays = Number(form.value.deliveryDays || 7)
	const proposalText = String(form.value.proposal || '').trim()
	if (!quoteAmount || quoteAmount <= 0) {
		uni.showToast({ title: '请输入有效报价', icon: 'none' })
		return
	}
	if (!proposalText) {
		uni.showToast({ title: '请填写竞标方案', icon: 'none' })
		return
	}
	if (!form.value.assets.length) {
		uni.showToast({ title: '请至少上传1个附件（图片/视频/模型）', icon: 'none' })
		return
	}
	const normalizedTaskId = String(taskId.value || '').trim()
	const normalizedBidId = String(bidId.value || '').trim()
	if (!normalizedTaskId) {
		uni.showToast({ title: '任务ID异常，请返回重试', icon: 'none' })
		return
	}
	try {
		if (isEditMode.value) {
			if (!normalizedBidId) {
				uni.showToast({ title: '竞标ID异常，请返回重试', icon: 'none' })
				return
			}
			await updateBountyBidApi({
				bidId: normalizedBidId,
				quoteAmount,
				deliveryDays,
				proposal: buildBidProposalPayload(proposalText, form.value.assets)
			})
			uni.showToast({ title: '竞标方案已更新', icon: 'success' })
		} else {
			await createBountyBidApi({
				taskId: normalizedTaskId,
				quoteAmount,
				deliveryDays,
				proposal: buildBidProposalPayload(proposalText, form.value.assets)
			})
			uni.showToast({ title: '竞标提交成功', icon: 'success' })
		}
		setTimeout(() => uni.navigateBack(), 400)
	} catch (error) {
		uni.showToast({ title: error?.message || '提交失败', icon: 'none' })
	}
}
</script>

<style scoped lang="scss">
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

@keyframes fadeInUp {
	from { opacity: 0; transform: translateY(24rpx); }
	to { opacity: 1; transform: translateY(0); }
}
@keyframes breathGlow {
	0%, 100% { box-shadow: 0 0 12rpx rgba(0,191,255,0.15); }
	50% { box-shadow: 0 0 24rpx rgba(0,191,255,0.35); }
}

.bid-page {
	min-height: 100vh;
	background-color: $bg;
	padding: 24rpx 32rpx 160rpx;
}

.card {
	background-color: $card;
	border-radius: 24rpx;
	padding: 32rpx;
	margin-bottom: 28rpx;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.4s ease-out both;
}

.header-card {
	.title { font-size: 30rpx; font-weight: 700; color: $text-primary; }
	.meta { font-size: 26rpx; color: $text-secondary; margin-top: 10rpx; display: block; }
}

.status-row {
	margin-top: 16rpx;
	display: flex;
	align-items: center;
	gap: 12rpx;
}

.status-label {
	font-size: 24rpx;
	color: $text-secondary;
}

.status-pill {
	font-size: 20rpx;
	padding: 4rpx 16rpx;
	border-radius: 999rpx;
	font-weight: 500;
	&.submitted { background-color: rgba(0, 191, 255, 0.1); color: $primary; animation: breathGlow 2s ease-in-out infinite; }
	&.winner { background-color: rgba(16,185,129,0.1); color: $success; }
	&.lost { background-color: rgba(255,77,109,0.1); color: $danger; }
	&.withdrawn { background-color: $bg; color: $text-secondary; }
}

.status-tip {
	margin-top: 10rpx;
	font-size: 24rpx;
	color: $text-secondary;
	line-height: 1.5;
}

.row {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 16rpx 0;
	& + .row {
		border-top: 1rpx solid rgba(0,0,0,0.04);
	}
}

.label {
	font-size: 28rpx;
	color: $text-secondary;
}

.input {
	flex: 1;
	text-align: right;
	font-size: 28rpx;
	color: $text-primary;
}

.desc-wrap {
	margin-top: 20rpx;
}

.textarea {
	width: 100%;
	min-height: 200rpx;
	background-color: $bg;
	border-radius: 16rpx;
	padding: 20rpx;
	font-size: 26rpx;
	box-sizing: border-box;
	color: $text-primary;
}

.upload-actions {
	margin-top: 20rpx;
	display: flex;
	gap: 14rpx;
	.btn {
		flex: 1;
		height: 76rpx;
		line-height: 76rpx;
		font-size: 24rpx;
		border-radius: 999rpx;
		background-color: rgba(0, 191, 255, 0.08);
		color: $primary;
		font-weight: 500;
		&:active { transform: scale(0.96); }
	}
}

.asset-list {
	margin-top: 20rpx;
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx;
}

.asset-item {
	position: relative;
	width: 140rpx;
	height: 140rpx;
	border-radius: 16rpx;
	overflow: hidden;
	background-color: rgba(0, 191, 255, 0.04);
}

.asset-image {
	width: 100%;
	height: 100%;
}

.asset-file {
	display: flex;
	flex-direction: column;
	justify-content: center;
	height: 100%;
	padding: 12rpx;
}

.asset-type {
	font-size: 20rpx;
	color: $primary;
	font-weight: 500;
}

.asset-name {
	font-size: 20rpx;
	color: $text-secondary;
	margin-top: 6rpx;
}

.asset-remove {
	position: absolute;
	top: 6rpx;
	right: 6rpx;
	width: 36rpx;
	height: 36rpx;
	line-height: 36rpx;
	text-align: center;
	border-radius: 50%;
	background: rgba(15, 23, 42, 0.55);
	color: #fff;
	font-size: 24rpx;
}

.blocked {
	font-size: 26rpx;
	color: $danger;
	text-align: center;
	padding: 40rpx 0;
}

.submit-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(255,255,255,0.72);
	backdrop-filter: blur(24px);
	padding: 20rpx 32rpx;
	padding-bottom: calc(env(safe-area-inset-bottom) + 20rpx);
	box-shadow: 0 -4rpx 24rpx rgba(0,0,0,0.06);
}

.submit-btn {
	height: 88rpx;
	line-height: 88rpx;
	border-radius: 999rpx;
	font-size: 30rpx;
	font-weight: 600;
	background: $gradient;
	color: #fff;
	&:active { transform: scale(0.96); }
}
</style>
