<template>
  <div class="statistics-page">
    <div class="filter-bar">
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" :shortcuts="dateShortcuts" @change="handleDateChange" />
      <el-button type="success" @click="handleExport"><el-icon><Download /></el-icon>导出报表</el-button>
    </div>

    <div class="kpi-grid" v-loading="loading">
      <div class="kpi-card"><div class="kpi-label">模型总数</div><div class="kpi-value">{{ summary.totalModels || 0 }}</div></div>
      <div class="kpi-card"><div class="kpi-label">新增模型</div><div class="kpi-value">{{ summary.newModels || 0 }}</div></div>
      <div class="kpi-card"><div class="kpi-label">上架模型</div><div class="kpi-value">{{ summary.activeModels || 0 }}</div></div>
      <div class="kpi-card"><div class="kpi-label">总收藏数</div><div class="kpi-value">{{ summary.totalFavorites || 0 }}</div></div>
    </div>

    <el-row :gutter="24" v-loading="loading">
      <el-col :span="12">
        <div class="chart-box"><div class="chart-title">分类分布</div><div ref="categoryChartRef" class="chart-instance"></div></div>
      </el-col>
      <el-col :span="12">
        <div class="chart-box"><div class="chart-title">状态分布</div><div ref="statusChartRef" class="chart-instance"></div></div>
      </el-col>
    </el-row>

    <div class="chart-box" v-loading="loading">
      <div class="chart-title">热门模型TOP10</div>
      <el-table :data="topModels" stripe border size="small">
        <el-table-column prop="modelName" label="模型名称" />
        <el-table-column prop="designerName" label="设计者" width="120" />
        <el-table-column prop="favoriteCount" label="收藏数" width="100" />
        <el-table-column label="价格" width="120">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { getModelStatistics, exportStatistics } from '@/api/statistics'
import * as echarts from 'echarts'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const dateRange = ref([])
const summary = ref({})
const topModels = ref([])
const statisticsData = ref({})

let categoryChart = null
let statusChart = null
const categoryChartRef = ref(null)
const statusChartRef = ref(null)

const dateShortcuts = [
  { text: '近7天', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 7 * 24 * 3600 * 1000); return [start, end] } },
  { text: '近30天', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 30 * 24 * 3600 * 1000); return [start, end] } }
]

const handleDateChange = () => fetchData()
const handleExport = async () => {
  try {
    const [start, end] = getDates()
    const blob = await exportStatistics('models', start, end)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `模型统计_${start}_${end}.xlsx`
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
    const data = await getModelStatistics(start, end)
    // request.js 已返回 res.data，无需再取 .data
    if (data) {
      statisticsData.value = data
      summary.value = data.summary || {}
      topModels.value = data.topModels || []
      await nextTick()
      renderCharts()
    }
  } catch (e) { ElMessage.error('获取统计数据失败') }
  finally { loading.value = false }
}

const renderCharts = () => {
  if (categoryChartRef.value) {
    if (categoryChart) categoryChart.dispose()
    categoryChart = echarts.init(categoryChartRef.value)
    const data = (statisticsData.value.categoryDistribution || []).map(c => ({ name: c.categoryName, value: c.modelCount }))
    categoryChart.setOption({ tooltip: { trigger: 'item' }, series: [{ type: 'pie', radius: '50%', data: data }] })
  }
  if (statusChartRef.value) {
    if (statusChart) statusChart.dispose()
    statusChart = echarts.init(statusChartRef.value)
    const data = (statisticsData.value.statusDistribution || []).map(s => ({ name: s.statusName, value: s.count }))
    statusChart.setOption({ tooltip: { trigger: 'item' }, series: [{ type: 'pie', radius: '50%', data: data }] })
  }
}

const handleResize = () => { categoryChart?.resize(); statusChart?.resize() }
onMounted(() => { fetchData(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); categoryChart?.dispose(); statusChart?.dispose() })
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
</style>
