<template>
	<view class="three-container">
		<view v-if="loading" class="loading-overlay">
			<uni-icons type="spinner-cycle" size="30" color="#4f46e5" class="spin"></uni-icons>
			<text>正在加载 3D 模型...</text>
		</view>
		<view v-if="notSupported" class="loading-overlay">
			<text>当前环境暂不支持 3D 预览，请稍后重试</text>
		</view>
		<view id="threejsContent" class="canvas-container" :prop="threejsProp" :change:prop="Threejs.create"></view>
	</view>
</template>

<script>
import { ref, computed } from 'vue'

export default {
	props: {
		modelUrl: { type: String, default: '' },
		modelType: { type: String, default: 'glb' }, // glb, stl, obj, 3mf
		modelColor: { type: String, default: '#4f46e5' },
		materialType: { type: String, default: 'standard' }, // standard, physical, shiny
		autoRotate: { type: Boolean, default: true }
	},
	emits: ['loaded', 'error'],
	setup(props, { emit }) {
		const loading = ref(false)
		const notSupported = ref(false)

		const threejsProp = computed(() => {
			return {
				modelUrl: props.modelUrl,
				modelType: props.modelType,
				modelColor: props.modelColor,
				materialType: props.materialType,
				autoRotate: props.autoRotate,
				time: new Date().getTime()
			}
		})

		const onModelLoaded = () => {
			loading.value = false
			emit('loaded')
		}

		const onModelError = (err) => {
			loading.value = false
			emit('error', err)
		}

		const onModelLoading = () => {
			loading.value = true
		}

		return {
			loading,
			notSupported,
			threejsProp,
			onModelLoaded,
			onModelError,
			onModelLoading
		}
	}
}
</script>

<script module="Threejs" lang="renderjs">
import * as THREE from '../static/lib/three/three.module.js'
import { OrbitControls } from '../static/lib/three/OrbitControls.js'
import { GLTFLoader } from '../static/lib/three/GLTFLoader.js'
import { STLLoader } from '../static/lib/three/STLLoader.js'
import { OBJLoader } from '../static/lib/three/OBJLoader.js'
import { ThreeMFLoader } from '../static/lib/three/ThreeMFLoader.js'

let scene, camera, renderer, controls, model, rafId

export default {
	data() {
		return {
			ownerInstance: null
		}
	},
	mounted() {
		// initScene()
	},
	unmounted() {
		this.dispose()
	},
	methods: {
		dispose() {
			if (rafId) cancelAnimationFrame(rafId)
			if (typeof window !== 'undefined' && window.removeEventListener) {
				window.removeEventListener('resize', this.handleResize)
			}
			if (renderer) {
				renderer.dispose()
				renderer.forceContextLoss()
				renderer.domElement = null
				renderer = null
			}
			if (controls) {
				controls.dispose()
				controls = null
			}
			scene = null
			camera = null
			model = null
		},
		async create(newVal, oldVal, ownerInstance) {
			this.ownerInstance = ownerInstance

			let props = newVal
			if (typeof props === 'string') {
				let value = props
				if (value.indexOf('json://') === 0) {
					value = value.replace('json://', '')
				}
				try {
					props = JSON.parse(value)
				} catch (error) {
					console.error('renderjs props 解析失败', error)
					return
				}
			}

			if (!props || !props.modelUrl) return

			if (!scene) {
				this.initScene()
			}
			
			this.loadModel(props)
		},
		initScene() {
			const containerEl = document.getElementById('threejsContent')
			if (!containerEl) return

			const width = containerEl.clientWidth || 300
			const height = containerEl.clientHeight || 300

			scene = new THREE.Scene()
			scene.background = new THREE.Color('#f8fafc')

			camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000)
			camera.position.set(5, 5, 5)

			renderer = new THREE.WebGLRenderer({
				antialias: true,
				alpha: true
			})
			renderer.setSize(width, height)
			renderer.setPixelRatio(window.devicePixelRatio || 1)
			renderer.shadowMap.enabled = true
			renderer.setClearColor(0xf8fafc, 1)
			
			// Clear previous canvas if any
			while(containerEl.firstChild) {
				containerEl.removeChild(containerEl.firstChild)
			}
			containerEl.appendChild(renderer.domElement)

			const ambientLight = new THREE.AmbientLight(0xffffff, 0.8)
			scene.add(ambientLight)

			const dirLight = new THREE.DirectionalLight(0xffffff, 1)
			dirLight.position.set(5, 10, 7)
			dirLight.castShadow = true
			scene.add(dirLight)

			controls = new OrbitControls(camera, renderer.domElement)
			controls.enableDamping = true

			const animate = () => {
				rafId = requestAnimationFrame(animate)
				if (controls) {
					controls.update()
				}
				if (renderer && scene && camera) {
					renderer.render(scene, camera)
				}
			}
			animate()

			window.addEventListener('resize', this.handleResize)
		},
		handleResize() {
			if (!renderer || !camera) return
			const containerEl = document.getElementById('threejsContent')
			if (!containerEl) return
			const width = containerEl.clientWidth
			const height = containerEl.clientHeight
			camera.aspect = width / height
			camera.updateProjectionMatrix()
			renderer.setSize(width, height)
		},
		async loadModel(props) {
			if (!scene) return
			
			if (this.ownerInstance) {
				this.ownerInstance.callMethod('onModelLoading')
			}

			if (model) {
				scene.remove(model)
				model = null
			}

			if (controls) {
				controls.autoRotate = props.autoRotate
			}

			const extension = props.modelType || props.modelUrl.split('.').pop().toLowerCase()
			let loader

			try {
				switch (extension) {
					case 'glb':
					case 'gltf':
						loader = new GLTFLoader()
						const gltf = await loader.loadAsync(props.modelUrl)
						model = gltf.scene
						break
					case 'stl':
						loader = new STLLoader()
						const geometry = await loader.loadAsync(props.modelUrl)
						const material = this.createMaterial(props)
						model = new THREE.Mesh(geometry, material)
						break
					case 'obj':
						loader = new OBJLoader()
						model = await loader.loadAsync(props.modelUrl)
						break
					case '3mf':
						loader = new ThreeMFLoader()
						model = await loader.loadAsync(props.modelUrl)
						break
					default:
						console.error('不支持的文件格式')
						this.createPlaceholder(props)
						return
				}

				const box = new THREE.Box3().setFromObject(model)
				const center = box.getCenter(new THREE.Vector3())
				const size = box.getSize(new THREE.Vector3())
				const maxDim = Math.max(size.x, size.y, size.z)
				const scale = 3 / maxDim
				model.scale.set(scale, scale, scale)
				model.position.sub(center.multiplyScalar(scale))

				scene.add(model)
				this.updateModelMaterial(props)
				
				if (this.ownerInstance) {
					this.ownerInstance.callMethod('onModelLoaded')
				}
			} catch (err) {
				console.error('加载模型失败:', err)
				if (this.ownerInstance) {
					this.ownerInstance.callMethod('onModelError', err.message || '加载失败')
				}
				this.createPlaceholder(props)
			}
		},
		createPlaceholder(props) {
			if (model) scene.remove(model)
			const geometry = new THREE.TorusKnotGeometry(1, 0.3, 100, 16)
			const material = this.createMaterial(props)
			model = new THREE.Mesh(geometry, material)
			scene.add(model)
		},
		createMaterial(props) {
			let material
			const color = props.modelColor
			
			switch (props.materialType) {
				case 'physical':
					material = new THREE.MeshPhysicalMaterial({
						color: color,
						metalness: 0.8,
						roughness: 0.2,
						clearcoat: 1.0
					})
					break
				case 'shiny':
					material = new THREE.MeshStandardMaterial({
						color: color,
						metalness: 0.9,
						roughness: 0.1
					})
					break
				default:
					material = new THREE.MeshStandardMaterial({
						color: color,
						metalness: 0.5,
						roughness: 0.5
					})
			}
			return material
		},
		updateModelMaterial(props) {
			if (!model) return
			const material = this.createMaterial(props)
			model.traverse((child) => {
				if (child.isMesh) {
					child.material = material
					child.castShadow = true
					child.receiveShadow = true
				}
			})
		}
	}
}
</script>

<style scoped>
.three-container {
	width: 100%;
	height: 100%;
	position: relative;
	background-color: #f8fafc;
	overflow: hidden;
}
.canvas-container {
	width: 100%;
	height: 100%;
	display: block;
}
.loading-overlay {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	background-color: rgba(248, 250, 252, 0.8);
	color: #4f46e5;
	font-size: 24rpx;
	z-index: 10;
}
.spin {
	animation: rotate 2s linear infinite;
	margin-bottom: 16rpx;
}
@keyframes rotate {
	from { transform: rotate(0deg); }
	to { transform: rotate(360deg); }
}
</style>
