<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="query.keyword" placeholder="名称/地址" style="width: 220px" @keyup.enter="handleSearch" />
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px" @change="handleSearch">
            <el-option label="空闲" :value="0" />
            <el-option label="忙碌" :value="1" />
            <el-option label="离线" :value="2" />
            <el-option label="故障" :value="3" />
          </el-select>
          <el-button @click="handleSearch">查询</el-button>
        </el-space>
        <el-button type="primary" @click="openCreate">新增打印机</el-button>
      </div>

      <el-table :data="rows" border stripe v-loading="loading">
        <el-table-column prop="printerCode" label="编码" width="220" />
        <el-table-column prop="printerName" label="名称" min-width="180" />
        <el-table-column prop="baseUrl" label="地址" min-width="240" />
        <el-table-column prop="statusDesc" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ scope.row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column label="操作" width="180" fixed="right">
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
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          :page-count="Math.max(1, Math.ceil((total || 0) / query.pageSize))"
          :hide-on-single-page="false"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑打印机' : '新增打印机'" width="520px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="打印机名称">
          <el-input v-model="form.printerName" />
        </el-form-item>
        <el-form-item label="IP地址">
          <el-input v-model="form.ip" placeholder="例如 192.168.1.80" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        <el-form-item label="HTTPS">
          <el-switch v-model="form.https" />
        </el-form-item>
        <el-form-item label="Header Key">
          <el-input v-model="form.authHeaderKey" placeholder="默认 X-Api-Key" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.authHeaderValue" show-password placeholder="可选：不填走全局配置" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="空闲" :value="0" />
            <el-option label="忙碌" :value="1" />
            <el-option label="离线" :value="2" />
            <el-option label="故障" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createPrinter, deletePrinter, getPrinterList, updatePrinter } from '../../api/print'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const editingId = ref(null)

const query = reactive({
  keyword: '',
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

const form = reactive({
  printerName: '',
  ip: '',
  port: 5000,
  https: false,
  authHeaderKey: 'X-Api-Key',
  authHeaderValue: '',
  status: 0,
  sort: 0
})

const statusType = (status) => {
  if (status === 0) return 'success'
  if (status === 1) return 'warning'
  if (status === 2) return 'info'
  return 'danger'
}

const parseAddress = (baseUrl) => {
  try {
    const url = new URL(baseUrl)
    return {
      ip: url.hostname,
      port: Number(url.port || (url.protocol === 'https:' ? 443 : 80)),
      https: url.protocol === 'https:'
    }
  } catch (_) {
    return { ip: '', port: 5000, https: false }
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      keyword: query.keyword,
      status: query.status,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    }
    Object.keys(params).forEach((key) => {
      if (params[key] === '' || params[key] === null || params[key] === undefined) {
        delete params[key]
      }
    })
    const response = await getPrinterList(params)
    rows.value = response?.records || []
    total.value = response?.total || 0
    query.pageNum = response?.pageNum || query.pageNum
    query.pageSize = response?.pageSize || query.pageSize
  } finally {
    loading.value = false
  }
}

const handlePageChange = (pageNum) => {
  query.pageNum = pageNum
  loadData()
}

const handleSearch = () => {
  query.pageNum = 1
  loadData()
}

const handleSizeChange = (pageSize) => {
  query.pageSize = pageSize
  query.pageNum = 1
  loadData()
}

const resetForm = () => {
  form.printerName = ''
  form.ip = ''
  form.port = 5000
  form.https = false
  form.authHeaderKey = 'X-Api-Key'
  form.authHeaderValue = ''
  form.status = 0
  form.sort = 0
}

const openCreate = () => {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  editingId.value = row.id
  const addr = parseAddress(row.baseUrl)
  form.printerName = row.printerName
  form.ip = addr.ip
  form.port = addr.port
  form.https = addr.https
  form.authHeaderKey = row.authHeaderKey || 'X-Api-Key'
  form.authHeaderValue = ''
  form.status = row.status
  form.sort = row.sort || 0
  dialogVisible.value = true
}

const submit = async () => {
  const payload = {
    printerName: form.printerName,
    ip: form.ip,
    port: form.port,
    https: form.https,
    authHeaderKey: form.authHeaderKey,
    authHeaderValue: form.authHeaderValue,
    status: form.status,
    sort: form.sort
  }
  if (editingId.value) {
    await updatePrinter({ id: editingId.value, ...payload })
    ElMessage.success('打印机已更新')
  } else {
    await createPrinter(payload)
    ElMessage.success('打印机已新增')
  }
  dialogVisible.value = false
  await loadData()
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确认删除打印机 ${row.printerName} 吗？`, '提示', {
    type: 'warning'
  })
  await deletePrinter(row.id)
  ElMessage.success('已删除')
  await loadData()
}

onMounted(loadData)
</script>

<style scoped>
.table-card {
  background: #fff;
  padding: 20px;
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
