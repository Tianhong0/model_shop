<template>
	<view class="search-page">
		<view class="search-bar">
			<input
				v-model="keyword"
				type="text"
				:placeholder="searchMode === 'semantic' ? '描述你想要的模型，如：可爱的卡通小动物' : '请输入模型名称'"
				confirm-type="search"
				@confirm="onSearch"
			/>
			<view class="search-mode-toggle" @click="toggleSearchMode">
				<text :class="['mode-text', searchMode === 'keyword' ? 'active' : '']">关键词</text>
				<text :class="['mode-text', searchMode === 'semantic' ? 'active' : '']">语义</text>
			</view>
			<button class="search-btn" @click="onSearch">搜索</button>
		</view>

		<view class="search-info">
			<text class="result-count">共找到 {{ modelList.length }} 个模型</text>
			<text v-if="searchMode === 'semantic'" class="search-hint">AI 语义搜索中...</text>
		</view>

		<view v-if="modelList.length" class="model-list">
			<view class="model-card" v-for="item in modelList" :key="item.id" @click="goDetail(item.id)">
				<image v-if="item.mainImageUrl || item.mainImage" class="model-image" :src="item.watermarkedMainImageUrl || item.mainImageUrl || item.mainImage" mode="aspectFill" />
				<view v-else class="model-image empty-image">暂无图片</view>
				<view class="model-content">
					<view class="model-name">{{ item.modelName || '未命名模型' }}</view>
					<view class="model-desc">{{ item.description || item.modelDescription || '暂无简介' }}</view>
					<view class="model-price">¥{{ item.basePrice || 0 }}</view>
				</view>
			</view>
		</view>

		<view v-else-if="searched && !loading" class="empty-wrap">
			<text class="empty-text">{{ searchMode === 'semantic' ? '未找到相关模型，试试其他描述' : '暂无匹配模型' }}</text>
		</view>

		<view v-if="loading" class="loading-wrap">
			<text class="loading-text">搜索中...</text>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getModelListApi, semanticSearchModelsApi } from '@/api/model.js'
import { ensureLoginOrRedirect } from '@/utils/auth.js'

const keyword = ref('')
const modelList = ref([])
const searchMode = ref('keyword') // 'keyword' | 'semantic'
const searched = ref(false)
const loading = ref(false)

const toggleSearchMode = () => {
	searchMode.value = searchMode.value === 'keyword' ? 'semantic' : 'keyword'
}

const loadList = async () => {
	if (!keyword.value.trim()) {
		modelList.value = []
		return
	}

	loading.value = true
	searched.value = false

	try {
		let res
		if (searchMode.value === 'semantic') {
			// 语义搜索
			res = await semanticSearchModelsApi({
				query: keyword.value.trim(),
				pageNum: 1,
				pageSize: 50
			})
		} else {
			// 关键词搜索
			res = await getModelListApi({
				pageNum: 1,
				pageSize: 50,
				modelName: keyword.value.trim(),
				status: 1
			})
		}
		modelList.value = Array.isArray(res?.records) ? res.records : []
		searched.value = true
	} catch (error) {
		modelList.value = []
		searched.value = true
		uni.showToast({ title: error?.message || '搜索失败', icon: 'none' })
	} finally {
		loading.value = false
	}
}

const onSearch = () => {
	const value = keyword.value.trim()
	if (!value) {
		uni.showToast({ title: '请输入搜索内容', icon: 'none' })
		return
	}
	loadList()
}

const goDetail = (rawId) => {
	const modelId = String(rawId || '').trim()
	if (!modelId) {
		uni.showToast({ title: '模型ID无效', icon: 'none' })
		return
	}
	uni.navigateTo({ url: `/pages/custom/detail?id=${encodeURIComponent(modelId)}` })
}

onLoad((options) => {
	if (!ensureLoginOrRedirect()) {
		return
	}
	keyword.value = decodeURIComponent(options?.keyword || '').trim()
	// 支持从外部指定搜索模式
	if (options?.mode === 'semantic') {
		searchMode.value = 'semantic'
	}
	if (keyword.value) {
		loadList()
	}
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

.search-page {
	min-height: 100vh;
	background: $surface;
	padding: 24rpx;
}

.search-bar {
	display: flex;
	align-items: center;
	gap: 16rpx;
	background: $surface-raised;
	border-radius: 24rpx;
	padding: 16rpx;
	box-shadow: $shadow-card;

	input {
		flex: 1;
		height: 68rpx;
		background: $surface;
		border-radius: 999rpx;
		padding: 0 24rpx;
		font-size: 28rpx;
		color: $text-primary;

		&::placeholder {
			color: $text-muted;
		}
	}

	.search-mode-toggle {
		display: flex;
		background: $surface;
		border-radius: 999rpx;
		padding: 4rpx;
		.mode-text {
			padding: 8rpx 16rpx;
			font-size: 24rpx;
			color: $text-muted;
			border-radius: 999rpx;
			transition: all 0.2s;
			&.active {
				background: $gradient-primary;
				color: #fff;
				font-weight: 600;
			}
		}
	}

	.search-btn {
		height: 68rpx;
		line-height: 68rpx;
		padding: 0 36rpx;
		border-radius: 999rpx;
		background: $gradient-primary;
		color: #fff;
		font-size: 28rpx;
		font-weight: 600;
		border: none;
		box-shadow: 0 4rpx 16rpx rgba(0, 191, 255, 0.35);

		&:active {
			transform: scale(0.96);
		}
	}
}

.search-info {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin: 28rpx 8rpx;

	.result-count {
		font-size: 24rpx;
		color: $text-secondary;
	}

	.search-hint {
		font-size: 22rpx;
		color: $sky-blue;
	}
}

.model-list {
	display: flex;
	flex-direction: column;
	gap: 24rpx;
}

.model-card {
	background: $surface-raised;
	border-radius: 24rpx;
	overflow: hidden;
	display: flex;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.35s ease-out both;
}

.model-image {
	width: 220rpx;
	height: 220rpx;
	background: $surface;
	animation: imgFadeIn 0.4s ease-out both;
}

.empty-image {
	display: flex;
	align-items: center;
	justify-content: center;
	color: $text-muted;
	font-size: 24rpx;
	background: $surface;
}

.model-content {
	flex: 1;
	padding: 24rpx;
	display: flex;
	flex-direction: column;
	justify-content: space-between;
}

.model-name {
	font-size: 30rpx;
	font-weight: 700;
	color: $text-primary;
}

.model-desc {
	margin-top: 12rpx;
	font-size: 26rpx;
	line-height: 1.5;
	color: $text-secondary;
	display: -webkit-box;
	line-clamp: 2;
	-webkit-line-clamp: 2;
	-webkit-box-orient: vertical;
	overflow: hidden;
}

.model-price {
	margin-top: 16rpx;
	font-size: 32rpx;
	font-weight: 700;
	color: $sky-blue;
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
	padding: 60rpx 0;
	text-align: center;
}

.loading-text {
	font-size: 28rpx;
	color: $text-muted;
}
</style>
