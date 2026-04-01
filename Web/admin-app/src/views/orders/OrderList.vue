<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="queryParams.orderSn" placeholder="订单号" clearable style="width: 220px" />
          <el-input-number v-model="queryParams.modelId" :min="1" controls-position="right" placeholder="模型ID" style="width: 160px" />
          <el-input-number v-model="queryParams.userId" :min="1" controls-position="right" placeholder="用户ID" style="width: 160px" />
          <el-select v-model="queryParams.orderStatus" placeholder="订单状态" clearable style="width: 150px">
            <el-option label="待支付" :value="0" />
            <el-option label="生产中" :value="1" />
            <el-option label="待发货" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已取消" :value="4" />
          </el-select>
          <el-button type="primary" @click="fetchOrderList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button type="success" icon="Download" @click="handleExport" :loading="exportLoading">导出</el-button>
        </el-space>
      </div>

      <el-table v-loading="loading" :data="orderData" stripe border highlight-current-row style="width: 100%">
        <el-table-column prop="id" label="订单ID" width="180" />
        <el-table-column prop="orderSn" label="订单号" width="200" />
        <el-table-column prop="modelId" label="模型ID" width="140" />
        <el-table-column prop="modelName" label="模型名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="orderPrice" label="金额" width="120">
          <template #default="scope">￥{{ Number(scope.row.orderPrice || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="orderStatus" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.orderStatus)">{{ statusLabel(scope.row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="handleDetail(scope.row)">详情</el-button>
            <el-button link type="primary" @click="openStatusDialog(scope.row)">修改状态</el-button>
            <el-button
              v-if="scope.row.orderStatus === 2"
              link
              type="success"
              @click="handleRetryShip(scope.row)"
            >重新发货</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          :page-count="Math.max(1, Math.ceil((total || 0) / queryParams.pageSize))"
          :hide-on-single-page="false"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="订单详情" width="820px">
      <el-descriptions v-if="currentOrder" :column="2" border>
        <el-descriptions-item label="订单ID">{{ currentOrder.id }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ currentOrder.orderSn }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentOrder.userId }}</el-descriptions-item>
        <el-descriptions-item label="模型ID">{{ currentOrder.modelId }}</el-descriptions-item>
        <el-descriptions-item label="模型名称">{{ currentOrder.modelName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="材质ID">{{ currentOrder.materialId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">￥{{ Number(currentOrder.orderPrice || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(currentOrder.orderStatus)">{{ statusLabel(currentOrder.orderStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="打印机ID">{{ currentOrder.printerId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentOrder.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ currentOrder.updateTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="定制参数" :span="2">
          <pre class="json-block">{{ formatJson(currentOrder.customParams) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusVisible" title="更新订单状态" width="500px">
      <el-form :model="statusForm" label-width="90px">
        <el-form-item label="订单号">
          <span>{{ statusForm.orderSn }}</span>
        </el-form-item>
        <el-form-item label="目标状态">
          <el-select v-model="statusForm.status" style="width: 100%">
            <el-option label="待支付" :value="0" />
            <el-option label="生产中" :value="1" />
            <el-option label="待发货" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已取消" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="打印机ID">
          <el-input-number v-model="statusForm.printerId" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" :loading="statusSubmitting" @click="submitStatus">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="shipVisible" title="重新发货" width="550px">
      <el-form :model="shipForm" label-width="100px">
        <el-form-item label="订单号">
          <span>{{ shipForm.orderSn }}</span>
        </el-form-item>
        <el-form-item label="收件人姓名">
          <el-input v-model="shipForm.receiverName" placeholder="留空则从订单信息提取" />
        </el-form-item>
        <el-form-item label="收件人电话">
          <el-input v-model="shipForm.receiverPhone" placeholder="留空则从订单信息提取" />
        </el-form-item>
        <el-form-item label="收件人地址">
          <el-input
            v-model="shipForm.receiverAddress"
            type="textarea"
            :rows="2"
            placeholder="留空则从订单信息提取"
          />
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          style="margin-top: 8px"
        >
          如订单信息中缺少收件信息，请手动填写后提交
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipSubmitting" @click="submitShip">确定发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminOrderDetail, getAdminOrderList, updateAdminOrderStatus, retryAutoShip, exportOrders } from '../../api/order'

const loading = ref(false)
const total = ref(0)
const orderData = ref([])
const exportLoading = ref(false)

const detailVisible = ref(false)
const currentOrder = ref(null)

const statusVisible = ref(false)
const statusSubmitting = ref(false)
const statusForm = reactive({
  orderId: null,
  orderSn: '',
  status: 0,
  printerId: null
})

const shipVisible = ref(false)
const shipSubmitting = ref(false)
const shipForm = reactive({
  orderId: null,
  orderSn: '',
  receiverName: '',
  receiverPhone: '',
  receiverAddress: ''
})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  orderStatus: null,
  orderSn: '',
  modelId: null,
  userId: null
})

const statusLabel = (status) => {
  const map = {
    0: '待支付',
    1: '生产中',
    2: '待发货',
    3: '已完成',
    4: '已取消'
  }
  return map[status] || `未知(${status})`
}

const statusType = (status) => {
  if (status === 0) return 'warning'
  if (status === 1) return 'primary'
  if (status === 2) return 'info'
  if (status === 3) return 'success'
  if (status === 4) return 'danger'
  return ''
}

const formatJson = (value) => {
  if (!value) return '-'
  try {
    return JSON.stringify(typeof value === 'string' ? JSON.parse(value) : value, null, 2)
  } catch {
    return String(value)
  }
}

const fetchOrderList = async () => {
  loading.value = true
  try {
    const response = await getAdminOrderList({ ...queryParams })
    orderData.value = response.records || []
    total.value = Number(response.total || 0)
    queryParams.pageNum = Number(response.pageNum || queryParams.pageNum)
    queryParams.pageSize = Number(response.pageSize || queryParams.pageSize)
  } catch (error) {
    console.error('获取订单列表失败:', error)
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  Object.assign(queryParams, {
    pageNum: 1,
    pageSize: 10,
    orderStatus: null,
    orderSn: '',
    modelId: null,
    userId: null
  })
  fetchOrderList()
}

const handlePageChange = (pageNum) => {
  queryParams.pageNum = pageNum
  fetchOrderList()
}

const handleSizeChange = (pageSize) => {
  queryParams.pageSize = pageSize
  queryParams.pageNum = 1
  fetchOrderList()
}

const handleDetail = async (row) => {
  try {
    currentOrder.value = await getAdminOrderDetail(row.id)
    detailVisible.value = true
  } catch (error) {
    console.error('获取订单详情失败:', error)
    ElMessage.error('获取订单详情失败')
  }
}

const openStatusDialog = (row) => {
  Object.assign(statusForm, {
    orderId: row.id,
    orderSn: row.orderSn,
    status: row.orderStatus,
    printerId: null
  })
  statusVisible.value = true
}

const submitStatus = async () => {
  statusSubmitting.value = true
  try {
    await updateAdminOrderStatus({
      orderId: statusForm.orderId,
      status: statusForm.status,
      printerId: statusForm.printerId || null
    })
    ElMessage.success('订单状态更新成功')
    statusVisible.value = false
    await fetchOrderList()
  } catch (error) {
    console.error('更新订单状态失败:', error)
    ElMessage.error('更新订单状态失败')
  } finally {
    statusSubmitting.value = false
  }
}

const handleRetryShip = (row) => {
  Object.assign(shipForm, {
    orderId: row.id,
    orderSn: row.orderSn,
    receiverName: '',
    receiverPhone: '',
    receiverAddress: ''
  })
  shipVisible.value = true
}

const submitShip = async () => {
  shipSubmitting.value = true
  try {
    const deliveryId = await retryAutoShip({
      orderId: shipForm.orderId,
      receiverName: shipForm.receiverName || null,
      receiverPhone: shipForm.receiverPhone || null,
      receiverAddress: shipForm.receiverAddress || null
    })
    ElMessage.success(`发货成功，物流单ID: ${deliveryId}`)
    shipVisible.value = false
    await fetchOrderList()
  } catch (error) {
    console.error('重新发货失败:', error)
    ElMessage.error(error.message || '重新发货失败')
  } finally {
    shipSubmitting.value = false
  }
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const blob = await exportOrders({
      orderSn: queryParams.orderSn || undefined,
      modelId: queryParams.modelId || undefined,
      userId: queryParams.userId || undefined,
      orderStatus: queryParams.orderStatus ?? undefined
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `订单数据_${new Date().toISOString().slice(0, 10)}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => {
  fetchOrderList()
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

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--border-light);
}

.json-block {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  color: var(--text-primary);
  background: var(--bg-secondary);
  padding: 12px;
  border-radius: var(--radius-md);
}
</style>
