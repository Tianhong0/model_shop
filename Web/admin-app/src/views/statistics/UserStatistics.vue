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
        <div class="kpi-label">用户总数</div>
        <div class="kpi-value">{{ summary.totalUsers || 0 }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">新增用户</div>
        <div class="kpi-value">{{ summary.newUsers || 0 }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">正常用户</div>
        <div class="kpi-value">{{ summary.normalUsers || 0 }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">设计者数量</div>
        <div class="kpi-value">{{ designerStats.totalDesigners || 0 }}</div>
      </div>
    </div>

    <!-- 图表区域 -->
    <el-row :gutter="24" v-loading="loading">
      <el-col :span="16">
        <div class="chart-box">
          <div class="chart-title">用户增长趋势</div>
          <div ref="growthChartRef" class="chart-instance"></div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="chart-box">
          <div class="chart-title">角色分布</div>
          <div ref="roleChartRef" class="chart-instance"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 设计者统计 -->
    <div class="chart-box" v-loading="loading">
      <div class="chart-title">设计者申请统计</div>
      <el-row :gutter="24">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">待审核</div>
            <div class="stat-value warning">{{ designerStats.pendingApplications || 0 }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">已通过</div>
            <div class="stat-value success">{{ designerStats.approvedApplications || 0 }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">已拒绝</div>
            <div class="stat-value danger">{{ designerStats.rejectedApplications || 0 }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">平均信誉分</div>
            <div class="stat-value">{{ designerStats.avgCreditScore || 0 }}</div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { getUserStatistics, exportStatistics } from '@/api/statistics'
import * as echarts from 'echarts'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const dateRange = ref([])
const summary = ref({})
const designerStats = ref({})
const statisticsData = ref({})

let growthChart = null
let roleChart = null

const growthChartRef = ref(null)
const roleChartRef = ref(null)

const dateShortcuts = [
  { text: '近7天', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 7 * 24 * 3600 * 1000); return [start, end] } },
  { text: '近30天', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 30 * 24 * 3600 * 1000); return [start, end] } },
  { text: '近90天', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 90 * 24 * 3600 * 1000); return [start, end] } }
]

const handleDateChange = () => fetchData()
const handleExport = () => {
  const [start, end] = getDates()
  exportStatistics('users', start, end)
}

const getDates = () => {
  if (dateRange.value && dateRange.value.length === 2) {
    return [dateRange.value[0], dateRange.value[1]]
  }
  const end = new Date()
  const start = new Date()
  start.setTime(start.getTime() - 30 * 24 * 3600 * 1000)
  return [start.toISOString().split('T')[0], end.toISOString().split('T')[0]]
}

const fetchData = async () => {
  loading.value = true
  try {
    const [start, end] = getDates()
    const data = await getUserStatistics(start, end)
    // request.js 已返回 res.data，无需再取 .data
    if (data) {
      statisticsData.value = data
      summary.value = data.summary || {}
      designerStats.value = data.designerStats || {}
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
  renderGrowthChart()
  renderRoleChart()
}

const renderGrowthChart = () => {
  if (!growthChartRef.value) return
  if (growthChart) growthChart.dispose()
  growthChart = echarts.init(growthChartRef.value)

  const trend = statisticsData.value.growthTrend || []
  growthChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: trend.map(t => t.date) },
    yAxis: { type: 'value' },
    series: [{
      name: '新增用户',
      type: 'line',
      data: trend.map(t => t.newUserCount),
      areaStyle: { opacity: 0.3 }
    }]
  })
}

const renderRoleChart = () => {
  if (!roleChartRef.value) return
  if (roleChart) roleChart.dispose()
  roleChart = echarts.init(roleChartRef.value)

  const data = (statisticsData.value.roleDistribution || []).map(r => ({
    name: r.roleName?.replace('ROLE_', ''),
    value: r.userCount
  }))

  roleChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{ type: 'pie', radius: '50%', data: data }]
  })
}

const handleResize = () => {
  growthChart?.resize()
  roleChart?.resize()
}

onMounted(() => {
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  growthChart?.dispose()
  roleChart?.dispose()
})
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
.stat-value.warning { color: #E6A23C; }
.stat-value.danger { color: #F56C6C; }
</style>
