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
			<!-- 左侧分类导航 -->
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

			<!-- 右侧模型列表 -->
			<scroll-view scroll-y class="model-content">
				<view class="grid-list">
					<view class="grid-item" v-for="(item, idx) in models" :key="item.id || idx" @click="goDetail(item.id)">
						<image :src="item.mainImageUrl || defaultBanner" mode="aspectFill"></image>
						<text class="name">{{item.modelName}}</text>
						<text class="price">￥{{item.basePrice || '0.00'}} 起</text>
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
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
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

const fetchCategories = async () => {
	const tree = await getCategoryTreeApi()
	const flattened = flattenCategories(Array.isArray(tree) ? tree : [])
	categories.value = [{ id: 0, name: '全部模型', banner: defaultBanner }, ...flattened]
}

const fetchModels = async () => {
	const current = currentCategory.value
	const payload = {
		pageNum: 1,
		pageSize: 50,
		modelName: keyword.value?.trim() || undefined,
		categoryId: current?.id && current.id !== 0 ? current.id : undefined,
		orderBy: 'create_time',
		status: 1
	}
	const res = await getModelListApi(payload)
	models.value = Array.isArray(res?.records) ? res.records : []
}

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
.mall-container {
	height: 100vh;
	display: flex;
	flex-direction: column;
	background-color: #ffffff;
}

.search-sticky {
	padding: 20rpx 30rpx;
	.search-bar {
		height: 72rpx;
		background-color: #f1f5f9;
		border-radius: 36rpx;
		display: flex;
		align-items: center;
		padding: 0 30rpx;
		input {
			flex: 1;
			margin-left: 12rpx;
			font-size: 26rpx;
		}
		.search-btn {
			font-size: 24rpx;
			color: #4f46e5;
			font-weight: 700;
		}
		.placeholder {
			font-size: 26rpx;
			color: #94a3b8;
		}
	}
	.reward-entry {
		margin-top: 20rpx;
		background-color: #fffbeb;
		border: 2rpx solid #fef3c7;
		padding: 20rpx 30rpx;
		border-radius: 16rpx;
		display: flex;
		align-items: center;
		text { flex: 1; margin-left: 16rpx; font-size: 26rpx; color: #92400e; font-weight: 700; }
	}
}

.main-body {
	flex: 1;
	display: flex;
	overflow: hidden;
}

.sidebar {
	width: 180rpx;
	background-color: #f8fafc;
	.side-item {
		height: 100rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 26rpx;
		color: #64748b;
		position: relative;
		&.active {
			background-color: #ffffff;
			color: #4f46e5;
			font-weight: 700;
			&::before {
				content: '';
				position: absolute;
				left: 0;
				width: 6rpx;
				height: 32rpx;
				background-color: #4f46e5;
				border-radius: 0 4rpx 4rpx 0;
			}
		}
	}
}

.model-content {
	flex: 1;
	padding: 30rpx;
}

.grid-list {
	display: flex;
	flex-wrap: wrap;
	gap: 20rpx;
	.grid-item {
		width: calc(50% - 10rpx);
		margin-bottom: 20rpx;
		image {
			width: 100%;
			height: 220rpx;
			border-radius: 16rpx;
			background-color: #f1f5f9;
		}
		.name {
			font-size: 24rpx;
			color: #1e293b;
			margin-top: 10rpx;
			display: block;
		}
		.price {
			font-size: 24rpx;
			color: #ef4444;
			font-weight: 700;
			margin-top: 4rpx;
			display: block;
		}
	}
}

.empty-tip {
	width: 100%;
	text-align: center;
	color: #94a3b8;
	font-size: 24rpx;
	padding: 40rpx 0;
}
</style>
