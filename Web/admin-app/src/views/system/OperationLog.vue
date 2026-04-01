<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space wrap>
          <el-input v-model="queryParams.operatorName" placeholder="操作人..." style="width: 150px" clearable />
          <el-select v-model="queryParams.operationType" placeholder="操作类型" clearable style="width: 130px">
            <el-option label="新增" value="CREATE" />
            <el-option label="修改" value="UPDATE" />
            <el-option label="删除" value="DELETE" />
            <el-option label="审核" value="REVIEW" />
            <el-option label="登录" value="LOGIN" />
            <el-option label="支付" value="PAY" />
            <el-option label="退款" value="REFUND" />
            <el-option label="发货" value="SHIP" />
          </el-select>
          <el-select v-model="queryParams.module" placeholder="模块" clearable style="width: 130px">
            <el-option label="用户管理" value="用户管理" />
            <el-option label="角色管理" value="角色管理" />
            <el-option label="订单管理" value="订单管理" />
            <el-option label="物流管理" value="物流管理" />
            <el-option label="售后管理" value="售后管理" />
            <el-option label="财务管理" value="财务管理" />
            <el-option label="认证管理" value="认证管理" />
            <el-option label="系统配置" value="系统配置" />
          </el-select>
          <el-select v-model="queryParams.success" placeholder="状态" clearable style="width: 100px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
            value-format="YYYY-MM-DD"
          />
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button icon="Delete" @click="handleClean">清理日志</el-button>
        </el-space>
      </div>

      <div class="table-wrapper">
        <el-table :data="logData" stripe border highlight-current-row style="width: 100%" v-loading="loading">
          <el-table-column prop="operatorName" label="操作人" width="100" />
          <el-table-column prop="operationType" label="操作类型" width="100">
            <template #default="scope">
              <el-tag :type="getTypeTagType(scope.row.operationType)" size="small">
                {{ scope.row.operationType }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="module" label="模块" width="100" />
          <el-table-column prop="description" label="操作描述" min-width="150" />
          <el-table-column prop="targetType" label="对象类型" width="100" />
          <el-table-column prop="ip" label="IP" width="130" />
          <el-table-column prop="success" label="状态" width="80">
            <template #default="scope">
              <el-tag :type="scope.row.success === 1 ? 'success' : 'danger'" size="small">
                {{ scope.row.success === 1 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="duration" label="耗时(ms)" width="90" />
          <el-table-column prop="createTime" label="操作时间" width="170" />
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="handleDetail(scope.row)">详情</el-button>
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
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="700px">
      <el-descriptions :column="2" border v-if="currentLog">
        <el-descriptions-item label="日志ID">{{ currentLog.id }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentLog.operatorName }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ currentLog.operationType }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ currentLog.module }}</el-descriptions-item>
        <el-descriptions-item label="操作描述" :span="2">{{ currentLog.description }}</el-descriptions-item>
        <el-descriptions-item label="对象类型">{{ currentLog.targetType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="对象ID">{{ currentLog.targetId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求路径">{{ currentLog.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ currentLog.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog.ip }}</el-descriptions-item>
        <el-descriptions-item label="执行状态">
          <el-tag :type="currentLog.success === 1 ? 'success' : 'danger'">
            {{ currentLog.success === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="执行时长">{{ currentLog.duration }} ms</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ currentLog.createTime }}</el-descriptions-item>
        <el-descriptions-item label="操作内容" :span="2">
          <el-text v-if="currentLog.content" truncated expanded>{{ currentLog.content }}</el-text>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="变更前数据" :span="2">
          <el-input v-if="currentLog.beforeData" :model-value="currentLog.beforeData" type="textarea" :rows="3" readonly />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="变更后数据" :span="2">
          <el-input v-if="currentLog.afterData" :model-value="currentLog.afterData" type="textarea" :rows="3" readonly />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2" v-if="currentLog.errorMsg">
          <el-text type="danger">{{ currentLog.errorMsg }}</el-text>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOperationLogPage, getOperationLogDetail, cleanOldLogs } from '../../api/operationLog'

const loading = ref(false)
const logData = ref([])
const total = ref(0)
const detailVisible = ref(false)
const currentLog = ref(null)
const dateRange = ref([])

const queryParams = reactive({
  operatorName: '',
  operationType: '',
  module: '',
  success: null,
  startTime: null,
  endTime: null,
  pageNum: 1,
  pageSize: 10
})

// 获取日志列表
const fetchLogList = async () => {
  loading.value = true
  try {
    const params = { ...queryParams }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0] + ' 00:00:00'
      params.endTime = dateRange.value[1] + ' 23:59:59'
    }
    Object.keys(params).forEach(key => {
      if (params[key] === '' || params[key] === null) {
        delete params[key]
      }
    })
    const response = await getOperationLogPage(params)
    logData.value = response?.records || []
    total.value = response?.total || 0
  } catch (error) {
    console.error('获取日志列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取类型标签颜色
const getTypeTagType = (type) => {
  const typeColors = {
    CREATE: 'success',
    UPDATE: 'warning',
    DELETE: 'danger',
    REVIEW: 'primary',
    LOGIN: '',
    PAY: 'success',
    REFUND: 'warning',
    SHIP: 'primary'
  }
  return typeColors[type] || ''
}

// 搜索
const handleSearch = () => {
  queryParams.pageNum = 1
  fetchLogList()
}

// 页码变化
const handlePageChange = (page) => {
  queryParams.pageNum = page
  fetchLogList()
}

// 每页大小变化
const handleSizeChange = (size) => {
  queryParams.pageSize = size
  queryParams.pageNum = 1
  fetchLogList()
}

// 查看详情
const handleDetail = async (row) => {
  try {
    const response = await getOperationLogDetail(row.id)
    currentLog.value = response
    detailVisible.value = true
  } catch (error) {
    console.error('获取日志详情失败:', error)
    currentLog.value = row
    detailVisible.value = true
  }
}

// 清理日志
const handleClean = () => {
  ElMessageBox.prompt('请输入保留天数（最少30天）', '清理历史日志', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /^[3-9]\d*$|^[1-9]\d{2,}$/,
    inputErrorMessage: '保留天数不能少于30天',
    inputValue: '90'
  }).then(async ({ value }) => {
    try {
      const result = await cleanOldLogs(parseInt(value))
      ElMessage.success(`清理完成，共删除 ${result} 条日志`)
      fetchLogList()
    } catch (error) {
      console.error('清理日志失败:', error)
      ElMessage.error('清理日志失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchLogList()
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
  margin-bottom: 24px;
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
