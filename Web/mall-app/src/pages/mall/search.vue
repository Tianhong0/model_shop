<template>
	<view class="search-page">
		<view class="search-bar">
			<input
				v-model="keyword"
				type="text"
				placeholder="请输入模型名称"
				confirm-type="search"
				@confirm="onSearch"
			/>
			<button class="search-btn" @click="onSearch">搜索</button>
		</view>

		<view class="result-header">共找到 {{ modelList.length }} 个模型</view>

		<view v-if="modelList.length" class="model-list">
			<view class="model-card" v-for="item in modelList" :key="item.id" @click="goDetail(item.id)">
				<image v-if="item.mainImageUrl || item.mainImage" class="model-image" :src="item.mainImageUrl || item.mainImage" mode="aspectFill" />
				<view v-else class="model-image empty-image">暂无图片</view>
				<view class="model-content">
					<view class="model-name">{{ item.modelName || '未命名模型' }}</view>
					<view class="model-desc">{{ item.description || item.modelDescription || '暂无简介' }}</view>
					<view class="model-price">¥{{ item.basePrice || 0 }}</view>
				</view>
			</view>
		</view>

		<view v-else class="empty-wrap">
			<text class="empty-text">暂无匹配模型</text>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getModelListApi } from '@/api/model.js'
import { ensureLoginOrRedirect } from '@/utils/auth.js'

const keyword = ref('')
const modelList = ref([])

const loadList = async () => {
	if (!keyword.value.trim()) {
		modelList.value = []
		return
	}
	uni.showLoading({ title: '搜索中...' })
	try {
		const res = await getModelListApi({
			pageNum: 1,
			pageSize: 50,
			modelName: keyword.value.trim(),
			status: 1
		})
		modelList.value = Array.isArray(res?.records) ? res.records : []
	} catch (error) {
		modelList.value = []
		uni.showToast({ title: error?.message || '搜索失败', icon: 'none' })
	} finally {
		uni.hideLoading()
	}
}

const onSearch = () => {
	const value = keyword.value.trim()
	if (!value) {
		uni.showToast({ title: '请输入搜索关键词', icon: 'none' })
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
	if (keyword.value) {
		loadList()
	}
})
</script>

<style scoped lang="scss">
.search-page {
	min-height: 100vh;
	background: #f6f7fb;
	padding: 24rpx;
}

.search-bar {
	display: flex;
	align-items: center;
	gap: 16rpx;
	background: #fff;
	border-radius: 16rpx;
	padding: 16rpx;

	input {
		flex: 1;
		height: 68rpx;
		background: #f8fafc;
		border-radius: 12rpx;
		padding: 0 20rpx;
		font-size: 28rpx;
	}

	.search-btn {
		height: 68rpx;
		line-height: 68rpx;
		padding: 0 28rpx;
		border-radius: 12rpx;
		background: #4f46e5;
		color: #fff;
		font-size: 26rpx;
	}
}

.result-header {
	font-size: 24rpx;
	color: #64748b;
	margin: 24rpx 8rpx;
}

.model-list {
	display: flex;
	flex-direction: column;
	gap: 20rpx;
}

.model-card {
	background: #fff;
	border-radius: 16rpx;
	overflow: hidden;
	display: flex;
	box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
}

.model-image {
	width: 220rpx;
	height: 220rpx;
	background: #e2e8f0;
}

.empty-image {
	display: flex;
	align-items: center;
	justify-content: center;
	color: #94a3b8;
	font-size: 24rpx;
}

.model-content {
	flex: 1;
	padding: 20rpx;
	display: flex;
	flex-direction: column;
	justify-content: space-between;
}

.model-name {
	font-size: 30rpx;
	font-weight: 700;
	color: #0f172a;
}

.model-desc {
	margin-top: 12rpx;
	font-size: 24rpx;
	line-height: 1.5;
	color: #64748b;
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
	color: #4f46e5;
}

.empty-wrap {
	padding: 120rpx 0;
	text-align: center;
}

.empty-text {
	font-size: 28rpx;
	color: #94a3b8;
}
</style>
