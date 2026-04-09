<template>
	<view class="webview-container">
		<!-- 外部链接模式 -->
		<web-view v-if="externalUrl" :src="externalUrl" class="external-webview"></web-view>
		<!-- 协议内容模式 -->
		<view v-else class="content-box">
			<view class="title">{{ pageTitle }}</view>
			<view class="update-time">更新时间：{{ updateTime }}</view>
			<view class="content" v-html="content"></view>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const pageTitle = ref('用户协议')
const content = ref('')
const updateTime = ref('2026年4月1日')
const externalUrl = ref('')

const userAgreementContent = `
	<h3>一、服务条款的确认和接纳</h3>
	<p>1.1 3D打印小物件定制商城的各项服务的所有权和运营权归本平台所有。</p>
	<p>1.2 用户在使用本平台提供的各项服务之前，应仔细阅读本服务协议。</p>
	<p>1.3 用户一旦注册使用本平台的服务，即视为用户已了解并完全同意本服务协议各项内容。</p>

	<h3>二、服务简介</h3>
	<p>2.1 本平台为用户提供3D打印模型定制服务，用户可以在平台上浏览、购买、定制3D打印产品。</p>
	<p>2.2 本平台提供的设计作品由入驻设计师提供，平台对作品的原创性进行审核。</p>
	<p>2.3 用户在平台上发布的定制需求，由设计师进行报价和设计。</p>

	<h3>三、用户注册</h3>
	<p>3.1 用户注册成功后，本平台将给予每个用户一个用户账号及相应的密码，该用户账号和密码由用户负责保管。</p>
	<p>3.2 用户对以其用户账号进行的所有活动和事件负法律责任。</p>
	<p>3.3 用户须对在本平台注册时所提供的真实、准确、合法的用户资料负责。</p>

	<h3>四、用户行为规范</h3>
	<p>4.1 用户在使用本平台服务过程中，必须遵循以下原则：</p>
	<p>（1）遵守中国有关的法律和法规；</p>
	<p>（2）不得为任何非法目的而使用网络服务系统；</p>
	<p>（3）遵守所有与网络服务有关的网络协议、规定和程序；</p>
	<p>（4）不得利用本平台服务进行任何可能对互联网正常运转造成不利影响的行为；</p>
	<p>（5）不得利用本平台服务传输任何骚扰性的、中伤他人的、辱骂性的、恐吓性的、庸俗淫秽的或其他任何非法的信息资料。</p>

	<h3>五、交易规则</h3>
	<p>5.1 用户在平台上下单购买商品，视为与卖家达成买卖合同关系。</p>
	<p>5.2 用户应当在收到商品后及时验收，如有质量问题应在规定时间内申请售后。</p>
	<p>5.3 定制类商品由于个性化特点，不支持无理由退货，具体以商品详情页说明为准。</p>

	<h3>六、知识产权</h3>
	<p>6.1 本平台上所有内容，包括但不限于文字、图片、音频、视频、软件、程序、版面设计等的知识产权归本平台或相关权利人所有。</p>
	<p>6.2 未经本平台或相关权利人书面许可，任何人不得擅自使用、转载上述内容。</p>

	<h3>七、免责声明</h3>
	<p>7.1 用户明确同意其使用本平台网络服务所存在的风险将完全由其自己承担。</p>
	<p>7.2 本平台不保证网络服务一定能满足用户的要求，也不保证网络服务不会中断，对网络服务的及时性、安全性、准确性也都不作保证。</p>

	<h3>八、协议修改</h3>
	<p>8.1 本平台有权在必要时修改本服务协议，并在平台上公布。</p>
	<p>8.2 如用户不同意本平台对本服务协议所做的修改，用户有权停止使用网络服务。</p>
	<p>8.3 如用户继续使用网络服务，则视为用户接受本平台对本服务协议所做的修改。</p>
`

const privacyPolicyContent = `
	<h3>一、信息收集</h3>
	<p>我们收集您在使用我们服务时主动提供的信息，包括：</p>
	<p>1. 账户信息：当您注册账户时，我们会收集您的用户名、密码、电子邮箱、手机号码等。</p>
	<p>2. 交易信息：当您在平台进行交易时，我们会收集您的收货地址、支付信息等。</p>
	<p>3. 设备信息：我们会收集您的设备型号、操作系统、唯一设备标识符等信息。</p>
	<p>4. 日志信息：当您使用我们的服务时，我们会自动收集您的访问时间、浏览记录等信息。</p>

	<h3>二、信息使用</h3>
	<p>我们使用收集的信息用于以下目的：</p>
	<p>1. 提供、维护、改进我们的服务；</p>
	<p>2. 处理您的订单和交易；</p>
	<p>3. 向您发送有关我们服务的信息；</p>
	<p>4. 分析用户行为，改善用户体验；</p>
	<p>5. 防止欺诈行为，保护平台安全。</p>

	<h3>三、信息共享</h3>
	<p>我们不会向第三方出售您的个人信息。我们仅在以下情况下与第三方共享您的信息：</p>
	<p>1. 获得您的明确同意后；</p>
	<p>2. 与服务提供商共享，以协助我们提供服务；</p>
	<p>3. 为遵守法律法规的要求；</p>
	<p>4. 为保护我们、用户或公众的权利、财产或安全。</p>

	<h3>四、信息安全</h3>
	<p>我们采取适当的安全措施来保护您的个人信息，包括：</p>
	<p>1. 使用加密技术保护数据传输；</p>
	<p>2. 限制员工访问个人信息的权限；</p>
	<p>3. 定期审查我们的信息收集、存储和处理实践。</p>

	<h3>五、您的权利</h3>
	<p>您对您的个人信息享有以下权利：</p>
	<p>1. 访问您的个人信息；</p>
	<p>2. 更正不准确的个人信息；</p>
	<p>3. 删除您的个人信息；</p>
	<p>4. 撤回您的同意；</p>
	<p>5. 注销您的账户。</p>

	<h3>六、Cookie使用</h3>
	<p>我们使用Cookie和类似技术来提供、保护和改进我们的服务。您可以通过浏览器设置管理Cookie。</p>

	<h3>七、未成年人保护</h3>
	<p>我们的服务不面向14岁以下的儿童。如果我们发现在未经父母同意的情况下收集了儿童的个人信息，我们会尽快删除该信息。</p>

	<h3>八、隐私政策更新</h3>
	<p>我们可能会不时更新本隐私政策。更新后的政策将在本页面发布，建议您定期查看。</p>

	<h3>九、联系我们</h3>
	<p>如果您对本隐私政策有任何疑问或建议，请通过以下方式联系我们：</p>
	<p>电子邮箱：support@3dprint.com</p>
`

onLoad((options) => {
	// 优先处理外部链接模式
	if (options?.url) {
		externalUrl.value = decodeURIComponent(options.url)
		const title = options?.title || '详情'
		pageTitle.value = decodeURIComponent(title)
		uni.setNavigationBarTitle({ title: pageTitle.value })
		return
	}

	// 协议内容模式
	const type = options?.type || 'userAgreement'
	const title = options?.title || '用户协议'

	pageTitle.value = decodeURIComponent(title)

	if (type === 'privacyPolicy') {
		content.value = privacyPolicyContent
		pageTitle.value = '隐私政策'
	} else {
		content.value = userAgreementContent
	}

	uni.setNavigationBarTitle({ title: pageTitle.value })
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
$gradient-primary: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);

@keyframes fadeInUp {
	from { opacity: 0; transform: translateY(24rpx); }
	to { opacity: 1; transform: translateY(0); }
}

.webview-container {
	min-height: 100vh;
	background-color: $surface;
}

.external-webview {
	width: 100%;
	height: 100vh;
}

.content-box {
	padding: 36rpx;
	background-color: $surface-raised;
	border-radius: 24rpx;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.35s ease-out both;
	min-height: 100vh;
}

.title {
	font-size: 36rpx;
	font-weight: 700;
	color: $text-primary;
	text-align: center;
	margin-bottom: 16rpx;
}

.update-time {
	font-size: 24rpx;
	color: $text-muted;
	text-align: center;
	margin-bottom: 32rpx;
	padding-bottom: 24rpx;
	position: relative;

	&::after {
		content: '';
		position: absolute;
		left: 0;
		right: 0;
		bottom: 0;
		height: 1rpx;
		background: rgba(0, 0, 0, 0.03);
	}
}

.content {
	font-size: 28rpx;
	color: $text-secondary;
	line-height: 1.8;

	:deep(h3) {
		font-size: 30rpx;
		font-weight: 600;
		color: $text-primary;
		margin-top: 36rpx;
		margin-bottom: 16rpx;
	}

	:deep(p) {
		margin-bottom: 12rpx;
		text-align: justify;
	}
}
</style>
