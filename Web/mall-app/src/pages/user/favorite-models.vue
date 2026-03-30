<template>
	<view class="favorite-page">
		<view class="summary">
			<view class="summary-main">
				<text class="title">收藏模型</text>
				<text class="desc">已收藏 {{ total }} 个模型，可查看详情并直接下单</text>
			</view>
			<view class="summary-badge">
				<text class="summary-num">{{ total }}</text>
				<text class="summary-label">模型</text>
			</view>
		</view>

		<view v-if="loading" class="state">
			<text>正在加载收藏模型...</text>
		</view>
		<view v-else-if="list.length === 0" class="state empty-state">
			<text class="empty-icon">♡</text>
			<text>暂无收藏模型</text>
		</view>

		<view v-else class="list-wrap">
			<view class="model-card" v-for="item in list" :key="item.id" @click="goDetail(item.id)">
				<view class="fav-btn" @click.stop="cancelFavorite(item)">
					<uni-icons type="heart-filled" size="18" color="#ef4444"></uni-icons>
				</view>
				<image class="cover" :src="resolveImage(item)" mode="aspectFill"></image>
				<view class="content">
					<text class="name">{{ item.modelName || '未命名模型' }}</text>
					<text class="meta">尺寸 {{ formatSize(item.baseSize) }} / 体积 {{ formatVolume(item.baseVolume) }}mm³</text>
					<view class="price-row">
						<text class="price">￥{{ formatPrice(item.basePrice) }}</text>
						<text class="price-tip">基础价</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import { getModelDetailApi, getMyFavoriteModelsApi, toggleModelFavoriteApi } from '../../api/model'
import { getApiBaseUrl } from '../../utils/apiBase'

const fallbackImage = 'https://images.unsplash.com/photo-1519710164239-da123dc03ef4?w=300'
const loading = ref(false)
const list = ref([])
const total = ref(0)

const formatPrice = (value) => Number(value || 0).toFixed(2)

const IMAGE_EXT_REG = /\.(png|jpe?g|webp|gif|bmp|svg)(\?.*)?$/i
const MODEL_FILE_EXT_REG = /\.(stl|obj|3mf|gltf|glb|fbx|step|stp)(\?.*)?$/i

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
	if (/^\/\//.test(value)) return `http:${value}`
	const base = String(getApiBaseUrl() || '').replace(/\/+$/, '')
	if (!base) return value
	return `${base}${value.startsWith('/') ? '' : '/'}${value}`
}

const isImageLikeUrl = (url) => {
	const value = String(url || '').trim()
	if (!value) return false
	if (/^data:image\//i.test(value)) return true
	if (MODEL_FILE_EXT_REG.test(value)) return false
	if (IMAGE_EXT_REG.test(value)) return true
	if (value.includes('/image') || value.includes('/images') || value.includes('/upload') || value.includes('/uploads')) {
		return true
	}
	return false
}

const resolveImage = (item) => {
	const raw = pickFirstString(
		item?.displayImageUrl,
		item?.mainImageUrl,
		item?.mainImage,
		item?.main_image_url,
		item?.main_image,
		item?.imageUrl,
		item?.image,
		item?.coverUrl,
		item?.cover
	)
	const finalUrl = toAbsoluteAssetUrl(raw)
	return isImageLikeUrl(finalUrl) ? finalUrl : fallbackImage
}

const enrichListImage = async (record) => {
	if (!record?.id) return record
	const current = toAbsoluteAssetUrl(pickFirstString(record?.mainImageUrl, record?.mainImage, record?.imageUrl, record?.image))
	if (isImageLikeUrl(current)) {
		return {
			...record,
			displayImageUrl: current
		}
	}
	try {
		const detail = await getModelDetailApi(record.id)
		const detailImage = toAbsoluteAssetUrl(pickFirstString(
			detail?.mainImageUrl,
			detail?.mainImage,
			detail?.main_image_url,
			detail?.main_image,
			detail?.imageUrl,
			detail?.image
		))
		return {
			...record,
			displayImageUrl: isImageLikeUrl(detailImage) ? detailImage : ''
		}
	} catch (_) {
		return {
			...record,
			displayImageUrl: ''
		}
	}
}

const formatSize = (value) => {
	const raw = String(value || '').trim()
	if (!raw) return '--*--*--'
	const normalized = raw
		.replace(/[xX×\*]/g, '*')
		.split('*')
		.map(part => part.trim())
		.filter(Boolean)
	if (normalized.length !== 3) return raw.replace(/[xX×]/g, '*')
	return normalized.join('*')
}

const formatVolume = (value) => {
	const volume = Number(value)
	if (!Number.isFinite(volume) || volume <= 0) return '--'
	return volume >= 1000 ? volume.toFixed(0) : volume.toFixed(2)
}

const loadList = async () => {
	loading.value = true
	try {
		const data = await getMyFavoriteModelsApi({ pageNum: 1, pageSize: 200 })
		const records = Array.isArray(data?.records) ? data.records : []
		const enrichedRecords = await Promise.all(records.map(enrichListImage))
		list.value = enrichedRecords
		total.value = Number(data?.total || list.value.length)
	} catch (error) {
		list.value = []
		total.value = 0
		uni.showToast({ title: error?.message || '加载收藏失败', icon: 'none' })
	} finally {
		loading.value = false
	}
}

const goDetail = (id) => {
	if (!id) {
		uni.showToast({ title: '模型ID无效', icon: 'none' })
		return
	}
	uni.navigateTo({ url: `/pages/custom/detail?id=${id}` })
}

const cancelFavorite = (item) => {
	uni.showModal({
		title: '提示',
		content: `确定取消收藏「${item?.modelName || '该模型'}」吗？`,
		success: async (res) => {
			if (!res.confirm) return
			try {
				await toggleModelFavoriteApi(item.id)
				await loadList()
				uni.$emit('favorite-model-changed')
				uni.showToast({ title: '已取消收藏', icon: 'success' })
			} catch (error) {
				uni.showToast({ title: error?.message || '操作失败', icon: 'none' })
			}
		}
	})
}

const handleFavoriteChanged = () => {
	loadList()
}

onLoad(() => {
	uni.$on('favorite-model-changed', handleFavoriteChanged)
})

onShow(() => {
	if (!ensureLoginOrRedirect()) return
	loadList()
})

onUnload(() => {
	uni.$off('favorite-model-changed', handleFavoriteChanged)
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

.favorite-page {
	min-height: 100vh;
	background: $bg;
	padding: 28rpx;
}

.summary {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 32rpx;
	margin-bottom: 28rpx;
	border-radius: 24rpx;
	background: $gradient;
	box-shadow: 0 12rpx 40rpx rgba(0, 191, 255, 0.2);

	.summary-main { min-width: 0; }
	.title { font-size: 36rpx; font-weight: 700; color: #ffffff; display: block; }
	.desc { margin-top: 10rpx; font-size: 24rpx; color: rgba(255, 255, 255, 0.9); display: block; line-height: 1.45; }
	.summary-badge {
		width: 128rpx;
		height: 128rpx;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.22);
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		backdrop-filter: blur(6px);
		-webkit-backdrop-filter: blur(6px);
	}
	.summary-num { font-size: 36rpx; font-weight: 700; color: #ffffff; line-height: 1; }
	.summary-label { font-size: 20rpx; margin-top: 6rpx; color: rgba(255, 255, 255, 0.9); }
}

.state {
	padding: 120rpx 0;
	text-align: center;
	font-size: 26rpx;
	color: $text3;
	line-height: 1.6;
}

.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 16rpx;
}

.empty-icon { font-size: 56rpx; color: #ddd; }

.list-wrap {
	display: flex;
	flex-direction: column;
	gap: 22rpx;
}

.model-card {
	position: relative;
	display: flex;
	padding: 24rpx;
	gap: 22rpx;
	border-radius: 24rpx;
	background: $card;
	box-shadow: $shadow;
	animation: fadeInUp 0.4s ease both;
}

.fav-btn {
	position: absolute;
	top: 18rpx;
	right: 18rpx;
	width: 56rpx;
	height: 56rpx;
	border-radius: 50%;
	background: rgba(255, 255, 255, 0.95);
	box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.08);
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 2;
	&:active { transform: scale(0.92); }
}

.cover {
	width: 180rpx;
	height: 180rpx;
	border-radius: 16rpx;
	background: $bg;
	flex-shrink: 0;
}

.content {
	flex: 1;
	display: flex;
	flex-direction: column;
}

.name { font-size: 30rpx; font-weight: 600; color: $text1; line-height: 1.35; }
.meta { margin-top: 10rpx; font-size: 22rpx; color: $text2; line-height: 1.45; }

.price-row {
	margin-top: 14rpx;
	display: flex;
	align-items: baseline;
	gap: 10rpx;
}

.price { font-size: 30rpx; font-weight: 700; color: $danger; }
.price-tip { font-size: 22rpx; color: $text3; }
</style>
