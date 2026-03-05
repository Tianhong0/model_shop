<template>
	<view class="home-page">
		<view class="top-header" :style="{ paddingTop: statusBarHeight + 'px' }">
			<view class="brand-wrap">
				<view class="brand-logo">
					<u-icon name="grid" size="18" color="#2563eb"></u-icon>
				</view>
				<text class="brand-title">3D模型商城</text>
			</view>
			<view class="header-actions">
				<view class="action-btn">
					<u-icon name="shopping-cart" size="20" color="#1f2937"></u-icon>
				</view>
			</view>
		</view>

		<view class="search-panel">
			<view class="search-box" @tap="handleSearch">
				<u-icon name="search" size="18" color="#64748b"></u-icon>
				<input
					v-model="searchKeyword"
					class="search-input"
					type="text"
					placeholder="搜索模型、零件、创作者..."
					placeholder-style="color:#64748b"
					confirm-type="search"
					@confirm="handleSearch"
				/>
				<u-icon name="mic" size="18" color="#2563eb"></u-icon>
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

			<view class="notice-wrap" v-if="noticeText">
				<view class="notice-inline">
					<u-icon name="volume" size="13" color="#2563eb"></u-icon>
					<text class="notice-inline-text">最新公告：{{ noticeText }}</text>
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
							<u-icon :name="cat.icon" size="22" :color="cat.iconColor"></u-icon>
						</view>
						<text class="cat-name">{{ cat.name }}</text>
					</u-grid-item>
				</u-grid>
			</view>

			<view class="section-head product-head">
				<text class="section-title">精选模型</text>
				<view class="filter-btn">
					<u-icon name="list" size="18" color="#64748b"></u-icon>
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
				>
					<view class="product-media">
						<image :src="model.mainImageUrl || defaultBanner" mode="aspectFill" class="product-image" lazy-load></image>
						<view class="fav-btn" :class="{ active: !!model.favorited }" @click.stop="toggleFavorite(model)">
							<u-icon :name="model.favorited ? 'heart-fill' : 'heart'" size="15" :color="model.favorited ? '#ef4444' : '#a6a6a6'"></u-icon>
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
import { onShow } from '@dcloudio/uni-app'
import { getHomeConfigApi, getHotModelsApi } from '../../api/home'
import { getCategoryTreeApi, getMyFavoriteModelIdsApi, toggleModelFavoriteApi } from '../../api/model'
import { getModelOrderCommentStatsApi } from '../../api/order'
import { ensureLoginOrRedirect } from '../../utils/auth'
// #ifdef APP-PLUS
import AppTabbar from '../../components/AppTabbar.vue'
// #endif

const CACHE_KEY = 'home_page_cache_v2'
const CACHE_TTL = 2 * 60 * 1000
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
				name: node.categoryName || node.name || '未命名分类'
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

const fetchQuickCategories = async () => {
	try {
		const tree = await getCategoryTreeApi()
		const flattened = flattenCategories(Array.isArray(tree) ? tree : [])
		if (!flattened.length) return

		quickCats.value = flattened.slice(0, 8).map((cat, index) => ({
			id: cat.id,
			name: cat.name,
			icon: resolveCategoryIcon(cat.name),
			iconColor: '#333333',
			iconBg: '#f4f4f4'
		}))
	} catch (error) {
		// ignore category fetch error and keep fallback
	}
}

const noticeText = computed(() => notices.value.map(item => item.title || item.content).filter(Boolean).join('  —  '))
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

const applyHomeData = (configData, modelData) => {
	if (Array.isArray(configData?.banners) && configData.banners.length) {
		banners.value = configData.banners
	}
	notices.value = Array.isArray(configData?.notices) ? configData.notices : []
	hotModels.value = Array.isArray(modelData?.records)
		? modelData.records.map(item => ({
			id: item.id,
			modelName: item.modelName,
			designerName: item.designerName || item.designer || '',
			basePrice: item.basePrice,
			mainImageUrl: item.mainImageUrl,
			baseSize: item.baseSize || item.base_size || '',
			baseVolume: item.baseVolume || item.base_volume || '',
			baseSizeDisplay: formatBaseSize(item.baseSize || item.base_size || ''),
			baseVolumeDisplay: formatBaseVolume(item.baseVolume || item.base_volume || ''),
			avgOverallScore: Number(item.avgOverallScore || item.avgScore || 0),
			favorited: !!favoriteMap.value[String(item.id)]
		}))
		: []
}

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

const readCache = () => {
	try {
		const cached = uni.getStorageSync(CACHE_KEY)
		if (!cached || !cached.timestamp) {
			return null
		}
		return cached
	} catch (error) {
		return null
	}
}

const saveCache = (configData, modelData) => {
	uni.setStorageSync(CACHE_KEY, {
		timestamp: Date.now(),
		configData,
		modelData
	})
}

const fetchHomeData = async (force = false) => {
	if (!ensureLoginOrRedirect()) return
	if (fetching.value) return
	await loadFavoriteMap()
	await fetchQuickCategories()

	const cached = readCache()
	if (cached?.configData && cached?.modelData) {
		applyHomeData(cached.configData, cached.modelData)
		enrichHomeModels()
		pageLoading.value = false
	}

	if (!force && cached?.timestamp && Date.now() - cached.timestamp < CACHE_TTL) {
		return
	}

	fetching.value = true
	try {
		const [configRes, modelRes] = await Promise.allSettled([
			getHomeConfigApi(),
			getHotModelsApi(1)
		])

		const configData = configRes.status === 'fulfilled' ? configRes.value : { banners: banners.value, notices: [] }
		const modelData = modelRes.status === 'fulfilled' ? modelRes.value : { records: hotModels.value }

		applyHomeData(configData, modelData)
		await enrichHomeModels()
		saveCache(configData, modelData)
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
@import "uview-plus/index.scss";

$bg: #ffffff;
$white: #ffffff;
$text-main: #111318;
$text-sub: #62708c;
$line: #e5ebf4;
$primary: #135bec;

.home-page {
	min-height: 100vh;
	display: flex;
	flex-direction: column;
	background: $bg;
	max-width: 750rpx;
	margin: 0 auto;
}

.top-header {
	position: sticky;
	top: 0;
	z-index: 20;
	background: $white;
	padding: 20rpx 28rpx 16rpx;
	display: flex;
	justify-content: space-between;
	align-items: center;
}

.brand-wrap {
	display: flex;
	align-items: center;
	gap: 14rpx;
}

.brand-logo {
	width: 52rpx;
	height: 52rpx;
	border-radius: 14rpx;
	background: #eff6ff;
	display: flex;
	align-items: center;
	justify-content: center;
}

.brand-title {
	font-size: 42rpx;
	font-weight: 800;
	color: $text-main;
}

.header-actions {
	display: flex;
	align-items: center;
	gap: 8rpx;
}

.action-btn {
	width: 64rpx;
	height: 64rpx;
	border-radius: 32rpx;
	background: transparent;
	display: flex;
	align-items: center;
	justify-content: center;
}

.search-panel {
	background: $white;
	padding: 0 28rpx;
	height: 92rpx;
	display: flex;
	align-items: center;
	position: relative;
	z-index: 1;
}

.search-box {
	width: 100%;
	height: 68rpx;
	background: #f1f5f9;
	border-radius: 18rpx;
	padding: 0 20rpx;
	display: flex;
	align-items: center;
	gap: 12rpx;
}

.search-input {
	flex: 1;
	font-size: 30rpx;
	color: #1f2937;
}

.content-scroll {
	flex: 1;
	padding-bottom: 132rpx;
}

.banner-swiper-wrap {
	padding: 16rpx 28rpx 0;
}

.banner-swiper {
	height: 330rpx;
}

.banner-card {
	width: 100%;
	height: 330rpx;
	border-radius: 26rpx;
	overflow: hidden;
	position: relative;
	flex-shrink: 0;
	background: #e5e7eb;
}

.banner-image {
	width: 100%;
	height: 100%;
}

.banner-mask {
	position: absolute;
	inset: 0;
	background: linear-gradient(180deg, rgba(0, 0, 0, 0.08) 20%, rgba(0, 0, 0, 0.72) 100%);
}

.banner-meta {
	position: absolute;
	left: 24rpx;
	right: 24rpx;
	bottom: 20rpx;
	display: flex;
	flex-direction: column;
	gap: 8rpx;
}

.banner-tag {
	align-self: flex-start;
	padding: 6rpx 16rpx;
	border-radius: 999rpx;
	font-size: 18rpx;
	font-weight: 600;
	background: rgba(37, 99, 235, 0.95);
	color: #fff;
}

.banner-title {
	font-size: 44rpx;
	font-weight: 700;
	color: #fff;
	line-height: 1.25;
}

.banner-sub {
	font-size: 34rpx;
	color: rgba(255, 255, 255, 0.88);
}

.notice-wrap {
	margin-top: 14rpx;
	padding: 0 0;
}

.notice-inline {
	height: 54rpx;
	padding: 0 28rpx;
	background: #eff6ff;
	display: flex;
	align-items: center;
	gap: 10rpx;
}

.notice-inline-text {
	font-size: 25rpx;
	color: #1d4ed8;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}

.section-head {
	margin-top: 24rpx;
	padding: 0 28rpx;
	display: flex;
	justify-content: space-between;
	align-items: center;
}

.section-title {
	font-size: 44rpx;
	font-weight: 800;
	color: $text-main;
}

.section-link {
	font-size: 32rpx;
	font-weight: 500;
	color: $primary;
}

.category-panel {
	margin: 18rpx 28rpx 0;
	padding: 4rpx 0 0;
}

:deep(.category-panel .u-grid-item) {
	padding-bottom: 16rpx;
}

.cat-icon-wrap {
	width: 86rpx;
	height: 86rpx;
	border-radius: 22rpx;
	background: #eef2f7;
	display: flex;
	align-items: center;
	justify-content: center;
	margin: 0 auto 10rpx;
}

.cat-name {
	font-size: 24rpx;
	color: #334155;
	line-height: 1.3;
	font-weight: 500;
}

.product-head {
	margin-top: 22rpx;
}

.filter-btn {
	width: 48rpx;
	height: 48rpx;
	border-radius: 50%;
	background: transparent;
	display: flex;
	align-items: center;
	justify-content: center;
}

.skeleton-wrap {
	margin: 24rpx 28rpx 0;
	background: #fafbfc;
	border-radius: 20rpx;
	padding: 26rpx;
}

.product-grid {
	margin-top: 20rpx;
	padding: 0 28rpx;
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 18rpx;
}

.product-card {
	background: #fff;
	border-radius: 18rpx;
	overflow: hidden;
}

.product-media {
	position: relative;
	aspect-ratio: 1 / 1.25;
	background: #edf2f8;
	overflow: hidden;
}

.fav-btn {
	position: absolute;
	top: 12rpx;
	right: 12rpx;
	width: 52rpx;
	height: 52rpx;
	border-radius: 50%;
	background: rgba(255, 255, 255, 0.92);
	border: 1rpx solid #e5e7eb;
	display: flex;
	align-items: center;
	justify-content: center;
}

.fav-btn.active {
	background: rgba(255, 255, 255, 0.95);
	border-color: #fecaca;
}

.product-image {
	width: 100%;
	height: 100%;
}

.product-media::after {
	content: '';
	position: absolute;
	left: 0;
	right: 0;
	bottom: 0;
	height: 42rpx;
	background: linear-gradient(180deg, rgba(250, 251, 252, 0) 0%, rgba(250, 251, 252, 0.92) 100%);
	pointer-events: none;
}

.product-info {
	padding: 12rpx 0 8rpx;
	margin-top: -6rpx;
	position: relative;
	background: #fff;
	border-top-left-radius: 14rpx;
	border-top-right-radius: 14rpx;
}

.title-row {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 10rpx;
}

.product-name {
	font-size: 37rpx;
	color: $text-main;
	line-height: 1.34;
	font-weight: 500;
	flex: 1;
}

.product-spec {
	margin-top: 6rpx;
	font-size: 22rpx;
	color: #94a3b8;
	line-height: 1.3;
}

.product-bottom {
	margin-top: 4rpx;
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
	font-weight: 400;
	color: $text-main;
	line-height: 1;
}

.product-price {
	font-size: 44rpx;
	font-weight: 800;
	color: $text-main;
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
	padding: 0 24rpx 10rpx;
}

.safe-area-bottom {
	height: calc(env(safe-area-inset-bottom) + 40rpx);
}
</style>
