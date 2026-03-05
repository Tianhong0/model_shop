<template>
	<view class="preview-page">
		<view class="viewer-card">
			<ThreeDViewer
				class="three-viewer"
				:modelUrl="modelUrl"
				:modelType="modelType"
				:modelColor="modelColor"
				:materialType="materialType"
				:autoRotate="isAutoRotate"
			/>
			<view class="badge-3d" @click="toggleRotation">
				<uni-icons :type="isAutoRotate ? 'refresh-filled' : 'eye'" size="16" color="#fff" :class="{ spin: isAutoRotate }"></uni-icons>
				<text>{{ isAutoRotate ? '自动旋转中' : '点击开启旋转' }}</text>
			</view>
		</view>

		<view class="info-card">
			<view class="name">{{ modelName || '模型文件预览' }}</view>
			<view class="meta">格式：{{ modelType.toUpperCase() }}</view>
			<view class="color-row">
				<view class="color-preview-circle" :style="{ backgroundColor: previewColorHex }"></view>
				<text class="color-value">当前颜色 {{ previewColorHex }}</text>
				<view class="color-picker-btn" @click="openColorPanel">选择</view>
			</view>
		</view>

		<view v-if="showColorPanel" class="color-popup-mask" @click="closeColorPanel">
			<view class="color-popup" @click.stop>
				<view class="popup-title">RGB 颜色设置</view>
				<view class="popup-preview-row">
					<view class="color-preview-circle large" :style="{ backgroundColor: popupPreviewColorHex }"></view>
					<text class="popup-color-text">{{ popupPreviewColorHex }}</text>
				</view>
				<view class="popup-slider-item">
					<text class="popup-label">R</text>
					<slider :value="tempRgbColor.r" :min="0" :max="255" @change="onPopupRgbChange('r', $event)" activeColor="#ef4444" block-size="16" />
					<input
						class="popup-input"
						type="number"
						:value="String(tempRgbColor.r)"
						@input="onPopupRgbInput('r', $event)"
						@blur="onPopupRgbBlur('r', $event)"
					/>
				</view>
				<view class="popup-slider-item">
					<text class="popup-label">G</text>
					<slider :value="tempRgbColor.g" :min="0" :max="255" @change="onPopupRgbChange('g', $event)" activeColor="#22c55e" block-size="16" />
					<input
						class="popup-input"
						type="number"
						:value="String(tempRgbColor.g)"
						@input="onPopupRgbInput('g', $event)"
						@blur="onPopupRgbBlur('g', $event)"
					/>
				</view>
				<view class="popup-slider-item">
					<text class="popup-label">B</text>
					<slider :value="tempRgbColor.b" :min="0" :max="255" @change="onPopupRgbChange('b', $event)" activeColor="#3b82f6" block-size="16" />
					<input
						class="popup-input"
						type="number"
						:value="String(tempRgbColor.b)"
						@input="onPopupRgbInput('b', $event)"
						@blur="onPopupRgbBlur('b', $event)"
					/>
				</view>
				<view class="popup-actions">
					<view class="popup-btn cancel" @click="closeColorPanel">取消</view>
					<view class="popup-btn confirm" @click="confirmColorPanel">确定</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ThreeDViewer from '@/components/ThreeDViewer.vue'

const modelUrl = ref('')
const modelType = ref('stl')
const modelName = ref('')
const isAutoRotate = ref(true)
const materialType = ref('standard')
const rgbColor = ref({ r: 79, g: 70, b: 229 })
const tempRgbColor = ref({ r: 79, g: 70, b: 229 })
const showColorPanel = ref(false)

const clamp = (value, min, max) => {
	if (value < min) return min
	if (value > max) return max
	return value
}

const rgbToHex = (rgb = {}) => {
	const r = clamp(Math.round(Number(rgb.r || 79)), 0, 255)
	const g = clamp(Math.round(Number(rgb.g || 70)), 0, 255)
	const b = clamp(Math.round(Number(rgb.b || 229)), 0, 255)
	const toHex = (num) => num.toString(16).padStart(2, '0').toUpperCase()
	return `#${toHex(r)}${toHex(g)}${toHex(b)}`
}

const modelColor = computed(() => rgbToHex(rgbColor.value))
const previewColorHex = computed(() => modelColor.value)
const popupPreviewColorHex = computed(() => rgbToHex(tempRgbColor.value))

const parseModelType = (url, explicitType) => {
	const rawType = String(explicitType || '').trim().toLowerCase()
	if (rawType) return rawType
	const cleanUrl = String(url || '').split('?')[0].split('#')[0]
	if (!cleanUrl.includes('.')) return 'stl'
	const ext = cleanUrl.split('.').pop().toLowerCase()
	return ext || 'stl'
}

const toggleRotation = () => {
	isAutoRotate.value = !isAutoRotate.value
}

const openColorPanel = () => {
	tempRgbColor.value = { ...rgbColor.value }
	showColorPanel.value = true
}

const closeColorPanel = () => {
	showColorPanel.value = false
}

const confirmColorPanel = () => {
	rgbColor.value = { ...tempRgbColor.value }
	showColorPanel.value = false
}

const onPopupRgbChange = (channel, event) => {
	const rawValue = Number(event?.detail?.value || 0)
	const next = clamp(rawValue, 0, 255)
	tempRgbColor.value = {
		...tempRgbColor.value,
		[channel]: Math.round(next)
	}
}

const sanitizeRgbInput = (value) => {
	const digits = String(value || '').replace(/\D/g, '')
	if (!digits) return ''
	return digits.slice(0, 3)
}

const onPopupRgbInput = (channel, event) => {
	const sanitized = sanitizeRgbInput(event?.detail?.value)
	if (sanitized === '') {
		tempRgbColor.value = {
			...tempRgbColor.value,
			[channel]: 0
		}
		return
	}
	const next = clamp(Number(sanitized), 0, 255)
	tempRgbColor.value = {
		...tempRgbColor.value,
		[channel]: Math.round(next)
	}
}

const onPopupRgbBlur = (channel, event) => {
	const sanitized = sanitizeRgbInput(event?.detail?.value)
	const next = clamp(Number(sanitized || 0), 0, 255)
	tempRgbColor.value = {
		...tempRgbColor.value,
		[channel]: Math.round(next)
	}
}

onLoad((options) => {
	const rawUrl = decodeURIComponent(String(options?.url || '').trim())
	const rawName = decodeURIComponent(String(options?.name || '').trim())
	const rawType = decodeURIComponent(String(options?.type || '').trim())

	if (!rawUrl) {
		uni.showToast({ title: '模型地址缺失', icon: 'none' })
		setTimeout(() => uni.navigateBack(), 300)
		return
	}

	modelUrl.value = rawUrl
	modelName.value = rawName
	modelType.value = parseModelType(rawUrl, rawType)
})
</script>

<style scoped lang="scss">
.preview-page {
	min-height: 100vh;
	background-color: #f8fafc;
	padding: 24rpx;
}

.viewer-card {
	position: relative;
	background: #fff;
	border-radius: 20rpx;
	overflow: hidden;
	box-shadow: 0 8rpx 20rpx rgba(15, 23, 42, 0.06);
	margin-bottom: 20rpx;
}

.three-viewer {
	width: 100%;
	height: 760rpx;
	display: block;
}

.badge-3d {
	position: absolute;
	right: 20rpx;
	top: 20rpx;
	padding: 10rpx 18rpx;
	border-radius: 999rpx;
	background: rgba(15, 23, 42, 0.65);
	display: flex;
	align-items: center;
	gap: 8rpx;
	color: #fff;
	font-size: 22rpx;
	z-index: 2;

	.spin {
		animation: spin 1.2s linear infinite;
	}
}

.info-card {
	background: #fff;
	border-radius: 20rpx;
	padding: 20rpx 24rpx;

	.name {
		font-size: 30rpx;
		font-weight: 600;
		color: #1e293b;
	}

	.meta {
		margin-top: 8rpx;
		font-size: 24rpx;
		color: #64748b;
	}

	.color-row {
		margin-top: 20rpx;
		display: flex;
		align-items: center;
		gap: 14rpx;
	}

	.color-preview-circle {
		width: 48rpx;
		height: 48rpx;
		border-radius: 50%;
		border: 2rpx solid #e2e8f0;
		flex-shrink: 0;

		&.large {
			width: 72rpx;
			height: 72rpx;
		}
	}

	.color-value {
		font-size: 24rpx;
		color: #334155;
		flex: 1;
	}

	.color-picker-btn {
		padding: 8rpx 20rpx;
		border-radius: 999rpx;
		background: #eef2ff;
		color: #4f46e5;
		font-size: 22rpx;
	}
}

.color-popup-mask {
	position: fixed;
	inset: 0;
	background: rgba(15, 23, 42, 0.42);
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 24rpx;
	z-index: 20;
}

.color-popup {
	width: 100%;
	max-width: 680rpx;
	background: #fff;
	border-radius: 20rpx;
	padding: 24rpx;
}

.popup-title {
	font-size: 30rpx;
	font-weight: 600;
	color: #1e293b;
}

.popup-preview-row {
	margin-top: 20rpx;
	display: flex;
	align-items: center;
	gap: 14rpx;
}

.popup-color-text {
	font-size: 24rpx;
	color: #334155;
}

.popup-slider-item {
	margin-top: 18rpx;
	display: flex;
	align-items: center;
	gap: 10rpx;

	slider {
		flex: 1;
	}
}

.popup-label {
	width: 30rpx;
	font-size: 24rpx;
	color: #334155;
}

.popup-input {
	width: 88rpx;
	height: 56rpx;
	border-radius: 12rpx;
	background: #f1f5f9;
	text-align: center;
	font-size: 22rpx;
	color: #334155;
}

.popup-actions {
	margin-top: 22rpx;
	display: flex;
	gap: 12rpx;
}

.popup-btn {
	flex: 1;
	height: 72rpx;
	line-height: 72rpx;
	border-radius: 14rpx;
	text-align: center;
	font-size: 26rpx;

	&.cancel {
		background: #f1f5f9;
		color: #475569;
	}

	&.confirm {
		background: #4f46e5;
		color: #fff;
	}
}

@keyframes spin {
	from { transform: rotate(0deg); }
	to { transform: rotate(360deg); }
}
</style>