<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="query.orderSn" placeholder="订单号" style="width: 220px" @keyup.enter="loadJobs(true)">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="query.status" placeholder="任务状态" clearable style="width: 140px">
            <el-option label="排队中" :value="0" />
            <el-option label="切片中" :value="1" />
            <el-option label="切片失败" :value="2" />
            <el-option label="待打印" :value="3" />
            <el-option label="打印中" :value="4" />
            <el-option label="已暂停" :value="5" />
            <el-option label="已完成" :value="6" />
            <el-option label="失败" :value="7" />
            <el-option label="已取消" :value="8" />
          </el-select>
          <el-select v-model="query.printerId" placeholder="打印机" clearable style="width: 180px">
            <el-option v-for="item in printers" :key="item.id" :label="item.printerName" :value="item.id" />
          </el-select>
          <el-button @click="loadJobs(true)">查询</el-button>
        </el-space>
        <el-space>
          <el-button @click="openAddPrinter">添加打印机</el-button>
          <el-button type="primary" icon="Plus" @click="handleAdd">手动排单</el-button>
        </el-space>
      </div>

      <el-table :data="jobs" stripe border highlight-current-row style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="任务ID" width="130" />
        <el-table-column prop="orderSn" label="订单号" width="170" />
        <el-table-column prop="printerName" label="打印机" width="140" />
        <el-table-column prop="modelName" label="模型" min-width="180" />
        <el-table-column prop="statusDesc" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ scope.row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="220">
          <template #default="scope">
            <el-progress :percentage="Number(scope.row.progress || 0)" :status="statusNum(scope.row) === 6 ? 'success' : ''" />
          </template>
        </el-table-column>
        <el-table-column label="温度(热端/热床)" width="190">
          <template #default="scope">
            <span>{{ tempText(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="剩余" width="110">
          <template #default="scope">
            <span>{{ formatLeft(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="scope">
            <el-button link @click="openEventDialog(scope.row)">日志</el-button>
            <el-button link type="primary" @click="handleRetry(scope.row)" :disabled="!canRetry(scope.row)">重试</el-button>
            <el-button link type="primary" @click="handleEdit(scope.row)" :disabled="!canAdjust(scope.row)">调整</el-button>
            <el-button link type="danger" @click="handleStop(scope.row)" :disabled="!canStop(scope.row)">终止</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50, 100]"
          :page-count="Math.max(1, Math.ceil((total || 0) / query.pageSize))"
          :hide-on-single-page="false"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 排产弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogType === 'add' ? '手动排产任务' : '调整打印任务'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="打印机">
          <el-select v-model="form.printerId" style="width: 100%">
            <el-option v-for="item in printers" :key="item.id" :label="item.printerName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单ID" v-if="dialogType === 'add'">
          <el-input v-model.number="form.orderId" placeholder="请输入订单ID" />
        </el-form-item>
        <el-form-item label="层高(mm)" v-if="dialogType === 'add'">
          <el-input-number v-model="form.layerHeight" :min="0.05" :step="0.05" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="填充(%)" v-if="dialogType === 'add'">
          <el-input-number v-model="form.fillDensity" :min="0" :max="100" :step="5" style="width: 100%" />
        </el-form-item>
        <el-form-item label="丝径(mm)" v-if="dialogType === 'add'">
          <el-input-number v-model="form.filamentDiameter" :min="1" :max="3" :step="0.05" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="1" :max="99" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="printerDialogVisible" title="添加打印机" width="480px">
      <el-form :model="printerForm" label-width="110px">
        <el-form-item label="打印机名称">
          <el-input v-model="printerForm.printerName" placeholder="例如：A区-01" />
        </el-form-item>
        <el-form-item label="IP地址">
          <el-input v-model="printerForm.ip" placeholder="例如：192.168.1.80" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input-number v-model="printerForm.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        <el-form-item label="HTTPS">
          <el-switch v-model="printerForm.https" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="printerForm.authHeaderValue" placeholder="可选：该打印机专属Key，不填则用后端全局配置" show-password />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="printerForm.sort" :min="0" :max="999" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="printerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPrinter">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="eventDialogVisible" :title="`任务日志 #${eventDialogJobId || ''}`" width="860px">
      <el-skeleton :loading="eventLoading" animated :rows="8">
        <el-empty v-if="jobEvents.length === 0" description="暂无事件日志" />
        <div v-else class="event-list-wrap">
          <div v-for="item in jobEvents" :key="item.id" class="event-item">
            <div class="event-head">
              <el-tag size="small">{{ item.eventType || '-' }}</el-tag>
              <span class="event-time">{{ item.createTime || '-' }}</span>
            </div>
            <div v-if="item.eventMessage" class="event-message">{{ item.eventMessage }}</div>
            <pre v-if="item.eventPayload" class="event-payload">{{ item.eventPayload }}</pre>
          </div>
        </div>
      </el-skeleton>
      <template #footer>
        <el-button @click="eventDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { adjustPrintJob, createPrinter, dispatchPrintJob, getPrintJobEvents, getPrintJobPage, getPrinterList, retryPrintJob, stopPrintJob } from '../../api/print'

const loading = ref(false)
const jobs = ref([])
const total = ref(0)
const printers = ref([])

let socket = null

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: undefined,
  printerId: undefined,
  orderSn: ''
})

const dialogVisible = ref(false)
const dialogType = ref('add')
const editingJobId = ref(null)
const printerDialogVisible = ref(false)
const eventDialogVisible = ref(false)
const eventDialogJobId = ref(null)
const eventLoading = ref(false)
const jobEvents = ref([])

const form = reactive({
  orderId: undefined,
  printerId: undefined,
  layerHeight: 0.2,
  fillDensity: 15,
  filamentDiameter: 1.75,
  priority: 1
})

const printerForm = reactive({
  printerName: '',
  ip: '',
  port: 5000,
  https: false,
  authHeaderKey: 'X-Api-Key',
  authHeaderValue: '',
  sort: 0
})

const statusType = (status) => {
  const value = Number(status)
  if (value === 6) return 'success'
  if (value === 7 || value === 2) return 'danger'
  if (value === 4 || value === 1) return 'warning'
  return 'info'
}

const statusNum = (row) => Number(row?.status)

const canAdjust = (row) => statusNum(row) === 0

const canStop = (row) => [0, 1, 3, 4, 5].includes(statusNum(row))

const canRetry = (row) => [2, 7, 8].includes(statusNum(row))

const tempText = (row) => {
  const tool = row.toolTempActual == null ? '-' : `${Number(row.toolTempActual).toFixed(1)}°C`
  const bed = row.bedTempActual == null ? '-' : `${Number(row.bedTempActual).toFixed(1)}°C`
  return `${tool} / ${bed}`
}

const formatLeft = (row) => {
  const status = statusNum(row)
  const raw = row?.estimatedSecondsLeft ?? row?.estimated_seconds_left ?? row?.printTimeLeft
  const seconds = raw == null ? null : Number(raw)
  if (seconds == null || Number.isNaN(seconds) || seconds < 0) return '-'
  if (seconds > 0 && seconds < 60) return '<1m'
  if (seconds === 0 && (status === 4 || status === 5 || status === 3)) return '计算中'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  if (h <= 0) return `${m}m`
  return `${h}h ${m}m`
}

const handleAdd = () => {
  editingJobId.value = null
  dialogType.value = 'add'
  form.orderId = undefined
  form.printerId = printers.value[0]?.id
  form.layerHeight = 0.2
  form.fillDensity = 15
  form.filamentDiameter = 1.75
  form.priority = 1
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editingJobId.value = row.id
  dialogType.value = 'edit'
  form.printerId = row.printerId
  form.priority = row.priority || 1
  dialogVisible.value = true
}

const handleStop = (row) => {
  ElMessageBox.confirm(`确定终止任务 ${row.id} 吗？`, '提示', {
    confirmButtonText: '确定终止',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    await stopPrintJob(row.id)
    ElMessage.success('任务已终止')
    loadJobs()
  })
}

const handleRetry = (row) => {
  ElMessageBox.confirm(`确定重试任务 ${row.id} 吗？`, '提示', {
    confirmButtonText: '确定重试',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await retryPrintJob(row.id)
    ElMessage.success('任务已重新排入队列')
    loadJobs()
  })
}

const openAddPrinter = () => {
  printerForm.printerName = ''
  printerForm.ip = ''
  printerForm.port = 5000
  printerForm.https = false
  printerForm.authHeaderKey = 'X-Api-Key'
  printerForm.authHeaderValue = ''
  printerForm.sort = 0
  printerDialogVisible.value = true
}

const loadPrinters = async () => {
  printers.value = await getPrinterList()
}

const loadJobs = async (reset = false) => {
  if (reset) query.pageNum = 1
  loading.value = true
  try {
    const data = await getPrintJobPage({ ...query })
    jobs.value = (data.records || []).map(normalizeJobRow)
    total.value = Number(data.total || 0)
    query.pageNum = Number(data.pageNum || query.pageNum)
    query.pageSize = Number(data.pageSize || query.pageSize)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (pageNum) => {
  query.pageNum = pageNum
  loadJobs()
}

const handleSizeChange = (pageSize) => {
  query.pageSize = pageSize
  query.pageNum = 1
  loadJobs()
}

const normalizeJobRow = (row) => {
  const status = row?.status == null ? null : Number(row.status)
  const leftRaw = row?.estimatedSecondsLeft ?? row?.estimated_seconds_left ?? row?.printTimeLeft
  const left = leftRaw == null ? null : Number(leftRaw)
  return {
    ...row,
    status: Number.isNaN(status) ? row?.status : status,
    estimatedSecondsLeft: Number.isNaN(left) ? null : left
  }
}

const submitForm = async () => {
  if (dialogType.value === 'add') {
    await dispatchPrintJob({
      orderId: form.orderId,
      printerId: form.printerId,
      layerHeight: form.layerHeight,
      fillDensity: form.fillDensity,
      filamentDiameter: form.filamentDiameter,
      priority: form.priority
    })
  } else {
    await adjustPrintJob({
      jobId: editingJobId.value,
      printerId: form.printerId,
      priority: form.priority
    })
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  loadJobs()
}

const submitPrinter = async () => {
  await createPrinter({ ...printerForm })
  ElMessage.success('打印机添加成功')
  printerDialogVisible.value = false
  await loadPrinters()
}

const openEventDialog = async (row) => {
  eventDialogJobId.value = row.id
  eventDialogVisible.value = true
  eventLoading.value = true
  try {
    jobEvents.value = await getPrintJobEvents(row.id, 120)
  } catch (e) {
    jobEvents.value = []
    ElMessage.error(e?.message || '日志加载失败')
  } finally {
    eventLoading.value = false
  }
}

const applySocketProgress = (payload) => {
  if (!payload || !payload.jobId) return
  const target = jobs.value.find(item => item.id === payload.jobId)
  if (!target) return
  const status = payload.status == null ? target.status : Number(payload.status)
  target.status = Number.isNaN(status) ? payload.status : status
  target.statusDesc = payload.statusDesc
  target.progress = payload.progress
  target.toolTempActual = payload.toolTempActual
  target.toolTempTarget = payload.toolTempTarget
  target.bedTempActual = payload.bedTempActual
  target.bedTempTarget = payload.bedTempTarget
  const leftRaw = payload.estimatedSecondsLeft ?? payload.estimated_seconds_left ?? payload.printTimeLeft
  const left = leftRaw == null ? null : Number(leftRaw)
  target.estimatedSecondsLeft = Number.isNaN(left) ? null : left
  target.errorMessage = payload.errorMessage
}

const connectWebSocket = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  socket = new WebSocket(`${protocol}://localhost:9999/ws/print/progress`)
  socket.onmessage = (event) => {
    try {
      const payload = JSON.parse(event.data)
      applySocketProgress(payload)
    } catch (e) {
      console.warn('print ws parse error', e)
    }
  }
}

onMounted(async () => {
  await loadPrinters()
  await loadJobs()
  connectWebSocket()
})

onUnmounted(() => {
  if (socket) {
    socket.close()
    socket = null
  }
})
</script>

<style scoped>
.header-actions {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}
.table-card {
  background: #fff;
  padding: 20px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}
.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  overflow-x: auto;
}
.event-list-wrap {
  max-height: 480px;
  overflow: auto;
  padding-right: 8px;
}
.event-item {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 10px;
}
.event-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.event-time {
  color: #64748b;
  font-size: 12px;
}
.event-message {
  color: #1e293b;
  margin-bottom: 8px;
  word-break: break-all;
}
.event-payload {
  margin: 0;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 10px;
  font-size: 12px;
  line-height: 1.4;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
