<template>
	<view class="promotion-page">
		<!-- 隐藏的canvas用于生成海报 -->
		<canvas canvas-id="sharePosterCanvas" class="hidden-canvas" :style="{ width: canvasWidth + 'px', height: canvasHeight + 'px' }"></canvas>

		<!-- 推广统计卡片 -->
		<view class="stats-card">
			<view class="stats-row">
				<view class="stat-item">
					<text class="value">{{ center.totalInvited || 0 }}</text>
					<text class="label">累计邀请</text>
				</view>
				<view class="stat-item">
					<text class="value">{{ center.totalPoints || 0 }}</text>
					<text class="label">获得积分</text>
				</view>
				<view class="stat-item">
					<text class="value">{{ center.totalOrders || 0 }}</text>
					<text class="label">推广订单</text>
				</view>
			</view>
			<view class="today-row">
				<text>今日邀请 {{ center.todayInvited || 0 }} 人，获得 {{ center.todayPoints || 0 }} 积分</text>
			</view>
		</view>

		<!-- 邀请码卡片 -->
		<view class="code-card">
			<text class="title">我的邀请码</text>
			<text class="code">{{ inviteCode.inviteCode }}</text>
			<view class="actions">
				<view class="btn" @click="copyCode">复制邀请码</view>
				<view class="btn primary" @click="shareToFriend">邀请好友</view>
			</view>
		</view>

		<!-- 功能入口 -->
		<view class="menu-card">
			<view class="menu-item" @click="goTo('/pages/promotion/invitees')">
				<text class="iconfont icon-team"></text>
				<text class="text">我的邀请</text>
				<text class="arrow">></text>
			</view>
			<view class="menu-item" @click="goTo('/pages/promotion/rewards')">
				<text class="iconfont icon-gift"></text>
				<text class="text">推广收益</text>
				<text class="arrow">></text>
			</view>
			<view class="menu-item" @click="goTo('/pages/promotion/rank')">
				<text class="iconfont icon-rank"></text>
				<text class="text">推广排行</text>
				<text class="arrow">></text>
			</view>
			<view class="menu-item" @click="goToPoster">
				<text class="iconfont icon-poster"></text>
				<text class="text">推广海报</text>
				<text class="arrow">></text>
			</view>
		</view>

		<!-- 推广规则 -->
		<view class="rules-card">
			<text class="title">推广规则</text>
			<view class="rule-item">
				<text class="dot"></text>
				<text>好友通过您的邀请码注册，您可获得{{ posterConfig?.inviteRegisterPoints || 50 }}积分奖励</text>
			</view>
			<view class="rule-item">
				<text class="dot"></text>
				<text>好友完成首单，您可获得{{ posterConfig?.firstOrderPoints || 100 }}积分奖励</text>
			</view>
			<view class="rule-item">
				<text class="dot"></text>
				<text>好友后续消费，您可获得订单金额{{ (posterConfig?.consumeRebateRate || 0.01) * 100 }}%的积分返利</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow, onShareAppMessage, onShareTimeline } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import { getPromotionCenterApi, getInviteCodeApi, getPosterConfigApi } from '../../api/promotion'

const center = ref({})
const inviteCode = ref({})
const posterUrl = ref('')
const posterConfig = ref(null)
const canvasWidth = ref(200)
const canvasHeight = ref(280)

let canvas = null

onMounted(() => {
	// #ifdef APP-PLUS
	canvas = uni.createCanvasContext('sharePosterCanvas')
	// #endif
	// 加载海报配置
	loadPosterConfig()
})

const loadPosterConfig = async () => {
	try {
		const config = await getPosterConfigApi()
		posterConfig.value = config || {}
		// 使用配置中的海报尺寸
		if (config?.width) canvasWidth.value = config.width
		if (config?.height) canvasHeight.value = config.height
	} catch (error) {
		console.error('加载海报配置失败', error)
	}
}

onShow(async () => {
	if (!ensureLoginOrRedirect()) return
	await loadData()
})

const loadData = async () => {
	try {
		uni.showLoading({ title: '加载中...' })
		const [centerData, codeData] = await Promise.all([
			getPromotionCenterApi(),
			getInviteCodeApi()
		])
		center.value = centerData || {}
		inviteCode.value = codeData || {}
	} catch (error) {
		uni.showToast({ title: error?.message || '加载失败', icon: 'none' })
	} finally {
		uni.hideLoading()
	}
}

const copyCode = () => {
	uni.setClipboardData({
		data: inviteCode.value.inviteCode,
		success: () => {
			uni.showToast({ title: '邀请码已复制', icon: 'success' })
		}
	})
}

const shareToFriend = () => {
	const code = inviteCode.value.inviteCode
	if (!code) {
		uni.showToast({ title: '邀请码加载中，请稍后', icon: 'none' })
		return
	}

	// #ifdef MP-WEIXIN
	uni.showToast({ title: '请点击右上角分享给好友', icon: 'none' })
	// #endif

	// #ifdef H5
	const link = inviteCode.value.inviteLink || ''
	const title = posterConfig.value?.title || '印力无限'
	if (navigator && navigator.share) {
		navigator.share({
			title: title,
			text: `邀请码: ${code}`,
			url: link
		}).catch(() => {
			copyShareLink(code, link)
		})
	} else {
		copyShareLink(code, link)
	}
	// #endif

	// #ifdef APP-PLUS
	uni.showActionSheet({
		itemList: ['分享到微信好友', '分享到朋友圈', '复制邀请链接'],
		success: async (res) => {
			if (res.tapIndex === 0) {
				await shareWithPoster('WXSceneSession')
			} else if (res.tapIndex === 1) {
				await shareWithPoster('WXSceneTimeline')
			} else if (res.tapIndex === 2) {
				const link = inviteCode.value.inviteLink || `https://your-domain.com/pages/auth/register?inviteCode=${code}`
				copyShareLink(code, link)
			}
		}
	})
	// #endif
}

// #ifdef APP-PLUS
const shareWithPoster = async (scene) => {
	// 检查微信是否安装
	uni.getProvider({
		service: 'share',
		success: (res) => {
			console.log('可用的分享服务:', res.provider)
			if (!res.provider.includes('weixin')) {
				uni.showToast({ title: '请先安装微信客户端', icon: 'none' })
				return
			}
			doShare(scene)
		},
		fail: () => {
			doShare(scene)
		}
	})
}

const doShare = async (scene) => {
	uni.showLoading({ title: '生成海报中...' })

	try {
		// 生成海报
		await generateSharePoster()

		if (!posterUrl.value) {
			throw new Error('海报生成失败')
		}

		console.log('海报路径:', posterUrl.value)
		uni.hideLoading()

		// 使用海报图片分享
		uni.share({
			provider: 'weixin',
			scene: scene,
			type: 2,
			imageUrl: posterUrl.value,
			success: () => {
				uni.showToast({ title: '分享成功', icon: 'success' })
			},
			fail: (err) => {
				console.error('分享失败详情:', JSON.stringify(err))
				const errCode = err.errMsg || ''
				let errMsg = '分享失败'

				if (errCode.includes('-6')) {
					errMsg = '微信分享配置有误，请检查AppID和签名配置'
				} else if (errCode.includes('-3')) {
					errMsg = '请先安装微信客户端'
				} else if (errCode.includes('-100')) {
					errMsg = '图片格式不支持，请重试'
				}

				uni.showModal({
					title: '分享失败',
					content: `${errMsg}\n\n是否保存到相册后手动分享？`,
					success: (res) => {
						if (res.confirm) {
							saveToAlbum()
						}
					}
				})
			}
		})
	} catch (error) {
		console.error('分享失败', error)
		uni.hideLoading()
		uni.showToast({ title: error?.message || '分享失败', icon: 'none' })
	}
}

const generateSharePoster = () => {
	return new Promise((resolve, reject) => {
		const code = inviteCode.value.inviteCode
		if (!code) {
			reject(new Error('邀请码不存在'))
			return
		}

		const ctx = canvas
		const w = canvasWidth.value
		const h = canvasHeight.value
		const config = posterConfig.value || {}

		// 使用配置的颜色
		const bgColorStart = config.bgColorStart || '#00bfff'
		const bgColorEnd = config.bgColorEnd || '#0099cc'
		const titleColor = config.titleColor || '#1a2030'
		const codeColor = config.codeColor || '#00bfff'
		const title = config.title || '印力无限'
		const subtitle = config.subtitle || '邀请好友注册获积分奖励'
		const tipsText = config.tipsText || '长按保存图片分享给好友'
		const qrcodeSize = config.qrcodeSize || 70

		// 绘制背景
		if (config.bgImage) {
			// 有背景图片时先下载
			uni.downloadFile({
				url: config.bgImage,
				success: (imgRes) => {
					if (imgRes.statusCode === 200) {
						ctx.drawImage(imgRes.tempFilePath, 0, 0, w, h)
						drawPosterContent(ctx, w, h, code, config, qrcodeSize, titleColor, codeColor, title, subtitle, tipsText, resolve, reject)
					} else {
						drawGradientBackground(ctx, w, h, bgColorStart, bgColorEnd)
						drawPosterContent(ctx, w, h, code, config, qrcodeSize, titleColor, codeColor, title, subtitle, tipsText, resolve, reject)
					}
				},
				fail: () => {
					drawGradientBackground(ctx, w, h, bgColorStart, bgColorEnd)
					drawPosterContent(ctx, w, h, code, config, qrcodeSize, titleColor, codeColor, title, subtitle, tipsText, resolve, reject)
				}
			})
		} else {
			drawGradientBackground(ctx, w, h, bgColorStart, bgColorEnd)
			drawPosterContent(ctx, w, h, code, config, qrcodeSize, titleColor, codeColor, title, subtitle, tipsText, resolve, reject)
		}
	})
}

const drawGradientBackground = (ctx, w, h, startColor, endColor) => {
	const gradient = ctx.createLinearGradient(0, 0, 0, h)
	gradient.addColorStop(0, startColor)
	gradient.addColorStop(1, endColor)
	ctx.setFillStyle(gradient)
	ctx.fillRect(0, 0, w, h)
}

const drawPosterContent = (ctx, w, h, code, config, qrcodeSize, titleColor, codeColor, title, subtitle, tipsText, resolve, reject) => {
	// 绘制装饰圆形
	ctx.setFillStyle('rgba(255, 255, 255, 0.1)')
	ctx.beginPath()
	ctx.arc(w - 20, 35, 40, 0, 2 * Math.PI)
	ctx.fill()
	ctx.beginPath()
	ctx.arc(20, h - 35, 35, 0, 2 * Math.PI)
	ctx.fill()

	// 绘制顶部标题区域
	ctx.setFillStyle('rgba(255, 255, 255, 0.95)')
	drawRoundRect(ctx, 10, 10, w - 20, 40, 6)
	ctx.fill()

	// 绘制标题文字
	ctx.setFillStyle(titleColor)
	ctx.setFontSize(12)
	ctx.setTextAlign('center')
	ctx.fillText(title, w / 2, 28)

	ctx.setFillStyle('#5a6a7a')
	ctx.setFontSize(8)
	ctx.fillText(subtitle, w / 2, 40)

	// 绘制中间白色卡片区域
	ctx.setFillStyle('#ffffff')
	drawRoundRect(ctx, 10, 55, w - 20, 145, 8)
	ctx.fill()

	// 绘制二维码区域
	const qrcodeX = (w - qrcodeSize) / 2
	const qrcodeY = 65

	// 二维码背景
	ctx.setFillStyle('#f5f5f5')
	drawRoundRect(ctx, qrcodeX - 5, qrcodeY - 5, qrcodeSize + 10, qrcodeSize + 10, 4)
	ctx.fill()

	// 绘制二维码占位符
	ctx.setFillStyle('#e0e0e0')
	ctx.fillRect(qrcodeX, qrcodeY, qrcodeSize, qrcodeSize)

	// 二维码中心文字
	ctx.setFillStyle('#999')
	ctx.setFontSize(7)
	ctx.setTextAlign('center')
	ctx.fillText('扫码注册', qrcodeX + qrcodeSize / 2, qrcodeY + qrcodeSize / 2 - 3)
	ctx.fillText('获取奖励', qrcodeX + qrcodeSize / 2, qrcodeY + qrcodeSize / 2 + 6)

	// 绘制邀请码
	ctx.setFillStyle('#1a2030')
	ctx.setFontSize(9)
	ctx.setTextAlign('center')
	ctx.fillText('邀请码', w / 2, qrcodeY + qrcodeSize + 18)

	// 邀请码背景
	ctx.setFillStyle(config.bgColorStart ? config.bgColorStart + '20' : '#f0f9ff')
	drawRoundRect(ctx, w / 2 - 40, qrcodeY + qrcodeSize + 22, 80, 22, 4)
	ctx.fill()

	// 邀请码文字
	ctx.setFillStyle(codeColor)
	ctx.setFontSize(14)
	ctx.font = 'bold 14px sans-serif'
	ctx.fillText(code, w / 2, qrcodeY + qrcodeSize + 38)

	// 重置字体
	ctx.font = 'normal 8px sans-serif'

	// 底部提示
	ctx.setFillStyle('#666')
	ctx.setFontSize(7)
	ctx.fillText(tipsText, w / 2, h - 10)

	// 生成二维码并绘制
	const link = inviteCode.value.inviteLink || `https://your-domain.com/pages/auth/register?inviteCode=${code}`
	const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=${qrcodeSize}x${qrcodeSize}&data=${encodeURIComponent(link)}`

	uni.downloadFile({
		url: qrUrl,
		success: (downloadRes) => {
			if (downloadRes.statusCode === 200) {
				ctx.drawImage(downloadRes.tempFilePath, qrcodeX, qrcodeY, qrcodeSize, qrcodeSize)
			}

			// 输出canvas - 使用jpg压缩确保小于32KB
			ctx.draw(false, () => {
				setTimeout(() => {
					uni.canvasToTempFilePath({
						canvasId: 'sharePosterCanvas',
						width: w,
						height: h,
						destWidth: w,
						destHeight: h,
						fileType: 'jpg',
						quality: 0.6,
						success: (res) => {
							console.log('canvasToTempFilePath成功:', res.tempFilePath)
							checkAndCompressImage(res.tempFilePath, resolve)
						},
						fail: (err) => {
							console.error('生成海报失败', err)
							reject(new Error('生成海报失败'))
						}
					})
				}, 100)
			})
		},
		fail: (err) => {
			console.error('下载二维码失败', err)
			ctx.draw(false, () => {
				setTimeout(() => {
					uni.canvasToTempFilePath({
						canvasId: 'sharePosterCanvas',
						width: w,
						height: h,
						destWidth: w,
						destHeight: h,
						fileType: 'jpg',
						quality: 0.6,
						success: (res) => {
							checkAndCompressImage(res.tempFilePath, resolve)
						},
						fail: (err) => {
							reject(new Error('生成海报失败'))
						}
					})
				}, 100)
			})
		}
	})
}

const checkAndCompressImage = (tempFilePath, resolve) => {
	plus.io.resolveLocalFileSystemURL(tempFilePath, (entry) => {
		entry.file((file) => {
			const fileSize = file.size
			console.log('图片大小:', fileSize, 'bytes (', (fileSize / 1024).toFixed(2), 'KB)')

			if (fileSize <= 32 * 1024) {
				const sharePath = entry.toLocalURL()
				posterUrl.value = sharePath
				resolve()
			} else {
				console.log('图片过大，进行压缩...')
				plus.zip.compressImage({
					src: tempFilePath,
					dst: `_doc/share_poster_${Date.now()}.jpg`,
					quality: 30,
					width: '80%',
					height: '80%',
					overwrite: true
				}, (event) => {
					console.log('压缩后大小:', event.size, 'bytes')
					posterUrl.value = event.target
					resolve()
				}, (err) => {
					console.error('压缩失败:', err)
					posterUrl.value = entry.toLocalURL()
					resolve()
				})
			}
		})
	}, () => {
		console.error('获取文件信息失败')
		posterUrl.value = tempFilePath
		resolve()
	})
}

const drawRoundRect = (ctx, x, y, width, height, radius) => {
	ctx.beginPath()
	ctx.moveTo(x + radius, y)
	ctx.lineTo(x + width - radius, y)
	ctx.quadraticCurveTo(x + width, y, x + width, y + radius)
	ctx.lineTo(x + width, y + height - radius)
	ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height)
	ctx.lineTo(x + radius, y + height)
	ctx.quadraticCurveTo(x, y + height, x, y + height - radius)
	ctx.lineTo(x, y + radius)
	ctx.quadraticCurveTo(x, y, x + radius, y)
	ctx.closePath()
}

const saveToAlbum = async () => {
	if (!posterUrl.value) {
		uni.showToast({ title: '海报还未生成', icon: 'none' })
		return
	}

	try {
		await uni.saveImageToPhotosAlbum({
			filePath: posterUrl.value
		})
		uni.showToast({ title: '已保存到相册', icon: 'success' })
	} catch (error) {
		if (error.errMsg?.includes('auth deny')) {
			uni.showModal({
				title: '提示',
				content: '需要相册权限才能保存图片，是否去设置开启权限？',
				success: (res) => {
					if (res.confirm) {
						uni.openSetting()
					}
				}
			})
		} else {
			uni.showToast({ title: '保存失败', icon: 'none' })
		}
	}
}
// #endif

const copyShareLink = (code, link) => {
	const title = posterConfig.value?.title || '印力无限'
	const shareText = `${title}邀请您\n邀请码: ${code}\n注册链接: ${link}\n\n打开链接注册即可获得积分奖励！`
	uni.setClipboardData({
		data: shareText,
		success: () => {
			uni.showToast({ title: '邀请链接已复制', icon: 'success' })
		},
		fail: () => {
			uni.showToast({ title: '复制失败', icon: 'none' })
		}
	})
}

const goTo = (url) => {
	uni.navigateTo({ url })
}

const goToPoster = () => {
	const code = inviteCode.value.inviteCode
	if (!code) {
		uni.showToast({ title: '邀请码加载中，请稍后', icon: 'none' })
		return
	}
	uni.navigateTo({ url: `/pages/promotion/poster?code=${code}&link=${encodeURIComponent(inviteCode.value.inviteLink || '')}` })
}

// #ifdef MP-WEIXIN
onShareAppMessage(() => {
	const title = posterConfig.value?.title || '印力无限'
	return {
		title: `${title}邀请您`,
		path: `/pages/auth/register?inviteCode=${inviteCode.value.inviteCode}`,
		imageUrl: '/static/share-logo.png'
	}
})

onShareTimeline(() => {
	const title = posterConfig.value?.title || '印力无限'
	return {
		title: title,
		query: `inviteCode=${inviteCode.value.inviteCode}`,
		imageUrl: '/static/share-logo.png'
	}
})
// #endif
</script>

<style scoped lang="scss">
$primary: #00bfff;
$deep: #0099cc;
$light: #5ce1ff;
$success: #10b981;
$danger: #ff4d6d;
$bg: #f8f8f8;
$card: #ffffff;
$text1: #1a2030;
$text2: #5a6a7a;
$text3: #8a9aaa;
$gradient: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
$shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);

.hidden-canvas {
	position: fixed;
	left: -9999rpx;
	top: -9999rpx;
}

.promotion-page {
	min-height: 100vh;
	padding: 28rpx;
	background: $bg;
}

.stats-card {
	background: $gradient;
	border-radius: 24rpx;
	padding: 32rpx;
	margin-bottom: 28rpx;
	box-shadow: 0 12rpx 40rpx rgba(0, 191, 255, 0.2);
	color: #ffffff;

	.stats-row {
		display: flex;
		justify-content: space-around;
	}

	.stat-item {
		text-align: center;

		.value {
			display: block;
			font-size: 44rpx;
			font-weight: 700;
		}

		.label {
			display: block;
			font-size: 24rpx;
			opacity: 0.9;
			margin-top: 8rpx;
		}
	}

	.today-row {
		margin-top: 24rpx;
		padding-top: 24rpx;
		border-top: 1rpx solid rgba(255, 255, 255, 0.2);
		font-size: 24rpx;
		text-align: center;
		opacity: 0.9;
	}
}

.code-card {
	background: $card;
	border-radius: 24rpx;
	padding: 32rpx;
	margin-bottom: 28rpx;
	box-shadow: $shadow;
	text-align: center;

	.title {
		font-size: 28rpx;
		color: $text2;
	}

	.code {
		display: block;
		font-size: 56rpx;
		font-weight: 700;
		color: $primary;
		letter-spacing: 8rpx;
		margin: 24rpx 0;
	}

	.actions {
		display: flex;
		gap: 24rpx;
		justify-content: center;

		.btn {
			padding: 16rpx 40rpx;
			border-radius: 40rpx;
			font-size: 28rpx;
			border: 1rpx solid $primary;
			color: $primary;

			&.primary {
				background: $primary;
				color: #ffffff;
			}
		}
	}
}

.menu-card {
	background: $card;
	border-radius: 24rpx;
	margin-bottom: 28rpx;
	box-shadow: $shadow;

	.menu-item {
		display: flex;
		align-items: center;
		padding: 32rpx;
		border-bottom: 1rpx solid rgba(0, 0, 0, 0.04);

		&:last-child {
			border-bottom: none;
		}

		.text {
			flex: 1;
			font-size: 30rpx;
			color: $text1;
			margin-left: 16rpx;
		}

		.arrow {
			font-size: 28rpx;
			color: $text3;
		}
	}
}

.rules-card {
	background: $card;
	border-radius: 24rpx;
	padding: 32rpx;
	box-shadow: $shadow;

	.title {
		font-size: 30rpx;
		font-weight: 600;
		color: $text1;
		margin-bottom: 24rpx;
		display: block;
	}

	.rule-item {
		display: flex;
		align-items: flex-start;
		margin-bottom: 16rpx;
		font-size: 26rpx;
		color: $text2;

		&:last-child {
			margin-bottom: 0;
		}

		.dot {
			width: 12rpx;
			height: 12rpx;
			border-radius: 50%;
			background: $primary;
			margin-right: 16rpx;
			margin-top: 10rpx;
			flex-shrink: 0;
		}
	}
}
</style>
