<template>
	<view class="mall-container">
		<view class="search-sticky">
			<view class="search-bar">
				<uni-icons type="search" size="18" color="#94a3b8" @click="fetchModels"></uni-icons>
				<input
					v-model="keyword"
					type="text"
					placeholder="搜索模型名称"
					placeholder-class="placeholder"
					confirm-type="search"
					@confirm="fetchModels"
				/>
				<view class="search-btn" @click="fetchModels">搜索</view>
			</view>
			<view class="reward-entry" @click="goRewardHall">
				<uni-icons type="fire-filled" size="20" color="#f59e0b"></uni-icons>
				<text>悬赏定制中心</text>
				<uni-icons type="right" size="14" color="#94a3b8"></uni-icons>
			</view>
		</view>

		<view class="main-body">
			<scroll-view scroll-y class="sidebar">
				<view
					v-for="(cat, index) in categories"
					:key="cat.id || index"
					class="side-item"
					:class="{ active: activeCat === index }"
					@click="switchCategory(index)"
				>
					<text>{{cat.name}}</text>
				</view>
			</scroll-view>

			<scroll-view scroll-y class="model-content">
				<view class="grid-list">
					<view class="grid-item" v-for="(item, idx) in models" :key="item.id || idx" @click="goDetail(item.id)" :style="{ animationDelay: `${idx * 0.05}s` }">
						<view class="grid-img-wrap">
							<image :src="item.watermarkedMainImageUrl || item.mainImageUrl || defaultBanner" mode="aspectFill"></image>
						</view>
						<view class="grid-info">
							<view class="name-row"><text class="name">{{item.modelName}}</text><text v-if="item.sourceType === 2" class="source-tag">设计者</text></view>
							<text class="price">￥{{item.basePrice || '0.00'}} 起</text>
						</view>
					</view>
				</view>
				<view v-if="!models.length" class="empty-tip">暂无模型数据</view>
			</scroll-view>
		</view>

		<!-- #ifdef APP-PLUS -->
		<AppTabbar />
		<!-- #endif -->
	</view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow, onUnload, onPullDownRefresh } from '@dcloudio/uni-app'
import { getCategoryTreeApi, getModelListApi } from '../../api/model'
import { ensureLoginOrRedirect } from '../../utils/auth'
// #ifdef APP-PLUS
import AppTabbar from '../../components/AppTabbar.vue'
// #endif

const activeCat = ref(0)
const MALL_TARGET_CATEGORY_KEY = 'mall_target_category_id'
const defaultBanner = 'https://images.unsplash.com/photo-1601121141461-9d6647bca1ed?w=600'
const categories = ref([{ id: 0, name: '全部模型', banner: defaultBanner }])
const models = ref([])
const keyword = ref('')

const currentCategory = computed(() => categories.value[activeCat.value] || categories.value[0])
const currentCategoryName = computed(() => currentCategory.value?.name || '全部模型')
const currentCategoryBanner = computed(() => currentCategory.value?.banner || defaultBanner)

const flattenCategories = (tree = []) => {
	const result = []
	const walk = (nodes) => {
		nodes.forEach(node => {
			result.push({ id: node.id, name: node.categoryName, banner: defaultBanner })
			if (Array.isArray(node.children) && node.children.length) {
				walk(node.children)
			}
		})
	}
	walk(tree)
	return result
}

const fetchCategories = async (force = false) => {
	const tree = await getCategoryTreeApi(force)
	const flattened = flattenCategories(Array.isArray(tree) ? tree : [])
	categories.value = [{ id: 0, name: '全部模型', banner: defaultBanner }, ...flattened]
}

const fetchModels = async (force = false) => {
	const current = currentCategory.value
	const payload = {
		pageNum: 1,
		pageSize: 50,
		modelName: keyword.value?.trim() || undefined,
		categoryId: current?.id && current.id !== 0 ? current.id : undefined,
		orderBy: 'create_time',
		status: 1
	}
	const res = await getModelListApi(payload, force)
	models.value = Array.isArray(res?.records) ? res.records : []
}

onPullDownRefresh(async () => {
	await Promise.all([
		fetchCategories(true),
		fetchModels(true)
	])
	uni.stopPullDownRefresh()
})

const applyTargetCategory = async (targetId) => {
	const categoryId = Number(targetId || 0)
	if (!categoryId) return false
	const targetIndex = categories.value.findIndex(cat => Number(cat.id) === categoryId)
	if (targetIndex < 0) return false
	activeCat.value = targetIndex
	await fetchModels()
	return true
}

const consumePendingCategory = async () => {
	try {
		const pendingId = Number(uni.getStorageSync(MALL_TARGET_CATEGORY_KEY) || 0)
		if (!pendingId) return
		const handled = await applyTargetCategory(pendingId)
		if (handled) {
			uni.removeStorageSync(MALL_TARGET_CATEGORY_KEY)
		}
	} catch (error) {
		// ignore pending category parse error
	}
}

const switchCategory = async (index) => {
	activeCat.value = index
	await fetchModels()
}

const goDetail = (rawId) => {
	const modelId = String(rawId || '').trim()
	if (!modelId) {
		uni.showToast({ title: '模型ID无效', icon: 'none' })
		return
	}
	uni.navigateTo({ url: '/pages/custom/detail?id=' + encodeURIComponent(modelId) })
}

const goRewardHall = () => {
	uni.navigateTo({ url: '/pages/reward/index' })
}

onLoad(async (options) => {
	if (!ensureLoginOrRedirect()) return
	try {
		await fetchCategories()
		const categoryId = Number(options?.categoryId || 0)
		const handledByQuery = await applyTargetCategory(categoryId)
		if (!handledByQuery) {
			await fetchModels()
		}
		await consumePendingCategory()
	} catch (error) {
		uni.showToast({ title: error.message || '模型数据加载失败', icon: 'none' })
	}
})

const onCategoryEvent = async (categoryId) => {
	const targetIndex = categories.value.findIndex(cat => cat.id === Number(categoryId))
	if (targetIndex >= 0) {
		activeCat.value = targetIndex
		await fetchModels()
	}
}

onShow(() => {
	uni.$on('mall-category-change', onCategoryEvent)
	consumePendingCategory()
})

onUnload(() => {
	uni.$off('mall-category-change', onCategoryEvent)
})
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
$gradient-primary: linear-gradient(135deg, $sky-blue 0%, $sky-light 100%);

.mall-container {
	height: 100vh;
	display: flex;
	flex-direction: column;
	background-color: $surface;
}

/* —— 搜索头部 —— */
.search-sticky {
	padding: 20rpx 32rpx;
	background: rgba(255, 255, 255, 0.72);
	backdrop-filter: blur(24px);
	-webkit-backdrop-filter: blur(24px);
	animation: fadeInDown 0.4s ease forwards;

	.search-bar {
		height: 76rpx;
		background-color: $surface;
		border-radius: 999rpx;
		display: flex;
		align-items: center;
		padding: 0 28rpx;
		transition: box-shadow 0.3s ease;

		&:focus-within {
			box-shadow: 0 0 0 3rpx rgba(0, 191, 255, 0.18);
		}

		input {
			flex: 1;
			margin-left: 14rpx;
			font-size: 28rpx;
			color: $text-primary;
		}

		.search-btn {
			font-size: 26rpx;
			color: $sky-blue;
			font-weight: 700;
			padding: 8rpx 20rpx;
			border-radius: 999rpx;
			transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);

			&:active {
				transform: scale(0.92);
			}
		}

		.placeholder {
			font-size: 28rpx;
			color: $text-muted;
		}
	}

	.reward-entry {
		margin-top: 20rpx;
		background-color: #fffbeb;
		padding: 22rpx 28rpx;
		border-radius: 24rpx;
		display: flex;
		align-items: center;
		box-shadow: $shadow-card;
		transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);

		&:active {
			transform: scale(0.97);
		}

		text {
			flex: 1;
			margin-left: 16rpx;
			font-size: 28rpx;
			color: #92400e;
			font-weight: 600;
		}
	}
}

@keyframes fadeInDown {
	from { opacity: 0; transform: translateY(-16rpx); }
	to { opacity: 1; transform: translateY(0); }
}

/* —— 主体布局 —— */
.main-body {
	flex: 1;
	display: flex;
	overflow: hidden;
	animation: fadeIn 0.5s ease 0.15s forwards;
	opacity: 0;
}

@keyframes fadeIn {
	to { opacity: 1; }
}

/* —— 左侧分类 —— */
.sidebar {
	width: 180rpx;
	background-color: $surface-raised;
	box-shadow: 4rpx 0 20rpx rgba(0, 0, 0, 0.02);

	.side-item {
		height: 108rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 26rpx;
		color: $text-secondary;
		position: relative;
		transition: all 0.25s ease;

		&:active {
			background-color: rgba(0, 191, 255, 0.06);
		}

		&.active {
			background-color: $surface;
			color: $sky-blue;
			font-weight: 700;

			&::before {
				content: '';
				position: absolute;
				left: 0;
				width: 6rpx;
				height: 36rpx;
				background: $gradient-primary;
				border-radius: 0 6rpx 6rpx 0;
				animation: indicatorIn 0.3s ease;
			}
		}
	}
}

@keyframes indicatorIn {
	from { height: 0; opacity: 0; }
	to { height: 36rpx; opacity: 1; }
}

/* —— 右侧模型网格 —— */
.model-content {
	flex: 1;
	padding: 24rpx;
}

.grid-list {
	display: flex;
	flex-wrap: wrap;
	gap: 20rpx;

	.grid-item {
		width: calc(50% - 10rpx);
		background: $surface-raised;
		border-radius: 24rpx;
		overflow: hidden;
		box-shadow: $shadow-card;
		animation: fadeInUp 0.5s ease forwards;
		opacity: 0;
		transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.25s ease;

		&:active {
			transform: scale(0.96);
			box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
		}
	}
}

.grid-img-wrap {
	aspect-ratio: 1 / 1;
	background: #f0f2f5;
	overflow: hidden;

	image {
		width: 100%;
		height: 100%;
		animation: imgFadeIn 0.5s ease forwards;
		opacity: 0;
	}
}

@keyframes imgFadeIn {
	from { opacity: 0; transform: scale(0.97); }
	to { opacity: 1; transform: scale(1); }
}

.grid-info {
	padding: 16rpx 18rpx 18rpx;

		.name-row {
			display: flex;
			align-items: center;
			gap: 6rpx;
		}

		.source-tag {
			font-size: 18rpx;
			color: #16a34a;
			background: #dcfce7;
			padding: 2rpx 8rpx;
			border-radius: 999rpx;
			font-weight: 600;
			flex-shrink: 0;
		}
	.name {
		font-size: 26rpx;
		color: $text-primary;
		font-weight: 600;
		display: block;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.price {
		font-size: 28rpx;
		color: $sky-deep;
		font-weight: 800;
		margin-top: 8rpx;
		display: block;
	}
}

@keyframes fadeInUp {
	from { opacity: 0; transform: translateY(24rpx); }
	to { opacity: 1; transform: translateY(0); }
}

.empty-tip {
	width: 100%;
	text-align: center;
	color: $text-muted;
	font-size: 26rpx;
	padding: 80rpx 0;
}
</style>
