<script>
	import { hasValidLogin } from './utils/auth'
	import { startNotificationRuntime, stopNotificationRuntime } from './utils/notificationRuntime'

	function parseInviteCode(args) {
		if (!args) return null
		const url = typeof args === 'string' ? args : (args.path || args.url || '')
		const match = url.match(/[?&]inviteCode=([A-Za-z0-9]+)/)
		return match ? match[1] : null
	}

	function parseGroupBuyShareCode(args) {
		if (!args) return null
		const url = typeof args === 'string' ? args : (args.path || args.url || '')
		const match = url.match(/[?&]shareCode=([A-Za-z0-9]+)/)
		return match ? match[1] : null
	}

	function handleInviteDeepLink(inviteCode) {
		if (!inviteCode) return
		if (hasValidLogin()) {
			uni.showToast({ title: '您已注册，无法接受邀请', icon: 'none', duration: 3000 })
		} else {
			uni.navigateTo({ url: '/pages/auth/register?inviteCode=' + inviteCode })
		}
	}

	function handleGroupBuyDeepLink(shareCode) {
		if (!shareCode) return
		uni.navigateTo({ url: '/pages/group-buy/group-detail?shareCode=' + shareCode })
	}

	export default {
		onLaunch: function() {
			console.log('App Launch')
			if (hasValidLogin()) {
				startNotificationRuntime()
			}

			// 冷启动时处理 deep link
			// #ifdef APP-PLUS
			try {
				const args = plus.runtime.arguments
				if (args) {
					const inviteCode = parseInviteCode(args)
					if (inviteCode) {
						this.$options.globalData.pendingInviteCode = inviteCode
					}
					const shareCode = parseGroupBuyShareCode(args)
					if (shareCode) {
						this.$options.globalData.pendingGroupBuyShareCode = shareCode
					}
				}
			} catch (e) {
				console.warn('读取启动参数失败:', e)
			}
			// #endif

			if (hasValidLogin()) {
				setTimeout(() => {
					uni.switchTab({ url: '/pages/index/index' })
				}, 0)
			}
		},
		onShow: function() {
			console.log('App Show')
			if (hasValidLogin()) {
				startNotificationRuntime()
			} else {
				stopNotificationRuntime()
			}

			// 热启动时处理 deep link
			// #ifdef APP-PLUS
			try {
				const args = plus.runtime.arguments
				if (args) {
					const inviteCode = parseInviteCode(args)
					if (inviteCode) {
						handleInviteDeepLink(inviteCode)
					}
					const shareCode = parseGroupBuyShareCode(args)
					if (shareCode) {
						handleGroupBuyDeepLink(shareCode)
					}
				}
			} catch (e) {
				console.warn('读取启动参数失败:', e)
			}

			// 处理冷启动时暂存的码（页面加载完成后处理）
			const pendingInviteCode = this.$options.globalData.pendingInviteCode
			if (pendingInviteCode) {
				this.$options.globalData.pendingInviteCode = null
				setTimeout(() => {
					handleInviteDeepLink(pendingInviteCode)
				}, 500)
			}
			const pendingGroupBuyShareCode = this.$options.globalData.pendingGroupBuyShareCode
			if (pendingGroupBuyShareCode) {
				this.$options.globalData.pendingGroupBuyShareCode = null
				setTimeout(() => {
					handleGroupBuyDeepLink(pendingGroupBuyShareCode)
				}, 500)
			}
			// #endif
		},
		onHide: function() {
			console.log('App Hide')
			stopNotificationRuntime()
		},
		globalData: {
			pendingInviteCode: null,
			pendingGroupBuyShareCode: null
		}
	}
</script>

<style lang="scss">
	@import "./uni.scss";

	/* ============================
	   全局基础样式
	   ============================ */
	page {
		background-color: #f8f8f8;
		font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
		color: #1a2030;
		-webkit-font-smoothing: antialiased;
	}

	/* —— 通用容器 —— */
	.container {
		padding: 32rpx;
	}

	/* —— 果冻卡片：无边框 + 柔和阴影 —— */
	.card {
		background-color: #ffffff;
		border-radius: 24rpx;
		padding: 32rpx;
		box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
		border: none;
	}

	/* —— 毛玻璃导航栏 —— */
	.glass-nav {
		background: rgba(255, 255, 255, 0.72);
		backdrop-filter: blur(24px);
		-webkit-backdrop-filter: blur(24px);
	}

	/* —— 胶囊按钮基础 —— */
	.capsule-btn {
		border-radius: 999rpx;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		font-weight: 600;
		transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.2s ease;
	}

	.capsule-btn:active,
	.btn-hover {
		transform: scale(0.96) !important;
	}

	/* —— 1:1 模型图片容器 —— */
	.model-img-square {
		aspect-ratio: 1 / 1;
		border-radius: 20rpx;
		overflow: hidden;
		background: #f0f2f5;
	}

	.model-img-square image {
		width: 100%;
		height: 100%;
		animation: imgFadeIn 0.5s ease forwards;
		opacity: 0;
	}

	@keyframes imgFadeIn {
		from { opacity: 0; transform: scale(0.97); }
		to { opacity: 1; transform: scale(1); }
	}

	/* —— 淡入动画 —— */
	@keyframes fadeInUp {
		from { opacity: 0; transform: translateY(24rpx); }
		to { opacity: 1; transform: translateY(0); }
	}

	@keyframes fadeIn {
		from { opacity: 0; }
		to { opacity: 1; }
	}

	@keyframes jellyPop {
		0% { opacity: 0; transform: scale(0.92); }
		60% { transform: scale(1.03); }
		100% { opacity: 1; transform: scale(1); }
	}

	/* —— 呼吸灯脉冲（科技蓝） —— */
	@keyframes breathGlow {
		0%, 100% { box-shadow: 0 0 12rpx rgba(0, 191, 255, 0.15); }
		50% { box-shadow: 0 0 24rpx rgba(0, 191, 255, 0.35); }
	}

	/* —— 字体分级 —— */
	.text-title {
		font-size: 36rpx;
		font-weight: 700;
		color: #1a2030;
		line-height: 1.35;
	}

	.text-subtitle {
		font-size: 30rpx;
		font-weight: 600;
		color: #1a2030;
		line-height: 1.4;
	}

	.text-body {
		font-size: 28rpx;
		font-weight: 400;
		color: #5a6a7a;
		line-height: 1.6;
	}

	.text-caption {
		font-size: 24rpx;
		color: #94a3b8;
		line-height: 1.4;
	}

	.text-small {
		font-size: 20rpx;
		color: #94a3b8;
	}
</style>
