<template>
	<view class="submission-detail-page">
		<!-- 作品信息 -->
		<view class="submission-header">
			<view class="author-info" @tap="goAuthorProfile">
				<image class="author-avatar" :src="submission.authorAvatar || '/static/default-avatar.png'" mode="aspectFill"></image>
				<view class="author-meta">
					<text class="author-name">{{submission.authorName || '匿名'}}</text>
					<text class="submit-time">{{formatTime(submission.createTime)}}</text>
				</view>
			</view>
			<text class="submission-title">{{submission.title}}</text>
			<text class="submission-desc" v-if="submission.description">{{submission.description}}</text>

			<!-- 图片展示 -->
			<view class="image-gallery" v-if="submission.imageUrls && submission.imageUrls.length">
				<image
					v-for="(img, idx) in submission.imageUrls"
					:key="idx"
					:src="img"
					mode="aspectFill"
					class="gallery-img"
					@tap="previewImage(img, submission.imageUrls)"
				></image>
			</view>

			<!-- 数据统计 -->
			<view class="stats-bar">
				<view class="stat-item" :class="{ active: submission.isLiked }" @tap="handleLike">
					<uni-icons :type="submission.isLiked ? 'heart-filled' : 'heart'" size="20" :color="submission.isLiked ? '#ff4d6d' : '#bbb'"></uni-icons>
					<text>{{submission.likeCount || 0}}</text>
				</view>
				<view class="stat-item">
					<uni-icons type="chat" size="20" color="#bbb"></uni-icons>
					<text>{{submission.commentCount || 0}}</text>
				</view>
			</view>
		</view>

		<!-- 评论区 -->
		<view class="comment-section">
			<view class="section-title">评论 ({{comments.length}})</view>

			<view v-if="loadingComments && !comments.length" class="loading-wrap">
				<view class="loading-spinner"></view>
				<text>加载中...</text>
			</view>

			<view v-else-if="!comments.length" class="empty-comments">
				<text>暂无评论，快来抢沙发吧~</text>
			</view>

			<view v-else class="comment-list">
				<view class="comment-item" v-for="comment in comments" :key="comment.id">
					<image class="comment-avatar" :src="comment.userAvatar || '/static/default-avatar.png'" mode="aspectFill"></image>
					<view class="comment-content">
						<view class="comment-header">
							<text class="comment-name">{{comment.userName || '匿名'}}</text>
							<text class="comment-time">{{formatTime(comment.createTime)}}</text>
						</view>
						<text class="comment-text">{{comment.content}}</text>
						<view class="comment-actions">
							<view class="action-item" :class="{ active: comment.isLiked }" @tap="handleLikeComment(comment)">
								<uni-icons :type="comment.isLiked ? 'heart-filled' : 'heart'" size="14" :color="comment.isLiked ? '#ff4d6d' : '#bbb'"></uni-icons>
								<text>{{comment.likeCount || 0}}</text>
							</view>
							<view class="action-item" @tap="openReply(comment)">
								<uni-icons type="chatbubble" size="14" color="#bbb"></uni-icons>
								<text>回复</text>
							</view>
							<view class="action-item delete" v-if="comment.userId === currentUserId" @tap="handleDeleteComment(comment)">
								<uni-icons type="trash" size="14" color="#ff4d6d"></uni-icons>
							</view>
						</view>

						<!-- 子评论 -->
						<view class="child-comments" v-if="comment.children && comment.children.length">
							<view class="child-item" v-for="child in comment.children" :key="child.id">
								<text class="child-name">{{child.userName}}</text>
								<text class="child-reply" v-if="child.replyToUserName">回复 <text class="reply-to">{{child.replyToUserName}}</text></text>
								<text class="child-content">：{{child.content}}</text>
								<view class="child-actions">
									<view class="action-item" :class="{ active: child.isLiked }" @tap="handleLikeComment(child)">
										<uni-icons :type="child.isLiked ? 'heart-filled' : 'heart'" size="12" :color="child.isLiked ? '#ff4d6d' : '#bbb'"></uni-icons>
										<text>{{child.likeCount || 0}}</text>
									</view>
								</view>
							</view>
						</view>
					</view>
				</view>

				<view v-if="hasMoreComments" class="load-more" @tap="loadMoreComments">
					<text>加载更多评论</text>
				</view>
			</view>
		</view>

		<!-- 底部评论输入栏 -->
		<view class="bottom-bar">
			<input
				v-model="commentText"
				class="comment-input"
				:placeholder="replyTo ? `回复 ${replyTo.userName}` : '写下你的评论...'"
				:focus="inputFocus"
				@blur="handleInputBlur"
			/>
			<button class="send-btn" :disabled="!commentText.trim()" @tap="submitComment">发送</button>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
	getSubmissionDetailApi,
	likeSubmissionApi,
	unlikeSubmissionApi,
	getSubmissionCommentsApi,
	commentSubmissionApi,
	deleteCommentApi,
	likeCommentApi,
	unlikeCommentApi
} from '../../api/event'

const currentUserId = computed(() => {
	const saved = uni.getStorageSync('user_profile')
	return saved?.id || null
})

const submissionId = ref(null)
const submission = ref({
	id: null,
	title: '',
	description: '',
	imageUrls: [],
	authorId: null,
	authorName: '',
	authorAvatar: '',
	likeCount: 0,
	commentCount: 0,
	isLiked: false,
	createTime: ''
})

const comments = ref([])
const loadingComments = ref(false)
const commentPage = ref(1)
const hasMoreComments = ref(true)

const commentText = ref('')
const replyTo = ref(null)
const inputFocus = ref(false)

const formatTime = (time) => {
	if (!time) return ''
	return time.replace('T', ' ').substring(0, 16)
}

const fetchDetail = async () => {
	try {
		const res = await getSubmissionDetailApi(submissionId.value)
		submission.value = res
	} catch (error) {
		uni.showToast({ title: error.message || '加载失败', icon: 'none' })
	}
}

const fetchComments = async (isRefresh = false) => {
	if (isRefresh) {
		commentPage.value = 1
		hasMoreComments.value = true
	}

	if (!hasMoreComments.value) return

	loadingComments.value = true
	try {
		const res = await getSubmissionCommentsApi(submissionId.value, {
			page: commentPage.value,
			size: 20
		})
		const list = res?.records || []
		if (isRefresh) {
			comments.value = list
		} else {
			comments.value = [...comments.value, ...list]
		}
		hasMoreComments.value = list.length >= 20
		commentPage.value++
	} catch (error) {
		console.error('加载评论失败:', error)
	} finally {
		loadingComments.value = false
	}
}

const loadMoreComments = () => {
	if (!loadingComments.value && hasMoreComments.value) {
		fetchComments()
	}
}

const handleLike = async () => {
	try {
		if (submission.value.isLiked) {
			await unlikeSubmissionApi(submissionId.value)
			submission.value.isLiked = false
			submission.value.likeCount = Math.max(0, (submission.value.likeCount || 1) - 1)
		} else {
			await likeSubmissionApi(submissionId.value)
			submission.value.isLiked = true
			submission.value.likeCount = (submission.value.likeCount || 0) + 1
		}
	} catch (error) {
		uni.showToast({ title: error.message || '操作失败', icon: 'none' })
	}
}

const handleLikeComment = async (comment) => {
	try {
		if (comment.isLiked) {
			await unlikeCommentApi(comment.id)
			comment.isLiked = false
			comment.likeCount = Math.max(0, (comment.likeCount || 1) - 1)
		} else {
			await likeCommentApi(comment.id)
			comment.isLiked = true
			comment.likeCount = (comment.likeCount || 0) + 1
		}
	} catch (error) {
		uni.showToast({ title: error.message || '操作失败', icon: 'none' })
	}
}

const openReply = (comment) => {
	replyTo.value = comment
	inputFocus.value = true
}

const handleInputBlur = () => {
	setTimeout(() => {
		if (!commentText.value) {
			replyTo.value = null
		}
	}, 100)
}

const submitComment = async () => {
	if (!commentText.value.trim()) return

	try {
		const data = {
			submissionId: submissionId.value,
			content: commentText.value.trim()
		}
		if (replyTo.value) {
			data.parentId = replyTo.value.id
			data.replyToUserId = replyTo.value.userId
		}

		await commentSubmissionApi(data)
		commentText.value = ''
		replyTo.value = null
		uni.showToast({ title: '评论成功', icon: 'success' })

		// 刷新评论列表
		await fetchComments(true)
		// 更新评论数
		submission.value.commentCount = (submission.value.commentCount || 0) + 1
	} catch (error) {
		uni.showToast({ title: error.message || '评论失败', icon: 'none' })
	}
}

const handleDeleteComment = async (comment) => {
	uni.showModal({
		title: '删除评论',
		content: '确定删除这条评论吗？',
		success: async (res) => {
			if (res.confirm) {
				try {
					await deleteCommentApi(comment.id)
					uni.showToast({ title: '已删除', icon: 'success' })
					await fetchComments(true)
					submission.value.commentCount = Math.max(0, (submission.value.commentCount || 1) - 1)
				} catch (error) {
					uni.showToast({ title: error.message || '删除失败', icon: 'none' })
				}
			}
		}
	})
}

const previewImage = (current, urls) => {
	uni.previewImage({ current, urls })
}

const goAuthorProfile = () => {
	if (submission.value.authorId) {
		uni.navigateTo({
			url: `/pages/user/profile?userId=${submission.value.authorId}`
		})
	}
}

onLoad((options) => {
	if (options.id) {
		submissionId.value = options.id
		fetchDetail()
		fetchComments(true)
	}
})
</script>

<style scoped lang="scss">
.submission-detail-page {
	min-height: 100vh;
	background: #f8f8f8;
	padding-bottom: 130rpx;
}

.submission-header {
	background: #ffffff;
	margin: 24rpx 28rpx;
	padding: 32rpx;
	border-radius: 24rpx;
	box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
	animation: fadeInUp 0.5s ease forwards;

	.author-info {
		display: flex;
		align-items: center;
		margin-bottom: 24rpx;

		&:active {
			opacity: 0.7;
		}

		.author-avatar {
			width: 80rpx;
			height: 80rpx;
			border-radius: 50%;
			margin-right: 18rpx;
			transition: opacity 0.4s ease;
		}

		.author-meta {
			.author-name {
				font-size: 28rpx;
				font-weight: 600;
				color: #1a1a1a;
				display: block;
			}

			.submit-time {
				font-size: 22rpx;
				color: #999;
				margin-top: 6rpx;
				display: block;
			}
		}
	}

	.submission-title {
		font-size: 36rpx;
		font-weight: 700;
		color: #1a1a1a;
		display: block;
		line-height: 1.4;
		margin-bottom: 16rpx;
	}

	.submission-desc {
		font-size: 28rpx;
		color: #666;
		line-height: 1.7;
		display: block;
		margin-bottom: 24rpx;
	}

	.image-gallery {
		display: flex;
		flex-wrap: wrap;
		gap: 12rpx;
		margin-bottom: 24rpx;

		.gallery-img {
			width: calc(33.33% - 8rpx);
			aspect-ratio: 1;
			border-radius: 16rpx;
			transition: opacity 0.4s ease;
		}
	}

	.stats-bar {
		display: flex;
		gap: 40rpx;
		padding-top: 24rpx;

		.stat-item {
			display: flex;
			align-items: center;
			gap: 8rpx;
			transition: all 0.2s;

			&:active {
				transform: scale(0.96);
			}

			text {
				font-size: 26rpx;
				color: #999;
			}

			&.active text {
				color: #ff4d6d;
			}
		}
	}
}

@keyframes fadeInUp {
	from {
		opacity: 0;
		transform: translateY(24rpx);
	}
	to {
		opacity: 1;
		transform: translateY(0);
	}
}

.comment-section {
	background: #ffffff;
	margin: 0 28rpx 28rpx;
	padding: 32rpx;
	border-radius: 24rpx;
	box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
	animation: fadeInUp 0.5s ease 0.1s forwards;
	opacity: 0;

	.section-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #1a1a1a;
		margin-bottom: 24rpx;
	}

	.loading-wrap {
		padding: 60rpx 0;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 16rpx;

		text {
			font-size: 26rpx;
			color: #999;
		}
	}

	.empty-comments {
		padding: 60rpx 0;
		text-align: center;

		text {
			font-size: 26rpx;
			color: #bbb;
		}
	}

	.comment-list {
		.comment-item {
			display: flex;
			padding: 24rpx 0;

			&:not(:last-child) {
				box-shadow: 0 1rpx 0 rgba(0, 0, 0, 0.04);
			}

			.comment-avatar {
				width: 64rpx;
				height: 64rpx;
				border-radius: 50%;
				margin-right: 18rpx;
				flex-shrink: 0;
			}

			.comment-content {
				flex: 1;
				min-width: 0;

				.comment-header {
					display: flex;
					justify-content: space-between;
					align-items: center;
					margin-bottom: 10rpx;

					.comment-name {
						font-size: 26rpx;
						font-weight: 600;
						color: #1a1a1a;
					}

					.comment-time {
						font-size: 22rpx;
						color: #bbb;
					}
				}

				.comment-text {
					font-size: 28rpx;
					color: #555;
					line-height: 1.6;
					display: block;
					margin-bottom: 14rpx;
				}

				.comment-actions {
					display: flex;
					gap: 32rpx;

					.action-item {
						display: flex;
						align-items: center;
						gap: 6rpx;
						transition: all 0.2s;

						&:active {
							transform: scale(0.96);
						}

						text {
							font-size: 22rpx;
							color: #bbb;
						}

						&.active text {
							color: #ff4d6d;
						}

						&.delete {
							margin-left: auto;
						}
					}
				}

				.child-comments {
					margin-top: 18rpx;
					padding: 20rpx;
					background: #f8f8f8;
					border-radius: 16rpx;

					.child-item {
						margin-bottom: 14rpx;
						line-height: 1.6;

						&:last-child {
							margin-bottom: 0;
						}

						.child-name {
							font-size: 24rpx;
							font-weight: 600;
							color: #00bfff;
						}

						.child-reply {
							font-size: 24rpx;
							color: #999;

							.reply-to {
								color: #00bfff;
							}
						}

						.child-content {
							font-size: 24rpx;
							color: #555;
						}

						.child-actions {
							display: flex;
							margin-top: 8rpx;

							.action-item {
								display: flex;
								align-items: center;
								gap: 4rpx;

								text {
									font-size: 20rpx;
									color: #bbb;
								}

								&.active text {
									color: #ff4d6d;
								}
							}
						}
					}
				}
			}
		}

		.load-more {
			padding: 32rpx 0;
			text-align: center;

			text {
				font-size: 26rpx;
				color: #00bfff;
				font-weight: 500;
			}

			&:active {
				opacity: 0.7;
			}
		}
	}
}

.loading-spinner {
	width: 40rpx;
	height: 40rpx;
	border: 3rpx solid #f0f0f0;
	border-top-color: #00bfff;
	border-radius: 50%;
	animation: spin 0.8s linear infinite;
}

@keyframes spin {
	to { transform: rotate(360deg); }
}

.bottom-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	height: 110rpx;
	background: rgba(255, 255, 255, 0.72);
	backdrop-filter: blur(24px);
	display: flex;
	align-items: center;
	padding: 0 24rpx;
	box-shadow: 0 -8rpx 40rpx rgba(0, 0, 0, 0.04);
	z-index: 100;

	.comment-input {
		flex: 1;
		height: 76rpx;
		padding: 0 28rpx;
		background: #f8f8f8;
		border-radius: 999rpx;
		font-size: 28rpx;
		color: #1a1a1a;
	}

	.send-btn {
		margin-left: 16rpx;
		width: 140rpx;
		height: 76rpx;
		background: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
		color: #fff;
		border-radius: 999rpx;
		font-size: 28rpx;
		font-weight: 600;
		box-shadow: 0 4rpx 16rpx rgba(0, 191, 255, 0.25);

		&:active {
			transform: scale(0.96);
		}

		&[disabled] {
			opacity: 0.4;
			box-shadow: none;
		}
	}
}
</style>
