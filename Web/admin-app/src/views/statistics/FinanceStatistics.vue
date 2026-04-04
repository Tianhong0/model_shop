<template>
  <div class="statistics-page">
    <div class="filter-bar">
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" :shortcuts="dateShortcuts" @change="handleDateChange" />
      <el-button type="success" @click="handleExport"><el-icon><Download /></el-icon>导出报表</el-button>
    </div>

    <div class="kpi-grid" v-loading="loading">
      <div class="kpi-card"><div class="kpi-label">总可用余额</div><div class="kpi-value">¥{{ formatMoney(walletSummary.totalAvailableBalance) }}</div></div>
      <div class="kpi-card"><div class="kpi-label">总冻结余额</div><div class="kpi-value">¥{{ formatMoney(walletSummary.totalFrozenBalance) }}</div></div>
      <div class="kpi-card"><div class="kpi-label">提现申请</div><div class="kpi-value">{{ withdrawStats.totalApplications || 0 }}</div></div>
      <div class="kpi-card"><div class="kpi-label">已打款金额</div><div class="kpi-value">¥{{ formatMoney(withdrawStats.totalPaidAmount) }}</div></div>
    </div>

    <el-row :gutter="24" v-loading="loading">
      <el-col :span="16">
        <div class="chart-box"><div class="chart-title">收支趋势</div><div ref="ledgerChartRef" class="chart-instance"></div></div>
      </el-col>
      <el-col :span="8">
        <div class="chart-box"><div class="chart-title">提现状态</div><div ref="withdrawChartRef" class="chart-instance"></div></div>
      </el-col>
    </el-row>

    <div class="chart-box" v-loading="loading">
      <div class="chart-title">积分统计</div>
      <el-row :gutter="24">
        <el-col :span="8"><div class="stat-item"><div class="stat-label">总可用积分</div><div class="stat-value">{{ pointStats.totalAvailablePoints || 0 }}</div></div></el-col>
        <el-col :span="8"><div class="stat-item"><div class="stat-label">累计获得</div><div class="stat-value success">{{ pointStats.totalEarned || 0 }}</div></div></el-col>
        <el-col :span="8"><div class="stat-item"><div class="stat-label">累计消耗</div><div class="stat-value danger">{{ pointStats.totalSpent || 0 }}</div></div></el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { getFinanceStatistics, exportStatistics } from '@/api/statistics'
import * as echarts from 'echarts'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const dateRange = ref([])
const walletSummary = ref({})
const withdrawStats = ref({})
const pointStats = ref({})
const statisticsData = ref({})

let ledgerChart = null
let withdrawChart = null
const ledgerChartRef = ref(null)
const withdrawChartRef = ref(null)

const dateShortcuts = [
  { text: '近7天', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 7 * 24 * 3600 * 1000); return [start, end] } },
  { text: '近30天', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 30 * 24 * 3600 * 1000); return [start, end] } }
]

const formatMoney = (value) => {
  if (!value) return '0.00'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const handleDateChange = () => fetchData()
const handleExport = async () => {
  try {
    const [start, end] = getDates()
    const blob = await exportStatistics('finance', start, end)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `财务统计_${start}_${end}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}
const getDates = () => {
  if (dateRange.value?.length === 2) return dateRange.value
  const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 30 * 24 * 3600 * 1000)
  return [start.toISOString().split('T')[0], end.toISOString().split('T')[0]]
}

const fetchData = async () => {
  loading.value = true
  try {
    const [start, end] = getDates()
    const data = await getFinanceStatistics(start, end)
    // request.js 已返回 res.data，无需再取 .data
    if (data) {
      statisticsData.value = data
      walletSummary.value = data.walletSummary || {}
      withdrawStats.value = data.withdrawStats || {}
      pointStats.value = data.pointStats || {}
      await nextTick()
      renderCharts()
    }
  } catch (e) { ElMessage.error('获取统计数据失败') }
  finally { loading.value = false }
}

const renderCharts = () => {
  if (ledgerChartRef.value) {
    if (ledgerChart) ledgerChart.dispose()
    ledgerChart = echarts.init(ledgerChartRef.value)
    const trend = statisticsData.value.ledgerTrend || []
    ledgerChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['收入', '支出'] },
      xAxis: { type: 'category', data: trend.map(t => t.date) },
      yAxis: { type: 'value' },
      series: [
        { name: '收入', type: 'bar', data: trend.map(t => t.income) },
        { name: '支出', type: 'bar', data: trend.map(t => t.expense) }
      ]
    })
  }
  if (withdrawChartRef.value) {
    if (withdrawChart) withdrawChart.dispose()
    withdrawChart = echarts.init(withdrawChartRef.value)
    const ws = withdrawStats.value
    withdrawChart.setOption({
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: '50%', data: [
        { name: '待审核', value: ws.pendingCount },
        { name: '待打款', value: ws.approvedCount },
        { name: '已拒绝', value: ws.rejectedCount },
        { name: '已打款', value: ws.paidCount }
      ]}]
    })
  }
}

const handleResize = () => { ledgerChart?.resize(); withdrawChart?.resize() }
onMounted(() => { fetchData(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); ledgerChart?.dispose(); withdrawChart?.dispose() })
</script>

<style scoped>
.statistics-page { padding: 20px; }
.filter-bar { display: flex; gap: 16px; margin-bottom: 20px; }
.kpi-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.kpi-card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1); }
.kpi-label { font-size: 14px; color: #909399; margin-bottom: 8px; }
.kpi-value { font-size: 24px; font-weight: 600; color: #303133; }
.chart-box { background: #fff; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1); }
.chart-title { font-size: 16px; font-weight: 600; margin-bottom: 16px; color: #303133; }
.chart-instance { height: 300px; }
.stat-item { text-align: center; padding: 20px; }
.stat-label { font-size: 14px; color: #909399; margin-bottom: 8px; }
.stat-value { font-size: 24px; font-weight: 600; }
.stat-value.success { color: #67C23A; }
.stat-value.danger { color: #F56C6C; }
</style>
