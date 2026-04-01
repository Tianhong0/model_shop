<template>
  <div class="statistics-page">
    <!-- 筛选区域 -->
    <div class="filter-bar">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        :shortcuts="dateShortcuts"
        @change="handleDateChange"
      />
      <el-button type="success" @click="handleExport">
        <el-icon><Download /></el-icon>
        导出报表
      </el-button>
    </div>

    <!-- KPI 卡片 -->
    <div class="kpi-grid" v-loading="loading">
      <div class="kpi-card">
        <div class="kpi-label">订单总数</div>
        <div class="kpi-value">{{ summary.totalOrders || 0 }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">销售总额</div>
        <div class="kpi-value">¥{{ formatMoney(summary.totalAmount) }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">平均客单价</div>
        <div class="kpi-value">¥{{ formatMoney(summary.avgOrderAmount) }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">完成率</div>
        <div class="kpi-value">{{ summary.completeRate || 0 }}%</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">取消率</div>
        <div class="kpi-value">{{ summary.cancelRate || 0 }}%</div>
      </div>
    </div>

    <!-- 图表区域 -->
    <el-row :gutter="24" v-loading="loading">
      <el-col :span="16">
        <div class="chart-box">
          <div class="chart-title">订单趋势</div>
          <div ref="trendChartRef" class="chart-instance"></div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="chart-box">
          <div class="chart-title">订单状态分布</div>
          <div ref="statusChartRef" class="chart-instance"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="24" v-loading="loading">
      <el-col :span="12">
        <div class="chart-box">
          <div class="chart-title">支付渠道分布</div>
          <div ref="paymentChartRef" class="chart-instance"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-box">
          <div class="chart-title">售后统计</div>
          <div ref="afterSaleChartRef" class="chart-instance"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <div class="chart-box" v-loading="loading">
      <div class="chart-title">详细数据</div>
      <el-table :data="tableData" stripe border size="small">
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="orderCount" label="订单量" width="100" />
        <el-table-column prop="orderAmount" label="销售额">
          <template #default="{ row }">¥{{ formatMoney(row.orderAmount) }}</template>
        </el-table-column>
        <el-table-column prop="paidUserCount" label="付费用户数" width="120" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { getOrderStatistics, exportStatistics } from '@/api/statistics'
import * as echarts from 'echarts'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const dateRange = ref([])
const summary = ref({})
const tableData = ref([])

let trendChart = null
let statusChart = null
let paymentChart = null
let afterSaleChart = null

const trendChartRef = ref(null)
const statusChartRef = ref(null)
const paymentChartRef = ref(null)
const afterSaleChartRef = ref(null)

const statisticsData = ref({})

const dateShortcuts = [
  { text: '近7天', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 7 * 24 * 3600 * 1000); return [start, end] } },
  { text: '近30天', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 30 * 24 * 3600 * 1000); return [start, end] } },
  { text: '近90天', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 90 * 24 * 3600 * 1000); return [start, end] } }
]

const handleDateChange = () => {
  fetchData()
}

const handleExport = () => {
  const [start, end] = getDates()
  exportStatistics('orders', start, end)
}

const getDates = () => {
  if (dateRange.value && dateRange.value.length === 2) {
    return [dateRange.value[0], dateRange.value[1]]
  }
  const end = new Date()
  const start = new Date()
  start.setTime(start.getTime() - 30 * 24 * 3600 * 1000)
  return [formatDate(start), formatDate(end)]
}

const formatDate = (date) => {
  return date.toISOString().split('T')[0]
}

const formatMoney = (value) => {
  if (!value) return '0.00'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const fetchData = async () => {
  loading.value = true
  try {
    const [start, end] = getDates()
    const data = await getOrderStatistics(start, end)
    // request.js 已返回 res.data，无需再取 .data
    if (data) {
      statisticsData.value = data
      summary.value = data.summary || {}
      tableData.value = data.trend || []
      await nextTick()
      renderCharts()
    }
  } catch (e) {
    ElMessage.error('获取统计数据失败')
  } finally {
    loading.value = false
  }
}

const renderCharts = () => {
  renderTrendChart()
  renderStatusChart()
  renderPaymentChart()
  renderAfterSaleChart()
}

const renderTrendChart = () => {
  if (!trendChartRef.value) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)

  const trend = statisticsData.value.trend || []
  const dates = trend.map(t => t.date)
  const counts = trend.map(t => t.orderCount || 0)
  const amounts = trend.map(t => t.orderAmount || 0)

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['订单量', '订单金额'] },
    xAxis: { type: 'category', data: dates },
    yAxis: [
      { type: 'value', name: '订单量' },
      { type: 'value', name: '金额(元)' }
    ],
    series: [
      { name: '订单量', type: 'bar', data: counts },
      { name: '订单金额', type: 'line', yAxisIndex: 1, data: amounts }
    ]
  })
}

const renderStatusChart = () => {
  if (!statusChartRef.value) return
  if (statusChart) statusChart.dispose()
  statusChart = echarts.init(statusChartRef.value)

  const data = (statisticsData.value.statusDistribution || []).map(s => ({
    name: s.statusName,
    value: s.count
  }))

  statusChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: '50%',
      data: data,
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' } }
    }]
  })
}

const renderPaymentChart = () => {
  if (!paymentChartRef.value) return
  if (paymentChart) paymentChart.dispose()
  paymentChart = echarts.init(paymentChartRef.value)

  const data = (statisticsData.value.paymentChannels || []).map(c => ({
    name: c.channelName,
    value: c.count
  }))

  paymentChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: data,
      label: { show: false }
    }]
  })
}

const renderAfterSaleChart = () => {
  if (!afterSaleChartRef.value) return
  if (afterSaleChart) afterSaleChart.dispose()
  afterSaleChart = echarts.init(afterSaleChartRef.value)

  const afterSale = statisticsData.value.afterSale || {}
  afterSaleChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['待处理', '已通过', '已拒绝'] },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: [afterSale.pendingCount || 0, afterSale.approvedCount || 0, afterSale.rejectedCount || 0],
      itemStyle: {
        color: (params) => {
          const colors = ['#E6A23C', '#67C23A', '#F56C6C']
          return colors[params.dataIndex]
        }
      }
    }]
  })
}

const handleResize = () => {
  trendChart?.resize()
  statusChart?.resize()
  paymentChart?.resize()
  afterSaleChart?.resize()
}

onMounted(() => {
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  statusChart?.dispose()
  paymentChart?.dispose()
  afterSaleChart?.dispose()
})
</script>

<style scoped>
.statistics-page {
  padding: 20px;
}

.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.kpi-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.kpi-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.kpi-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.chart-box {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #303133;
}

.chart-instance {
  height: 300px;
}
</style>
