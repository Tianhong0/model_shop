<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="query.title" clearable placeholder="标题关键词" style="width: 220px" />
          <el-select v-model="query.noticeType" clearable placeholder="类型" style="width: 140px">
            <el-option label="系统通知" :value="1" />
            <el-option label="活动促销" :value="2" />
            <el-option label="维护公告" :value="3" />
          </el-select>
          <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px">
            <el-option label="已发布" :value="1" />
            <el-option label="草稿" :value="0" />
          </el-select>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-space>
        <el-button type="primary" @click="openCreate">新增公告</el-button>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="id" label="ID" width="180" />
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column label="内容" min-width="260" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.content }}</template>
        </el-table-column>
        <el-table-column prop="noticeType" label="类型" width="120">
          <template #default="scope">{{ typeLabel(scope.row.noticeType) }}</template>
        </el-table-column>
        <el-table-column prop="level" label="级别" width="120" />
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑公告' : '新增公告'" width="720px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="200" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="6" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.noticeType" style="width: 100%">
            <el-option label="系统通知" :value="1" />
            <el-option label="活动促销" :value="2" />
            <el-option label="维护公告" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="form.level" style="width: 100%">
            <el-option label="NORMAL" value="NORMAL" />
            <el-option label="IMPORTANT" value="IMPORTANT" />
            <el-option label="URGENT" value="URGENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="已发布" :value="1" />
            <el-option label="草稿" :value="0" />
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
import {
  createNotice,
  deleteNotice,
  getNoticeAdminList,
  updateNotice,
  updateNoticeStatus
} from '../../api/operation'

const loading = ref(false)
const saving = ref(false)
const total = ref(0)
const records = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  noticeType: null,
  status: null
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive({
  id: null,
  title: '',
  content: '',
  noticeType: 1,
  level: 'NORMAL',
  status: 1
})

const typeLabel = (type) => {
  if (type === 1) return '系统通知'
  if (type === 2) return '活动促销'
  if (type === 3) return '维护公告'
  return `未知(${type})`
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getNoticeAdminList({ ...query })
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
  query.noticeType = null
  query.status = null
  fetchList()
}

const resetForm = () => {
  form.id = null
  form.title = ''
  form.content = ''
  form.noticeType = 1
  form.level = 'NORMAL'
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
    content: row.content,
    noticeType: row.noticeType ?? 1,
    level: row.level || 'NORMAL',
    status: row.status ?? 1
  })
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.title || !form.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.title,
      content: form.content,
      noticeType: form.noticeType,
      level: form.level,
      status: form.status
    }
    if (isEdit.value) {
      await updateNotice({ id: form.id, ...payload })
      ElMessage.success('更新成功')
    } else {
      await createNotice(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    saving.value = false
  }
}

const onStatusChange = async (row, value) => {
  await updateNoticeStatus({ id: row.id, status: value ? 1 : 0 })
  ElMessage.success('状态已更新')
  fetchList()
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确认删除公告【${row.title}】吗？`, '提示', { type: 'warning' })
  await deleteNotice(row.id)
  ElMessage.success('删除成功')
  fetchList()
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
