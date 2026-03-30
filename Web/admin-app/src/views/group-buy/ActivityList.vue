<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="query.activityName" clearable placeholder="活动名称" style="width: 220px" />
          <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px">
            <el-option label="禁用" :value="0" />
            <el-option label="启用" :value="1" />
            <el-option label="已结束" :value="2" />
          </el-select>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-space>
        <el-button type="primary" @click="openCreate">新增活动</el-button>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="id" label="ID" width="180" />
        <el-table-column label="封面" width="100">
          <template #default="scope">
            <el-image :src="scope.row.coverImage || scope.row.modelImage" style="width: 80px; height: 80px; border-radius: 6px" fit="cover" />
          </template>
        </el-table-column>
        <el-table-column prop="activityName" label="活动名称" min-width="180" />
        <el-table-column prop="modelName" label="关联模型" width="150" />
        <el-table-column label="价格" width="150">
          <template #default="scope">
            <span style="color: #f56c6c; font-weight: bold;">¥{{ scope.row.groupPrice }}</span>
            <span style="color: #999; text-decoration: line-through; margin-left: 4px; font-size: 12px;">¥{{ scope.row.originalPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column label="成团人数" width="100">
          <template #default="scope">
            {{ scope.row.minPeople }}人团
          </template>
        </el-table-column>
        <el-table-column label="销量/库存" width="100">
          <template #default="scope">
            {{ scope.row.soldCount || 0 }}/{{ scope.row.totalStock || '不限' }}
          </template>
        </el-table-column>
        <el-table-column label="活动时间" width="200">
          <template #default="scope">
            {{ formatTime(scope.row.startTime) }} ~ {{ formatTime(scope.row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
            <el-button link :type="scope.row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(scope.row)">
              {{ scope.row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          :page-count="Math.max(1, Math.ceil((total || 0) / query.pageSize))"
          :hide-on-single-page="false"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 创建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑活动' : '新增活动'" width="800px">
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="活动名称" prop="activityName">
              <el-input v-model="form.activityName" maxlength="100" placeholder="请输入活动名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="关联模型" prop="modelId">
              <el-select v-model="form.modelId" filterable placeholder="请选择模型" style="width: 100%" @change="onModelChange">
                <el-option v-for="model in modelList" :key="model.id" :label="model.modelName" :value="model.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原价" prop="originalPrice">
              <el-input-number v-model="form.originalPrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="拼团价" prop="groupPrice">
              <el-input-number v-model="form.groupPrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最小人数" prop="minPeople">
              <el-input-number v-model="form.minPeople" :min="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大人数">
              <el-input-number v-model="form.maxPeople" :min="0" placeholder="留空不限" style="width: 100%" />
              <span style="color: #999; font-size: 12px; margin-left: 8px;">留空不限制人数</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="折扣类型" prop="discountType">
              <el-select v-model="form.discountType" style="width: 100%">
                <el-option label="固定折扣" :value="1" />
                <el-option label="阶梯折扣" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="折扣值" prop="discountValue">
              <el-input-number v-model="form.discountValue" :min="1" :max="100" style="width: 100%" />
              <span style="color: #999; font-size: 12px; margin-left: 8px;">如90表示9折</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="拼团限时">
              <el-input-number v-model="form.timeoutHours" :min="1" :max="72" style="width: 100%" />
              <span style="color: #999; font-size: 12px; margin-left: 8px;">小时</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="总库存">
              <el-input-number v-model="form.totalStock" :min="0" placeholder="不填则不限" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面图">
              <div style="display: flex; gap: 10px; width: 100%; align-items: center;">
                <el-input v-model="form.coverImage" placeholder="请上传或粘贴图片URL" />
                <el-button :loading="uploading" @click.prevent="triggerUpload">上传</el-button>
                <input ref="fileRef" type="file" accept="image/*" style="display:none" @change="uploadImage" />
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="活动描述">
              <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入活动描述" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadFileWithProgress } from '../../api/model'
import { getModelList } from '../../api/model'
import {
  getActivityListApi,
  getActivityDetailApi,
  createActivityApi,
  updateActivityApi,
  updateActivityStatusApi
} from '../../api/groupBuy'

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const total = ref(0)
const records = ref([])
const modelList = ref([])
const fileRef = ref(null)
const formRef = ref(null)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  activityName: '',
  status: null
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive({
  id: null,
  activityName: '',
  modelId: null,
  originalPrice: 0,
  groupPrice: 0,
  minPeople: 2,
  maxPeople: null,
  discountType: 1,
  discountValue: 90,
  startTime: null,
  endTime: null,
  timeoutHours: 24,
  totalStock: null,
  coverImage: '',
  description: ''
})

const rules = {
  activityName: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  modelId: [{ required: true, message: '请选择关联模型', trigger: 'change' }],
  originalPrice: [{ required: true, message: '请输入原价', trigger: 'blur' }],
  groupPrice: [{ required: true, message: '请输入拼团价', trigger: 'blur' }],
  minPeople: [{ required: true, message: '请输入最小成团人数', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

const statusText = (status) => {
  const map = { 0: '禁用', 1: '启用', 2: '已结束' }
  return map[status] || '未知'
}

const statusType = (status) => {
  const map = { 0: 'info', 1: 'success', 2: 'warning' }
  return map[status] || 'info'
}

const formatTime = (time) => {
  if (!time) return ''
  return time.substring(0, 10)
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getActivityListApi({ ...query })
    records.value = res.records || []
    total.value = Number(res.total || 0)
    query.pageNum = Number(res.pageNum || query.pageNum)
    query.pageSize = Number(res.pageSize || query.pageSize)
  } finally {
    loading.value = false
  }
}

const fetchModelList = async () => {
  try {
    const res = await getModelList({ pageNum: 1, pageSize: 1000 })
    modelList.value = res.records || []
  } catch (e) {
    console.error('获取模型列表失败', e)
  }
}

const handlePageChange = (pageNum) => {
  query.pageNum = pageNum
  fetchList()
}

const handleSizeChange = (pageSize) => {
  query.pageSize = pageSize
  query.pageNum = 1
  fetchList()
}

const resetQuery = () => {
  query.pageNum = 1
  query.activityName = ''
  query.status = null
  fetchList()
}

const resetForm = () => {
  form.id = null
  form.activityName = ''
  form.modelId = null
  form.originalPrice = 0
  form.groupPrice = 0
  form.minPeople = 2
  form.maxPeople = null
  form.discountType = 1
  form.discountValue = 90
  form.startTime = null
  form.endTime = null
  form.timeoutHours = 24
  form.totalStock = null
  form.coverImage = ''
  form.description = ''
}

const openCreate = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const openEdit = async (row) => {
  isEdit.value = true
  try {
    const res = await getActivityDetailApi(row.id)
    form.id = res.id
    form.activityName = res.activityName
    form.modelId = res.modelId
    form.originalPrice = res.originalPrice
    form.groupPrice = res.groupPrice
    form.minPeople = res.minPeople
    form.maxPeople = res.maxPeople || null  // 0转为null
    form.discountType = res.discountType
    form.discountValue = res.discountValue
    form.startTime = res.startTime
    form.endTime = res.endTime
    form.timeoutHours = res.timeoutHours
    form.totalStock = res.totalStock || null  // 0转为null
    form.coverImage = res.coverImage || ''
    form.description = res.description || ''
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取活动详情失败')
  }
}

const onModelChange = (modelId) => {
  const model = modelList.value.find(m => m.id === modelId)
  if (model) {
    form.originalPrice = model.basePrice || 0
    form.groupPrice = model.basePrice || 0
    form.coverImage = model.filePath || ''
  }
}

const submit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  if (form.groupPrice > form.originalPrice) {
    ElMessage.warning('拼团价不能高于原价')
    return
  }

  saving.value = true
  try {
    const payload = {
      activityName: form.activityName,
      modelId: form.modelId,
      originalPrice: form.originalPrice,
      groupPrice: form.groupPrice,
      minPeople: form.minPeople,
      maxPeople: form.maxPeople || null,  // 0或空转为null表示不限制
      discountType: form.discountType,
      discountValue: form.discountValue,
      startTime: form.startTime,
      endTime: form.endTime,
      timeoutHours: form.timeoutHours,
      totalStock: form.totalStock || null,  // 0或空转为null表示不限制
      coverImage: form.coverImage,
      description: form.description
    }

    if (isEdit.value) {
      await updateActivityApi({ id: form.id, ...payload })
      ElMessage.success('更新成功')
    } else {
      await createActivityApi(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'

  await ElMessageBox.confirm(`确认${action}活动【${row.activityName}】吗？`, '提示', { type: 'warning' })
  await updateActivityStatusApi(row.id, newStatus)
  ElMessage.success(`${action}成功`)
  fetchList()
}

const triggerUpload = () => {
  fileRef.value?.click()
}

const resolveUploadUrl = (response) => {
  if (!response) return ''
  if (typeof response === 'string') return response
  if (typeof response === 'object') {
    if (typeof response.data === 'string') return response.data
    if (typeof response.url === 'string') return response.url
    if (typeof response.message === 'string') return response.message
  }
  return ''
}

const uploadImage = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const response = await uploadFileWithProgress(file, 'group-buy', () => {})
    const uploadUrl = resolveUploadUrl(response)
    if (!uploadUrl) {
      throw new Error('未获取到图片地址')
    }
    form.coverImage = uploadUrl
    ElMessage.success('图片上传成功')
  } catch (error) {
    console.error('图片上传失败:', error)
    ElMessage.error('上传失败，请重试')
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

onMounted(() => {
  fetchList()
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
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-light);
  flex-wrap: wrap;
  gap: 16px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border-light);
}
</style>
