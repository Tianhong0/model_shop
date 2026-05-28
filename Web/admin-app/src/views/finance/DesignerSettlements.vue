<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-select v-model="queryParams.status" placeholder="结算状态" clearable style="width: 130px" @change="handleSearch">
            <el-option label="待结算" :value="0" />
            <el-option label="已结算" :value="1" />
            <el-option label="结算失败" :value="2" />
          </el-select>
          <el-input v-model="queryParams.designerId" placeholder="设计师ID" style="width: 140px" clearable @clear="handleSearch" />
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
        </el-space>
      </div>

      <div class="table-wrapper">
        <el-table :data="tableData" stripe border highlight-current-row style="width: 100%" v-loading="loading">
          <el-table-column prop="settlementSn" label="结算流水号" width="200" />
          <el-table-column prop="orderSn" label="订单编号" width="200" />
          <el-table-column prop="modelName" label="模型名称" min-width="150" />
          <el-table-column prop="designerId" label="设计师ID" width="120" />
          <el-table-column prop="orderPrice" label="订单金额" width="110">
            <template #default="scope">
              ￥{{ Number(scope.row.orderPrice).toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column prop="profitShareRatio" label="分润比例" width="100">
            <template #default="scope">
              {{ scope.row.profitShareRatio }}%
            </template>
          </el-table-column>
          <el-table-column prop="settlementAmount" label="结算金额" width="110">
            <template #default="scope">
              <span style="color: #22c55e; font-weight: 600">￥{{ Number(scope.row.settlementAmount).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)" effect="light" round>
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" width="100" fixed="right" v-if="false">
            <template #default="scope">
              <el-button
                v-if="scope.row.status === 2"
                link
                type="warning"
                @click="handleRetry(scope.row)"
                :loading="retryingId === scope.row.id"
              >
                重试
              </el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDesignerSettlements, retrySettlement } from '../../api/designer'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const retryingId = ref(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  status: null,
  designerId: null
})

const getStatusType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { 0: '待结算', 1: '已结算', 2: '结算失败' }
  return map[status] || '未知'
}

const fetchList = async () => {
  loading.value = true
  try {
    const params = { ...queryParams }
    Object.keys(params).forEach(key => {
      if (params[key] === '' || params[key] === null) delete params[key]
    })
    const res = await getDesignerSettlements(params)
    tableData.value = res?.records || []
    total.value = res?.total || 0
  } catch (e) {
    console.error('获取分润列表失败:', e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchList()
}

const handlePageChange = (page) => {
  queryParams.pageNum = page
  fetchList()
}

const handleSizeChange = (size) => {
  queryParams.pageSize = size
  queryParams.pageNum = 1
  fetchList()
}

const handleRetry = async (row) => {
  try {
    await ElMessageBox.confirm('确认重试该结算？', '确认', { type: 'warning' })
  } catch {
    return
  }
  retryingId.value = row.id
  try {
    await retrySettlement(row.id)
    ElMessage.success('重试成功')
    fetchList()
  } catch (e) {
    console.error('重试失败:', e)
    ElMessage.error('重试失败')
  } finally {
    retryingId.value = null
  }
}

onMounted(() => {
  fetchList()
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
</style>
