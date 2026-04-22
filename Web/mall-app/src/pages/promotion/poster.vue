<template>
	<view class="poster-page">
		<!-- 海报预览 -->
		<view class="poster-container">
			<canvas canvas-id="posterCanvas" class="poster-canvas" :style="{ width: canvasWidth + 'px', height: canvasHeight + 'px' }"></canvas>
			<image v-if="posterUrl" :src="posterUrl" class="poster-image" mode="widthFix" @click="previewImage" />
		</view>

		<!-- 操作按钮 -->
		<view class="actions">
			<button class="btn-primary" @click="saveToAlbum">保存到相册</button>
			<button class="btn-secondary" @click="shareImage">分享图片</button>
		</view>

		<!-- 提示 -->
		<view class="tips">
			<text>长按海报可保存到相册，分享给好友注册后可获得积分奖励</text>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getInviteCodeApi, getPosterConfigApi } from '../../api/promotion'

const inviteCode = ref('')
const inviteLink = ref('')
const posterUrl = ref('')
const posterConfig = ref(null)
const canvasWidth = ref(375)
const canvasHeight = ref(600)

let canvas = null

onLoad(async (options) => {
	// 从URL参数获取邀请码，或从API获取
	if (options?.code) {
		inviteCode.value = options.code
		inviteLink.value = options.link ? decodeURIComponent(options.link) : ''
	} else {
		await loadInviteCode()
	}
})

onMounted(() => {
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

		// 延迟获取canvas并生成海报
		setTimeout(() => {
			canvas = uni.createCanvasContext('posterCanvas')
			generatePoster()
		}, 100)
	} catch (error) {
		console.error('加载海报配置失败', error)
		// 使用默认配置生成海报
		setTimeout(() => {
			canvas = uni.createCanvasContext('posterCanvas')
			generatePoster()
		}, 100)
	}
}

const loadInviteCode = async () => {
	try {
		uni.showLoading({ title: '加载中...' })
		const data = await getInviteCodeApi()
		inviteCode.value = data?.inviteCode || ''
		inviteLink.value = data?.inviteLink || ''
	} catch (error) {
		uni.showToast({ title: error?.message || '加载失败', icon: 'none' })
	} finally {
		uni.hideLoading()
	}
}

const generatePoster = async () => {
	if (!inviteCode.value) {
		uni.showToast({ title: '邀请码加载失败', icon: 'none' })
		return
	}

	uni.showLoading({ title: '生成海报中...' })

	try {
		const ctx = canvas
		const w = canvasWidth.value
		const h = canvasHeight.value
		const config = posterConfig.value || {}

		// 使用配置的颜色和文字
		const bgColorStart = config.bgColorStart || '#00bfff'
		const bgColorEnd = config.bgColorEnd || '#0099cc'
		const titleColor = config.titleColor || '#1a2030'
		const codeColor = config.codeColor || '#00bfff'
		const title = config.title || '印力无限'
		const subtitle = config.subtitle || '邀请好友注册，双方均可获得积分奖励'
		const tipsText = config.tipsText || '长按保存图片，分享给好友'
		const qrcodeSize = config.qrcodeSize || 160

		// 绘制背景（支持背景图片或渐变色）
		if (config.bgImage) {
			// 有背景图片时先下载
			const [err, imgRes] = await new Promise((resolve) => {
				uni.downloadFile({
					url: config.bgImage,
					success: (res) => resolve([null, res]),
					fail: (err) => resolve([err, null])
				})
			})
			if (!err && imgRes && imgRes.tempFilePath) {
				ctx.drawImage(imgRes.tempFilePath, 0, 0, w, h)
			} else {
				drawGradientBackground(ctx, w, h, bgColorStart, bgColorEnd)
			}
		} else {
			drawGradientBackground(ctx, w, h, bgColorStart, bgColorEnd)
		}

		// 绘制装饰圆形
		ctx.setFillStyle('rgba(255, 255, 255, 0.1)')
		ctx.beginPath()
		ctx.arc(w - 50, 80, 100, 0, 2 * Math.PI)
		ctx.fill()
		ctx.beginPath()
		ctx.arc(50, h - 100, 80, 0, 2 * Math.PI)
		ctx.fill()

		// 绘制顶部标题区域
		ctx.setFillStyle('rgba(255, 255, 255, 0.95)')
		drawRoundRect(ctx, 20, 20, w - 40, 100, 12)
		ctx.fill()

		// 绘制标题文字
		ctx.setFillStyle(titleColor)
		ctx.setFontSize(22)
		ctx.setTextAlign('center')
		ctx.fillText(title, w / 2, 55)

		ctx.setFillStyle('#5a6a7a')
		ctx.setFontSize(14)
		ctx.fillText(subtitle, w / 2, 85)

		// 绘制中间白色卡片区域
		ctx.setFillStyle('#ffffff')
		drawRoundRect(ctx, 20, 140, w - 40, 320, 16)
		ctx.fill()

		// 绘制二维码区域
		const qrcodeX = (w - qrcodeSize) / 2
		const qrcodeY = 170

		// 二维码背景
		ctx.setFillStyle('#f5f5f5')
		drawRoundRect(ctx, qrcodeX - 10, qrcodeY - 10, qrcodeSize + 20, qrcodeSize + 20, 8)
		ctx.fill()

		// 绘制二维码占位符
		ctx.setFillStyle('#e0e0e0')
		ctx.fillRect(qrcodeX, qrcodeY, qrcodeSize, qrcodeSize)

		// 二维码中心文字
		ctx.setFillStyle('#999')
		ctx.setFontSize(12)
		ctx.setTextAlign('center')
		ctx.fillText('扫码注册', qrcodeX + qrcodeSize / 2, qrcodeY + qrcodeSize / 2 - 10)
		ctx.fillText('获取积分奖励', qrcodeX + qrcodeSize / 2, qrcodeY + qrcodeSize / 2 + 10)

		// 尝试生成真实二维码
		await generateQRCode(ctx, qrcodeX, qrcodeY, qrcodeSize)

		// 绘制邀请码
		ctx.setFillStyle('#1a2030')
		ctx.setFontSize(16)
		ctx.setTextAlign('center')
		ctx.fillText('邀请码', w / 2, qrcodeY + qrcodeSize + 40)

		// 邀请码背景
		ctx.setFillStyle(config.bgColorStart ? config.bgColorStart + '20' : '#f0f9ff')
		drawRoundRect(ctx, w / 2 - 80, qrcodeY + qrcodeSize + 50, 160, 44, 8)
		ctx.fill()

		// 邀请码文字
		ctx.setFillStyle(codeColor)
		ctx.setFontSize(24)
		ctx.font = 'bold 24px sans-serif'
		ctx.fillText(inviteCode.value, w / 2, qrcodeY + qrcodeSize + 82)

		// 重置字体
		ctx.font = 'normal 14px sans-serif'

		// 底部提示
		ctx.setFillStyle('#666')
		ctx.setFontSize(12)
		ctx.fillText(tipsText, w / 2, h - 50)

		// 输出canvas
		ctx.draw(false, () => {
			// 延迟一下确保绘制完成
			setTimeout(() => {
				convertCanvasToImage()
			}, 300)
		})

	} catch (error) {
		console.error('生成海报失败', error)
		uni.hideLoading()
		uni.showToast({ title: '生成海报失败', icon: 'none' })
	}
}

const drawGradientBackground = (ctx, w, h, startColor, endColor) => {
	const gradient = ctx.createLinearGradient(0, 0, 0, h)
	gradient.addColorStop(0, startColor)
	gradient.addColorStop(1, endColor)
	ctx.setFillStyle(gradient)
	ctx.fillRect(0, 0, w, h)
}

const convertCanvasToImage = () => {
	const w = canvasWidth.value
	const h = canvasHeight.value

	// #ifdef APP-PLUS
	uni.canvasToTempFilePath({
		canvasId: 'posterCanvas',
		width: w,
		height: h,
		destWidth: w * 2,
		destHeight: h * 2,
		success: (res) => {
			posterUrl.value = res.tempFilePath
			uni.hideLoading()
		},
		fail: (err) => {
			console.error('生成海报失败', err)
			uni.hideLoading()
			uni.showToast({ title: '生成海报失败', icon: 'none' })
		}
	})
	// #endif

	// #ifdef H5
	const canvasEl = document.querySelector('.poster-canvas')
	if (canvasEl) {
		posterUrl.value = canvasEl.toDataURL('image/png')
	}
	uni.hideLoading()
	// #endif
}

// 生成二维码
const generateQRCode = async (ctx, x, y, size) => {
	try {
		const link = inviteLink.value || `https://your-domain.com/pages/auth/register?inviteCode=${inviteCode.value}`
		const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=${size}x${size}&data=${encodeURIComponent(link)}`

		// #ifdef APP-PLUS
		const [err, res] = await new Promise((resolve) => {
			uni.downloadFile({
				url: qrUrl,
				success: (res) => resolve([null, res]),
				fail: (err) => resolve([err, null])
			})
		})

		if (!err && res && res.tempFilePath) {
			ctx.drawImage(res.tempFilePath, x, y, size, size)
		}
		// #endif

		// #ifdef H5
		const img = new Image()
		img.crossOrigin = 'anonymous'
		img.src = qrUrl
		await new Promise((resolve) => {
			img.onload = () => {
				ctx.drawImage(img, x, y, size, size)
				resolve()
			}
			img.onerror = () => resolve()
		})
		// #endif
	} catch (error) {
		console.error('生成二维码失败', error)
	}
}

// 绘制圆角矩形
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

// 保存到相册
const saveToAlbum = async () => {
	if (!posterUrl.value) {
		uni.showToast({ title: '海报还未生成', icon: 'none' })
		return
	}

	// #ifdef APP-PLUS
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
	// #endif

	// #ifdef H5
	uni.showToast({ title: '请长按图片保存', icon: 'none' })
	// #endif
}

// 分享图片
const shareImage = () => {
	if (!posterUrl.value) {
		uni.showToast({ title: '海报还未生成', icon: 'none' })
		return
	}

	// #ifdef APP-PLUS
	uni.showActionSheet({
		itemList: ['分享到微信好友', '分享到朋友圈'],
		success: (res) => {
			if (res.tapIndex === 0) {
				shareToWeixin('WXSceneSession')
			} else if (res.tapIndex === 1) {
				shareToWeixin('WXSceneTimeline')
			}
		}
	})
	// #endif

	// #ifdef H5
	uni.showToast({ title: '请长按图片保存后分享', icon: 'none' })
	// #endif
}

// #ifdef APP-PLUS
const shareToWeixin = (scene) => {
	uni.share({
		provider: 'weixin',
		scene: scene,
		type: 2, // 图片类型
		imageUrl: posterUrl.value,
		success: () => {
			uni.showToast({ title: '分享成功', icon: 'success' })
		},
		fail: (err) => {
			console.error('分享失败', err)
			uni.showModal({
				title: '分享失败',
				content: '是否保存到相册后手动分享？',
				success: (res) => {
					if (res.confirm) {
						saveToAlbum()
					}
				}
			})
		}
	})
}
// #endif

// 预览图片
const previewImage = () => {
	if (posterUrl.value) {
		uni.previewImage({
			urls: [posterUrl.value]
		})
	}
}
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

.poster-page {
	min-height: 100vh;
	background: $bg;
	padding: 28rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.poster-container {
	width: 100%;
	display: flex;
	justify-content: center;
	margin-bottom: 32rpx;
}

.poster-canvas {
	position: fixed;
	left: -9999rpx;
	top: -9999rpx;
}

.poster-image {
	width: 100%;
	max-width: 750rpx;
	border-radius: 16rpx;
	box-shadow: $shadow;
}

.actions {
	display: flex;
	gap: 24rpx;
	width: 100%;
	max-width: 750rpx;
	margin-bottom: 24rpx;
}

.btn-primary {
	flex: 1;
	height: 88rpx;
	background: $gradient;
	color: #ffffff;
	border-radius: 44rpx;
	font-size: 30rpx;
	font-weight: 600;
	display: flex;
	align-items: center;
	justify-content: center;
	border: none;

	&:active {
		opacity: 0.9;
	}
}

.btn-secondary {
	flex: 1;
	height: 88rpx;
	background: #ffffff;
	color: $primary;
	border-radius: 44rpx;
	font-size: 30rpx;
	font-weight: 600;
	display: flex;
	align-items: center;
	justify-content: center;
	border: 2rpx solid $primary;

	&:active {
		background: #f5f5f5;
	}
}

.tips {
	text-align: center;
	font-size: 24rpx;
	color: $text3;
	padding: 0 32rpx;
}
</style>
