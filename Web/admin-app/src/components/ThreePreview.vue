<template>
  <div class="three-preview-container" ref="containerRef">
    <div v-if="loading" class="loading-overlay">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <span>正在加载 3D 模型...</span>
    </div>
    <div v-if="error" class="error-overlay">
      <el-result icon="error" title="加载失败" :sub-title="error">
        <template #extra>
          <el-button type="primary" @click="$emit('download')">下载模型文件</el-button>
        </template>
      </el-result>
    </div>
    <div ref="canvasRef" class="canvas-wrapper"></div>
    <div v-if="!loading && !error" class="controls-hint">
      <span>左键拖拽旋转 | 滚轮缩放 | 右键拖拽平移</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick, shallowRef } from 'vue'
import { Loading } from '@element-plus/icons-vue'

const props = defineProps({
  modelUrl: { type: String, required: true },
  modelType: { type: String, default: 'glb' }
})

const emit = defineEmits(['loaded', 'error', 'download'])

const containerRef = ref(null)
const canvasRef = ref(null)
const loading = ref(false)
const error = ref(null)

const scene = shallowRef(null)
const camera = shallowRef(null)
const renderer = shallowRef(null)
const controls = shallowRef(null)
let currentModel = null
let animationId = null
let THREE = null

// 初始化场景
const initScene = () => {
  if (!canvasRef.value || !THREE) return

  const width = canvasRef.value.clientWidth || 750
  const height = canvasRef.value.clientHeight || 500

  scene.value = new THREE.Scene()
  scene.value.background = new THREE.Color('#f8fafc')

  camera.value = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000)
  camera.value.position.set(5, 5, 5)

  renderer.value = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.value.setSize(width, height)
  renderer.value.setPixelRatio(window.devicePixelRatio || 1)
  renderer.value.shadowMap.enabled = true

  canvasRef.value.innerHTML = ''
  canvasRef.value.appendChild(renderer.value.domElement)

  // OrbitControls - 兼容window和THREE命名空间
  const OrbitControlsClass = window.OrbitControls || THREE.OrbitControls
  if (OrbitControlsClass) {
    controls.value = new OrbitControlsClass(camera.value, renderer.value.domElement)
    controls.value.enableDamping = true
    controls.value.dampingFactor = 0.05
  }

  // 灯光
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6)
  scene.value.add(ambientLight)

  const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8)
  directionalLight.position.set(10, 10, 10)
  directionalLight.castShadow = true
  scene.value.add(directionalLight)

  const directionalLight2 = new THREE.DirectionalLight(0xffffff, 0.5)
  directionalLight2.position.set(-10, 5, -10)
  scene.value.add(directionalLight2)

  // 网格
  const gridHelper = new THREE.GridHelper(10, 10, 0xcccccc, 0xe5e7eb)
  scene.value.add(gridHelper)

  animate()
}

const animate = () => {
  animationId = requestAnimationFrame(animate)
  if (controls.value) controls.value.update()
  if (renderer.value && scene.value && camera.value) {
    renderer.value.render(scene.value, camera.value)
  }
}

const getLoader = () => {
  if (!THREE) return null

  const type = props.modelType?.toLowerCase()
  console.log('可用加载器:', {
    GLTFLoader: typeof window.GLTFLoader,
    STLLoader: typeof window.STLLoader,
    OBJLoader: typeof window.OBJLoader,
    THREE_GLTFLoader: typeof THREE?.GLTFLoader,
    THREE_STLLoader: typeof THREE?.STLLoader
  })

  // 优先从window获取，否则从THREE获取
  const GLTFLoader = window.GLTFLoader || THREE?.GLTFLoader
  const STLLoader = window.STLLoader || THREE?.STLLoader
  const OBJLoader = window.OBJLoader || THREE?.OBJLoader
  const ThreeMFLoader = window.ThreeMFLoader || THREE?.ThreeMFLoader

  switch (type) {
    case 'glb':
    case 'gltf':
      return GLTFLoader ? new GLTFLoader() : null
    case 'stl':
      return STLLoader ? new STLLoader() : null
    case 'obj':
      return OBJLoader ? new OBJLoader() : null
    case '3mf':
      return ThreeMFLoader ? new ThreeMFLoader() : null
    default:
      return null
  }
}

const loadModel = async () => {
  if (!props.modelUrl || !THREE) {
    console.warn('无法加载模型: 缺少URL或Three.js未初始化', { url: props.modelUrl, THREE: !!THREE })
    loading.value = false
    return
  }

  loading.value = true
  error.value = null
  console.log('开始加载模型:', props.modelUrl)

  try {
    // 清理之前的模型
    if (currentModel && scene.value) {
      scene.value.remove(currentModel)
      currentModel = null
    }

    const loader = getLoader()
    console.log('加载器:', loader, '类型:', props.modelType)
    if (!loader) {
      throw new Error(`不支持的文件格式: ${props.modelType}`)
    }

    // 设置超时
    const timeout = new Promise((_, reject) =>
      setTimeout(() => reject(new Error('模型加载超时')), 60000)
    )

    const model = await Promise.race([
      new Promise((resolve, reject) => {
        loader.load(props.modelUrl,
          (obj) => resolve(obj),
          (progress) => console.log('加载进度:', (progress.loaded / progress.total * 100).toFixed(1) + '%'),
          (err) => reject(err)
        )
      }),
      timeout
    ])

    console.log('模型加载完成:', model, '类型:', model?.constructor?.name)

    // 处理模型
    if (model.scene) {
      // GLTF/GLB 格式
      currentModel = model.scene
      currentModel.traverse((child) => {
        if (child.isMesh) {
          child.castShadow = true
          child.receiveShadow = true
        }
      })
    } else if (model.isMesh) {
      // 已经是 mesh
      currentModel = model
      currentModel.castShadow = true
      currentModel.receiveShadow = true

      if (!currentModel.material) {
        currentModel.material = new THREE.MeshStandardMaterial({
          color: 0x4f46e5,
          metalness: 0.3,
          roughness: 0.7
        })
      }
    } else if (model.isGeometry || model.type === 'BufferGeometry') {
      // STL/OBJ 等直接返回 geometry，需要创建 mesh
      console.log('创建 mesh，从 geometry')
      const mesh = new THREE.Mesh(
        model,
        new THREE.MeshStandardMaterial({
          color: 0x4f46e5,
          metalness: 0.3,
          roughness: 0.7,
          side: THREE.DoubleSide
        })
      )
      mesh.castShadow = true
      mesh.receiveShadow = true
      currentModel = mesh
    }

    if (currentModel && scene.value) {
      const box = new THREE.Box3().setFromObject(currentModel)
      const center = box.getCenter(new THREE.Vector3())
      const size = box.getSize(new THREE.Vector3())
      const maxDim = Math.max(size.x, size.y, size.z)
      const scale = maxDim > 0 ? 3 / maxDim : 1

      currentModel.scale.setScalar(scale)
      currentModel.position.sub(center.multiplyScalar(scale))

      scene.value.add(currentModel)
      camera.value.position.set(5, 5, 5)
      camera.value.lookAt(0, 0, 0)

      emit('loaded')
    }
  } catch (e) {
    console.error('加载模型失败:', e)
    error.value = e.message || '模型加载失败'
    emit('error', e)
  } finally {
    loading.value = false
  }
}

const handleResize = () => {
  if (!canvasRef.value || !camera.value || !renderer.value) return
  const width = canvasRef.value.clientWidth || 750
  const height = canvasRef.value.clientHeight || 500
  camera.value.aspect = width / height
  camera.value.updateProjectionMatrix()
  renderer.value.setSize(width, height)
}

// 加载 Three.js 和加载器 - 使用本地文件
const loadThreeJs = async () => {
  // 检查是否已经加载
  if (window.THREE) {
    THREE = window.THREE
    console.log('Three.js 已加载')
    return
  }

  // 使用本地文件
  const localUrls = [
    '/three/three.min.js',
    '/three/GLTFLoader.js',
    '/three/OrbitControls.js',
    '/three/STLLoader.js',
    '/three/OBJLoader.js'
  ]

  for (const url of localUrls) {
    try {
      await loadScript(url)
      console.log('本地加载成功:', url)
    } catch (e) {
      console.warn('本地加载失败，尝试CDN:', url)
      // 回退到CDN
      const cdnUrl = url.replace('/three/', 'https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/')
      await loadScript(cdnUrl)
    }
  }

  if (!window.THREE) {
    throw new Error('Three.js 加载失败')
  }
  THREE = window.THREE
}

// 辅助函数：加载脚本
const loadScript = (url) => {
  return new Promise((resolve, reject) => {
    // 检查是否已加载
    if (url.includes('three.min.js') && window.THREE) {
      resolve()
      return
    }
    const script = document.createElement('script')
    script.src = url
    script.onload = () => resolve()
    script.onerror = () => reject(new Error(`加载失败: ${url}`))
    document.head.appendChild(script)
  })
}

onMounted(async () => {
  try {
    await loadThreeJs()
    if (THREE && props.modelUrl) {
      await nextTick()
      initScene()
      await loadModel()
    }
    window.addEventListener('resize', handleResize)
  } catch (e) {
    console.error('初始化失败:', e)
    error.value = e.message || '初始化失败'
  }
})

onUnmounted(() => {
  if (animationId) cancelAnimationFrame(animationId)
  if (renderer.value) {
    renderer.value.dispose()
    renderer.value.forceContextLoss()
  }
  if (controls.value) controls.value.dispose()
  window.removeEventListener('resize', handleResize)
})

watch(() => props.modelUrl, () => {
  if (THREE && scene.value) loadModel()
})
</script>

<style scoped>
.three-preview-container {
  width: 100%;
  height: 500px;
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  background: #f8fafc;
}
.canvas-wrapper {
  width: 100%;
  height: 100%;
}
.loading-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(248, 250, 252, 0.9);
  z-index: 10;
  gap: 12px;
  color: #64748b;
}
.loading-overlay .el-icon { color: #4f46e5; }
.error-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(248, 250, 252, 0.95);
  z-index: 10;
}
.controls-hint {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 12px;
  pointer-events: none;
}
</style>
