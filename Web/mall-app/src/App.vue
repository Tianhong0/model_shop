<script>
	import { hasValidLogin } from './utils/auth'
	import { startNotificationRuntime, stopNotificationRuntime } from './utils/notificationRuntime'

	export default {
		onLaunch: function() {
			console.log('App Launch')
			if (hasValidLogin()) {
				startNotificationRuntime()
			}
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
		},
		onHide: function() {
			console.log('App Hide')
			stopNotificationRuntime()
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
