<template>
  <div class="statistics-page">
    <div class="filter-bar">
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" :shortcuts="dateShortcuts" @change="handleDateChange" />
      <el-button type="success" @click="handleExport"><el-icon><Download /></el-icon>导出报表</el-button>
    </div>

    <div class="kpi-grid" v-loading="loading">
      <div class="kpi-card"><div class="kpi-label">任务总数</div><div class="kpi-value">{{ taskSummary.totalTasks || 0 }}</div></div>
      <div class="kpi-card"><div class="kpi-label">已完成</div><div class="kpi-value">{{ taskSummary.completedTasks || 0 }}</div></div>
      <div class="kpi-card"><div class="kpi-label">总成交额</div><div class="kpi-value">¥{{ formatMoney(taskSummary.totalFinalAmount) }}</div></div>
      <div class="kpi-card"><div class="kpi-label">平均评分</div><div class="kpi-value">{{ ratingStats.avgRating || 0 }}</div></div>
    </div>

    <el-row :gutter="24" v-loading="loading">
      <el-col :span="16">
        <div class="chart-box"><div class="chart-title">任务趋势</div><div ref="trendChartRef" class="chart-instance"></div></div>
      </el-col>
      <el-col :span="8">
        <div class="chart-box"><div class="chart-title">状态分布</div><div ref="statusChartRef" class="chart-instance"></div></div>
      </el-col>
    </el-row>

    <div class="chart-box" v-loading="loading">
      <div class="chart-title">评分分布</div>
      <el-row :gutter="24">
        <el-col :span="4"><div class="stat-item"><div class="stat-label">五星</div><div class="stat-value">{{ ratingStats.fiveStarCount || 0 }}</div></div></el-col>
        <el-col :span="4"><div class="stat-item"><div class="stat-label">四星</div><div class="stat-value">{{ ratingStats.fourStarCount || 0 }}</div></div></el-col>
        <el-col :span="4"><div class="stat-item"><div class="stat-label">三星</div><div class="stat-value">{{ ratingStats.threeStarCount || 0 }}</div></div></el-col>
        <el-col :span="4"><div class="stat-item"><div class="stat-label">二星</div><div class="stat-value">{{ ratingStats.twoStarCount || 0 }}</div></div></el-col>
        <el-col :span="4"><div class="stat-item"><div class="stat-label">一星</div><div class="stat-value">{{ ratingStats.oneStarCount || 0 }}</div></div></el-col>
      </el-row>
    </div>

    <div class="chart-box" v-loading="loading">
      <div class="chart-title">设计师排行TOP10</div>
      <el-table :data="topDesigners" stripe border size="small">
        <el-table-column prop="nickname" label="设计师" />
        <el-table-column prop="completedTasks" label="完成任务数" width="120" />
        <el-table-column label="总收入" width="120">
          <template #default="{ row }">¥{{ formatMoney(row.totalIncome) }}</template>
        </el-table-column>
        <el-table-column prop="avgRating" label="平均评分" width="100" />
        <el-table-column prop="creditScore" label="信誉分" width="100" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { getBountyStatistics, exportStatistics } from '@/api/statistics'
import * as echarts from 'echarts'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const dateRange = ref([])
const taskSummary = ref({})
const ratingStats = ref({})
const topDesigners = ref([])
const statisticsData = ref({})

let trendChart = null
let statusChart = null
const trendChartRef = ref(null)
const statusChartRef = ref(null)

const dateShortcuts = [
  { text: '近7天', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 7 * 24 * 3600 * 1000); return [start, end] } },
  { text: '近30天', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 30 * 24 * 3600 * 1000); return [start, end] } }
]

const formatMoney = (value) => {
  if (!value) return '0.00'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const handleDateChange = () => fetchData()
const handleExport = () => { const [start, end] = getDates(); exportStatistics('bounty', start, end) }
const getDates = () => {
  if (dateRange.value?.length === 2) return dateRange.value
  const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 30 * 24 * 3600 * 1000)
  return [start.toISOString().split('T')[0], end.toISOString().split('T')[0]]
}

const fetchData = async () => {
  loading.value = true
  try {
    const [start, end] = getDates()
    const data = await getBountyStatistics(start, end)
    // request.js 已返回 res.data，无需再取 .data
    if (data) {
      statisticsData.value = data
      taskSummary.value = data.taskSummary || {}
      ratingStats.value = data.ratingStats || {}
      topDesigners.value = data.topDesigners || []
      await nextTick()
      renderCharts()
    }
  } catch (e) { ElMessage.error('获取统计数据失败') }
  finally { loading.value = false }
}

const renderCharts = () => {
  if (trendChartRef.value) {
    if (trendChart) trendChart.dispose()
    trendChart = echarts.init(trendChartRef.value)
    const trend = statisticsData.value.taskTrend || []
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['新增任务', '完成任务'] },
      xAxis: { type: 'category', data: trend.map(t => t.date) },
      yAxis: { type: 'value' },
      series: [
        { name: '新增任务', type: 'bar', data: trend.map(t => t.newTaskCount) },
        { name: '完成任务', type: 'line', data: trend.map(t => t.completedTaskCount) }
      ]
    })
  }
  if (statusChartRef.value) {
    if (statusChart) statusChart.dispose()
    statusChart = echarts.init(statusChartRef.value)
    const data = (statisticsData.value.statusDistribution || []).map(s => ({ name: s.statusName, value: s.count }))
    statusChart.setOption({ tooltip: { trigger: 'item' }, series: [{ type: 'pie', radius: '50%', data: data }] })
  }
}

const handleResize = () => { trendChart?.resize(); statusChart?.resize() }
onMounted(() => { fetchData(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); trendChart?.dispose(); statusChart?.dispose() })
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
</style>
