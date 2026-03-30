<template>
	<view class="home-page">
		<view class="top-header glass-nav" :style="{ paddingTop: statusBarHeight + 'px' }">
			<view class="brand-wrap">
				<view class="brand-logo">
					<u-icon name="grid" size="18" color="#00bfff"></u-icon>
				</view>
				<text class="brand-title">3D模型商城</text>
			</view>
	
		</view>

		<view class="search-panel glass-nav">
			<view class="search-box">
				<view class="search-icon-wrap" @tap="handleSearch">
					<u-icon name="search" size="18" color="#94a3b8"></u-icon>
				</view>
				<input
					v-model="searchKeyword"
					class="search-input"
					type="text"
					placeholder="搜索模型、零件、创作者..."
					placeholder-style="color:#94a3b8"
					confirm-type="search"
					@confirm="handleSearch"
				/>
			</view>
		</view>

		<scroll-view scroll-y class="content-scroll" @scrolltolower="loadMoreCards" lower-threshold="100">
			<view v-if="banners.length" class="banner-swiper-wrap">
				<swiper
					class="banner-swiper"
					:autoplay="true"
					:circular="true"
					:interval="3200"
					:duration="500"
				>
					<swiper-item v-for="(item, index) in banners" :key="index">
						<view class="banner-card" @tap="onBannerTap(item)">
							<image :src="item.imageUrl" mode="aspectFill" class="banner-image"></image>
							<view class="banner-mask"></view>
							<view class="banner-meta">
								<text class="banner-tag">热门推荐</text>
								<text class="banner-title">{{ item.title || '定制手办专区' }}</text>
								<text class="banner-sub">{{ item.linkValue || '立即开启你的专属3D创作之旅' }}</text>
							</view>
						</view>
					</swiper-item>
				</swiper>
			</view>

			<view class="notice-wrap" v-if="notices.length">
				<view class="notice-inline">
					<u-icon name="volume" size="13" color="#00bfff"></u-icon>
					<swiper
						class="notice-swiper"
						:autoplay="true"
						:circular="true"
						:interval="3000"
						:duration="500"
						vertical
					>
						<swiper-item v-for="(item, index) in notices" :key="index">
							<text class="notice-inline-text">{{ item.title || item.content }}</text>
						</swiper-item>
					</swiper>
				</view>
			</view>

			<view class="section-head">
				<text class="section-title">分类浏览</text>
				<text class="section-link" @tap="goMall()">查看全部</text>
			</view>

			<view class="category-panel">
				<u-grid :border="false" :col="4" align="center">
					<u-grid-item v-for="(cat, index) in quickCats" :key="index" @tap="goMall(cat)">
						<view class="cat-icon-wrap" :style="{ background: cat.iconBg }">
							<image v-if="isImageUrl(cat.icon)" :src="cat.icon" class="cat-icon-img" mode="aspectFit" />
							<u-icon v-else :name="cat.icon" size="22" :color="cat.iconColor"></u-icon>
						</view>
						<text class="cat-name">{{ cat.name }}</text>
					</u-grid-item>
				</u-grid>
			</view>

			<view class="section-head product-head">
				<text class="section-title">精选模型</text>
				<view class="filter-btn">
					<u-icon name="list" size="18" color="#94a3b8"></u-icon>
				</view>
			</view>

			<view class="skeleton-wrap" v-if="pageLoading && !hotModels.length">
				<u-skeleton title :rows="4" :loading="pageLoading" :animate="true"></u-skeleton>
			</view>

			<view class="product-grid" v-else>
				<view
					class="product-card"
					v-for="(model, index) in visibleModels"
					:key="model.id || index"
					@click="goDetail(model.id)"
					:style="{ animationDelay: `${index * 0.08}s` }"
				>
					<view class="product-media">
						<image :src="model.mainImageUrl || defaultBanner" mode="aspectFill" class="product-image" lazy-load></image>
						<view class="fav-btn" :class="{ active: !!model.favorited }" @click.stop="toggleFavorite(model)">
							<u-icon :name="model.favorited ? 'heart-fill' : 'heart'" size="15" :color="model.favorited ? '#ef4444' : '#c0c0c0'"></u-icon>
						</view>
					</view>
					<view class="product-info">
						<view class="title-row">
							<text class="product-name u-line-2">{{ model.modelName }}</text>
							<view class="product-rate">
								<u-icon name="star-fill" size="11" color="#f59e0b"></u-icon>
								<text class="rate-text">{{ formatScore(model.avgOverallScore) }}</text>
							</view>
						</view>
						<text class="product-spec">{{ model.baseSizeDisplay || '--*--*--' }} · {{ model.baseVolumeDisplay || '--' }} mm³</text>
						<view class="product-bottom">
							<view class="price-wrap">
								<text class="price-symbol">¥</text>
								<text class="product-price">{{ formatPrice(model.basePrice) }}</text>
							</view>
						</view>
					</view>
				</view>
			</view>

			<view class="loading-wrap" v-if="fetching">
				<u-loadmore status="loading" loadingText="加载中..." loadmoreText="" nomoreText=""></u-loadmore>
			</view>

			<view class="safe-area-bottom"></view>
		</scroll-view>

		<!-- #ifdef APP-PLUS -->
		<AppTabbar />
		<!-- #endif -->

	</view>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { getHomeConfigApi, getHotModelsApi } from '../../api/home'
import { getCategoryTreeApi, getMyFavoriteModelIdsApi, toggleModelFavoriteApi } from '../../api/model'
import { getModelOrderCommentStatsApi } from '../../api/order'
import { ensureLoginOrRedirect } from '../../utils/auth'
// #ifdef APP-PLUS
import AppTabbar from '../../components/AppTabbar.vue'
// #endif

const MALL_TARGET_CATEGORY_KEY = 'mall_target_category_id'
const statusBarHeight = ref(0)
const searchKeyword = ref('')
const banners = ref([
	{ imageUrl: 'https://images.unsplash.com/photo-1581092160562-40aa08e78837?w=800', title: '创意定制', linkValue: '开启您的3D打印之旅' },
	{ imageUrl: 'https://images.unsplash.com/photo-1601121141461-9d6647bca1ed?w=800', title: '艺术摆件', linkValue: '让家更有温度' }
])
const notices = ref([])
const defaultBanner = 'https://images.unsplash.com/photo-1601121141461-9d6647bca1ed?w=800'
const pageLoading = ref(true)
const fetching = ref(false)
const renderCount = ref(4)

const quickCats = ref([
	{ id: 0, name: '全部', icon: 'grid', iconColor: '#333333', iconBg: '#f4f4f4' }
])

const hotModels = ref([])

const flattenCategories = (tree = []) => {
	const result = []
	const walk = (nodes) => {
		nodes.forEach((node) => {
			result.push({
				id: node.id,
				name: node.categoryName || node.name || '未命名分类',
				icon: node.icon || ''
			})
			if (Array.isArray(node.children) && node.children.length) {
				walk(node.children)
			}
		})
	}
	walk(tree)
	return result
}

const resolveCategoryIcon = (categoryName = '') => {
	const name = String(categoryName || '')
	if (/家居|家具|摆件|生活/.test(name)) return 'home'
	if (/工程|工业|零件|机械/.test(name)) return 'setting'
	if (/艺术|创意|手办|潮玩|动漫/.test(name)) return 'gift'
	if (/摄影|图片|渲染|展示/.test(name)) return 'camera'
	if (/精选|热门|推荐/.test(name)) return 'star'
	if (/社区|交流|论坛/.test(name)) return 'chat'
	if (/我的|个人|账户/.test(name)) return 'account'
	if (/购物|商城|商品/.test(name)) return 'shopping-cart'
	return 'grid'
}

const isImageUrl = (url) => {
	if (!url) return false
	return url.startsWith('http://') || url.startsWith('https://')
}

const fetchQuickCategories = async () => {
	try {
		// 强制刷新获取最新分类数据（包括 icon）
		const tree = await getCategoryTreeApi(true)
		console.log('分类树数据:', JSON.stringify(tree))
		const flattened = flattenCategories(Array.isArray(tree) ? tree : [])
		if (!flattened.length) return

		quickCats.value = flattened.slice(0, 8).map((cat, index) => ({
			id: cat.id,
			name: cat.name,
			icon: cat.icon || resolveCategoryIcon(cat.name),
			iconColor: '#333333',
			iconBg: '#f4f4f4'
		}))
		console.log('quickCats:', JSON.stringify(quickCats.value))
	} catch (error) {
		// ignore category fetch error and keep fallback
	}
}

const visibleModels = computed(() => hotModels.value.slice(0, renderCount.value))

const favoriteMap = ref({})

const loadFavoriteMap = async () => {
	try {
		const ids = await getMyFavoriteModelIdsApi()
		if (!Array.isArray(ids)) {
			favoriteMap.value = {}
			return
		}
		favoriteMap.value = ids.reduce((acc, id) => {
			acc[String(id)] = true
			return acc
		}, {})
	} catch (error) {
		favoriteMap.value = {}
	}
}

const initSystemInfo = () => {
	const sys = uni.getSystemInfoSync()
	statusBarHeight.value = sys.statusBarHeight || 20
}

initSystemInfo()

const formatBaseSize = (value) => {
	const raw = String(value || '').trim()
	if (!raw) return '--*--*--'
	const normalized = raw
		.replace(/[xX×\*]/g, '*')
		.split('*')
		.map(part => part.trim())
		.filter(Boolean)
	if (normalized.length !== 3) {
		return raw.replace(/[xX×]/g, '*')
	}
	return normalized.join('*')
}

const formatBaseVolume = (value) => {
	if (value == null || value === '') return '--'
	const volume = Number(value)
	if (!Number.isFinite(volume) || volume <= 0) return '--'
	return volume >= 1000 ? volume.toFixed(0) : volume.toFixed(2)
}

const formatScore = (value) => {
	const score = Number(value)
	if (!Number.isFinite(score) || score <= 0) return '--'
	return Math.max(0, Math.min(5, score)).toFixed(1)
}

const enrichHomeModels = async () => {
	if (!hotModels.value.length) return

	const tasks = hotModels.value.map(async (model) => {
		const modelId = model.id
		if (!modelId) {
			return {
				...model,
				baseSizeDisplay: formatBaseSize(model.baseSize),
				avgOverallScore: Number(model.avgOverallScore || 0),
				favorited: !!favoriteMap.value[String(model.id)]
			}
		}

		const [statsRes, detailRes] = await Promise.allSettled([
			getModelOrderCommentStatsApi(modelId),
			Promise.resolve({
				baseSize: model.baseSize,
				baseVolume: model.baseVolume
			})
		])

		const score = statsRes.status === 'fulfilled'
			? Number(statsRes.value?.avgOverallScore || statsRes.value?.avgModelScore || 0)
			: Number(model.avgOverallScore || 0)

		const detailBaseSize = detailRes.status === 'fulfilled'
			? (detailRes.value?.baseSize || detailRes.value?.base_size || detailRes.value?.data?.baseSize || detailRes.value?.data?.base_size || '')
			: ''
        const detailBaseVolume = detailRes.status === 'fulfilled'
			? (detailRes.value?.baseVolume || detailRes.value?.base_volume || detailRes.value?.data?.baseVolume || detailRes.value?.data?.base_volume || '')
			: ''

		const resolvedBaseSize = model.baseSize || detailBaseSize
		const resolvedBaseVolume = model.baseVolume || detailBaseVolume

		return {
			...model,
			baseSize: resolvedBaseSize,
			baseVolume: resolvedBaseVolume,
			baseSizeDisplay: formatBaseSize(resolvedBaseSize),
			baseVolumeDisplay: formatBaseVolume(resolvedBaseVolume),
			avgOverallScore: score,
			designerName: model.designerName || '平台设计师',
			favorited: !!favoriteMap.value[String(modelId)]
		}
	})

	hotModels.value = await Promise.all(tasks)
}

const fetchHomeData = async (force = false) => {
	if (!ensureLoginOrRedirect()) return
	if (fetching.value) return
	await loadFavoriteMap()
	await fetchQuickCategories()

	fetching.value = true
	try {
		const [configRes, modelRes] = await Promise.allSettled([
			getHomeConfigApi(force),
			getHotModelsApi(1, force)
		])

		const configData = configRes.status === 'fulfilled' ? configRes.value : null
		const modelData = modelRes.status === 'fulfilled' ? modelRes.value : null

		if (configData) {
			banners.value = Array.isArray(configData.banners) && configData.banners.length
				? configData.banners
				: banners.value
			notices.value = Array.isArray(configData.notices) ? configData.notices : []
		}

		if (modelData?.records) {
			hotModels.value = modelData.records.map(item => ({
				id: item.id,
				modelName: item.modelName,
				designerName: item.designerName || item.designer || '',
				basePrice: item.basePrice,
				mainImageUrl: item.mainImageUrl,
				baseSize: item.baseSize || item.base_size || '',
				baseVolume: item.baseVolume || item.base_volume || '',
				avgOverallScore: Number(item.avgOverallScore || item.avgScore || 0),
				favorited: !!favoriteMap.value[String(item.id)]
			}))
		}

		await enrichHomeModels()

		renderCount.value = 4
		await nextTick()
		setTimeout(() => {
			renderCount.value = Math.min(6, hotModels.value.length)
		}, 120)
		pageLoading.value = false
	} catch (error) {
		if (!hotModels.value.length) {
			uni.showToast({
				title: error.message || '首页数据加载失败',
				icon: 'none'
			})
		}
	} finally {
		fetching.value = false
	}
}

onShow(() => {
	fetchHomeData(false)
})

onPullDownRefresh(async () => {
	await fetchHomeData(true)
	uni.stopPullDownRefresh()
})

const loadMoreCards = () => {
	if (renderCount.value < hotModels.value.length) {
		renderCount.value = Math.min(renderCount.value + 2, hotModels.value.length)
	}
}

const goMall = (cat) => {
	const categoryId = Number(cat?.id || 0)
	try {
		if (categoryId > 0) {
			uni.setStorageSync(MALL_TARGET_CATEGORY_KEY, categoryId)
		} else {
			uni.removeStorageSync(MALL_TARGET_CATEGORY_KEY)
		}
	} catch (error) {
		// ignore storage error
	}

	uni.switchTab({
		url: '/pages/mall/list',
		success: () => {
			if (categoryId > 0) {
				setTimeout(() => {
					uni.$emit('mall-category-change', categoryId)
				}, 120)
			}
		},
		fail: () => {
			uni.reLaunch({
				url: '/pages/mall/list'
			})
		}
	})
}

const goDetail = (rawId) => {
	const modelId = String(rawId || '').trim()
	if (!modelId) {
		uni.showToast({ title: '模型ID无效', icon: 'none' })
		return
	}
	uni.navigateTo({ url: '/pages/custom/detail?id=' + encodeURIComponent(modelId) })
}

const handleSearch = () => {
	const keyword = (searchKeyword.value || '').trim()
	if (!keyword) {
		uni.showToast({ title: '请输入搜索关键词', icon: 'none' })
		return
	}
	uni.navigateTo({
		url: `/pages/mall/search?keyword=${encodeURIComponent(keyword)}`
	})
}

const formatPrice = (value) => {
	const amount = Number(value)
	if (Number.isNaN(amount)) return '0.00'
	return amount.toFixed(2)
}

const onBannerTap = (item) => {
	if (!item?.linkValue) return
	if (item.linkValue.startsWith('/pages/')) {
		uni.navigateTo({ url: item.linkValue })
	}
}

const toggleFavorite = (model) => {
	const modelId = String(model?.id || '').trim()
	if (!modelId) return
	toggleModelFavoriteApi(modelId).then((res) => {
		const nextState = !!res?.active
		favoriteMap.value = {
			...favoriteMap.value,
			[modelId]: nextState
		}
		hotModels.value = hotModels.value.map(item => {
			if (String(item.id) !== modelId) return item
			return {
				...item,
				favorited: nextState
			}
		})
		uni.$emit('favorite-model-changed')
	}).catch((error) => {
		uni.showToast({ title: error.message || '收藏操作失败', icon: 'none' })
	})
}
</script>

<style scoped lang="scss">
/* ============================================
   首页 — 果冻质感极简设计
   ============================================ */

$sky-blue: #00bfff;
$sky-light: #5ce1ff;
$sky-deep: #0099cc;

$surface: #f8f8f8;
$surface-raised: #ffffff;
$text-primary: #1a2030;
$text-secondary: #5a6a7a;
$text-muted: #94a3b8;

$shadow-card: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
$gradient-primary: linear-gradient(135deg, $sky-blue 0%, $sky-light 100%);

.home-page {
	min-height: 100vh;
	display: flex;
	flex-direction: column;
	background: $surface;
	max-width: 750rpx;
	margin: 0 auto;
}

/* —— 毛玻璃顶栏 —— */
.top-header {
	position: sticky;
	top: 0;
	z-index: 20;
	padding: 20rpx 32rpx 16rpx;
	display: flex;
	justify-content: space-between;
	align-items: center;
	background: rgba(255, 255, 255, 0.72);
	backdrop-filter: blur(24px);
	-webkit-backdrop-filter: blur(24px);
}

.brand-wrap {
	display: flex;
	align-items: center;
	gap: 14rpx;
}

.brand-logo {
	width: 52rpx;
	height: 52rpx;
	border-radius: 16rpx;
	background: rgba(0, 191, 255, 0.08);
	display: flex;
	align-items: center;
	justify-content: center;
}

.brand-title {
	font-size: 40rpx;
	font-weight: 800;
	color: $text-primary;
}

.header-actions {
	display: flex;
	align-items: center;
}

.action-btn {
	width: 64rpx;
	height: 64rpx;
	border-radius: 999rpx;
	background: rgba(0, 0, 0, 0.03);
	display: flex;
	align-items: center;
	justify-content: center;
	transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);

	&:active {
		transform: scale(0.92);
	}
}

/* —— 搜索栏 —— */
.search-panel {
	padding: 0 32rpx;
	height: 92rpx;
	display: flex;
	align-items: center;
	background: rgba(255, 255, 255, 0.72);
	backdrop-filter: blur(24px);
	-webkit-backdrop-filter: blur(24px);
}

.search-box {
	width: 100%;
	height: 72rpx;
	background: $surface;
	border-radius: 999rpx;
	padding: 0 24rpx;
	display: flex;
	align-items: center;
	gap: 12rpx;
	transition: box-shadow 0.3s ease;

	&:focus-within {
		box-shadow: 0 0 0 3rpx rgba(0, 191, 255, 0.18);
	}
}

.search-icon-wrap {
	display: flex;
	align-items: center;
	justify-content: center;
}

.search-input {
	flex: 1;
	font-size: 28rpx;
	color: $text-primary;
}

.content-scroll {
	flex: 1;
	padding-bottom: 132rpx;
}

/* —— 轮播图 —— */
.banner-swiper-wrap {
	padding: 20rpx 32rpx 0;
	animation: fadeInUp 0.6s ease forwards;
	opacity: 0;
}

.banner-swiper {
	height: 340rpx;
}

.banner-card {
	width: 100%;
	height: 340rpx;
	border-radius: 28rpx;
	overflow: hidden;
	position: relative;
	background: #e8ecf0;
	transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);

	&:active {
		transform: scale(0.98);
	}
}

.banner-image {
	width: 100%;
	height: 100%;
}

.banner-mask {
	position: absolute;
	inset: 0;
	background: linear-gradient(180deg, rgba(0, 0, 0, 0.05) 30%, rgba(0, 0, 0, 0.65) 100%);
}

.banner-meta {
	position: absolute;
	left: 28rpx;
	right: 28rpx;
	bottom: 24rpx;
	display: flex;
	flex-direction: column;
	gap: 8rpx;
}

.banner-tag {
	align-self: flex-start;
	padding: 6rpx 18rpx;
	border-radius: 999rpx;
	font-size: 18rpx;
	font-weight: 600;
	background: $gradient-primary;
	color: #fff;
}

.banner-title {
	font-size: 40rpx;
	font-weight: 700;
	color: #fff;
	line-height: 1.25;
}

.banner-sub {
	font-size: 28rpx;
	color: rgba(255, 255, 255, 0.85);
}

/* —— 公告条 —— */
.notice-wrap {
	margin-top: 20rpx;
	padding: 0 32rpx;
	animation: fadeInUp 0.5s ease 0.15s forwards;
	opacity: 0;
}

.notice-inline {
	height: 64rpx;
	padding: 0 24rpx;
	background: rgba(0, 191, 255, 0.06);
	border-radius: 999rpx;
	display: flex;
	align-items: center;
	gap: 12rpx;
}

.notice-swiper {
	flex: 1;
	height: 64rpx;
}

.notice-inline-text {
	font-size: 24rpx;
	color: $sky-deep;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
	line-height: 64rpx;
}

/* —— 区块标题 —— */
.section-head {
	margin-top: 36rpx;
	padding: 0 32rpx;
	display: flex;
	justify-content: space-between;
	align-items: center;
	animation: fadeInUp 0.5s ease forwards;
	opacity: 0;

	&:nth-of-type(2) {
		animation-delay: 0.1s;
	}
}

.section-title {
	font-size: 36rpx;
	font-weight: 700;
	color: $text-primary;
}

.section-link {
	font-size: 26rpx;
	font-weight: 500;
	color: $sky-deep;
}

/* —— 分类网格 —— */
.category-panel {
	margin: 24rpx 32rpx 0;
}

:deep(.category-panel .u-grid-item) {
	padding-bottom: 20rpx;
}

.cat-icon-wrap {
	width: 88rpx;
	height: 88rpx;
	border-radius: 24rpx;
	background: rgba(0, 191, 255, 0.06);
	display: flex;
	align-items: center;
	justify-content: center;
	margin: 0 auto 10rpx;
	transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
	animation: jellyPop 0.45s ease forwards;
	opacity: 0;
}

.cat-icon-img {
	width: 48rpx;
	height: 48rpx;
}

:deep(.category-panel .u-grid-item) {
	&:active .cat-icon-wrap {
		transform: scale(0.92);
	}
}

.cat-name {
	font-size: 24rpx;
	color: $text-secondary;
	line-height: 1.3;
	font-weight: 500;
}

/* —— 产品区块 —— */
.product-head {
	margin-top: 32rpx;
}

.filter-btn {
	width: 48rpx;
	height: 48rpx;
	border-radius: 50%;
	display: flex;
	align-items: center;
	justify-content: center;
}

.skeleton-wrap {
	margin: 28rpx 32rpx 0;
	background: #fff;
	border-radius: 24rpx;
	padding: 28rpx;
	box-shadow: $shadow-card;
}

/* —— 产品双列网格 —— */
.product-grid {
	margin-top: 24rpx;
	padding: 0 32rpx;
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 20rpx;
}

.product-card {
	background: $surface-raised;
	border-radius: 24rpx;
	overflow: hidden;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.5s ease forwards;
	opacity: 0;
	transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.25s ease;

	&:active {
		transform: scale(0.97);
		box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);
	}
}

.product-media {
	position: relative;
	aspect-ratio: 1 / 1;
	background: #f0f2f5;
	overflow: hidden;
}

.fav-btn {
	position: absolute;
	top: 14rpx;
	right: 14rpx;
	width: 52rpx;
	height: 52rpx;
	border-radius: 999rpx;
	background: rgba(255, 255, 255, 0.85);
	backdrop-filter: blur(8px);
	display: flex;
	align-items: center;
	justify-content: center;
	transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);

	&:active {
		transform: scale(0.88);
	}
}

.fav-btn.active {
	background: #fff1f2;
}

.product-image {
	width: 100%;
	height: 100%;
	animation: imgFadeIn 0.5s ease forwards;
	opacity: 0;
}

@keyframes imgFadeIn {
	from { opacity: 0; transform: scale(0.97); }
	to { opacity: 1; transform: scale(1); }
}

.product-info {
	padding: 16rpx 18rpx 14rpx;
	background: $surface-raised;
}

.title-row {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 10rpx;
}

.product-name {
	font-size: 28rpx;
	color: $text-primary;
	line-height: 1.38;
	font-weight: 600;
	flex: 1;
}

.product-spec {
	margin-top: 8rpx;
	font-size: 22rpx;
	color: $text-muted;
	line-height: 1.3;
}

.product-bottom {
	margin-top: 10rpx;
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.price-wrap {
	display: inline-flex;
	align-items: baseline;
	gap: 4rpx;
}

.price-symbol {
	font-size: 22rpx;
	font-weight: 500;
	color: $sky-deep;
	line-height: 1;
}

.product-price {
	font-size: 38rpx;
	font-weight: 800;
	color: $sky-deep;
	line-height: 1;
}

.product-rate {
	display: inline-flex;
	align-items: center;
	gap: 4rpx;
	margin-top: 2rpx;
}

.rate-text {
	font-size: 22rpx;
	font-weight: 600;
	color: #f59e0b;
}

.loading-wrap {
	margin-top: 32rpx;
	padding: 0 32rpx 10rpx;
}

.safe-area-bottom {
	height: calc(env(safe-area-inset-bottom) + 50rpx);
}

/* —— 全局动画复用 —— */
@keyframes fadeInUp {
	from { opacity: 0; transform: translateY(24rpx); }
	to { opacity: 1; transform: translateY(0); }
}

@keyframes jellyPop {
	0% { opacity: 0; transform: scale(0.88); }
	60% { transform: scale(1.05); }
	100% { opacity: 1; transform: scale(1); }
}
</style>
