<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="query.keyword" clearable placeholder="搜索分类名称" style="width: 240px" />
          <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 140px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-space>
        <el-button type="primary" @click="openCreate">新增分类</el-button>
      </div>

      <el-table v-loading="loading" :data="records" stripe border highlight-current-row style="width: 100%">
        <el-table-column prop="id" label="分类ID" width="120" />
        <el-table-column prop="name" label="分类名称" min-width="220" />
        <el-table-column prop="sortNo" label="排序" width="120" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-switch
              :model-value="scope.row.status === 1"
              active-text="启用"
              inactive-text="禁用"
              inline-prompt
              @change="(val) => onStatusChange(scope.row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
            <el-divider direction="vertical" />
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="分类名称" required>
          <el-input v-model="form.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序" required>
          <el-input-number v-model="form.sortNo" :min="0" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
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
  createPostCategory,
  deletePostCategory,
  getAdminCategoryPage,
  updatePostCategory
} from '../../api/community'

const loading = ref(false)
const saving = ref(false)
const records = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: null
})

const dialogVisible = ref(false)
const isEdit = ref(false)

const form = reactive({
  id: null,
  name: '',
  sortNo: 0,
  status: 1
})

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword,
      status: query.status
    }
    Object.keys(params).forEach((key) => {
      if (params[key] === '' || params[key] === null || params[key] === undefined) {
        delete params[key]
      }
    })
    const res = await getAdminCategoryPage(params)
    records.value = res?.records || []
    total.value = Number(res?.total || 0)
    query.pageNum = Number(res?.pageNum || query.pageNum)
    query.pageSize = Number(res?.pageSize || query.pageSize)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.pageNum = 1
  fetchList()
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
  query.keyword = ''
  query.status = null
  fetchList()
}

const resetForm = () => {
  form.id = null
  form.name = ''
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
    name: row.name,
    sortNo: row.sortNo,
    status: row.status
  })
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.name) {
    ElMessage.warning('请输入分类名称')
    return
  }

  saving.value = true
  try {
    if (isEdit.value) {
      await updatePostCategory({
        id: form.id,
        name: form.name,
        sortNo: form.sortNo,
        status: form.status
      })
      ElMessage.success('更新成功')
    } else {
      await createPostCategory({
        name: form.name,
        sortNo: form.sortNo,
        status: form.status
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    saving.value = false
  }
}

const onStatusChange = async (row, value) => {
  await updatePostCategory({
    id: row.id,
    status: value ? 1 : 0
  })
  ElMessage.success('状态已更新')
  fetchList()
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确认删除分类【${row.name}】吗？`, '提示', { type: 'warning' })
  await deletePostCategory(row.id)
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
  margin-top: 24px;
  overflow-x: auto;
}
</style>
