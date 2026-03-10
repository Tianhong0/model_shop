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
	/*每个页面公共css */
	@import "./uni.scss";
	
	page {
		background-color: #f8fafc;
		font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
	}

	.container {
		padding: 30rpx;
	}
	
	.card {
		background-color: #ffffff;
		border-radius: 24rpx;
		padding: 30rpx;
		box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
	}
</style>
