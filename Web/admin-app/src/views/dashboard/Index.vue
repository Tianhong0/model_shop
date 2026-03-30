<template>
  <div class="dashboard-container" v-loading="loading">
    <div class="stat-grid">
      <div class="modern-card" v-for="item in kpiCards" :key="item.label">
        <div class="stat-label">{{ item.label }}</div>
        <div class="stat-value">{{ item.value }}</div>
        <div class="stat-trend" :class="item.trendClass">
          <span class="trend-arrow">{{ item.trendArrow }}</span>
          <span>{{ item.trendText }}</span>
        </div>
        <div class="stat-desc">{{ item.desc }}</div>
      </div>
    </div>

    <el-row :gutter="24">
      <el-col :span="12">
        <div class="chart-box todo-box">
          <div class="chart-title">待办看板</div>
          <div class="todo-grid">
            <div class="todo-item" @click="goTo('/orders/after-sales')">
              <span class="todo-label">售后待处理</span>
              <span class="todo-value">{{ Number(overview.todo.afterSalePending || 0) }}</span>
            </div>
            <div class="todo-item" @click="goTo('/finance/withdraws')">
              <span class="todo-label">提现待审核</span>
              <span class="todo-value">{{ Number(overview.todo.withdrawPending || 0) }}</span>
            </div>
            <div class="todo-item" @click="goTo('/users/deletion-requests')">
              <span class="todo-label">注销申请待审核</span>
              <span class="todo-value">{{ Number(overview.todo.deletionPending || 0) }}</span>
            </div>
            <div class="todo-item" @click="goTo('/models/list')">
              <span class="todo-label">模型待审核</span>
              <span class="todo-value">{{ Number(overview.todo.modelReviewPending || 0) }}</span>
            </div>
            <div class="todo-item" @click="goTo('/bounty')">
              <span class="todo-label">悬赏待审核</span>
              <span class="todo-value">{{ Number(overview.todo.bountyReviewPending || 0) }}</span>
            </div>
            <div class="todo-item" @click="goTo('/users/admin-register-requests')">
              <span class="todo-label">管理员注册待审核</span>
              <span class="todo-value">{{ Number(overview.todo.adminRegisterPending || 0) }}</span>
            </div>
            <div class="todo-item" @click="goTo('/users/designer-apply-requests')">
              <span class="todo-label">设计者申请待审核</span>
              <span class="todo-value">{{ Number(overview.todo.designerApplyPending || 0) }}</span>
            </div>
            <div class="todo-item" @click="goTo('/print-queue')">
              <span class="todo-label">打印异常</span>
              <span class="todo-value">{{ Number(overview.todo.printException || 0) }}</span>
            </div>
            <div class="todo-item" @click="goTo('/bounty')">
              <span class="todo-label">悬赏争议</span>
              <span class="todo-value">{{ Number(overview.todo.bountyDisputed || 0) }}</span>
            </div>
            <div class="todo-item" @click="goTo('/models/model-lists')">
              <span class="todo-label">清单总数</span>
              <span class="todo-value">{{ Number(overview.kpi.totalModelLists || 0) }}</span>
            </div>
            <div class="todo-item" @click="goTo('/models/model-lists')">
              <span class="todo-label">清单互动</span>
              <span class="todo-value">{{ Number(overview.kpi.totalModelListInteractions || 0) }}</span>
            </div>
            <div class="todo-item" @click="goTo('/events')">
              <span class="todo-label">活动评审中</span>
              <span class="todo-value">{{ Number(overview.todo.eventReviewing || 0) }}</span>
            </div>
            <div class="todo-item" @click="goTo('/used/reports')">
              <span class="todo-label">二手举报待处理</span>
              <span class="todo-value">{{ Number(overview.todo.usedReportPending || 0) }}</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-box">
          <div class="chart-title">统计时间</div>
          <div class="meta-wrap">
            <div class="meta-item">
              <span class="meta-k">数据更新时间</span>
              <span class="meta-v">{{ formatDateTime(overview.generatedAt) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-k">趋势周期</span>
              <span class="meta-v">近7天（订单量+订单金额）</span>
            </div>
            <div class="meta-item">
              <span class="meta-k">金额口径</span>
              <span class="meta-v">排除已取消订单</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="24">
      <el-col :span="14">
        <div class="chart-box large">
          <div class="chart-title">近7天订单趋势（双轴）</div>
          <div ref="mainRef" class="chart-instance"></div>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="chart-box large">
          <div class="chart-title">最近订单</div>
          <el-table :data="overview.recentOrders" stripe border height="320">
            <el-table-column prop="orderSn" label="订单号" min-width="130" show-overflow-tooltip />
            <el-table-column prop="userNickname" label="用户" min-width="90" show-overflow-tooltip />
            <el-table-column label="金额" min-width="90">
              <template #default="scope">
                {{ formatMoney(scope.row.orderPrice) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="scope">
                {{ formatOrderStatus(scope.row.orderStatus) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="24">
      <el-col :span="12">
        <div class="chart-box">
          <div class="chart-title">近7天订单量分布</div>
          <div ref="orderBarRef" class="chart-instance"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-box">
          <div class="chart-title">最近订单状态分布</div>
          <div ref="statusPieRef" class="chart-instance"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="24">
      <el-col :span="12">
        <div class="chart-box">
          <div class="chart-title">近7天客单价趋势</div>
          <div ref="avgAmountRef" class="chart-instance"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-box">
          <div class="chart-title">近7天订单金额累计</div>
          <div ref="cumAmountRef" class="chart-instance"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { useRouter } from 'vue-router'
import { getDashboardOverview } from '../../api/dashboard'

const mainRef = ref(null)
const orderBarRef = ref(null)
const statusPieRef = ref(null)
const avgAmountRef = ref(null)
const cumAmountRef = ref(null)
const loading = ref(false)
const router = useRouter()

const overview = ref({
  kpi: {
    totalUsers: 0,
    totalOrders: 0,
    totalModels: 0,
    totalModelLists: 0,
    totalModelListInteractions: 0,
    totalTodos: 0,
    orderAmount7d: 0
  },
  todo: {
    afterSalePending: 0,
    withdrawPending: 0,
    deletionPending: 0,
    modelReviewPending: 0,
    bountyReviewPending: 0,
    adminRegisterPending: 0,
    designerApplyPending: 0,
    printException: 0,
    bountyDisputed: 0,
    eventReviewing: 0,
    usedReportPending: 0
  },
  trend7d: [],
  recentOrders: [],
  generatedAt: ''
})

let mainChart = null
let orderBarChart = null
let statusPieChart = null
let avgAmountChart = null
let cumAmountChart = null

const kpiCards = computed(() => {
  const kpi = overview.value.kpi || {}
  return [
    {
      label: '用户总数',
      value: formatInteger(kpi.totalUsers),
      desc: '当前启用用户',
      ...buildTrendMeta(kpi.usersTrendPct, '较昨日新增')
    },
    {
      label: '订单总数',
      value: formatInteger(kpi.totalOrders),
      desc: '累计有效订单',
      ...buildTrendMeta(kpi.ordersTrendPct, '较昨日新增')
    },
    {
      label: '模型总数',
      value: formatInteger(kpi.totalModels),
      desc: '当前可管理模型',
      ...buildTrendMeta(kpi.modelsTrendPct, '较昨日新增')
    },
    {
      label: '清单总数',
      value: formatInteger(kpi.totalModelLists),
      desc: '已发布清单',
      ...buildTrendMeta(kpi.modelListsTrendPct, '较昨日新增')
    },
    {
      label: '清单互动数',
      value: formatInteger(kpi.totalModelListInteractions),
      desc: '点赞+收藏总计',
      ...buildTrendMeta(null, '')
    },
    {
      label: '待办总数',
      value: formatInteger(kpi.totalTodos),
      desc: '待处理业务事项',
      ...buildTrendMeta(kpi.todosTrendPct, '较昨日新增')
    },
    {
      label: '近7天订单金额',
      value: formatMoney(kpi.orderAmount7d),
      desc: '排除取消订单口径',
      ...buildTrendMeta(kpi.orderAmount7dTrendPct, '较前7天')
    }
  ]
})

onMounted(() => {
  initMainChart()
  initOrderBarChart()
  initStatusPieChart()
  initAvgAmountChart()
  initCumAmountChart()
  fetchOverview()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (mainChart) {
    mainChart.dispose()
    mainChart = null
  }
  if (orderBarChart) {
    orderBarChart.dispose()
    orderBarChart = null
  }
  if (statusPieChart) {
    statusPieChart.dispose()
    statusPieChart = null
  }
  if (avgAmountChart) {
    avgAmountChart.dispose()
    avgAmountChart = null
  }
  if (cumAmountChart) {
    cumAmountChart.dispose()
    cumAmountChart = null
  }
})

const fetchOverview = async () => {
  loading.value = true
  try {
    const data = await getDashboardOverview()
    overview.value = {
      ...overview.value,
      ...data,
      kpi: { ...overview.value.kpi, ...(data?.kpi || {}) },
      todo: { ...overview.value.todo, ...(data?.todo || {}) },
      trend7d: Array.isArray(data?.trend7d) ? data.trend7d : [],
      recentOrders: Array.isArray(data?.recentOrders) ? data.recentOrders : []
    }
    renderTrendChart()
    renderOrderBarChart()
    renderStatusPieChart()
    renderAvgAmountChart()
    renderCumAmountChart()
  } finally {
    loading.value = false
  }
}

const initMainChart = () => {
  if (!mainRef.value) return
  mainChart = echarts.init(mainRef.value)
  renderTrendChart()
}

const initOrderBarChart = () => {
  if (!orderBarRef.value) return
  orderBarChart = echarts.init(orderBarRef.value)
  renderOrderBarChart()
}

const initStatusPieChart = () => {
  if (!statusPieRef.value) return
  statusPieChart = echarts.init(statusPieRef.value)
  renderStatusPieChart()
}

const initAvgAmountChart = () => {
  if (!avgAmountRef.value) return
  avgAmountChart = echarts.init(avgAmountRef.value)
  renderAvgAmountChart()
}

const initCumAmountChart = () => {
  if (!cumAmountRef.value) return
  cumAmountChart = echarts.init(cumAmountRef.value)
  renderCumAmountChart()
}

const renderTrendChart = () => {
  if (!mainChart) return
  const trend = Array.isArray(overview.value.trend7d) ? overview.value.trend7d : []
  const xAxis = trend.map(item => item.date || '-')
  const orderCountData = trend.map(item => Number(item.orderCount || 0))
  const orderAmountData = trend.map(item => Number(item.orderAmount || 0))

  mainChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['订单量', '订单金额'] },
    xAxis: {
      type: 'category',
      data: xAxis
    },
    yAxis: [
      {
        type: 'value',
        name: '订单量'
      },
      {
        type: 'value',
        name: '金额(元)'
      }
    ],
    series: [
      {
        name: '订单量',
        type: 'bar',
        data: orderCountData,
        itemStyle: { color: '#4f46e5' }
      },
      {
        name: '订单金额',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: orderAmountData,
        itemStyle: { color: '#10b981' }
      }
    ]
  })
}

const renderOrderBarChart = () => {
  if (!orderBarChart) return
  const trend = Array.isArray(overview.value.trend7d) ? overview.value.trend7d : []
  const yAxis = trend.map(item => item.date || '-').reverse()
  const orderCountData = trend.map(item => Number(item.orderCount || 0)).reverse()

  orderBarChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: '3%',
      right: '8%',
      bottom: '3%',
      top: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      name: '订单量'
    },
    yAxis: {
      type: 'category',
      data: yAxis,
      axisLabel: {
        formatter: (value) => value.slice(5) // 只显示 MM-DD
      }
    },
    series: [
      {
        name: '订单量',
        type: 'bar',
        data: orderCountData,
        barWidth: '60%',
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 1,
            y2: 0,
            colorStops: [
              { offset: 0, color: '#4f46e5' },
              { offset: 1, color: '#818cf8' }
            ]
          },
          borderRadius: [0, 4, 4, 0]
        },
        label: {
          show: true,
          position: 'right',
          color: '#4f46e5',
          fontWeight: 600
        }
      }
    ]
  })
}

const renderStatusPieChart = () => {
  if (!statusPieChart) return
  const statusCounter = new Map()
  const orders = Array.isArray(overview.value.recentOrders) ? overview.value.recentOrders : []

  orders.forEach((item) => {
    const status = Number(item?.orderStatus)
    const label = formatOrderStatus(status)
    statusCounter.set(label, (statusCounter.get(label) || 0) + 1)
  })

  const data = Array.from(statusCounter.entries()).map(([name, value]) => ({ name, value }))

  statusPieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        name: '订单状态',
        type: 'pie',
        radius: '70%',
        center: ['50%', '45%'],
        label: { formatter: '{b}: {c}' },
        data
      }
    ]
  })
}

const renderAvgAmountChart = () => {
  if (!avgAmountChart) return
  const trend = Array.isArray(overview.value.trend7d) ? overview.value.trend7d : []
  const xAxis = trend.map(item => item.date || '-')
  const avgAmountData = trend.map(item => {
    const count = Number(item.orderCount || 0)
    const amount = Number(item.orderAmount || 0)
    if (!count) return 0
    return Number((amount / count).toFixed(2))
  })

  avgAmountChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: xAxis
    },
    yAxis: {
      type: 'value',
      name: '客单价(元)'
    },
    series: [
      {
        name: '客单价',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.12 },
        data: avgAmountData,
        itemStyle: { color: '#f59e0b' }
      }
    ]
  })
}

const renderCumAmountChart = () => {
  if (!cumAmountChart) return
  const trend = Array.isArray(overview.value.trend7d) ? overview.value.trend7d : []
  const xAxis = trend.map(item => item.date || '-')
  const cumData = []
  let sum = 0
  trend.forEach((item) => {
    sum += Number(item.orderAmount || 0)
    cumData.push(Number(sum.toFixed(2)))
  })

  cumAmountChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: xAxis
    },
    yAxis: {
      type: 'value',
      name: '累计金额(元)'
    },
    series: [
      {
        name: '累计金额',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.15 },
        data: cumData,
        itemStyle: { color: '#6366f1' }
      }
    ]
  })
}

const handleResize = () => {
  if (mainChart) {
    mainChart.resize()
  }
  if (orderBarChart) {
    orderBarChart.resize()
  }
  if (statusPieChart) {
    statusPieChart.resize()
  }
  if (avgAmountChart) {
    avgAmountChart.resize()
  }
  if (cumAmountChart) {
    cumAmountChart.resize()
  }
}

const goTo = (path) => {
  router.push(path)
}

const formatInteger = (value) => {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num.toLocaleString('zh-CN') : '0'
}

const formatMoney = (value) => {
  const num = Number(value || 0)
  if (!Number.isFinite(num)) return '￥0.00'
  return `￥${num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

const formatOrderStatus = (status) => {
  const map = {
    0: '待支付',
    1: '生产中',
    2: '待发货',
    3: '已完成',
    4: '已取消'
  }
  return map[Number(status)] || '-'
}

const buildTrendMeta = (trendValue, suffix) => {
  const numeric = Number(trendValue)
  if (!Number.isFinite(numeric)) {
    return {
      trendArrow: '•',
      trendText: `-- ${suffix}`,
      trendClass: 'flat'
    }
  }

  if (numeric > 0) {
    return {
      trendArrow: '↑',
      trendText: `${Math.abs(numeric).toFixed(2)}% ${suffix}`,
      trendClass: 'up'
    }
  }

  if (numeric < 0) {
    return {
      trendArrow: '↓',
      trendText: `${Math.abs(numeric).toFixed(2)}% ${suffix}`,
      trendClass: 'down'
    }
  }

  return {
    trendArrow: '→',
    trendText: `0.00% ${suffix}`,
    trendClass: 'flat'
  }
}
</script>

<style scoped>
.dashboard-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 统计卡片网格 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 20px;
}

/* 现代卡片样式 */
.modern-card {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: 24px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.modern-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--primary-color), var(--primary-light));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.modern-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.modern-card:hover::before {
  opacity: 1;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin-top: 8px;
  letter-spacing: -1px;
}

.stat-trend {
  margin-top: 12px;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 20px;
}

.trend-arrow {
  font-size: 14px;
}

.stat-trend.up {
  color: var(--success-color);
  background: var(--success-light);
}

.stat-trend.down {
  color: var(--danger-color);
  background: var(--danger-light);
}

.stat-trend.flat {
  color: var(--text-secondary);
  background: var(--bg-tertiary);
}

.stat-desc {
  margin-top: 10px;
  font-size: 12px;
  color: var(--text-muted);
}

/* 图表卡片 */
.chart-box {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: 24px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  height: 400px;
  display: flex;
  flex-direction: column;
  transition: box-shadow 0.3s ease;
}

.chart-box:hover {
  box-shadow: var(--shadow-md);
}

.chart-title {
  font-weight: 600;
  font-size: 16px;
  margin-bottom: 16px;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.chart-title::before {
  content: '';
  width: 4px;
  height: 18px;
  background: var(--primary-color);
  border-radius: 2px;
}

.chart-instance {
  flex: 1;
  width: 100%;
}

.large {
  height: 440px;
}

/* 待办事项 */
.todo-box {
  height: auto;
  min-height: 280px;
}

.todo-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.todo-item {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 16px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.2s ease;
  background: var(--bg-secondary);
}

.todo-item:hover {
  border-color: var(--primary-light);
  background: var(--primary-lighter);
  transform: translateX(4px);
}

.todo-label {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
}

.todo-value {
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 700;
}

/* 元信息 */
.meta-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.meta-item {
  padding: 16px;
  border-radius: var(--radius-md);
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  gap: 12px;
  transition: all 0.2s ease;
}

.meta-item:hover {
  background: var(--bg-tertiary);
}

.meta-k {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
}

.meta-v {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}

/* 响应式布局 */
@media (max-width: 1600px) {
  .stat-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
  .todo-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1400px) {
  .stat-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .todo-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
  .todo-grid {
    grid-template-columns: 1fr;
  }
}
</style>
