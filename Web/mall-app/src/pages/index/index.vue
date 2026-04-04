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

		<scroll-view scroll-y class="content-scroll" @scrolltolower="loadMoreModels" lower-threshold="100">
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
				<text class="section-title">现有模型</text>
				<view class="sort-tabs">
					<view
						v-for="tab in sortTabs"
						:key="tab.value"
						:class="['sort-tab', currentSort === tab.value ? 'active' : '']"
						@tap="changeSort(tab.value)"
					>
						<text class="sort-tab-text">{{ tab.label }}</text>
						<view v-if="currentSort === tab.value && tab.canReverse" class="sort-arrow">
							<u-icon :name="sortAsc ? 'arrow-up' : 'arrow-down'" size="10" color="#fff"></u-icon>
						</view>
					</view>
				</view>
			</view>

			<view class="skeleton-wrap" v-if="pageLoading && !allModels.length">
				<u-skeleton title :rows="4" :loading="pageLoading" :animate="true"></u-skeleton>
			</view>

			<!-- 瀑布流布局 -->
			<view class="waterfall-container" v-else-if="allModels.length">
				<view class="waterfall-column" v-for="(column, colIndex) in waterfallColumns" :key="colIndex">
					<view
						class="waterfall-card"
						v-for="model in column"
						:key="model.id"
						@click="goDetail(model.id)"
					>
						<view class="card-media">
							<!-- 图片容器 -->
							<view class="image-wrapper">
								<!-- 缩略图（如果有） -->
								<image
									v-if="model.thumbnailUrl"
									:src="model.thumbnailUrl"
									mode="aspectFill"
									class="card-image thumb-image"
									:class="{ hidden: model._imgLoaded }"
									lazy-load
								></image>
								<!-- 主图 -->
								<image
									:src="model.watermarkedMainImageUrl || model.mainImageUrl || defaultBanner"
									mode="aspectFill"
									class="card-image main-image"
									:class="{ loaded: model._imgLoaded }"
									lazy-load
									@load="onImgLoad(model)"
									@error="onImgError(model)"
								></image>
								<!-- 加载失败的占位图 -->
								<view v-if="model._imgError" class="error-placeholder">
									<u-icon name="photo" size="30" color="#c0c0c0"></u-icon>
								</view>
							</view>
							<view class="fav-btn" :class="{ active: !!model.favorited }" @click.stop="toggleFavorite(model)">
								<u-icon :name="model.favorited ? 'heart-fill' : 'heart'" size="14" :color="model.favorited ? '#ef4444' : '#c0c0c0'"></u-icon>
							</view>
							<view class="card-stats">
								<view class="stat-item">
									<u-icon name="download" size="11" color="#fff"></u-icon>
									<text class="stat-text">{{ formatCount(model.downloadCount) }}</text>
								</view>
							</view>
						</view>
						<view class="card-info">
							<text class="card-name u-line-2">{{ model.modelName }}</text>
							<view class="card-meta">
								<view class="card-rate" v-if="model.avgScore > 0">
									<u-icon name="star-fill" size="11" color="#f59e0b"></u-icon>
									<text class="rate-text">{{ model.avgScore?.toFixed(1) || '--' }}</text>
								</view>
								<text class="card-price">¥{{ formatPrice(model.basePrice) }}</text>
							</view>
						</view>
					</view>
				</view>
			</view>

			<view class="empty-wrap" v-else-if="!pageLoading && !allModels.length">
				<text class="empty-text">暂无模型</text>
			</view>

			<view class="loading-wrap" v-if="fetching">
				<u-loadmore status="loading" loadingText="加载中..." loadmoreText="" nomoreText=""></u-loadmore>
			</view>

			<view class="loadmore-wrap" v-else-if="allModels.length && !hasMore">
				<u-loadmore status="nomore" nomoreText="没有更多了"></u-loadmore>
			</view>

			<view class="safe-area-bottom"></view>
		</scroll-view>

		<!-- #ifdef APP-PLUS -->
		<AppTabbar />
		<!-- #endif -->

	</view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { getHomeConfigApi } from '../../api/home'
import { getCategoryTreeApi, getMyFavoriteModelIdsApi, toggleModelFavoriteApi, getModelListApi } from '../../api/model'
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

// 排序相关
const sortTabs = [
	{ label: '最新', value: 'create_time', canReverse: true },
	{ label: '销量', value: 'sales', canReverse: true },
	{ label: '评分', value: 'score', canReverse: true },
	{ label: '价格', value: 'price', canReverse: true }
]
const currentSort = ref('create_time')
const sortAsc = ref(false) // false = 降序(倒序), true = 升序(正序)

// 分页相关
const allModels = ref([])
const pageNum = ref(1)
const pageSize = 20
const hasMore = ref(true)

const quickCats = ref([
	{ id: 0, name: '全部', icon: 'grid', iconColor: '#333333', iconBg: '#f4f4f4' }
])

const favoriteMap = ref({})

// 瀑布流双列布局
const waterfallColumns = computed(() => {
	const leftColumn = []
	const rightColumn = []

	allModels.value.forEach((model, index) => {
		if (index % 2 === 0) {
			leftColumn.push(model)
		} else {
			rightColumn.push(model)
		}
	})

	return [leftColumn, rightColumn]
})

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
		const tree = await getCategoryTreeApi(true)
		const flattened = flattenCategories(Array.isArray(tree) ? tree : [])
		if (!flattened.length) return

		quickCats.value = flattened.slice(0, 8).map((cat) => ({
			id: cat.id,
			name: cat.name,
			icon: cat.icon || resolveCategoryIcon(cat.name),
			iconColor: '#333333',
			iconBg: '#f4f4f4'
		}))
	} catch (error) {
		// ignore
	}
}

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

// 格式化数量显示
const formatCount = (value) => {
	const count = Number(value) || 0
	if (count >= 10000) {
		return (count / 10000).toFixed(1) + 'w'
	}
	if (count >= 1000) {
		return (count / 1000).toFixed(1) + 'k'
	}
	return String(count)
}

const formatPrice = (value) => {
	const amount = Number(value)
	if (Number.isNaN(amount)) return '0.00'
	return amount.toFixed(2)
}

// 切换排序
const changeSort = (sortValue) => {
	if (currentSort.value === sortValue) {
		// 点击已选中的标签，切换排序方向
		sortAsc.value = !sortAsc.value
	} else {
		// 切换到新标签，默认降序
		currentSort.value = sortValue
		sortAsc.value = false
	}
	// 重新加载数据
	allModels.value = []
	pageNum.value = 1
	hasMore.value = true
	fetchModelList()
}

// 获取模型列表
const fetchModelList = async (force = false) => {
	if (fetching.value || !hasMore.value) return

	fetching.value = true
	try {
		// 根据排序字段和方向生成 orderBy 参数
		let orderBy = currentSort.value
		if (currentSort.value === 'price') {
			orderBy = sortAsc.value ? 'price_asc' : 'price_desc'
		} else if (currentSort.value === 'create_time') {
			orderBy = sortAsc.value ? 'create_time_asc' : 'create_time'
		} else if (currentSort.value === 'sales') {
			orderBy = sortAsc.value ? 'sales_asc' : 'sales'
		} else if (currentSort.value === 'score') {
			orderBy = sortAsc.value ? 'score_asc' : 'score'
		}

		const res = await getModelListApi({
			pageNum: pageNum.value,
			pageSize: pageSize,
			status: 1,
			orderBy: orderBy
		}, force)

		const records = Array.isArray(res?.records) ? res.records : []

		// 处理模型数据
		const processedRecords = records.map(item => ({
			id: item.id,
			modelName: item.modelName,
			mainImageUrl: item.mainImageUrl,
			watermarkedMainImageUrl: item.watermarkedMainImageUrl,
			thumbnailUrl: item.thumbnailUrl,
			basePrice: item.basePrice,
			downloadCount: item.downloadCount || 0,
			avgScore: item.avgScore || 0,
			favorited: !!favoriteMap.value[String(item.id)],
			_imgLoaded: false,
			_imgError: false
		}))

		if (pageNum.value === 1) {
			allModels.value = processedRecords
		} else {
			allModels.value = [...allModels.value, ...processedRecords]
		}

		// 判断是否还有更多
		hasMore.value = records.length >= pageSize
		if (hasMore.value) {
			pageNum.value++
		}

		pageLoading.value = false
	} catch (error) {
		uni.showToast({
			title: error.message || '加载失败',
			icon: 'none'
		})
	} finally {
		fetching.value = false
	}
}

// 加载更多
const loadMoreModels = () => {
	if (hasMore.value && !fetching.value) {
		fetchModelList()
	}
}

const fetchHomeData = async (force = false) => {
	if (!ensureLoginOrRedirect()) return

	await loadFavoriteMap()
	await fetchQuickCategories()

	// 获取首页配置
	try {
		const configData = await getHomeConfigApi(force)
		if (configData) {
			banners.value = Array.isArray(configData.banners) && configData.banners.length
				? configData.banners
				: banners.value
			notices.value = Array.isArray(configData.notices) ? configData.notices : []
		}
	} catch (error) {
		// ignore
	}

	// 获取模型列表
	await fetchModelList(force)
}

onShow(() => {
	fetchHomeData(false)
})

onPullDownRefresh(async () => {
	// 重置状态
	allModels.value = []
	pageNum.value = 1
	hasMore.value = true

	await fetchHomeData(true)
	uni.stopPullDownRefresh()
})

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

const onBannerTap = (item) => {
	if (!item?.linkValue) return
	if (item.linkValue.startsWith('/pages/')) {
		uni.navigateTo({ url: item.linkValue })
	}
}

// 图片加载完成
const onImgLoad = (model) => {
	const idx = allModels.value.findIndex(m => m.id === model.id)
	if (idx >= 0) {
		allModels.value[idx] = { ...allModels.value[idx], _imgLoaded: true, _imgError: false }
	}
}

// 图片加载失败
const onImgError = (model) => {
	const idx = allModels.value.findIndex(m => m.id === model.id)
	if (idx >= 0) {
		allModels.value[idx] = { ...allModels.value[idx], _imgLoaded: true, _imgError: true }
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
		// 更新列表中的收藏状态
		allModels.value = allModels.value.map(item => {
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
   首页 — 果冻质感极简设计 + 瀑布流布局
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

/* —— 排序标签 —— */
.sort-tabs {
	display: flex;
	gap: 16rpx;
}

.sort-tab {
	padding: 10rpx 20rpx;
	border-radius: 999rpx;
	background: $surface;
	transition: all 0.2s ease;
	display: flex;
	align-items: center;
	gap: 4rpx;

	&.active {
		background: $gradient-primary;

		.sort-tab-text {
			color: #fff;
			font-weight: 600;
		}
	}
}

.sort-tab-text {
	font-size: 24rpx;
	color: $text-muted;
}

.sort-arrow {
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

/* —— 瀑布流布局 —— */
.waterfall-container {
	display: flex;
	gap: 16rpx;
	padding: 24rpx 32rpx 0;
}

.waterfall-column {
	flex: 1;
	display: flex;
	flex-direction: column;
	gap: 16rpx;
}

.waterfall-card {
	background: $surface-raised;
	border-radius: 20rpx;
	overflow: hidden;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.4s ease forwards;
	opacity: 0;
	transition: transform 0.2s ease;

	&:active {
		transform: scale(0.98);
	}
}

.card-media {
	position: relative;
	background: #f0f2f5;
	overflow: hidden;
}

.image-wrapper {
	position: relative;
	width: 100%;
	aspect-ratio: 1 / 1;
	background: linear-gradient(135deg, #f0f2f5 0%, #e8eaed 100%);
}

.card-image {
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 100%;
	object-fit: cover;
}

/* 缩略图样式 - 模糊效果 */
.thumb-image {
	filter: blur(8px);
	transform: scale(1.05);
	transition: opacity 0.4s ease;
	opacity: 1;
}

.thumb-image.hidden {
	opacity: 0;
}

/* 主图样式 */
.main-image {
	opacity: 0;
	transition: opacity 0.4s ease;
	z-index: 1;
}

.main-image.loaded {
	opacity: 1;
}

/* 加载失败占位 */
.error-placeholder {
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 100%;
	display: flex;
	align-items: center;
	justify-content: center;
	background: linear-gradient(135deg, #f0f2f5 0%, #e8eaed 100%);
	z-index: 2;
}

.card-stats {
	position: absolute;
	bottom: 10rpx;
	left: 10rpx;
	display: flex;
	gap: 12rpx;
}

.stat-item {
	display: flex;
	align-items: center;
	gap: 4rpx;
	padding: 4rpx 10rpx;
	background: rgba(0, 0, 0, 0.45);
	border-radius: 999rpx;
	backdrop-filter: blur(4px);
}

.stat-text {
	font-size: 20rpx;
	color: #fff;
}

.card-info {
	padding: 16rpx;
}

.card-name {
	font-size: 26rpx;
	color: $text-primary;
	line-height: 1.4;
	font-weight: 600;
}

.card-meta {
	margin-top: 12rpx;
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.card-rate {
	display: flex;
	align-items: center;
	gap: 4rpx;
}

.rate-text {
	font-size: 22rpx;
	font-weight: 600;
	color: #f59e0b;
}

.card-price {
	font-size: 32rpx;
	font-weight: 700;
	color: $sky-deep;
}

.empty-wrap {
	padding: 120rpx 0;
	text-align: center;
}

.empty-text {
	font-size: 28rpx;
	color: $text-muted;
}

.loading-wrap {
	margin-top: 32rpx;
	padding: 0 32rpx 10rpx;
}

.loadmore-wrap {
	margin-top: 24rpx;
	padding: 0 32rpx;
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

/* —— 收藏按钮 —— */
.fav-btn {
	position: absolute;
	top: 10rpx;
	right: 10rpx;
	width: 48rpx;
	height: 48rpx;
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
</style>
