<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="queryParams.modelName" placeholder="搜索模型名称..." style="width: 200px" clearable @clear="handleSearch">
            <template #prefix><el-icon><search /></el-icon></template>
          </el-input>
          <el-select v-model="queryParams.categoryId" placeholder="所有分类" clearable style="width: 150px" @change="handleSearch">
            <el-option v-for="cat in categoryOptions" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
          </el-select>
          <el-select v-model="queryParams.status" placeholder="所有状态" clearable style="width: 120px" @change="handleSearch">
            <el-option label="审核中" :value="0" />
            <el-option label="已上架" :value="1" />
            <el-option label="已下架" :value="2" />
          </el-select>
          <el-select v-model="queryParams.sourceType" placeholder="来源类型" clearable style="width: 120px" @change="handleSearch">
            <el-option label="官方" :value="1" />
            <el-option label="设计者作品" :value="2" />
          </el-select>
          <el-select v-model="queryParams.orderBy" placeholder="排序方式" style="width: 140px" @change="handleSearch">
            <el-option label="创建时间" value="create_time" />
            <el-option label="价格升序" value="price_asc" />
            <el-option label="价格降序" value="price_desc" />
          </el-select>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button type="success" icon="Download" @click="handleExport" :loading="exportLoading">导出</el-button>
        </el-space>
        <el-button type="primary" icon="Plus" @click="handleAdd">新增模型</el-button>
      </div>

      <div class="table-wrapper">
        <el-table :data="modelData" stripe border highlight-current-row style="width: 100%" v-loading="loading" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" />
          <el-table-column prop="id" label="模型ID" width="160" />
          <el-table-column label="主图" width="100">
            <template #default="scope">
              <el-image
                v-if="scope.row.mainImageUrl"
                :src="scope.row.mainImageUrl"
                style="width:60px; height:60px; border-radius:8px"
                :preview-src-list="[scope.row.mainImageUrl]"
                preview-teleported
              />
              <div v-else class="no-image">无图片</div>
            </template>
          </el-table-column>
          <el-table-column prop="modelName" label="模型名称" min-width="150" />
          <el-table-column prop="categoryName" label="分类" width="120">
            <template #default="scope">
              {{ scope.row.categoryName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="designerName" label="设计者" width="150" />
          <el-table-column label="来源" width="110">
            <template #default="scope">
              <el-tag :type="scope.row.sourceType === 1 ? 'primary' : 'success'" effect="light" round>
                {{ scope.row.sourceTypeDesc || '-' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="basePrice" label="基础价格" width="100">
            <template #default="scope">
              ￥{{ Number(scope.row.basePrice).toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)" effect="light" round>
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="handleViewDetail(scope.row)">详情</el-button>
              <el-divider direction="vertical" />
              <el-button link type="info" @click="handleWatermark(scope.row)">水印</el-button>
              <el-divider direction="vertical" />
              <el-button link type="warning" @click="handleEdit(scope.row)">编辑</el-button>
              <el-divider direction="vertical" />
              <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-count="Math.max(1, Math.ceil((total || 0) / queryParams.pageSize))"
          :page-sizes="[10, 20, 50, 100]"
          :hide-on-single-page="false"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 模型编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogType === 'add' ? '新增模型' : '编辑模型'" width="650px">
      <el-form :model="form" label-width="130px">
        <el-form-item label="模型名称" required>
          <el-input v-model="form.modelName" placeholder="请输入模型名称" />
        </el-form-item>
          <el-form-item label="模型描述" required>
          <el-input v-model="form.description" placeholder="请输入模型描述" />
        </el-form-item>
        <el-form-item label="所属分类">
          <el-select v-model="form.categoryId" style="width: 100%" placeholder="请选择分类" clearable>
            <el-option v-for="cat in categoryOptions" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="基础价格(元)" required>
          <el-input-number v-model="form.basePrice" :precision="2" :step="0.01" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="原始体积(mm³)" required>
          <el-input-number v-model="form.baseVolume" :min="0" :precision="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="三维尺寸(L*W*H)" required>
          <el-input v-model="form.baseSize" placeholder="例如: 100x80x60" />
        </el-form-item>
        <el-form-item label="模型文件" required>
          <div class="file-upload-container">
            <!-- 模型文件上传进度条 -->
            <el-progress
              v-if="modelFileUploading"
              :percentage="modelFileUploadProgress"
              :stroke-width="8"
              status="success"
              text-inside
              :format="formatProgress"
            />
            <div class="file-info" v-if="form.filePath">
              <span class="file-name">{{ getFileName(form.filePath) }}</span>
              <el-button link type="danger" @click="removeModelFile">删除</el-button>
            </div>
            <div v-else class="upload-btn" @click="triggerModelFileUpload">
              <el-icon><Upload /></el-icon>
              <div>上传模型文件</div>
            </div>
            <input
              ref="modelFileInputRef"
              type="file"
              accept=".stl,.obj,.ply,.3mf"
              style="display: none"
              @change="handleModelFileUpload"
            />
            <div class="file-tip">支持的格式：STL, OBJ, PLY, 3MF</div>
          </div>
        </el-form-item>
        <el-form-item label="模型图片">
          <div class="image-upload-container">
            <!-- 图片上传进度条 -->
            <el-progress
              v-if="imageUploading"
              :percentage="imageUploadProgress"
              :stroke-width="8"
              status="success"
              text-inside
              :format="formatProgress"
            />
            <div class="image-list">
              <div
                v-for="(img, index) in form.images"
                :key="img.id || index"
                class="image-item"
                :class="{ 'is-main': img.isMain === 1 }"
              >
                <el-image
                  :src="img.imageUrl"
                  fit="cover"
                  style="width: 100px; height: 100px; border-radius: 8px"
                  :preview-src-list="[img.imageUrl]"
                  preview-teleported
                />
                <div class="image-actions">
                  <el-button
                    v-if="img.isMain !== 1"
                    type="primary"
                    size="small"
                    circle
                    @click="setMainImage(img)"
                  >
                    <el-icon><Star /></el-icon>
                  </el-button>
                  <el-tag v-else type="success" size="small">主图</el-tag>
                  <el-button
                    type="danger"
                    size="small"
                    circle
                    @click="removeImage(index)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
              <div class="upload-btn" @click="triggerFileUpload">
                <el-icon><Plus /></el-icon>
                <div>上传图片</div>
              </div>
            </div>
            <input
              ref="fileInputRef"
              type="file"
              accept="image/*"
              multiple
              style="display: none"
              @change="handleFileUpload"
            />
          </div>
        </el-form-item>
        <el-form-item label="授权说明">
          <el-input v-model="form.licenseType" type="textarea" :rows="2" placeholder="请输入授权说明" />
        </el-form-item>
        <el-form-item label="可用材质">
          <div class="materials-container">
            <div class="materials-header">
              <span>材质列表</span>
              <el-button type="primary" size="small" @click="showMaterialDialog">
                <el-icon><Plus /></el-icon>
                添加材质
              </el-button>
            </div>
            <div v-if="form.materials && form.materials.length > 0" class="materials-list">
              <div v-for="(material, index) in form.materials" :key="material.id || index" class="material-item">
                <div class="material-info">
                  <div class="material-name">
                    {{ material.name }}
                    <el-tag v-if="material.isEco" type="success" size="small" style="margin-left: 8px">
                      <el-icon style="margin-right: 2px"><SuccessFilled /></el-icon>
                      环保
                    </el-tag>
                  </div>
                  <div class="material-details">
                    <el-input-number
                      v-model="material.price"
                      :precision="2"
                      :step="0.01"
                      :min="0"
                      size="small"
                      style="width: 120px; margin-right: 10px"
                      @change="updateMaterialInfo(index, material)"
                    />

                    <span>元/克</span>

                  </div>
                </div>
                <div class="material-actions">
                  <el-button
                    type="primary"
                    size="small"
                    circle
                    @click="showMaterialDialog(material, index)"
                  >
                    <el-icon><Edit /></el-icon>
                  </el-button>
                  <el-button
                    type="danger"
                    size="small"
                    circle
                    @click="removeMaterial(index)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
            <div v-else class="no-materials">
              <span>暂无材质，请点击"添加材质"添加</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="上架状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">审核中</el-radio>
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="2">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 材质编辑弹窗 -->
    <el-dialog v-model="materialDialogVisible" :title="editingMaterialIndex >= 0 ? '编辑材质' : '添加材质'" width="500px">
      <el-form :model="materialForm" label-width="100px">
        <el-form-item label="材质名称" required>
          <el-input v-model="materialForm.name" placeholder="请输入材质名称" />
        </el-form-item>
        <el-form-item label="价格(元/克)" required>
          <el-input-number
            v-model="materialForm.price"
            :precision="2"
            :step="0.01"
            :min="0"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="环保材质">
          <el-switch v-model="materialForm.isEco" active-text="是" inactive-text="否" />
          <div class="eco-tip" v-if="materialForm.isEco">
            <el-icon color="#22c55e"><SuccessFilled /></el-icon>
            <span>选择环保材质的用户将获得额外积分奖励</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="materialDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMaterial">保存</el-button>
      </template>
    </el-dialog>

    <!-- 模型详情弹窗 -->
    <el-dialog v-model="detailVisible" title="模型详情" width="750px">
      <el-descriptions :column="2" border v-if="currentModel">
        <el-descriptions-item label="模型ID" :span="2">{{ currentModel.id }}</el-descriptions-item>
        <el-descriptions-item label="模型名称">{{ currentModel.modelName }}</el-descriptions-item>
          <el-descriptions-item label="模型描述">{{ currentModel.description }}</el-descriptions-item>
        <el-descriptions-item label="设计者">{{ currentModel.designerName }}</el-descriptions-item>
        <el-descriptions-item label="所属分类">{{ currentModel.categoryName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="基础价格">￥{{ Number(currentModel.basePrice).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="原始体积">{{ currentModel.baseVolume }} mm³</el-descriptions-item>
        <el-descriptions-item label="三维尺寸" :span="2">{{ currentModel.baseSize }}</el-descriptions-item>
        <el-descriptions-item label="文件路径" :span="2">
          <div v-if="currentModel.filePath">
            <span>{{ getFileName(currentModel.filePath) }}</span>
            <el-button link type="primary" @click="handlePreview3D">预览3D</el-button>
            <el-button link type="primary" @click="window.open(currentModel.filePath, '_blank')">下载</el-button>
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="授权说明" :span="2">{{ currentModel.licenseType|| '-' }}</el-descriptions-item>
        <el-descriptions-item label="主展示图" :span="2">
          <el-image
            v-if="currentModel.mainImageUrl"
            :src="currentModel.mainImageUrl"
            style="width:120px; height:120px; border-radius:8px"
            :preview-src-list="[currentModel.mainImageUrl]"
            preview-teleported
          />
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentModel.status)" effect="light" round>
            {{ getStatusText(currentModel.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="可用材质" :span="2" v-if="currentModel.materials && currentModel.materials.length > 0">
          <el-tag
            v-for="material in currentModel.materials"
            :key="material.id"
            :type="material.isEco ? 'success' : 'info'"
            size="small"
            style="margin-right: 8px; margin-bottom: 4px;"
          >
            {{ material.name }} (￥{{ material.price }}/克)
            <el-icon v-if="material.isEco" style="margin-left: 4px"><SuccessFilled /></el-icon>
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="可用材质" :span="2" v-else>
          <span style="color: #999;">暂无材质</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 3D模型预览弹窗 -->
    <el-dialog v-model="preview3DVisible" title="3D模型预览" width="900px" top="5vh" destroy-on-close>
      <ThreePreview
        v-if="preview3DVisible"
        :model-url="preview3DUrl"
        :model-type="preview3DType"
        @loaded="onPreviewLoaded"
        @error="onPreviewError"
      />
      <template #footer>
        <el-button @click="downloadModelFile">下载模型</el-button>
        <el-button @click="preview3DVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 水印管理弹窗 -->
    <el-dialog v-model="watermarkDialogVisible" title="水印管理" width="500px">
      <div v-if="watermarkModel" class="watermark-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="模型名称">{{ watermarkModel.modelName }}</el-descriptions-item>
          <el-descriptions-item label="模型ID">{{ watermarkModel.id }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="watermarkStatus" class="watermark-status">
          <el-row :gutter="20" style="margin-top: 20px">
            <el-col :span="8">
              <div class="status-card">
                <div class="status-value">{{ watermarkStatus.totalImages || 0 }}</div>
                <div class="status-label">图片总数</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="status-card">
                <div class="status-value" style="color: #67c23a">{{ watermarkStatus.watermarkedImages || 0 }}</div>
                <div class="status-label">已生成水印</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="status-card">
                <div class="status-value">{{ watermarkStatus.coveragePercent || 0 }}%</div>
                <div class="status-label">覆盖率</div>
              </div>
            </el-col>
          </el-row>

          <el-progress
            :percentage="watermarkStatus.coveragePercent || 0"
            :stroke-width="10"
            style="margin-top: 20px"
            :status="watermarkStatus.isComplete ? 'success' : ''"
          />
        </div>

        <el-skeleton v-else :rows="3" animated style="margin-top: 20px" />
      </div>

      <template #footer>
        <el-button @click="watermarkDialogVisible = false">关闭</el-button>
        <el-button type="info" @click="handleGenerateThumbnails" :loading="thumbnailLoading">
          生成缩略图
        </el-button>
        <el-button type="warning" @click="handleRegenerateWatermark" :loading="watermarkLoading">
          重新生成
        </el-button>
        <el-button type="primary" @click="handleGenerateWatermark" :loading="watermarkLoading">
          生成水印
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Star, Upload, Edit, SuccessFilled } from '@element-plus/icons-vue'
import ThreePreview from '../../components/ThreePreview.vue'
import request from '../../utils/request'
import {
  getModelList,
  getModelDetail,
  createModel,
  updateModel,
  deleteModel,
  getModelCategories,
  getCategoryTreeRecursive,
  uploadFile,
  uploadFileWithProgress,
  addModelImage,
  setMainImage as apiSetMainImage,
  deleteModelImage as apiDeleteModelImage,
  addMaterial,
  updateMaterial as apiUpdateMaterial,
  deleteMaterial as apiDeleteMaterial,
  getModelMaterials,
  exportModels,
  generateWatermark,
  regenerateWatermark,
  getWatermarkStatus,
  generateThumbnails
} from '../../api/model'

const loading = ref(false)
const modelData = ref([])
const categoryOptions = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const preview3DVisible = ref(false)
const preview3DUrl = ref('')
const preview3DType = ref('glb')
const preview3DError = ref(false)
const dialogType = ref('add')
const currentModel = ref(null)
const fileInputRef = ref(null)
const modelViewerContainer = ref(null)
const modelFileInputRef = ref(null)
const imageUploading = ref(false)
const modelFileUploading = ref(false)
const imageUploadProgress = ref(0)
const modelFileUploadProgress = ref(0)
const materialDialogVisible = ref(false)
const editingMaterialIndex = ref(-1)
const exportLoading = ref(false)
const selectedModels = ref([]) // 选中的模型列表

// 水印管理相关
const watermarkDialogVisible = ref(false)
const watermarkModel = ref(null)
const watermarkStatus = ref(null)
const watermarkLoading = ref(false)
const thumbnailLoading = ref(false)

const queryParams = reactive({
  modelName: '',
  categoryId: null,
  designerId: null,
  status: null,
  sourceType: null,
  orderBy: 'create_time',
  pageNum: 1,
  pageSize: 10
})

const form = reactive({
  id: null,
  modelName: '',
  description: '',
  categoryId: null,
  basePrice: 0,
  baseVolume: 0,
  baseSize: '',
  filePath: '',
  mainImageUrl: '', // Deprecated, kept for backward compatibility
  images: [], // Array of { id, imageUrl, isMain, imgType, sortOrder }
  materials: [], // Array of { id, name, price }
  licenseType: '',
  status: 0
})

// 获取分类列表
const fetchCategories = async () => {
  try {
    // 优先使用递归查询API获取完整的分类树
    let categories = []
    try {
      const response = await getCategoryTreeRecursive()
      console.log('分类树响应:', response)

      if (response) {
        if (Array.isArray(response)) {
          categories = response
        } else if (response.records) {
          // IPage结构
          categories = response.records
        } else if (response.list) {
          categories = response.list
        } else {
          categories = []
        }
      }
    } catch (treeError) {
      console.warn('递归API失败，尝试使用普通API:', treeError)
      // 回退到普通API
      const response = await getModelCategories()
      console.log('分类列表响应:', response)

      if (response) {
        if (Array.isArray(response)) {
          categories = response
        } else if (response.records) {
          categories = response.records
        } else if (response.list) {
          categories = response.list
        } else {
          categories = []
        }
      }
    }

    // 如果有树形结构，展平为一维数组
    if (categories.length > 0 && categories[0]?.children) {
      categoryOptions.value = flattenCategories(categories)
    } else {
      categoryOptions.value = categories.map(cat => ({
        id: cat.id,
        categoryName: cat.categoryName,
        categoryCode: cat.categoryCode
      }))
    }
    console.log('分类选项:', categoryOptions.value)
  } catch (error) {
    console.error('获取分类列表失败:', error)
    categoryOptions.value = []
  }
}

// 展平分类树（用于下拉选择）
const flattenCategories = (categories) => {
  const result = []
  const flatten = (items) => {
    for (const item of items) {
      result.push({
        id: item.id,
        categoryName: item.categoryName,
        categoryCode: item.categoryCode
      })
      if (item.children && item.children.length > 0) {
        flatten(item.children)
      }
    }
  }
  flatten(categories)
  return result
}


// 获取模型列表
const fetchModelList = async () => {
  loading.value = true
  try {
    const params = { ...queryParams }
    // 移除空值
    Object.keys(params).forEach(key => {
      if (params[key] === '' || params[key] === null) {
        delete params[key]
      }
    })
    const response = await getModelList(params)
    modelData.value = response?.records || []
    total.value = response?.total || 0
    queryParams.pageNum = response?.pageNum || queryParams.pageNum
    queryParams.pageSize = response?.pageSize || queryParams.pageSize

    // 处理图片URL，确保完整路径
    modelData.value = modelData.value.map(model => ({
      ...model,
      mainImageUrl: model.mainImageUrl || ''
    }))
  } catch (error) {
    console.error('获取模型列表失败:', error)
    modelData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  queryParams.pageNum = 1
  fetchModelList()
}

// 页码变化
const handlePageChange = (page) => {
  queryParams.pageNum = page
  fetchModelList()
}

// 每页大小变化
const handleSizeChange = (size) => {
  queryParams.pageSize = size
  queryParams.pageNum = 1
  fetchModelList()
}

// 获取状态类型
const getStatusType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'info' }
  return map[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const map = { 0: '审核中', 1: '已上架', 2: '已下架' }
  return map[status] || '未知'
}

// 新增
const handleAdd = () => {
  dialogType.value = 'add'
  Object.assign(form, {
    id: null,
    modelName: '',
    description: '',
    categoryId: null,
    basePrice: 0,
    baseVolume: 0,
    baseSize: '',
    filePath: '',
    mainImageUrl: '',
    images: [],
    materials: [],
    licenseType: '',
    status: 0
  })
  dialogVisible.value = true
}

// ==================== 图片管理函数 ====================

// 触发文件上传
const triggerFileUpload = () => {
  fileInputRef.value?.click()
}

// 处理文件上传
const handleFileUpload = async (event) => {
  const files = event.target.files
  if (!files || files.length === 0) return

  imageUploading.value = true
  imageUploadProgress.value = 0

  try {
    for (let i = 0; i < files.length; i++) {
      const file = files[i]
      imageUploadProgress.value = 0

      ElMessage.info(`正在上传第 ${i + 1}/${files.length} 张图片...`)

      // 使用 XMLHttpRequest 上传以支持进度条
      const response = await uploadFileWithProgress(file, 'modelImg', (progress) => {
        imageUploadProgress.value = progress
      })
      console.log('上传文件响应:', response)

      // 如果是第一张图片，默认设置为主图
      const isFirstImage = form.images.length === 0

      // 创建临时ID避免重复
      const tempId = 'temp_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)

      form.images.push({
        id: tempId, // 使用临时ID
        imageUrl: response, // 直接使用返回的URL，不添加前缀
        isMain: isFirstImage ? 1 : 0,
        imgType: 1,
        sortOrder: form.images.length
      })

      imageUploadProgress.value = 100
    }
    ElMessage.success(`成功上传 ${files.length} 张图片`)
  } catch (error) {
    console.error('上传图片失败:', error)
    ElMessage.error('上传图片失败')
  } finally {
    imageUploading.value = false
    imageUploadProgress.value = 0
    // 清空文件输入，允许重复上传同一文件
    event.target.value = ''
  }
}

// ==================== 模型文件管理函数 ====================

// 触发模型文件上传
const triggerModelFileUpload = () => {
  modelFileInputRef.value?.click()
}

// 处理模型文件上传
const handleModelFileUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  // 检查文件类型
  const allowedTypes = ['.stl', '.obj', '.ply', '.3mf']
  const fileExtension = '.' + file.name.split('.').pop().toLowerCase()
  if (!allowedTypes.includes(fileExtension)) {
    ElMessage.error('不支持的文件类型，请上传 STL、OBJ、PLY 或 3MF 格式的文件')
    return
  }

  // 检查文件大小（限制为 500MB）
  const maxSize = 500 * 1024 * 1024; // 500MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 500MB')
    return
  }

  modelFileUploading.value = true
  modelFileUploadProgress.value = 0
  try {
    ElMessage.info('正在上传模型文件...')
    const response = await uploadFileWithProgress(file, 'modelFile', (progress) => {
      modelFileUploadProgress.value = progress
    })
    console.log('上传模型文件响应:', response)

    form.filePath = response
    ElMessage.success('模型文件上传成功')
  } catch (error) {
    console.error('上传模型文件失败:', error)
    ElMessage.error('上传模型文件失败')
  } finally {
    modelFileUploading.value = false
    modelFileUploadProgress.value = 0
    // 清空文件输入，允许重复上传同一文件
    event.target.value = ''
  }
}

// 删除模型文件
const removeModelFile = () => {
  form.filePath = ''
}

// 获取文件名
const getFileName = (filePath) => {
  if (!filePath) return ''
  return filePath.split('/').pop().split('\\').pop()
}

// 格式化进度条显示
const formatProgress = (percentage) => {
  return `${percentage}%`
}

// 材质表单
const materialForm = reactive({
  name: '',
  price: 0,
  isEco: false
})

// 显示材质编辑对话框
const showMaterialDialog = (material = null, index = -1) => {
  if (material) {
    // 编辑模式
    editingMaterialIndex.value = index
    Object.assign(materialForm, {
      ...material,
      isEco: material.isEco || false
    })
  } else {
    // 新增模式
    editingMaterialIndex.value = -1
    Object.assign(materialForm, {
      name: '',
      price: 0,
      isEco: false
    })
  }
  materialDialogVisible.value = true
}

// 更新材质
const updateMaterialInfo = async (index, material) => {
  try {
    const payload = {
      ...material,
      price: Number(material.price),
      isEco: material.isEco || false
    }
    if (form.id && material.id && !material.id.toString().startsWith('temp_')) {
      // 调用API更新材质
      await apiUpdateMaterial(form.id, material.id, payload)
      ElMessage.success('材质更新成功')
    }
    // 如果是临时材质，不需要调用API，直接更新本地数据
  } catch (error) {
    console.error('更新材质失败:', error)
    ElMessage.error('更新材质失败')
  }
}

// 保存材质
const saveMaterial = async () => {
  if (!materialForm.name || materialForm.price === null || materialForm.price === undefined) {
    ElMessage.warning('请填写完整的材质信息')
    return
  }

  try {
    const submitMaterial = {
      ...materialForm,
      price: Number(materialForm.price)
    }

    if (editingMaterialIndex.value >= 0) {
      // 更新现有材质
      form.materials[editingMaterialIndex.value] = { ...submitMaterial }

      // 如果是编辑模式且有真实材质ID，调用API
      if (form.id && submitMaterial.id && !submitMaterial.id.toString().startsWith('temp_')) {
        try {
          await apiUpdateMaterial(form.id, submitMaterial.id, submitMaterial)
          ElMessage.success('材质更新成功')
        } catch (error) {
          console.log('API更新失败，但本地已更新', error)
          ElMessage.success('材质更新成功（本地保存）')
        }
      } else {
        ElMessage.success('材质更新成功')
      }
    } else {
      // 添加新材质
      const newMaterial = {
        id: 'temp_' + Date.now(),
        ...submitMaterial
      }
      form.materials.push(newMaterial)

      // 如果是编辑模式，尝试调用API保存材质
      if (form.id) {
        try {
          await addMaterial(form.id, newMaterial)
          // 更新临时ID为真实ID（如果API返回了新ID）
          form.materials[form.materials.length - 1].id = newMaterial.id
          ElMessage.success('材质添加成功')
        } catch (error) {
          console.log('API添加失败，但本地已保存', error)
          ElMessage.success('材质添加成功（本地保存）')
        }
      } else {
        ElMessage.success('材质添加成功')
      }
    }

    materialDialogVisible.value = false
  } catch (error) {
    console.error('保存材质失败:', error)
    ElMessage.error('保存材质失败')
  }
}

// 删除材质
const removeMaterial = async (index) => {
  const material = form.materials[index]

  ElMessageBox.confirm('确定要删除这个材质吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      // 如果是编辑模式且有真实材质ID，调用API删除
      if (form.id && material.id && !material.id.toString().startsWith('temp_')) {
        await apiDeleteMaterial(form.id, material.id)
      }

      form.materials.splice(index, 1)
      ElMessage.success('材质删除成功')
    } catch (error) {
      console.error('删除材质失败:', error)
      ElMessage.error('删除材质失败')
    }
  })
}

// 设置主图
const setMainImage = async (image) => {
  try {
    // 检查是否是临时图片
    const isTempImage = image.id && image.id.toString().startsWith('temp_')

    // 如果是编辑模式且有真实图片ID，调用API
    if (dialogType.value === 'edit' && form.id && image.id && !isTempImage) {
      console.log('调用API设置主图:', form.id, image.id)
      await apiSetMainImage(form.id, image.id)
      console.log('API调用成功')

      // 重新获取模型详情以更新图片列表
      const response = await getModelDetail(form.id)
      form.images = response.images?.map(img => ({
        id: img.id,
        imageUrl: img.imageUrl || '',
        isMain: img.isMain,
        imgType: img.imgType,
        sortOrder: img.sortOrder
      })) || []

      // 更新主图URL显示
      const mainImage = form.images.find(img => img.isMain === 1)
      if (mainImage) {
        form.mainImageUrl = mainImage.imageUrl
      }
    } else {
      // 本地更新（新增模式或临时图片）
      form.images.forEach(img => img.isMain = 0)
      image.isMain = 1
      form.mainImageUrl = image.imageUrl
    }

    ElMessage.success('已设置为主图')
  } catch (error) {
    console.error('设置主图失败:', error)
    ElMessage.error('设置主图失败: ' + (error.message || '未知错误'))
  }
}

// 删除图片
const removeImage = async (index) => {
  const image = form.images[index]

  // 检查是否是临时图片（新上传但未保存到数据库的图片）
  const isTempImage = image.id && image.id.toString().startsWith('temp_')

  // 如果是编辑模式且有真实图片ID，调用API删除
  if (dialogType.value === 'edit' && image.id && !isTempImage) {
    try {
      await apiDeleteModelImage(image.id)
      ElMessage.success('删除成功')

      // 重新获取模型详情以更新图片列表
      if (form.id) {
        const response = await getModelDetail(form.id)
        form.images = response.images?.map(img => ({
          id: img.id,
          imageUrl: img.imageUrl || '',
          isMain: img.isMain,
          imgType: img.imgType,
          sortOrder: img.sortOrder
        })) || []

        // 更新主图URL显示
        const mainImage = form.images.find(img => img.isMain === 1)
        if (mainImage) {
          form.mainImageUrl = mainImage.imageUrl
        } else if (form.images.length > 0) {
          form.images[0].isMain = 1
          form.mainImageUrl = form.images[0].imageUrl
        } else {
          form.mainImageUrl = ''
        }
      }
    } catch (error) {
      console.error('删除图片失败:', error)
      ElMessage.error('删除图片失败')
      return
    }
  } else {
    // 本地删除（新增模式或临时图片）
    form.images.splice(index, 1)

    // 如果删除的是主图，设置第一张图片为主图
    if (image.isMain === 1 && form.images.length > 0) {
      form.images[0].isMain = 1
      form.mainImageUrl = form.images[0].imageUrl
    } else if (form.images.length === 0) {
      form.mainImageUrl = ''
    }

    ElMessage.success('删除成功')
  }
}

// 编辑
const handleEdit = async (row) => {
  try {
    // 先获取完整的模型详情，确保所有字段都有值
    const response = await getModelDetail(row.id)
    console.log('编辑时获取模型详情:', response)

    dialogType.value = 'edit'
    Object.assign(form, {
      id: response.id,
      modelName: response.modelName || '',
      categoryId: response.categoryId || null,
      basePrice: Number(response.basePrice) || 0,
      baseVolume: response.baseVolume || 0,
      baseSize: response.baseSize || '',
      filePath: response.filePath || '',
      mainImageUrl: response.mainImageUrl || 'default-image-url',
      images: response.images?.map(img => ({
        id: img.id,
        imageUrl: img.imageUrl || '',
        isMain: img.isMain,
        imgType: img.imgType,
        sortOrder: img.sortOrder
      })) || [],
      materials: response.materials || [],
      licenseType: response.licenseType  || '',
      status: response.status ?? 0,
      description: response.description || ''
    })

    form.materials = response.materials || []

    // 确保至少有一张图片作为主图
    const hasMainImage = form.images.some(img => img.isMain === 1)
    if (!hasMainImage && form.images.length > 0) {
      form.images[0].isMain = 1
      form.mainImageUrl = form.images[0].imageUrl
    }

    dialogVisible.value = true
  } catch (error) {
    console.error('获取模型详情失败，使用表格数据:', error)
    // 如果获取详情失败，回退到使用表格数据
    dialogType.value = 'edit'
    Object.assign(form, {
      id: row.id,
      modelName: row.modelName || '',
      categoryId: row.categoryId || null,
      basePrice: Number(row.basePrice) || 0,
      baseVolume: row.baseVolume || 0,
      baseSize: row.baseSize || '',
      filePath: row.filePath || '',
      mainImageUrl: row.mainImageUrl || 'default-image-url',
      images: [],
      licenseType: row.licenseType  || '',
      status: row.status ?? 0,
      description: row.description || ''
    })
    dialogVisible.value = true
  }
}

// 查看详情
const handleViewDetail = async (row) => {
  try {
    const response = await getModelDetail(row.id)
    console.log('模型详情响应:', response)
    // 响应拦截器已经返回了res.data
    currentModel.value = {
      ...response,
      mainImageUrl: response.mainImageUrl || ''
    } || row
    detailVisible.value = true
  } catch (error) {
    console.error('获取模型详情失败:', error)
    currentModel.value = row
    detailVisible.value = true
  }
}

// 动态加载 Three.js
const loadThreeJs = () => {
  return new Promise((resolve) => {
    if (window.THREE) {
      resolve()
      return
    }
    const baseUrl = window.location.origin
    // 加载 Three.js 主库
    const loader = new window.XMLHttpRequest()
    loader.open('GET', baseUrl + '/three/three.module.js', true)
    loader.onload = () => {
      // 动态执行脚本
      const script = document.createElement('script')
      script.textContent = loader.responseText
      document.head.appendChild(script)
      resolve()
    }
    loader.onerror = () => {
      // 如果本地没有，尝试 CDN
      loadFromCDN().then(resolve)
    }
    loader.send()
  })
}

// 从 CDN 加载（备选方案）
const loadFromCDN = () => {
  return new Promise((resolve) => {
    const script = document.createElement('script')
    // 使用 unpkg 国内镜像
    script.src = 'https://unpkg.com/three@0.160.0/build/three.module.js'
    script.type = 'module'
    script.onload = () => resolve()
    script.onerror = () => {
      ElMessage.error('3D预览组件加载失败')
      resolve()
    }
    document.head.appendChild(script)
  })
}

// 预览3D模型
// 预览3D模型 - 在新窗口打开
// 预览3D模型 - 使用 ThreePreview 组件
const handlePreview3D = () => {
  if (!currentModel.value?.filePath) {
    ElMessage.warning('暂无模型文件，无法预览')
    return
  }

  // 检查文件格式是否支持
  const filePath = currentModel.value.filePath
  const ext = filePath.split('.').pop().toLowerCase()
  const supportedFormats = ['glb', 'gltf', 'stl', 'obj', '3mf']

  if (!supportedFormats.includes(ext)) {
    ElMessage.warning('该文件格式暂不支持')
    return
  }

  // 设置模型URL - 使用动态baseURL
  let modelUrl = filePath
  if (!filePath.startsWith('http://') && !filePath.startsWith('https://')) {
    const baseURL = request.defaults.baseURL || 'http://127.0.0.1:9999'
    modelUrl = baseURL + (filePath.startsWith('/') ? '' : '/') + filePath
  }

  preview3DUrl.value = modelUrl
  preview3DType.value = ext
  preview3DVisible.value = true
}

// 预览加载完成
const onPreviewLoaded = () => {
  console.log('3D模型加载成功')
}

// 预览加载失败
const onPreviewError = (err) => {
  console.error('3D模型加载失败:', err)
  ElMessage.error('3D模型加载失败，请尝试下载查看')
}

// 下载模型文件
const downloadModelFile = () => {
  if (currentModel.value?.filePath) {
    window.open(currentModel.value.filePath, '_blank')
  }
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除模型 ${row.modelName} 吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteModel(row.id)
      ElMessage.success('删除成功')
      fetchModelList()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  })
}

// 提交表单
const submitForm = async () => {
  if (!form.modelName || !form.basePrice || !form.baseVolume || !form.baseSize || !form.filePath) {
    ElMessage.warning('请填写必填项')
    return
  }

  try {
    // 获取主图URL
    const mainImage = form.images.find(img => img.isMain === 1)
    const mainImageUrl = mainImage?.imageUrl || ''
    console.log('mainImage:', mainImage)
    console.log('mainImageUrl:', mainImageUrl)

    // 构建提交数据
    const submitData = {
      id: form.id,
      modelName: form.modelName,
      categoryId: form.categoryId,
      basePrice: Number(form.basePrice),
      baseVolume: Number(form.baseVolume),
      baseSize: form.baseSize,
      filePath: form.filePath,
      description: form.description || '',
      mainImageUrl: mainImageUrl || 'default-image-url', // 确保总是有值，使用默认值
      materials: form.materials,
      licenseType: form.licenseType || 'Commercial',
      status: form.status
    }

    let modelId = form.id

    if (dialogType.value === 'add') {
      // 新增模型
      modelId = await createModel(submitData)
      ElMessage.success('新增成功')

      // 添加材质到模型（新增模型时材质需在模型创建后单独落库）
      for (let i = 0; i < form.materials.length; i++) {
        const material = form.materials[i]
        await addMaterial(modelId, {
          materialId: material.materialId || null,
          name: material.name,
          price: Number(material.price),
          isEco: material.isEco || false
        })
      }

      // 添加图片到模型
      for (let i = 0; i < form.images.length; i++) {
        const img = form.images[i]
        await addModelImage(modelId, img.imageUrl, img.isMain, img.imgType, img.sortOrder)
      }
    } else {
      // 更新模型
      await updateModel(submitData)
      ElMessage.success('更新成功')

      // 添加新上传的图片（临时ID的图片）
      for (let i = 0; i < form.images.length; i++) {
        const img = form.images[i]
        if (img.id && img.id.toString().startsWith('temp_')) {
          await addModelImage(form.id, img.imageUrl, img.isMain, img.imgType, img.sortOrder)
        }
      }
    }
    dialogVisible.value = false
    // 刷新模型列表
    await fetchModelList()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error(dialogType.value === 'add' ? '新增失败' : '更新失败')
  }
}

// 导出模型数据
const handleExport = async () => {
  if (selectedModels.value.length === 0) {
    ElMessage.warning('请先勾选要导出的模型')
    return
  }

  exportLoading.value = true
  try {
    const params = {
      modelIds: selectedModels.value.map(m => m.id)
    }

    const blob = await exportModels(params)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `模型数据_${new Date().toISOString().slice(0, 10)}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success(`成功导出 ${selectedModels.value.length} 个模型`)
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

// 表格选择变化
const handleSelectionChange = (selection) => {
  selectedModels.value = selection
}

// ==================== 水印管理 ====================

// 打开水印管理弹窗
const handleWatermark = async (row) => {
  watermarkModel.value = row
  watermarkStatus.value = null
  watermarkDialogVisible.value = true

  // 加载水印状态
  try {
    const status = await getWatermarkStatus(row.id)
    watermarkStatus.value = status
  } catch (error) {
    console.error('获取水印状态失败:', error)
    ElMessage.error('获取水印状态失败')
  }
}

// 生成水印
const handleGenerateWatermark = async () => {
  if (!watermarkModel.value) return

  watermarkLoading.value = true
  try {
    const result = await generateWatermark(watermarkModel.value.id)
    ElMessage.success(`成功生成 ${result.processedCount || 0} 张图片的水印`)

    // 刷新状态
    const status = await getWatermarkStatus(watermarkModel.value.id)
    watermarkStatus.value = status
  } catch (error) {
    console.error('生成水印失败:', error)
    ElMessage.error('生成水印失败')
  } finally {
    watermarkLoading.value = false
  }
}

// 重新生成水印
const handleRegenerateWatermark = async () => {
  if (!watermarkModel.value) return

  try {
    await ElMessageBox.confirm('重新生成会删除现有水印，确定继续？', '提示', { type: 'warning' })
  } catch {
    return
  }

  watermarkLoading.value = true
  try {
    const result = await regenerateWatermark(watermarkModel.value.id)
    ElMessage.success(`成功重新生成 ${result.processedCount || 0} 张图片的水印`)

    // 刷新状态
    const status = await getWatermarkStatus(watermarkModel.value.id)
    watermarkStatus.value = status
  } catch (error) {
    console.error('重新生成水印失败:', error)
    ElMessage.error('重新生成水印失败')
  } finally {
    watermarkLoading.value = false
  }
}

// 生成缩略图
const handleGenerateThumbnails = async () => {
  if (!watermarkModel.value) return

  thumbnailLoading.value = true
  try {
    const result = await generateThumbnails(watermarkModel.value.id)
    ElMessage.success(`成功生成 ${result.processedCount || 0} 张图片的缩略图`)

    // 刷新状态
    const status = await getWatermarkStatus(watermarkModel.value.id)
    watermarkStatus.value = status
  } catch (error) {
    console.error('生成缩略图失败:', error)
    ElMessage.error('生成缩略图失败')
  } finally {
    thumbnailLoading.value = false
  }
}

onMounted(() => {
  fetchCategories()
  fetchModelList()
})

// 监听弹窗关闭，清理 model-viewer
watch(preview3DVisible, (val) => {
  if (!val && modelViewerContainer.value) {
    modelViewerContainer.value.innerHTML = ''
  }
})
</script>

<style scoped>
.page-container {
  padding: 0;
}

.table-card {
  background: var(--bg-primary);
  padding: 28px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-light);
}

.header-actions :deep(.el-input__wrapper),
.header-actions :deep(.el-select .el-input__wrapper) {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  transition: all 0.2s ease;
}

.header-actions :deep(.el-input__wrapper:hover) {
  border-color: var(--border-dark);
}

.header-actions :deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px var(--primary-lighter);
}

.table-wrapper {
  overflow-x: auto;
  margin: 0 -4px;
  padding: 0 4px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--border-light);
}

.no-image {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  font-size: 12px;
  color: var(--text-muted);
}

/* 图片上传样式 */
.image-upload-container {
  margin-top: 12px;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.image-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--border-color);
  transition: all 0.2s ease;
}

.image-item:hover {
  box-shadow: var(--shadow-md);
}

.image-item.is-main {
  border: 2px solid var(--success-color);
  box-shadow: 0 0 0 3px var(--success-light);
}

.image-actions {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  gap: 8px;
  padding: 6px;
}

.upload-btn {
  width: 100px;
  height: 100px;
  border: 2px dashed var(--border-dark);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: var(--bg-secondary);
  transition: all 0.3s ease;
}

.upload-btn:hover {
  border-color: var(--primary-color);
  background: var(--primary-lighter);
}

.upload-btn .el-icon {
  font-size: 24px;
  color: var(--text-muted);
  margin-bottom: 4px;
}

.upload-btn:hover .el-icon {
  color: var(--primary-color);
}

.upload-btn div {
  font-size: 12px;
  color: var(--text-muted);
}

/* 文件上传样式 */
.file-upload-container {
  margin-top: 12px;
}

.file-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  margin-bottom: 12px;
}

.file-name {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-upload-container .upload-btn {
  width: 100%;
  height: 80px;
  border: 2px dashed var(--border-dark);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: var(--bg-secondary);
  transition: all 0.3s ease;
  margin-bottom: 12px;
}

.file-upload-container .upload-btn:hover {
  border-color: var(--primary-color);
  background: var(--primary-lighter);
}

.file-upload-container .upload-btn .el-icon {
  font-size: 24px;
  color: var(--text-muted);
  margin-bottom: 4px;
}

.file-upload-container .upload-btn div {
  font-size: 12px;
  color: var(--text-muted);
}

.file-tip {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 6px;
}

/* 自定义进度条样式 */
:deep(.el-progress--text-inside .el-progress-bar__outer) {
  border-radius: var(--radius-sm);
}

:deep(.el-progress-bar__inner) {
  transition: width 0.3s ease;
}

:deep(.el-progress--success .el-progress-bar__inner) {
  background: linear-gradient(90deg, var(--success-color), #34d399);
}

/* 材质管理样式 */
.materials-container {
  margin-top: 12px;
}

.materials-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.materials-list {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 12px;
  background: var(--bg-secondary);
}

.material-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  margin: 4px 0;
  border-radius: var(--radius-md);
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  transition: all 0.2s ease;
}

.material-item:hover {
  box-shadow: var(--shadow-sm);
}

.material-item:last-child {
  border-bottom: none;
}

.material-name {
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.material-details {
  display: flex;
  align-items: center;
  gap: 8px;
}

.no-materials {
  padding: 24px;
  text-align: center;
  color: var(--text-muted);
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
}

.material-actions {
  display: flex;
  gap: 8px;
}

.eco-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  padding: 10px 14px;
  background: var(--success-light);
  border-radius: var(--radius-md);
  color: var(--success-color);
  font-size: 13px;
  font-weight: 500;
}

.preview-3d-container {
  width: 100%;
  min-height: 400px;
}

.preview-iframe {
  width: 100%;
  height: 500px;
  border-radius: var(--radius-md);
  border: none;
}

.preview-tip {
  width: 100%;
  padding: 40px 0;
}

/* 水印管理样式 */
.watermark-content {
  padding: 10px 0;
}

.watermark-status {
  margin-top: 10px;
}

.status-card {
  text-align: center;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
}

.status-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.status-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}
</style>
