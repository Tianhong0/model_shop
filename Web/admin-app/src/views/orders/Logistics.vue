<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="queryParams.orderSn" placeholder="订单号" clearable style="width: 220px" />
          <el-input v-model="queryParams.deliverySn" placeholder="物流单号" clearable style="width: 220px" />
          <el-input v-model="queryParams.deliveryCompany" placeholder="物流公司" clearable style="width: 180px" />
          <el-select v-model="queryParams.status" placeholder="物流状态" clearable style="width: 140px">
            <el-option label="待发货" :value="0" />
            <el-option label="已发货" :value="1" />
            <el-option label="运输中" :value="2" />
            <el-option label="已签收" :value="3" />
            <el-option label="异常" :value="4" />
          </el-select>
          <el-button type="primary" @click="fetchDeliveryList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-space>
        <el-space>
          <el-button type="primary" @click="openShipDialog">录入发货</el-button>
        </el-space>
      </div>

      <el-table v-loading="loading" :data="deliveryData" stripe border highlight-current-row style="width: 100%">
        <el-table-column prop="id" label="物流ID" width="180" />
        <el-table-column prop="orderSn" label="订单号" width="200" />
        <el-table-column prop="deliverySn" label="运单号" width="220" />
        <el-table-column prop="deliveryCompany" label="物流公司" width="130" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="scope">
            <el-tag :type="deliveryStatusType(scope.row.status)">{{ deliveryStatusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deliveryTime" label="发货时间" width="180" />
        <el-table-column prop="receiveTime" label="签收时间" width="180" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="handleDetail(scope.row)">详情</el-button>
            <el-button link type="primary" @click="openStatusDialog(scope.row)">更新状态</el-button>
            <el-button link type="primary" @click="openTrackDialog(scope.row)">追加轨迹</el-button>
            <el-button link type="success" @click="simulateTrack(scope.row)">轨迹仿真</el-button>
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

    <el-dialog v-model="shipVisible" title="录入发货" width="620px">
      <el-form :model="shipForm" label-width="100px">
        <el-form-item label="订单号"><el-input v-model="shipForm.orderSn" /></el-form-item>
        <el-form-item label="物流公司"><el-input v-model="shipForm.deliveryCompany" /></el-form-item>
        <el-form-item label="运单号"><el-input v-model="shipForm.deliverySn" /></el-form-item>
        <el-form-item label="收货人"><el-input v-model="shipForm.receiverName" /></el-form-item>
        <el-form-item label="收货电话"><el-input v-model="shipForm.receiverPhone" /></el-form-item>
        <el-form-item label="收货地址"><el-input v-model="shipForm.receiverAddress" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipSubmitting" @click="submitShip">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusVisible" title="更新物流状态" width="500px">
      <el-form :model="statusForm" label-width="100px">
        <el-form-item label="物流单ID"><span>{{ statusForm.deliveryId }}</span></el-form-item>
        <el-form-item label="目标状态">
          <el-select v-model="statusForm.status" style="width: 100%">
            <el-option label="待发货" :value="0" />
            <el-option label="已发货" :value="1" />
            <el-option label="运输中" :value="2" />
            <el-option label="已签收" :value="3" />
            <el-option label="异常" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="轨迹描述"><el-input v-model="statusForm.trackContent" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="操作人"><el-input v-model="statusForm.operatorInfo" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" :loading="statusSubmitting" @click="submitStatus">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="trackVisible" title="追加物流轨迹" width="500px">
      <el-form :model="trackForm" label-width="100px">
        <el-form-item label="物流单ID"><span>{{ trackForm.deliveryId }}</span></el-form-item>
        <el-form-item label="轨迹描述"><el-input v-model="trackForm.trackContent" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="发生时间"><el-date-picker v-model="trackForm.trackTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" /></el-form-item>
        <el-form-item label="操作信息"><el-input v-model="trackForm.operatorInfo" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="trackVisible = false">取消</el-button>
        <el-button type="primary" :loading="trackSubmitting" @click="submitTrack">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="物流详情" width="760px">
      <el-descriptions v-if="currentDelivery" :column="2" border>
        <el-descriptions-item label="物流ID">{{ currentDelivery.id }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ currentDelivery.orderSn }}</el-descriptions-item>
        <el-descriptions-item label="物流公司">{{ currentDelivery.deliveryCompany }}</el-descriptions-item>
        <el-descriptions-item label="运单号">{{ currentDelivery.deliverySn }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ currentDelivery.receiverName }}</el-descriptions-item>
        <el-descriptions-item label="收货电话">{{ currentDelivery.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">{{ currentDelivery.receiverAddress }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="section-title">物流轨迹</h4>
      <el-timeline v-if="currentDelivery?.tracks?.length">
        <el-timeline-item
          v-for="track in currentDelivery.tracks"
          :key="track.id"
          :timestamp="track.trackTime"
          placement="top"
        >
          <div>{{ track.trackContent }}</div>
          <div class="track-operator">{{ track.operatorInfo || '-' }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无轨迹" />

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  addDeliveryTrack,
  getDeliveryAdminDetail,
  getDeliveryAdminList,
  shipOrder,
  simulateDeliveryTrack,
  updateDeliveryStatus
} from '../../api/order'

const loading = ref(false)
const total = ref(0)
const deliveryData = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  orderSn: '',
  deliverySn: '',
  deliveryCompany: '',
  status: null
})

const shipVisible = ref(false)
const shipSubmitting = ref(false)
const shipForm = reactive({
  orderSn: '',
  deliveryCompany: '',
  deliverySn: '',
  receiverName: '',
  receiverPhone: '',
  receiverAddress: ''
})

const statusVisible = ref(false)
const statusSubmitting = ref(false)
const statusForm = reactive({
  deliveryId: null,
  status: 0,
  trackContent: '',
  operatorInfo: ''
})

const trackVisible = ref(false)
const trackSubmitting = ref(false)
const trackForm = reactive({
  deliveryId: null,
  trackContent: '',
  trackTime: '',
  operatorInfo: ''
})

const detailVisible = ref(false)
const currentDelivery = ref(null)

const deliveryStatusLabel = (status) => {
  const map = {
    0: '待发货',
    1: '已发货',
    2: '运输中',
    3: '已签收',
    4: '异常'
  }
  return map[status] || `未知(${status})`
}

const deliveryStatusType = (status) => {
  if (status === 0) return 'info'
  if (status === 1) return 'primary'
  if (status === 2) return 'warning'
  if (status === 3) return 'success'
  if (status === 4) return 'danger'
  return ''
}

const fetchDeliveryList = async () => {
  loading.value = true
  try {
    const response = await getDeliveryAdminList({ ...queryParams })
    deliveryData.value = response.records || []
    total.value = Number(response.total || 0)
    queryParams.pageNum = Number(response.pageNum || queryParams.pageNum)
    queryParams.pageSize = Number(response.pageSize || queryParams.pageSize)
  } catch (error) {
    console.error('获取物流列表失败:', error)
    ElMessage.error('获取物流列表失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  Object.assign(queryParams, {
    pageNum: 1,
    pageSize: 10,
    orderSn: '',
    deliverySn: '',
    deliveryCompany: '',
    status: null
  })
  fetchDeliveryList()
}

const handlePageChange = (pageNum) => {
  queryParams.pageNum = pageNum
  fetchDeliveryList()
}

const handleSizeChange = (pageSize) => {
  queryParams.pageSize = pageSize
  queryParams.pageNum = 1
  fetchDeliveryList()
}

const openShipDialog = () => {
  Object.assign(shipForm, {
    orderSn: '',
    deliveryCompany: '',
    deliverySn: '',
    receiverName: '',
    receiverPhone: '',
    receiverAddress: ''
  })
  shipVisible.value = true
}

const submitShip = async () => {
  shipSubmitting.value = true
  try {
    await shipOrder({ ...shipForm })
    ElMessage.success('发货录入成功')
    shipVisible.value = false
    await fetchDeliveryList()
  } catch (error) {
    console.error('录入发货失败:', error)
    ElMessage.error('录入发货失败')
  } finally {
    shipSubmitting.value = false
  }
}

const openStatusDialog = (row) => {
  Object.assign(statusForm, {
    deliveryId: row.id,
    status: row.status,
    trackContent: '',
    operatorInfo: ''
  })
  statusVisible.value = true
}

const submitStatus = async () => {
  statusSubmitting.value = true
  try {
    await updateDeliveryStatus({ ...statusForm })
    ElMessage.success('物流状态更新成功')
    statusVisible.value = false
    await fetchDeliveryList()
  } catch (error) {
    console.error('更新物流状态失败:', error)
    ElMessage.error('更新物流状态失败')
  } finally {
    statusSubmitting.value = false
  }
}

const openTrackDialog = (row) => {
  Object.assign(trackForm, {
    deliveryId: row.id,
    trackContent: '',
    trackTime: '',
    operatorInfo: ''
  })
  trackVisible.value = true
}

const submitTrack = async () => {
  trackSubmitting.value = true
  try {
    await addDeliveryTrack({ ...trackForm })
    ElMessage.success('轨迹追加成功')
    trackVisible.value = false
    if (detailVisible.value && currentDelivery.value?.id === trackForm.deliveryId) {
      currentDelivery.value = await getDeliveryAdminDetail(trackForm.deliveryId)
    }
  } catch (error) {
    console.error('追加轨迹失败:', error)
    ElMessage.error('追加轨迹失败')
  } finally {
    trackSubmitting.value = false
  }
}

const simulateTrack = async (row) => {
  try {
    await simulateDeliveryTrack({ deliveryId: row.id })
    ElMessage.success('轨迹仿真已完成')
    await fetchDeliveryList()
  } catch (error) {
    console.error('轨迹仿真失败:', error)
    ElMessage.error('轨迹仿真失败')
  }
}

const handleDetail = async (row) => {
  try {
    currentDelivery.value = await getDeliveryAdminDetail(row.id)
    detailVisible.value = true
  } catch (error) {
    console.error('获取物流详情失败:', error)
    ElMessage.error('获取物流详情失败')
  }
}

onMounted(() => {
  fetchDeliveryList()
})
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
}
.section-title {
  margin: 18px 0 10px;
  font-size: 15px;
  font-weight: 600;
}
.track-operator {
  color: #64748b;
  font-size: 12px;
  margin-top: 4px;
}
</style>
