<template>
	<view class="detail-container">
		<scroll-view scroll-y class="detail-scroll">
			<view class="preview-section">
				<template v-if="!showReal3D">
					<swiper class="main-swiper" circular :indicator-dots="modelImages.length > 1" autoplay interval="3000" duration="500">
						<swiper-item v-for="(img, index) in modelImages" :key="index">
							<image :src="img" mode="aspectFill" class="main-img"></image>
						</swiper-item>
					</swiper>
					<view class="preview-entry" @click.stop="handlePreviewClick">
						<uni-icons type="eye" size="16" color="#1a2030"></uni-icons>
						<text>点击进入交互式 3D 预览</text>
					</view>
					<view class="preview-tags">
						<view class="tag">{{ modelTypeTag }}</view>
						<view class="tag">{{ modelFileSizeTag }}</view>
					</view>
				</template>
				<template v-else>
					<ThreeDViewer
						class="three-viewer"
						:modelUrl="modelInfo.modelUrl"
						:modelType="modelInfo.modelType"
						:modelColor="selectedColorValue"
						:materialType="selectedMaterialType"
						:autoRotate="is3DRotating"
					/>
					<view class="preview-entry solid" @click.stop="toggleRotation">
						<uni-icons :type="is3DRotating ? 'refresh-filled' : 'eye'" size="16" color="#00bfff" :class="{ spin: is3DRotating }"></uni-icons>
						<text>{{ is3DRotating ? '自动旋转中' : '点击开启旋转' }}</text>
					</view>
					<view class="preview-close" @click.stop="showReal3D = false">
						<uni-icons type="closeempty" size="16" color="#fff"></uni-icons>
					</view>
				</template>
			</view>

			<view class="info-card">
				<text class="series-text">模型详情</text>
				<view class="title-row">
					<text class="name">{{ modelInfo.name }}</text>
					<view class="fav-btn" :class="{ active: isFavorited }" @click="toggleFavorite">
						<uni-icons :type="isFavorited ? 'heart-filled' : 'heart'" size="18" :color="isFavorited ? '#ef4444' : '#5a6a7a'"></uni-icons>
					</view>
				</view>
				<text class="desc">{{ modelInfo.desc }}</text>
				<view class="price-row">
					<view class="price-box">
						<text class="symbol">￥</text>
						<text class="price">{{ totalPrice }}</text>
					</view>
					<view class="metrics">
						<view class="metric-item">
							<text class="metric-label">体积</text>
							<text class="metric-val">{{ rawVolumeText }}mm³</text>
						</view>
						<view class="metric-item">
							<text class="metric-label">尺寸</text>
							<text class="metric-val">{{ rawSizeText }}</text>
						</view>
					</view>
				</view>
			</view>

			<view class="custom-card">
				<view class="section-title clickable" @click="toggleParamExpand">
					<view class="section-title-left">
						<view class="dot"></view>
						<text>模型定制</text>
					</view>
					<uni-icons :type="isParamExpanded ? 'up' : 'down'" size="16" color="#5a6a7a"></uni-icons>
				</view>

				<view v-if="isParamExpanded" class="options-wrap">
					<view class="option-panel">
						<view class="panel-head">
							<text class="label">材料</text>
						</view>
						<view class="option-list">
							<view
								v-for="(item, index) in materials"
								:key="index"
								class="option-item"
								:class="{ active: selectedMaterial === index, 'eco-material': item.isEco }"
								@click="selectedMaterial = index"
							>
								<text>{{ item.name }}</text>
								<view v-if="item.isEco" class="eco-badge">
									<uni-icons type="checkbox-filled" size="12" color="#22c55e"></uni-icons>
									<text class="eco-text">环保</text>
								</view>
							</view>
						</view>
					</view>

					<view class="option-panel">
						<view class="panel-head">
							<text class="label">颜色</text>
							<text class="sub-val">{{ selectedColorValue }}</text>
						</view>
						<view class="color-row">
							<view class="color-preview-circle" :style="{ backgroundColor: selectedColorValue }"></view>
							<text class="color-value">{{ selectedColorName }}</text>
							<view class="color-picker-btn" @click="openColorPanel">选择颜色</view>
						</view>
					</view>

					<view class="option-panel">
						<view class="panel-head">
							<text class="label">打印精度</text>
							<input
								class="num-input"
								type="digit"
								:value="precisionInput"
								@input="onPrecisionInput"
								@blur="onPrecisionInputBlur"
							/>
						</view>
						<text class="current-text">当前精度 {{ precisionLabel }}</text>
						<slider :value="precisionSliderValue" :min="5" :max="50" @change="onPrecisionChange" activeColor="#00bfff" block-size="18" />
						<view class="range-labels">
							<text>0.05mm</text>
							<text>高精细</text>
							<text>0.50mm</text>
						</view>
					</view>

					<view class="option-panel">
						<view class="panel-head">
							<text class="label">缩放比例</text>
							<text class="sub-val">{{ Math.round(scale * 100) }}%</text>
						</view>
						<slider :value="scale * 10" :min="5" :max="20" @change="onScaleChange" activeColor="#00bfff" block-size="18" />
						<view class="range-labels">
							<text>50%</text>
							<text>原始</text>
							<text>200%</text>
						</view>
					</view>

					<view class="option-panel">
						<view class="panel-head">
							<text class="label">填充密度</text>
							<input
								class="num-input"
								type="number"
								:value="fillDensityInput"
								@input="onFillDensityInput"
								@blur="onFillDensityInputBlur"
							/>
						</view>
						<text class="current-text">当前密度 {{ fillDensityLabel }}</text>
						<slider :value="fillDensity" :min="0" :max="100" @change="onFillDensityChange" activeColor="#00bfff" block-size="18" />
					</view>

					<view class="option-panel">
						<view class="panel-head">
							<text class="label">耗材线径 (mm)</text>
							<input
								class="num-input"
								type="digit"
								:value="filamentInput"
								@input="onFilamentInput"
								@blur="onFilamentInputBlur"
							/>
						</view>
						<text class="current-text">范围 {{ filamentRangeLabel }}</text>
						<slider :value="filamentSliderValue" :min="50" :max="300" @change="onFilamentSliderChange" activeColor="#00bfff" block-size="18" />
					</view>

					<view class="option-panel">
						<text class="label">备注说明</text>
						<textarea v-model="customRemark" class="remark-input" placeholder="请输入您的特殊定制要求..." />
					</view>
				</view>
			</view>

			<view class="comment-card">
				<view class="comment-header-row">
					<view class="section-title">用户评价</view>
					<view class="more-link" v-if="modelCommentTotal > 2" @click="goCommentList">查看更多</view>
				</view>
				<view v-if="commentLoading" class="comment-empty">评价加载中...</view>
				<view v-else-if="topComments.length === 0" class="comment-empty">暂无评价</view>
				<view v-else>
					<view class="comment-item" v-for="comment in topComments" :key="comment.id" @click="openCommentDetail(comment)">
						<view class="comment-head">
							<view class="comment-user-wrap">
								<image class="comment-avatar" :src="getCommentAvatar(comment)" mode="aspectFill" />
								<view class="comment-user-meta">
									<text class="comment-user">{{ getCommentDisplayName(comment) }}</text>
									<text class="comment-time">{{ formatCommentTime(comment.createTime) }}</text>
								</view>
							</view>
						</view>
						<view class="comment-score-row">
							<text class="comment-score-stars">{{ renderStars(comment.avgScore) }}</text>
							<text class="comment-score-text">{{ formatScore(comment.avgScore) }}</text>
						</view>
						<text class="comment-text">{{ comment.commentText || '用户未填写文字评价' }}</text>
						<view class="comment-media" v-if="getCommentMediaList(comment).length">
							<view class="comment-media-item" v-for="(media, idx) in getCommentMediaList(comment)" :key="`${comment.id}-${idx}`" @click.stop>
								<image
									v-if="media.type === 'image'"
									:src="media.url"
									mode="aspectFill"
									class="comment-media-thumb"
									@click="previewCommentImage(comment.commentImages, media.url)"
								/>
								<video
									v-else
									class="comment-media-video"
									:src="media.url"
									controls
									show-fullscreen-btn
									@tap="previewCommentVideo(media.url)"
									preload="metadata"
								/>
							</view>
						</view>
						<view class="comment-actions">
							<view class="comment-like" :class="{ active: isCommentLiked(comment) }" @click.stop="toggleCommentLike(comment)">
								<uni-icons :type="isCommentLiked(comment) ? 'heart-filled' : 'heart'" size="18" :color="isCommentLiked(comment) ? '#ef4444' : '#5a6a7a'" />
								<text class="comment-like-text">{{ Number(comment.likeCount || 0) }}</text>
							</view>
						</view>
					</view>
				</view>
			</view>

			<view class="footer-placeholder"></view>
		</scroll-view>

		<view class="bottom-bar">
			<view class="action-icons">
				<view class="icon-item" @click="goCustomerService">
					<uni-icons type="chat" size="22" color="#5a6a7a"></uni-icons>
					<text>客服</text>
				</view>
				<view class="icon-item" @click="goCart">
					<uni-icons type="cart" size="22" color="#5a6a7a"></uni-icons>
					<text>购物车</text>
				</view>
			</view>
			<view class="btns">
				<view class="btn group-buy" @click="goGroupBuy">参与拼团</view>
				<view class="btn add-cart" @click="addToCart">加入购物车</view>
				<view class="btn buy-now" @click="buyNow">立即下单</view>
			</view>
		</view>

		<view v-if="showColorPanel" class="color-popup-mask" @click="closeColorPanel">
			<view class="color-popup" @click.stop>
				<view class="popup-title">RGB 颜色设置</view>
				<view class="popup-preview-row">
					<view class="color-preview-circle large" :style="{ backgroundColor: previewColorHex }"></view>
					<text class="popup-color-text">{{ previewColorText }} / {{ previewColorHex }}</text>
				</view>
				<view class="popup-slider-item">
					<text class="popup-label">R</text>
					<slider :value="tempRgbColor.r" :min="0" :max="255" @change="onPopupRgbChange('r', $event)" activeColor="#ef4444" block-size="16" />
					<text class="popup-num">{{ tempRgbColor.r }}</text>
				</view>
				<view class="popup-slider-item">
					<text class="popup-label">G</text>
					<slider :value="tempRgbColor.g" :min="0" :max="255" @change="onPopupRgbChange('g', $event)" activeColor="#22c55e" block-size="16" />
					<text class="popup-num">{{ tempRgbColor.g }}</text>
				</view>
				<view class="popup-slider-item">
					<text class="popup-label">B</text>
					<slider :value="tempRgbColor.b" :min="0" :max="255" @change="onPopupRgbChange('b', $event)" activeColor="#3b82f6" block-size="16" />
					<text class="popup-num">{{ tempRgbColor.b }}</text>
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
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import ThreeDViewer from '@/components/ThreeDViewer.vue'
import { getModelDetailApi, getMyFavoriteModelIdsApi, toggleModelFavoriteApi } from '../../api/model'
import { getModelOrderCommentsApi, toggleModelCommentLikeApi, getMyOrdersApi } from '../../api/order'
import { ensureLoginOrRedirect } from '../../utils/auth'
import { getApiBaseUrl } from '../../utils/apiBase'

const modelInfo = ref({
	id: null,
	name: '模型加载中...',
	desc: '',
	image: '',
	basePrice: 0,
	baseVolume: 45,
	baseSize: '--',
	modelUrl: '',
	modelType: 'glb'
})
const modelImages = ref([])

const materialMapping = ['standard', 'physical', 'shiny']

const materials = ref([
	{ id: 0, name: '默认材质', add: 0, color: '#4f46e5' }
])
const selectedMaterial = ref(0)
const scale = ref(1.0)
const is3DRotating = ref(true)
const showReal3D = ref(false)
const customRemark = ref('')
const isParamExpanded = ref(false)
const precisionMm = ref(0.1)
const precisionInput = ref('0.10')
const fillDensity = ref(20)
const fillDensityInput = ref('20')
const filamentDiameter = ref(1.75)
const filamentInput = ref('1.75')
const rgbColor = ref({ r: 79, g: 70, b: 229 })
const tempRgbColor = ref({ r: 79, g: 70, b: 229 })
const showColorPanel = ref(false)
const isFavorited = ref(false)
const modelFileSize = ref('--')
const commentLoading = ref(false)
const modelComments = ref([])
const modelCommentTotal = ref(0)
const COMMENT_DETAIL_STORAGE_KEY = 'model_comment_detail'
const COMMENT_LIKE_STORAGE_KEY = 'model_comment_like_flags'
const DEFAULT_AVATAR = 'https://api.dicebear.com/7.x/avataaars/svg?seed=3dshop-user'
const ANONYMOUS_AVATAR = 'https://api.dicebear.com/7.x/identicon/svg?seed=anonymous'
const likedCommentMap = ref({})

const handlePreviewClick = () => {
	if (!showReal3D.value) {
		showReal3D.value = true
	}
}

const toggleParamExpand = () => {
	isParamExpanded.value = !isParamExpanded.value
}

const toggleRotation = () => {
	is3DRotating.value = !is3DRotating.value
}

onLoad((options) => {
	if (!ensureLoginOrRedirect()) return
	loadLikedCommentMap()

	const modelId = decodeURIComponent(String(options?.id || '').trim())
	if (!modelId) {
		uni.showToast({ title: '模型参数缺失', icon: 'none' })
		return
	}

	// 保存模型ID供刷新使用
	modelInfo.value.id = modelId
	loadModelDetail(modelId)
	loadFavoriteState(modelId)
})

// 下拉刷新
onPullDownRefresh(async () => {
	try {
		const modelId = modelInfo.value.id
		if (modelId) {
			// 强制刷新模型详情（跳过缓存）
			await loadModelDetail(modelId, true)
			loadFavoriteState(modelId)
			uni.showToast({ title: '刷新成功', icon: 'success' })
		}
	} catch (error) {
		uni.showToast({ title: '刷新失败', icon: 'none' })
	} finally {
		uni.stopPullDownRefresh()
	}
})

const topComments = computed(() => (Array.isArray(modelComments.value) ? modelComments.value.slice(0, 2) : []))

const isVideoMedia = (url) => {
	const lower = String(url || '').toLowerCase().split('?')[0]
	return /\.(mp4|mov|m4v|webm|ogg)$/i.test(lower) || lower.includes('/videos/')
}

const parseCommentMediaList = (raw) => {
	return String(raw || '')
		.split(',')
		.map(item => item.trim())
		.filter(Boolean)
		.map(url => {
			const mediaUrl = toAbsoluteAssetUrl(url)
			return { url: mediaUrl, type: isVideoMedia(mediaUrl) ? 'video' : 'image' }
		})
}

const getCommentMediaList = (comment) => parseCommentMediaList(comment?.commentImages)

const getCommentDisplayName = (comment) => {
	if (comment?.isMine) return '我的评价'
	if (Number(comment?.isAnonymous) === 1) return '匿名用户'
	return comment?.userNickname || '用户'
}

const getCommentAvatar = (comment) => {
	if (Number(comment?.isAnonymous) === 1 && !comment?.isMine) {
		return ANONYMOUS_AVATAR
	}
	const avatar = toAbsoluteAssetUrl(comment?.userAvatar)
	return avatar || DEFAULT_AVATAR
}

const isCommentLiked = (comment) => {
	return !!comment?.liked
}

const loadLikedCommentMap = () => {
	const stored = uni.getStorageSync(COMMENT_LIKE_STORAGE_KEY)
	if (stored && typeof stored === 'object') {
		likedCommentMap.value = stored
		return
	}
	likedCommentMap.value = {}
}

const saveLikedCommentMap = () => {
	uni.setStorageSync(COMMENT_LIKE_STORAGE_KEY, likedCommentMap.value)
}

const toStarScore = (value) => {
	const numeric = Number(value)
	if (!Number.isFinite(numeric)) return 0
	return clamp(Math.round(numeric), 0, 5)
}

const renderStars = (value) => {
	const score = toStarScore(value)
	return `${'★'.repeat(score)}${'☆'.repeat(5 - score)}`
}

const formatScore = (value) => {
	const numeric = Number(value)
	if (!Number.isFinite(numeric)) return '-'
	return numeric.toFixed(1)
}

const openCommentDetail = (comment) => {
	if (!comment) return
	uni.setStorageSync(COMMENT_DETAIL_STORAGE_KEY, comment)
	uni.navigateTo({ url: '/pages/custom/comment-detail' })
}

const toggleCommentLike = async (comment) => {
	if (!ensureLoginOrRedirect()) return
	const commentId = String(comment?.id || '').trim()
	if (!commentId) return
	try {
		const result = await toggleModelCommentLikeApi({ commentId })
		comment.liked = !!result?.active
		likedCommentMap.value = {
			...likedCommentMap.value,
			[commentId]: comment.liked
		}
		saveLikedCommentMap()
		const current = Number(comment.likeCount || 0)
		const safeCurrent = Number.isFinite(current) ? current : 0
		comment.likeCount = result?.active ? safeCurrent + 1 : Math.max(0, safeCurrent - 1)
	} catch (error) {
		uni.showToast({ title: error?.message || '操作失败', icon: 'none' })
	}
}

const goCommentList = () => {
	if (!modelInfo.value.id) return
	uni.navigateTo({ url: `/pages/custom/comment-list?modelId=${encodeURIComponent(String(modelInfo.value.id))}` })
}

const previewCommentImage = (raw, current) => {
	const urls = parseCommentMediaList(raw).filter(item => item.type === 'image').map(item => item.url)
	if (!urls.length) return
	uni.previewImage({ urls, current })
}

const previewCommentVideo = (url) => {
	const videoUrl = String(url || '').trim()
	if (!videoUrl) return
	if (typeof uni.previewMedia === 'function') {
		uni.previewMedia({
			sources: [{ url: videoUrl, type: 'video' }],
			current: 0
		})
		return
	}
	uni.showToast({ title: '当前平台不支持全屏预览', icon: 'none' })
}

const formatCommentTime = (value) => {
	if (!value) return '-'
	return String(value).replace('T', ' ').slice(0, 19)
}

const loadModelComments = async (modelId) => {
	commentLoading.value = true
	try {
		const publicData = await getModelOrderCommentsApi({ modelId, pageNum: 1, pageSize: 2, sortType: 'hot' })
		modelComments.value = Array.isArray(publicData?.records)
			? publicData.records.map(item => {
				const commentId = String(item?.id || '').trim()
				return {
					...item,
					liked: !!likedCommentMap.value[commentId]
				}
			})
			: []
		modelCommentTotal.value = Number(publicData?.total || 0)
	} catch (_) {
		modelComments.value = []
		modelCommentTotal.value = 0
	} finally {
		commentLoading.value = false
	}
}

const currentMaterial = computed(() => materials.value[selectedMaterial.value] || materials.value[0] || { id: 0, name: '默认材质', add: 0 })
const selectedColorValue = computed(() => rgbToHex(rgbColor.value))
const selectedColorName = computed(() => `RGB(${rgbColor.value.r}, ${rgbColor.value.g}, ${rgbColor.value.b})`)
const selectedColorRgb = computed(() => ({ ...rgbColor.value }))
const selectedMaterialType = computed(() => materialMapping[selectedMaterial.value % materialMapping.length] || 'standard')
const precisionSliderValue = computed(() => Math.round((precisionMm.value || 0.1) * 100))
const precisionLabel = computed(() => `${precisionMm.value.toFixed(2)}mm`)
const fillDensityLabel = computed(() => `${Math.round(fillDensity.value)}%`)
const filamentSliderValue = computed(() => Math.round(filamentDiameter.value * 100))
const filamentRangeLabel = computed(() => '0.50 - 3.00mm')
const previewColorHex = computed(() => rgbToHex(tempRgbColor.value))
const previewColorText = computed(() => `RGB(${tempRgbColor.value.r}, ${tempRgbColor.value.g}, ${tempRgbColor.value.b})`)
const modelTypeTag = computed(() => String(modelInfo.value.modelType || 'GLB').toUpperCase())
const modelFileSizeTag = computed(() => modelFileSize.value || '--')
const rawVolumeText = computed(() => Math.max(0, toSafeNumber(modelInfo.value.baseVolume, 0)).toFixed(2))
const rawSizeText = computed(() => {
	const sizeText = String(modelInfo.value.baseSize || '').trim()
	return sizeText || '--'
})

const toSafeNumber = (value, fallback = 0) => {
	const num = Number(value)
	return Number.isFinite(num) ? num : fallback
}

const clamp = (value, min, max) => Math.min(max, Math.max(min, value))

const sanitizeDecimalInput = (value, decimalPlaces = 2) => {
	const clean = String(value || '').replace(/[^\d.]/g, '')
	const [intPart, ...rest] = clean.split('.')
	const decimalPart = rest.join('')
	if (rest.length === 0) return intPart
	return `${intPart}.${decimalPart.slice(0, decimalPlaces)}`
}

const rgbToHex = (rgb = {}) => {
	const r = clamp(Math.round(toSafeNumber(rgb.r, 79)), 0, 255)
	const g = clamp(Math.round(toSafeNumber(rgb.g, 70)), 0, 255)
	const b = clamp(Math.round(toSafeNumber(rgb.b, 229)), 0, 255)
	const toHex = (num) => num.toString(16).padStart(2, '0').toUpperCase()
	return `#${toHex(r)}${toHex(g)}${toHex(b)}`
}

const precisionPriceAdjustment = computed(() => {
	const p = clamp(toSafeNumber(precisionMm.value, 0.1), 0.05, 0.5)
	if (p <= 0.1) {
		const ratio = (0.1 - p) / 0.05
		return ratio * 30
	}
	if (p <= 0.2) {
		const ratio = (p - 0.1) / 0.1
		return -ratio * 15
	}
	const ratio = (p - 0.2) / 0.3
	return -15 - ratio * 15
})

const parseModelType = (url) => {
	if (!url) return 'glb'
	const cleanUrl = url.split('?')[0].split('#')[0]
	const ext = cleanUrl.includes('.') ? cleanUrl.split('.').pop().toLowerCase() : 'glb'
	return ext || 'glb'
}

const formatFileSize = (value) => {
	if (value == null || value === '') return '--'
	if (typeof value === 'string' && /[A-Za-z]/.test(value)) {
		return value.toUpperCase().replace(/\s+/g, '')
	}
	const size = Number(value)
	if (!Number.isFinite(size) || size <= 0) return '--'
	if (size >= 1024 * 1024 * 1024) return `${(size / (1024 * 1024 * 1024)).toFixed(1)}GB`
	if (size >= 1024 * 1024) return `${(size / (1024 * 1024)).toFixed(1)}MB`
	if (size >= 1024) return `${Math.round(size / 1024)}KB`
	return `${size}B`
}

const loadFavoriteState = async (modelId) => {
	try {
		const ids = await getMyFavoriteModelIdsApi()
		if (!Array.isArray(ids)) {
			isFavorited.value = false
			return
		}
		isFavorited.value = ids.map(item => String(item)).includes(String(modelId))
	} catch (_) {
		isFavorited.value = false
	}
}

const toggleFavorite = async () => {
	if (!ensureLoginOrRedirect()) return
	const modelId = String(modelInfo.value.id || '').trim()
	if (!modelId) {
		uni.showToast({ title: '模型信息未加载完成', icon: 'none' })
		return
	}
	try {
		const res = await toggleModelFavoriteApi(modelId)
		isFavorited.value = !!res?.active
		uni.$emit('favorite-model-changed')
	} catch (error) {
		uni.showToast({ title: error?.message || '收藏操作失败', icon: 'none' })
	}
}

const pickFirstString = (...values) => {
	for (const value of values) {
		const text = String(value || '').trim()
		if (text) return text
	}
	return ''
}

const toAbsoluteAssetUrl = (url) => {
	const value = String(url || '').trim()
	if (!value) return ''
	if (/^https?:\/\//i.test(value)) return value
	const base = String(getApiBaseUrl() || '').replace(/\/+$/, '')
	if (!base) return value
	return `${base}${value.startsWith('/') ? '' : '/'}${value}`
}

const parseColor = (color) => {
	if (!color) return '#4f46e5'
	if (color.startsWith('#')) return color
	const mapping = {
		'纯白': '#F8FAFC',
		'黑色': '#111827',
		'深空灰': '#4B5563',
		'透明蓝': '#3B82F6',
		'天蓝': '#4f46e5'
	}
	return mapping[color] || '#4f46e5'
}

const parseRgbFromColor = (color) => {
	if (!color) {
		return { r: 79, g: 70, b: 229 }
	}
	const value = String(color).trim()
	if (value.startsWith('#')) {
		let hex = value.slice(1)
		if (hex.length === 3) {
			hex = hex.split('').map(ch => ch + ch).join('')
		}
		if (hex.length >= 6) {
			return {
				r: parseInt(hex.slice(0, 2), 16),
				g: parseInt(hex.slice(2, 4), 16),
				b: parseInt(hex.slice(4, 6), 16)
			}
		}
	}
	const match = value.match(/rgb\s*\((\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)/i)
	if (match) {
		return {
			r: Number(match[1]),
			g: Number(match[2]),
			b: Number(match[3])
		}
	}
	return { r: 79, g: 70, b: 229 }
}

const onPrecisionChange = (event) => {
	const raw = toSafeNumber(event?.detail?.value, 10)
	precisionMm.value = clamp(raw / 100, 0.05, 0.5)
	precisionInput.value = precisionMm.value.toFixed(2)
}

const onPrecisionInput = (event) => {
	precisionInput.value = sanitizeDecimalInput(event?.detail?.value, 2)
}

const onPrecisionInputBlur = () => {
	const next = clamp(toSafeNumber(precisionInput.value, 0.1), 0.05, 0.5)
	precisionMm.value = Number(next.toFixed(2))
	precisionInput.value = precisionMm.value.toFixed(2)
}

const onFillDensityChange = (event) => {
	const raw = toSafeNumber(event?.detail?.value, 20)
	fillDensity.value = Math.round(clamp(raw, 0, 100))
	fillDensityInput.value = String(fillDensity.value)
}

const onFillDensityInput = (event) => {
	fillDensityInput.value = String(event?.detail?.value || '').replace(/[^\d]/g, '')
}

const onFillDensityInputBlur = () => {
	const next = Math.round(clamp(toSafeNumber(fillDensityInput.value, 20), 0, 100))
	fillDensity.value = next
	fillDensityInput.value = String(next)
}

const onFilamentInput = (event) => {
	filamentInput.value = sanitizeDecimalInput(event?.detail?.value, 2)
}

const onFilamentInputBlur = () => {
	const next = clamp(toSafeNumber(filamentInput.value, 1.75), 0.5, 3)
	filamentDiameter.value = Number(next.toFixed(2))
	filamentInput.value = filamentDiameter.value.toFixed(2)
}

const onFilamentSliderChange = (event) => {
	const raw = toSafeNumber(event?.detail?.value, 175)
	const next = clamp(raw / 100, 0.5, 3)
	filamentDiameter.value = Number(next.toFixed(2))
	filamentInput.value = filamentDiameter.value.toFixed(2)
}

const onPopupRgbChange = (channel, event) => {
	const next = clamp(toSafeNumber(event?.detail?.value, 0), 0, 255)
	tempRgbColor.value = {
		...tempRgbColor.value,
		[channel]: Math.round(next)
	}
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

const loadModelDetail = async (modelId, forceUpdate = false) => {
	try {
		const detail = await getModelDetailApi(modelId, forceUpdate)
		const rawModelUrl = pickFirstString(
			detail?.filePath,
			detail?.file_path,
			detail?.modelUrl,
			detail?.model_url,
			detail?.url,
			detail?.path
		)
		const modelUrl = toAbsoluteAssetUrl(rawModelUrl)
		const mainImage = toAbsoluteAssetUrl(
			pickFirstString(
				detail?.mainImageUrl,
				detail?.main_image_url,
				detail?.images?.[0]?.imageUrl,
				detail?.images?.[0]?.image_url,
				rawModelUrl,
				'https://images.unsplash.com/photo-1581092160562-40aa08e78837?w=800'
			)
		)
		const modelType = parseModelType(modelUrl)

		modelInfo.value = {
			id: detail?.id,
			name: detail?.modelName || '未知模型',
			desc: detail?.description || detail?.modelDescription || '暂无模型描述',
			image: mainImage,
			basePrice: Number(detail?.basePrice || 0),
			baseVolume: Number(detail?.baseVolume || 45),
			baseSize: detail?.baseSize || '--',
			modelUrl,
			modelType
		}
		modelFileSize.value = formatFileSize(detail?.fileSize || detail?.file_size || detail?.size)
		// 优先使用水印图片，如果没有则使用原图
		// 添加时间戳参数防止图片缓存
		const cacheBuster = `t=${Date.now()}`
		const imageList = Array.isArray(detail?.images)
			? detail.images.map(item => {
				// 优先使用水印图片URL
				const watermarkedUrl = pickFirstString(item?.watermarkedUrl, item?.watermarked_url)
				if (watermarkedUrl) {
					const fullUrl = toAbsoluteAssetUrl(watermarkedUrl)
					return fullUrl.includes('?') ? `${fullUrl}&${cacheBuster}` : `${fullUrl}?${cacheBuster}`
				}
				// 回退到原图
				const originalUrl = toAbsoluteAssetUrl(pickFirstString(item?.imageUrl, item?.image_url, item?.url))
				return originalUrl.includes('?') ? `${originalUrl}&${cacheBuster}` : `${originalUrl}?${cacheBuster}`
			}).filter(Boolean)
			: []
		modelImages.value = imageList.length ? imageList : [mainImage]
		showReal3D.value = false

		if (Array.isArray(detail?.materials) && detail.materials.length) {
			materials.value = detail.materials.map((item, index) => ({
				id: item.id,
				name: item.name || `材质${index + 1}`,
				add: Number(item.price || 0),
				color: parseColor(item.color),
				isEco: item.isEco || false
			}))
			rgbColor.value = parseRgbFromColor(detail.materials[0]?.color || '#4f46e5')
			tempRgbColor.value = { ...rgbColor.value }
			selectedMaterial.value = 0
		} else {
			materials.value = [{ id: 0, name: '默认材质', add: 0, color: '#4f46e5', isEco: false }]
			rgbColor.value = { r: 79, g: 70, b: 229 }
			tempRgbColor.value = { ...rgbColor.value }
			selectedMaterial.value = 0
		}
		precisionMm.value = 0.1
		precisionInput.value = '0.10'
		fillDensity.value = 20
		fillDensityInput.value = '20'
		filamentDiameter.value = 1.75
		filamentInput.value = '1.75'
		loadModelComments(modelId)
	} catch (error) {
		modelFileSize.value = '--'
		modelImages.value = modelInfo.value.image ? [modelInfo.value.image] : []
		uni.showToast({ title: error.message || '加载模型详情失败', icon: 'none' })
	}
}

const totalPrice = computed(() => {
	return Math.max(0, toSafeNumber(modelInfo.value.basePrice, 0)).toFixed(2)
})

const onScaleChange = (e) => {
	const sliderValue = toSafeNumber(e?.detail?.value, 10)
	scale.value = clamp(sliderValue / 10, 0.5, 2)
}

const addToCart = () => {
	if (!modelInfo.value.id) {
		uni.showToast({ title: '模型信息未加载完成', icon: 'none' })
		return
	}
	const paramsText = `${currentMaterial.value.name} / 层高:${precisionLabel.value} / 填充:${fillDensityLabel.value} / 线径:${filamentDiameter.value.toFixed(2)}mm / 颜色:${selectedColorName.value}${customRemark.value.trim() ? ` / 备注:${customRemark.value.trim()}` : ''}`
	const customParams = {
		layer_height: Number(precisionMm.value.toFixed(2)),
		fill_density: Number(fillDensity.value),
		filament_diameter: Number(filamentDiameter.value.toFixed(2)),
		material_name: currentMaterial.value.name,
		color_name: selectedColorName.value,
		color_hex: selectedColorValue.value,
		rgb: { ...selectedColorRgb.value },
		est_material: Number(rawVolumeText.value),
		raw_volume: Number(rawVolumeText.value),
		base_size: rawSizeText.value,
		params_text: paramsText,
		remark: customRemark.value.trim() || ''
	}
	const orderPayload = {
		modelId: modelInfo.value.id,
		materialId: currentMaterial.value.id || null,
		scale: Number(scale.value.toFixed(2)),
		fillPercent: Number(fillDensity.value),
		color: selectedColorValue.value,
		note: customRemark.value.trim() || '',
		customParams,
		custom_params: customParams
	}
	const cartItem = {
		modelId: modelInfo.value.id,
		name: modelInfo.value.name,
		params: paramsText,
		price: totalPrice.value,
		num: 1,
		selected: true,
		image: modelInfo.value.image,
		orderPayload
	}
	
	let cartList = uni.getStorageSync('cart_list') || []
	// 检查是否已存在相同参数的商品
	const existingIndex = cartList.findIndex(item => item.name === cartItem.name && item.params === cartItem.params)
	
	if (existingIndex > -1) {
		cartList[existingIndex].num += 1
	} else {
		cartList.unshift(cartItem)
	}
	
	uni.setStorageSync('cart_list', cartList)
	uni.showToast({ title: '已加入购物车', icon: 'success' })
}

const showConfirmModal = (options = {}) => {
	return new Promise((resolve) => {
		uni.showModal({
			title: options.title || '提示',
			content: options.content || '',
			confirmText: options.confirmText || '确定',
			cancelText: options.cancelText || '取消',
			success: (res) => resolve(res || {}),
			fail: () => resolve({ confirm: false, cancel: true })
		})
	})
}

const getPendingOrdersByModelId = async (modelId) => {
	const result = await getMyOrdersApi({
		pageNum: 1,
		pageSize: 100,
		modelId,
		orderStatus: 0
	})
	const records = Array.isArray(result?.records) ? result.records : []
	return records.filter(item => Number(item?.orderStatus) === 0)
}

const goPayForOrder = (order) => {
	const orderId = order?.id || order?.orderId
	if (orderId) {
		uni.navigateTo({ url: `/pages/user/order-detail?id=${orderId}` })
		return
	}
	uni.navigateTo({ url: '/pages/user/orders?status=1' })
}

const goCheckoutWithCurrentConfig = () => {
	if (!modelInfo.value.id) {
		uni.showToast({ title: '模型信息未加载完成', icon: 'none' })
		return
	}
	const paramsText = `${currentMaterial.value.name} / 层高:${precisionLabel.value} / 填充:${fillDensityLabel.value} / 线径:${filamentDiameter.value.toFixed(2)}mm / 颜色:${selectedColorName.value}${customRemark.value.trim() ? ` / 备注:${customRemark.value.trim()}` : ''}`
	const customParams = {
		layer_height: Number(precisionMm.value.toFixed(2)),
		fill_density: Number(fillDensity.value),
		filament_diameter: Number(filamentDiameter.value.toFixed(2)),
		material_name: currentMaterial.value.name,
		color_name: selectedColorName.value,
		color_hex: selectedColorValue.value,
		rgb: { ...selectedColorRgb.value },
		est_material: Number(rawVolumeText.value),
		raw_volume: Number(rawVolumeText.value),
		base_size: rawSizeText.value,
		params_text: paramsText,
		remark: customRemark.value.trim() || ''
	}
	const orderPayload = {
		modelId: modelInfo.value.id,
		materialId: currentMaterial.value.id || null,
		scale: Number(scale.value.toFixed(2)),
		fillPercent: Number(fillDensity.value),
		color: selectedColorValue.value,
		note: customRemark.value.trim() || '',
		customParams,
		custom_params: customParams
	}
	const orderItem = {
		modelId: modelInfo.value.id,
		name: modelInfo.value.name,
		params: paramsText,
		price: totalPrice.value,
		num: 1,
		image: modelInfo.value.image,
		orderPayload
	}
	uni.setStorageSync('checkout_items', [orderItem])
	uni.setStorageSync('checkout_from', 'buyNow')
	uni.navigateTo({ url: '/pages/cart/checkout' })
}

const buyNow = async () => {
	if (!modelInfo.value.id) {
		uni.showToast({ title: '模型信息未加载完成', icon: 'none' })
		return
	}
	const confirmed = await showConfirmModal({
		title: '确认下单',
		content: '确认使用当前定制参数立即下单并进入支付吗？',
		confirmText: '确认下单',
		cancelText: '再看看'
	})
	if (!confirmed?.confirm) {
		return
	}

	uni.showLoading({ title: '检查待支付订单...' })
	try {
		const pendingOrders = await getPendingOrdersByModelId(modelInfo.value.id)
		if (pendingOrders.length > 0) {
			const latestOrder = pendingOrders[0]
			uni.hideLoading()
			const action = await showConfirmModal({
				title: '存在待支付订单',
				content: '当前模型已有待支付订单，是否前往支付？',
				confirmText: '去支付',
				cancelText: '继续新建'
			})
			if (action?.confirm) {
				goPayForOrder(latestOrder)
				return
			}
		}
		goCheckoutWithCurrentConfig()
	} catch (error) {
		uni.showToast({ title: error?.message || '检查订单状态失败', icon: 'none' })
	} finally {
		uni.hideLoading()
	}
}

const goCart = () => {
	uni.switchTab({ url: '/pages/cart/cart' })
}

const goGroupBuy = () => {
	if (!modelInfo.value.id) {
		uni.showToast({ title: '模型信息未加载完成', icon: 'none' })
		return
	}
	// 保存当前定制参数到本地存储，供拼团页面使用
	const customParams = {
		modelId: modelInfo.value.id,
		materialId: currentMaterial.value.id || null,
		materialName: currentMaterial.value.name,
		colorHex: selectedColorValue.value,
		colorRgb: { ...rgbColor.value },
		scale: Number(scale.value.toFixed(2)),
		precision: Number(precisionMm.value.toFixed(2)),
		fillDensity: Number(fillDensity.value),
		filamentDiameter: Number(filamentDiameter.value.toFixed(2)),
		remark: customRemark.value.trim() || ''
	}
	uni.setStorageSync('group_buy_custom_params', customParams)
	uni.navigateTo({ url: `/pages/group-buy/list?modelId=${modelInfo.value.id}` })
}

const goCustomerService = () => {
	const token = uni.getStorageSync('token')
	if (!token) {
		uni.navigateTo({ url: '/pages/auth/login' })
		return
	}
	uni.navigateTo({ url: '/pages/custom/customer-service' })
}
</script>

<style scoped lang="scss">
$sky-blue: #00bfff;
$sky-light: #5ce1ff;
$sky-deep: #0099cc;
$surface: #f8f8f8;
$surface-raised: #ffffff;
$text-primary: #1a2030;
$text-secondary: #5a6a7a;
$text-muted: #94a3b8;
$shadow-card: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
$danger: #ff4d6d;
$gradient-primary: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);

@keyframes fadeInUp {
	from { opacity: 0; transform: translateY(24rpx); }
	to { opacity: 1; transform: translateY(0); }
}

@keyframes imgFadeIn {
	from { opacity: 0; transform: scale(0.97); }
	to { opacity: 1; transform: scale(1); }
}

.detail-container {
	height: 100vh;
	display: flex;
	flex-direction: column;
	background: $surface;
}

.detail-scroll {
	flex: 1;
}

.preview-section {
	position: relative;
	width: 100%;
	padding-bottom: 100%;
	background: linear-gradient(151deg, $surface 0%, rgba(0, 191, 255, 0.05) 100%);

	.main-swiper,
	.three-viewer {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		display: block;
	}

	.main-img {
		width: 100%;
		height: 100%;
		display: block;
		animation: imgFadeIn 0.4s ease-out both;
	}
}

.preview-entry {
	position: absolute;
	left: 24rpx;
	right: 24rpx;
	bottom: 24rpx;
	height: 88rpx;
	border-radius: 999rpx;
	background: rgba(255, 255, 255, 0.3);
	backdrop-filter: blur(12px);
	-webkit-backdrop-filter: blur(12px);
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 10rpx;
	color: $text-primary;
	font-size: 26rpx;
	font-weight: 500;

	&.solid {
		left: auto;
		right: 24rpx;
		bottom: 24rpx;
		height: 64rpx;
		padding: 0 24rpx;
		background: $gradient-primary;
		color: #ffffff;
		font-size: 24rpx;
		border-radius: 999rpx;
	}
}

.preview-tags {
	position: absolute;
	top: 20rpx;
	right: 20rpx;
	display: flex;
	flex-direction: column;
	gap: 12rpx;

	.tag {
		padding: 8rpx 16rpx;
		font-size: 20rpx;
		font-weight: 700;
		border-radius: 999rpx;
		background: rgba(255, 255, 255, 0.75);
		backdrop-filter: blur(8px);
		-webkit-backdrop-filter: blur(8px);
		color: $text-secondary;
	}
}

.preview-close {
	position: absolute;
	top: 24rpx;
	right: 24rpx;
	width: 60rpx;
	height: 60rpx;
	border-radius: 50%;
	background: rgba(0, 0, 0, 0.25);
	backdrop-filter: blur(8px);
	-webkit-backdrop-filter: blur(8px);
	display: flex;
	align-items: center;
	justify-content: center;
}

.info-card,
.custom-card,
.comment-card {
	margin: 24rpx;
	padding: 32rpx;
	background: $surface-raised;
	border-radius: 24rpx;
	box-shadow: $shadow-card;
	box-sizing: border-box;
	animation: fadeInUp 0.35s ease-out both;
}

.info-card {
	margin-top: -36rpx;
	position: relative;
	z-index: 3;
}

.custom-card {
	animation-delay: 0.05s;
}

.comment-card {
	animation-delay: 0.1s;
}

.series-text {
	font-size: 22rpx;
	font-weight: 600;
	color: $sky-blue;
	padding: 8rpx 16rpx;
	border-radius: 999rpx;
	background: rgba(0, 191, 255, 0.1);
	display: inline-block;
}

.title-row {
	margin-top: 16rpx;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 20rpx;

	.name {
		flex: 1;
		font-size: 36rpx;
		font-weight: 700;
		color: $text-primary;
		line-height: 1.4;
	}
}

.fav-btn {
	width: 64rpx;
	height: 64rpx;
	border-radius: 50%;
	background: $surface;
	display: flex;
	align-items: center;
	justify-content: center;

	&.active {
		background: #fff1f2;
	}
}

.desc {
	display: block;
	margin-top: 16rpx;
	font-size: 28rpx;
	line-height: 1.7;
	color: $text-secondary;
}

.price-row {
	margin-top: 24rpx;
	padding-top: 24rpx;
	position: relative;
	display: flex;
	justify-content: space-between;
	align-items: center;
	gap: 20rpx;

	&::before {
		content: '';
		position: absolute;
		left: 0;
		right: 0;
		top: 0;
		height: 1rpx;
		background: rgba(0, 0, 0, 0.03);
	}
}

.price-box {
	display: flex;
	align-items: baseline;

	.symbol {
		font-size: 30rpx;
		font-weight: 700;
		color: $text-primary;
	}

	.price {
		margin-left: 4rpx;
		font-size: 56rpx;
		font-weight: 800;
		color: $text-primary;
	}
}

.metrics {
	display: flex;
	flex-direction: column;
	gap: 6rpx;

	.metric-item {
		display: flex;
		justify-content: flex-end;
		gap: 10rpx;
		font-size: 24rpx;
	}

	.metric-label {
		color: $text-muted;
	}

	.metric-val {
		color: $text-primary;
		font-weight: 600;
	}
}

.section-title {
	font-size: 30rpx;
	font-weight: 700;
	color: $text-primary;
	margin-bottom: 20rpx;
}

.section-title.clickable {
	margin-bottom: 0;
	display: flex;
	justify-content: space-between;
	align-items: center;
}

.section-title-left {
	display: flex;
	align-items: center;
	gap: 14rpx;
}

.dot {
	width: 18rpx;
	height: 18rpx;
	border-radius: 50%;
	background: $sky-blue;
}

.options-wrap {
	margin-top: 24rpx;
	display: flex;
	flex-direction: column;
	gap: 24rpx;
}

.option-panel {
	padding: 24rpx;
	border-radius: 20rpx;
	background: $surface;
}

.panel-head {
	display: flex;
	justify-content: space-between;
	align-items: center;
	gap: 14rpx;

	.label {
		font-size: 26rpx;
		font-weight: 600;
		color: $text-primary;
	}

	.sub-val {
		font-size: 24rpx;
		color: $text-secondary;
	}
}

.option-list {
	margin-top: 16rpx;
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx;
}

.option-item {
	padding: 12rpx 28rpx;
	border-radius: 999rpx;
	font-size: 24rpx;
	color: $text-secondary;
	background: $surface-raised;
	display: flex;
	align-items: center;
	gap: 8rpx;

	&:active {
		transform: scale(0.96);
	}

	&.active {
		color: $sky-blue;
		background: rgba(0, 191, 255, 0.1);
		box-shadow: inset 0 0 0 2rpx rgba(0, 191, 255, 0.3);
	}

	&.eco-material {
		background: rgba(34, 197, 94, 0.08);

		&.active {
			background: rgba(34, 197, 94, 0.15);
			box-shadow: inset 0 0 0 2rpx rgba(34, 197, 94, 0.5);
		}
	}
}

.eco-badge {
	display: flex;
	align-items: center;
	gap: 4rpx;
	margin-left: 4rpx;
}

.eco-text {
	font-size: 20rpx;
	color: #22c55e;
	font-weight: 500;
}

.current-text {
	display: block;
	margin-top: 10rpx;
	font-size: 24rpx;
	color: $text-secondary;
}

.range-labels {
	margin-top: 6rpx;
	display: flex;
	justify-content: space-between;
	font-size: 20rpx;
	color: $text-muted;
}

.num-input {
	width: 170rpx;
	height: 58rpx;
	border-radius: 999rpx;
	background: $surface;
	padding: 0 18rpx;
	font-size: 24rpx;
	text-align: right;
	box-sizing: border-box;
}

.color-row {
	margin-top: 14rpx;
	display: flex;
	align-items: center;
	gap: 14rpx;
}

.color-preview-circle {
	width: 40rpx;
	height: 40rpx;
	border-radius: 50%;
	box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);

	&.large {
		width: 52rpx;
		height: 52rpx;
	}
}

.color-value {
	flex: 1;
	font-size: 24rpx;
	color: $text-secondary;
}

.color-picker-btn {
	height: 56rpx;
	padding: 0 24rpx;
	border-radius: 999rpx;
	background: rgba(0, 191, 255, 0.1);
	color: $sky-blue;
	font-size: 24rpx;
	font-weight: 600;
	display: flex;
	align-items: center;
	justify-content: center;

	&:active {
		transform: scale(0.96);
	}
}

.remark-input {
	margin-top: 16rpx;
	width: 100%;
	height: 160rpx;
	padding: 20rpx;
	font-size: 24rpx;
	border-radius: 16rpx;
	background: $surface-raised;
	box-sizing: border-box;
}

.comment-empty {
	font-size: 24rpx;
	color: $text-muted;
	padding: 16rpx 0;
}

.comment-header-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 16rpx;
}

.more-link {
	font-size: 24rpx;
	color: $sky-blue;
	font-weight: 600;
}

.comment-item {
	padding: 20rpx 0;
	position: relative;

	&:not(:last-child)::after {
		content: '';
		position: absolute;
		left: 0;
		right: 0;
		bottom: 0;
		height: 1rpx;
		background: rgba(0, 0, 0, 0.03);
	}
}

.comment-item:last-child {
	padding-bottom: 0;
}

.comment-head,
.comment-user-wrap {
	display: flex;
	align-items: center;
}

.comment-avatar {
	width: 64rpx;
	height: 64rpx;
	border-radius: 50%;
	background-color: $surface;
}

.comment-user-meta {
	margin-left: 14rpx;
	display: flex;
	flex-direction: column;
	gap: 4rpx;
}

.comment-user {
	font-size: 26rpx;
	font-weight: 600;
	color: $text-primary;
}

.comment-time {
	font-size: 24rpx;
	color: $text-muted;
}

.comment-score-row {
	margin-top: 12rpx;
	display: flex;
	align-items: center;
	gap: 12rpx;
}

.comment-score-stars {
	font-size: 26rpx;
	letter-spacing: 2rpx;
	color: #f59e0b;
}

.comment-score-text {
	font-size: 24rpx;
	color: $sky-blue;
}

.comment-text {
	display: block;
	margin-top: 12rpx;
	font-size: 28rpx;
	line-height: 1.6;
	color: $text-primary;
}

.comment-media {
	display: flex;
	gap: 12rpx;
	margin-top: 16rpx;
	flex-wrap: wrap;
}

.comment-media-item,
.comment-media-thumb,
.comment-media-video {
	width: 180rpx;
	height: 180rpx;
	border-radius: 16rpx;
	background: $surface;
}

.comment-media-thumb {
	animation: imgFadeIn 0.4s ease-out both;
}

.comment-actions {
	margin-top: 12rpx;
	display: flex;
	justify-content: flex-end;
}

.comment-like {
	display: inline-flex;
	align-items: center;
	gap: 6rpx;
	padding: 8rpx 16rpx;
	border-radius: 999rpx;
	background-color: $surface;
}

.comment-like.active {
	background-color: #fff1f2;
}

.comment-like-text {
	font-size: 24rpx;
	color: $text-secondary;
}

.comment-like.active .comment-like-text {
	color: #ef4444;
}

.footer-placeholder {
	height: calc(120rpx + env(safe-area-inset-bottom) + 40rpx);
}

.bottom-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 100;
	height: 120rpx;
	padding: 0 24rpx;
	padding-bottom: env(safe-area-inset-bottom);
	display: flex;
	align-items: center;
	background: rgba(255, 255, 255, 0.88);
	backdrop-filter: blur(24px);
	-webkit-backdrop-filter: blur(24px);
	box-shadow: 0 -4rpx 24rpx rgba(0, 0, 0, 0.04);

	.action-icons {
		display: flex;
		gap: 28rpx;
		margin-right: 20rpx;
	}

	.icon-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		text {
			margin-top: 2rpx;
			font-size: 20rpx;
			color: $text-secondary;
		}
	}

	.btns {
		flex: 1;
		height: 80rpx;
		display: flex;
		gap: 10rpx;
	}

	.btn {
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 24rpx;
		font-weight: 600;
		border-radius: 999rpx;

		&:active {
			transform: scale(0.96);
		}
	}

	.add-cart {
		color: $text-primary;
		background: $surface;
		box-shadow: $shadow-card;
	}

	.group-buy {
		color: #ffffff;
		background: linear-gradient(135deg, #ff6b6b 0%, #ff8e53 100%);
		box-shadow: 0 4rpx 16rpx rgba(255, 107, 107, 0.35);
	}

	.buy-now {
		color: #ffffff;
		background: $gradient-primary;
		box-shadow: 0 4rpx 16rpx rgba(0, 191, 255, 0.35);
	}
}

.color-popup-mask {
	position: fixed;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
	background-color: rgba(0, 0, 0, 0.35);
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 999;
}

.color-popup {
	width: 620rpx;
	background-color: $surface-raised;
	border-radius: 24rpx;
	padding: 32rpx;
	box-sizing: border-box;
	box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.12);
}

.popup-title {
	font-size: 30rpx;
	font-weight: 700;
	color: $text-primary;
}

.popup-preview-row {
	display: flex;
	align-items: center;
	gap: 14rpx;
	margin-top: 20rpx;
}

.popup-color-text {
	font-size: 24rpx;
	color: $text-secondary;
}

.popup-slider-item {
	display: flex;
	align-items: center;
	gap: 12rpx;
	margin-top: 16rpx;

	slider {
		flex: 1;
	}
}

.popup-label {
	width: 24rpx;
	font-size: 24rpx;
	font-weight: 700;
	color: $text-primary;
}

.popup-num {
	width: 64rpx;
	text-align: right;
	font-size: 24rpx;
	color: $text-secondary;
}

.popup-actions {
	margin-top: 28rpx;
	display: flex;
	justify-content: flex-end;
	gap: 16rpx;
}

.popup-btn {
	min-width: 130rpx;
	height: 68rpx;
	border-radius: 999rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 26rpx;
	font-weight: 600;

	&:active {
		transform: scale(0.96);
	}

	&.cancel {
		background-color: $surface;
		color: $text-secondary;
	}

	&.confirm {
		background: $gradient-primary;
		color: #ffffff;
		box-shadow: 0 4rpx 16rpx rgba(0, 191, 255, 0.35);
	}
}

@keyframes spin {
	from {
		transform: rotate(0deg);
	}
	to {
		transform: rotate(360deg);
	}
}
</style>
