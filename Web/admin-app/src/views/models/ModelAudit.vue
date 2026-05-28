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
          <el-select v-model="queryParams.sourceType" placeholder="来源类型" clearable style="width: 120px" @change="handleSearch">
            <el-option label="官方" :value="1" />
            <el-option label="设计者作品" :value="2" />
          </el-select>
          <el-select v-model="queryParams.status" placeholder="所有状态" clearable style="width: 130px" @change="handleSearch">
            <el-option label="待审核" :value="0" />
            <el-option label="已上架" :value="1" />
            <el-option label="已下架" :value="2" />
          </el-select>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-badge :value="pendingCount" :max="999" class="item">
            <el-tag type="warning">待审核</el-tag>
          </el-badge>
        </el-space>
      </div>

      <div class="table-wrapper">
        <el-table :data="modelData" stripe border highlight-current-row style="width: 100%" v-loading="loading">
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
          <el-table-column label="来源" width="110">
            <template #default="scope">
              <el-tag :type="scope.row.sourceType === 1 ? 'primary' : 'success'" effect="light" round>
                {{ scope.row.sourceTypeDesc || '-' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="designerName" label="设计者" width="150" />
          <el-table-column prop="categoryName" label="分类" width="120">
            <template #default="scope">
              {{ scope.row.categoryName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="basePrice" label="基础价格" width="100">
            <template #default="scope">
              ￥{{ Number(scope.row.basePrice).toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row.status)" effect="light" round>
                {{ statusLabel(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="handleAudit(scope.row)">
                {{ scope.row.status === 0 ? '审核' : '复审/修改' }}
              </el-button>
              <el-divider direction="vertical" />
              <el-button link type="info" @click="handleViewDetail(scope.row)">详情</el-button>
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

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditVisible" :title="dialogTitle" width="800px" top="3vh">
      <div v-if="auditModelData" class="audit-content">
        <el-descriptions :column="2" border style="margin-bottom: 20px">
          <el-descriptions-item label="模型ID">{{ auditModelData.id }}</el-descriptions-item>
          <el-descriptions-item label="模型名称">{{ auditModelData.modelName }}</el-descriptions-item>
          <el-descriptions-item label="设计者">{{ auditModelData.designerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ auditModelData.categoryName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="基础价格">￥{{ Number(auditModelData.basePrice || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="原始体积">{{ auditModelData.baseVolume }} mm³</el-descriptions-item>
          <el-descriptions-item label="三维尺寸" :span="2">{{ auditModelData.baseSize || '-' }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ auditModelData.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="授权说明" :span="2">{{ auditModelData.licenseType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前状态" :span="2">
            <el-tag :type="statusTagType(auditModelData.status)" effect="light" round>
              {{ statusLabel(auditModelData.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="文件路径" :span="2">
            <div v-if="auditModelData.filePath">
              <span>{{ getFileName(auditModelData.filePath) }}</span>
              <el-button link type="primary" @click="handlePreview3D(auditModelData.filePath)">预览3D</el-button>
            </div>
            <span v-else>-</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 图片展示 -->
        <div v-if="auditModelData.images && auditModelData.images.length > 0" class="audit-images">
          <div class="section-title">模型图片</div>
          <div class="image-list">
            <el-image
              v-for="img in auditModelData.images"
              :key="img.id"
              :src="img.imageUrl"
              fit="cover"
              style="width:120px; height:120px; border-radius:8px; margin-right:10px"
              :preview-src-list="auditModelData.images.map(i => i.imageUrl)"
              preview-teleported
            />
          </div>
        </div>

        <!-- 材质列表 -->
        <div v-if="auditModelData.materials && auditModelData.materials.length > 0" class="audit-materials">
          <div class="section-title">可用材质</div>
          <div class="material-list">
            <el-tag
              v-for="m in auditModelData.materials"
              :key="m.id"
              :type="m.isEco ? 'success' : 'info'"
              size="small"
              style="margin-right:8px; margin-bottom:4px"
            >
              {{ m.name }} (￥{{ m.price }}/克)
            </el-tag>
          </div>
        </div>

        <!-- 审核表单 -->
        <div class="section-title">审核操作</div>
        <el-form :model="auditForm" label-width="140px" style="margin-top: 16px">
          <el-form-item label="分润比例(%)">
            <el-input-number v-model="auditForm.profitShareRatio" :min="0" :max="100" :step="1" style="width: 150px" />
            <span style="margin-left: 8px; color: #999; font-size: 12px">设计师获得的订单金额百分比，0表示不分润</span>
          </el-form-item>
          <el-form-item label="调整基础价格">
            <el-input-number v-model="auditForm.basePrice" :precision="2" :step="0.01" :min="0" style="width: 200px" />
          </el-form-item>
          <el-form-item label="调整原始体积">
            <el-input-number v-model="auditForm.baseVolume" :min="0" :precision="0" style="width: 200px" />
          </el-form-item>
          <el-form-item label="调整三维尺寸">
            <el-input v-model="auditForm.baseSize" placeholder="如: 100x80x60" style="width: 200px" />
          </el-form-item>
          <el-form-item label="审核备注">
            <el-input v-model="auditForm.note" type="textarea" :rows="3" placeholder="请输入审核备注（驳回时必填）" />
          </el-form-item>
        </el-form>

        <!-- 审核历史 -->
        <div v-if="auditRecords.length > 0" class="audit-history">
          <div class="section-title">审核历史</div>
          <el-timeline>
            <el-timeline-item
              v-for="r in auditRecords"
              :key="r.id"
              :timestamp="r.createTime"
              :type="r.action === 1 ? 'success' : 'danger'"
            >
              <div>
                <el-tag :type="r.action === 1 ? 'success' : 'danger'" size="small" style="margin-right: 8px">
                  {{ r.actionDesc }}
                </el-tag>
                <span>{{ r.auditByName }}</span>
              </div>
              <div v-if="r.profitShareRatio" style="font-size: 12px; color: #666; margin-top: 4px">
                分润比例: {{ r.profitShareRatio }}%
              </div>
              <div v-if="r.note" style="font-size: 12px; color: #999; margin-top: 2px">
                {{ r.note }}
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>

      <template #footer>
        <el-button @click="auditVisible = false">关闭</el-button>
        <el-button type="danger" @click="handleReject" :loading="submitting">
          {{ isPending ? '驳回' : '驳回并下架' }}
        </el-button>
        <el-button type="success" @click="handleApprove" :loading="submitting">
          {{ isPending ? '通过审核' : '保存并上架' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 模型详情弹窗（复用） -->
    <el-dialog v-model="detailVisible" title="模型详情" width="750px">
      <el-descriptions :column="2" border v-if="detailModel">
        <el-descriptions-item label="模型ID" :span="2">{{ detailModel.id }}</el-descriptions-item>
        <el-descriptions-item label="模型名称">{{ detailModel.modelName }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ detailModel.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设计者">{{ detailModel.designerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ detailModel.categoryName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="基础价格">￥{{ Number(detailModel.basePrice || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="原始体积">{{ detailModel.baseVolume }} mm³</el-descriptions-item>
        <el-descriptions-item label="三维尺寸" :span="2">{{ detailModel.baseSize || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源" :span="2">
          <el-tag :type="detailModel.sourceType === 1 ? 'primary' : 'success'" effect="light" round>
            {{ detailModel.sourceTypeDesc || '-' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 3D预览弹窗 -->
    <el-dialog v-model="preview3DVisible" title="3D模型预览" width="900px" top="5vh" destroy-on-close>
      <ThreePreview
        v-if="preview3DVisible"
        :model-url="preview3DUrl"
        :model-type="preview3DType"
        @loaded="() => {}"
        @error="() => {}"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ThreePreview from '../../components/ThreePreview.vue'
import request from '../../utils/request'
import {
  getModelList,
  getModelDetail,
  getModelCategories,
  getCategoryTreeRecursive,
  auditModel,
  getModelAuditRecords
} from '../../api/model'

const loading = ref(false)
const submitting = ref(false)
const modelData = ref([])
const categoryOptions = ref([])
const total = ref(0)
const pendingCount = ref(0)
const auditVisible = ref(false)
const detailVisible = ref(false)
const preview3DVisible = ref(false)
const preview3DUrl = ref('')
const preview3DType = ref('glb')
const auditModelData = ref(null)
const detailModel = ref(null)
const auditRecords = ref([])

const queryParams = reactive({
  modelName: '',
  categoryId: null,
  sourceType: null,
  status: 0,   // 默认显示待审核，可在筛选框中改为"所有状态"或其他状态
  orderBy: 'create_time',
  pageNum: 1,
  pageSize: 10
})

const auditForm = reactive({
  profitShareRatio: 30,
  basePrice: null,
  baseVolume: null,
  baseSize: '',
  note: ''
})

// 是否为首次待审核（决定弹窗标题与按钮文案）
const isPending = computed(() => auditModelData.value?.status === 0)
const dialogTitle = computed(() => isPending.value ? '审核模型' : '复审/修改模型')

const statusLabel = (status) => {
  switch (status) {
    case 0: return '待审核'
    case 1: return '已上架'
    case 2: return '已下架'
    default: return '-'
  }
}
const statusTagType = (status) => {
  switch (status) {
    case 0: return 'warning'
    case 1: return 'success'
    case 2: return 'danger'
    default: return 'info'
  }
}

const getFileName = (filePath) => {
  if (!filePath) return ''
  return filePath.split('/').pop().split('\\').pop()
}

const fetchCategories = async () => {
  try {
    const response = await getCategoryTreeRecursive()
    let categories = []
    if (Array.isArray(response)) {
      categories = response
    } else if (response && response.records) {
      categories = response.records
    }
    if (categories.length > 0 && categories[0]?.children) {
      const result = []
      const flatten = (items) => {
        for (const item of items) {
          result.push({ id: item.id, categoryName: item.categoryName })
          if (item.children && item.children.length > 0) flatten(item.children)
        }
      }
      flatten(categories)
      categoryOptions.value = result
    } else {
      categoryOptions.value = categories.map(c => ({ id: c.id, categoryName: c.categoryName }))
    }
  } catch (e) {
    console.error('获取分类失败:', e)
  }
}

const fetchModelList = async () => {
  loading.value = true
  try {
    const params = { ...queryParams }
    Object.keys(params).forEach(key => {
      if (params[key] === '' || params[key] === null) delete params[key]
    })
    const response = await getModelList(params)
    modelData.value = response?.records || []
    total.value = response?.total || 0

    // 单独查询一次待审核总数
    const countResp = await getModelList({ status: 0, pageNum: 1, pageSize: 1 })
    pendingCount.value = countResp?.total || 0
  } catch (e) {
    console.error('获取模型列表失败:', e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchModelList()
}

const handlePageChange = (page) => {
  queryParams.pageNum = page
  fetchModelList()
}

const handleSizeChange = (size) => {
  queryParams.pageSize = size
  queryParams.pageNum = 1
  fetchModelList()
}

const handleAudit = async (row) => {
  try {
    const detail = await getModelDetail(row.id)
    auditModelData.value = detail
    auditForm.profitShareRatio = detail.profitShareRatio || 30
    auditForm.basePrice = detail.basePrice
    auditForm.baseVolume = detail.baseVolume
    auditForm.baseSize = detail.baseSize || ''
    auditForm.note = ''

    // 加载审核记录
    try {
      const recordsResp = await getModelAuditRecords(row.id, { pageNum: 1, pageSize: 20 })
      auditRecords.value = recordsResp?.records || []
    } catch {
      auditRecords.value = []
    }

    auditVisible.value = true
  } catch (e) {
    console.error('获取模型详情失败:', e)
    ElMessage.error('获取模型详情失败')
  }
}

const handleViewDetail = async (row) => {
  try {
    const detail = await getModelDetail(row.id)
    detailModel.value = detail
    detailVisible.value = true
  } catch (e) {
    detailModel.value = row
    detailVisible.value = true
  }
}

const handleApprove = async () => {
  const tipMsg = isPending.value
    ? '确认通过该模型审核？审核通过后模型将立即上架。'
    : '确认保存修改并上架该模型？将覆盖现有配置（分润比例/基础价格/体积/尺寸）。'
  try {
    await ElMessageBox.confirm(tipMsg, '确认操作', { type: 'warning' })
  } catch {
    return
  }

  submitting.value = true
  try {
    await auditModel({
      modelId: auditModelData.value.id,
      action: 1,
      profitShareRatio: auditForm.profitShareRatio,
      basePrice: auditForm.basePrice,
      baseVolume: auditForm.baseVolume,
      baseSize: auditForm.baseSize || null,
      note: auditForm.note || null
    })
    ElMessage.success(isPending.value ? '审核通过，模型已上架' : '修改已保存，模型保持上架')
    auditVisible.value = false
    fetchModelList()
  } catch (e) {
    console.error('审核失败:', e)
    ElMessage.error('审核失败')
  } finally {
    submitting.value = false
  }
}

const handleReject = async () => {
  if (!auditForm.note || !auditForm.note.trim()) {
    ElMessage.warning('驳回时必须填写审核备注')
    return
  }
  const tipMsg = isPending.value ? '确认驳回该模型？' : '确认驳回并下架该模型？'
  try {
    await ElMessageBox.confirm(tipMsg, '确认操作', { type: 'warning' })
  } catch {
    return
  }

  submitting.value = true
  try {
    await auditModel({
      modelId: auditModelData.value.id,
      action: 2,
      profitShareRatio: auditForm.profitShareRatio,
      note: auditForm.note
    })
    ElMessage.success(isPending.value ? '模型已驳回' : '模型已下架')
    auditVisible.value = false
    fetchModelList()
  } catch (e) {
    console.error('驳回失败:', e)
    ElMessage.error('驳回失败')
  } finally {
    submitting.value = false
  }
}

const handlePreview3D = (filePath) => {
  let modelUrl = filePath
  if (!filePath.startsWith('http://') && !filePath.startsWith('https://')) {
    modelUrl = (request.defaults.baseURL || 'http://127.0.0.1:9999') + (filePath.startsWith('/') ? '' : '/') + filePath
  }
  preview3DUrl.value = modelUrl
  const ext = filePath.split('.').pop().toLowerCase()
  preview3DType.value = ext
  preview3DVisible.value = true
}

onMounted(() => {
  fetchCategories()
  fetchModelList()
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

.table-wrapper {
  overflow-x: auto;
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

.audit-content .section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 20px 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-light);
}

.audit-images .image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.audit-materials .material-list {
  display: flex;
  flex-wrap: wrap;
}

.audit-history {
  margin-top: 20px;
}
</style>
