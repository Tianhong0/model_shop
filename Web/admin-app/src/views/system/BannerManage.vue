<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="query.title" clearable placeholder="标题关键词" style="width: 220px" />
          <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-space>
        <el-button type="primary" @click="openCreate">新增轮播</el-button>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="id" label="ID" width="180" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column label="图片" width="140">
          <template #default="scope">
            <el-image :src="scope.row.imageUrl" style="width: 100px; height: 56px; border-radius: 6px" fit="cover" />
          </template>
        </el-table-column>
        <el-table-column prop="linkType" label="跳转类型" width="120" />
        <el-table-column prop="sortNo" label="排序" width="100" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-switch
              :model-value="scope.row.status === 1"
              @change="(val) => onStatusChange(scope.row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
            <el-button link type="danger" @click="remove(scope.row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑轮播' : '新增轮播'" width="680px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="100" />
        </el-form-item>
        <el-form-item label="图片">
          <div style="display: flex; gap: 10px; width: 100%; align-items: center;">
            <el-input v-model="form.imageUrl" placeholder="请上传或粘贴图片URL" />
            <el-button :loading="uploading" @click.prevent="triggerUpload">上传</el-button>
            <input ref="fileRef" type="file" accept="image/*" style="display:none" @change="uploadImage" />
          </div>
        </el-form-item>
        <el-form-item label="跳转类型">
          <el-select v-model="form.linkType" style="width: 100%">
            <el-option label="无跳转" :value="0" />
            <el-option label="模型详情" :value="1" />
            <el-option label="活动页" :value="2" />
            <el-option label="外部链接" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="跳转值">
          <el-input v-model="form.linkValue" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortNo" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
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
import {
  createBanner,
  deleteBanner,
  getBannerAdminList,
  updateBanner,
  updateBannerStatus
} from '../../api/operation'

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const total = ref(0)
const records = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  status: null
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const fileRef = ref(null)
const form = reactive({
  id: null,
  title: '',
  imageUrl: '',
  linkType: 0,
  linkValue: '',
  sortNo: 0,
  status: 1
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getBannerAdminList({ ...query })
    records.value = res.records || []
    total.value = Number(res.total || 0)
    query.pageNum = Number(res.pageNum || query.pageNum)
    query.pageSize = Number(res.pageSize || query.pageSize)
  } finally {
    loading.value = false
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
  query.pageSize = 10
  query.title = ''
  query.status = null
  fetchList()
}

const resetForm = () => {
  form.id = null
  form.title = ''
  form.imageUrl = ''
  form.linkType = 0
  form.linkValue = ''
  form.sortNo = 0
  form.status = 1
}

const openCreate = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    title: row.title,
    imageUrl: row.imageUrl,
    linkType: row.linkType ?? 0,
    linkValue: row.linkValue ?? '',
    sortNo: row.sortNo ?? 0,
    status: row.status ?? 1
  })
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.title || !form.imageUrl) {
    ElMessage.warning('请填写标题并上传图片')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.title,
      imageUrl: form.imageUrl,
      linkType: form.linkType,
      linkValue: form.linkValue,
      sortNo: form.sortNo,
      status: form.status
    }
    if (isEdit.value) {
      await updateBanner({ id: form.id, ...payload })
      ElMessage.success('更新成功')
    } else {
      await createBanner(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    saving.value = false
  }
}

const onStatusChange = async (row, value) => {
  await updateBannerStatus({ id: row.id, status: value ? 1 : 0 })
  ElMessage.success('状态已更新')
  fetchList()
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确认删除轮播【${row.title}】吗？`, '提示', { type: 'warning' })
  await deleteBanner(row.id)
  ElMessage.success('删除成功')
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
    const response = await uploadFileWithProgress(file, 'banner', () => {})
    const uploadUrl = resolveUploadUrl(response)
    if (!uploadUrl) {
      throw new Error('未获取到图片地址')
    }
    form.imageUrl = uploadUrl
    ElMessage.success('图片上传成功')
  } catch (error) {
    console.error('轮播图上传失败:', error)
    ElMessage.error('上传失败，请重试')
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

onMounted(fetchList)
</script>

<style scoped>
.page-container { padding: 0; }
.table-card {
  background: #fff;
  padding: 24px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}
.header-actions {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}
.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  overflow-x: auto;
}
</style>
