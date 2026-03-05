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
          <div class="chart-title">待办构成分布</div>
          <div ref="todoPieRef" class="chart-instance"></div>
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
const todoPieRef = ref(null)
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
    bountyDisputed: 0
  },
  trend7d: [],
  recentOrders: [],
  generatedAt: ''
})

let mainChart = null
let todoPieChart = null
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
  initTodoPieChart()
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
  if (todoPieChart) {
    todoPieChart.dispose()
    todoPieChart = null
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
    renderTodoPieChart()
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

const initTodoPieChart = () => {
  if (!todoPieRef.value) return
  todoPieChart = echarts.init(todoPieRef.value)
  renderTodoPieChart()
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

const renderTodoPieChart = () => {
  if (!todoPieChart) return
  const todo = overview.value.todo || {}
  const data = [
    { name: '售后待处理', value: Number(todo.afterSalePending || 0) },
    { name: '提现待审核', value: Number(todo.withdrawPending || 0) },
    { name: '注销申请待审核', value: Number(todo.deletionPending || 0) },
    { name: '模型待审核', value: Number(todo.modelReviewPending || 0) },
    { name: '悬赏待审核', value: Number(todo.bountyReviewPending || 0) },
    { name: '管理员注册待审核', value: Number(todo.adminRegisterPending || 0) },
    { name: '设计者申请待审核', value: Number(todo.designerApplyPending || 0) },
    { name: '打印异常', value: Number(todo.printException || 0) },
    { name: '悬赏争议', value: Number(todo.bountyDisputed || 0) }
  ]

  todoPieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        name: '待办构成',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '45%'],
        label: { formatter: '{b}: {c}' },
        data
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
  if (todoPieChart) {
    todoPieChart.resize()
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

.stat-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.modern-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e2e8f0;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
  margin-top: 6px;
}

.stat-trend {
  margin-top: 8px;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
}

.trend-arrow {
  font-size: 12px;
}

.stat-trend.up {
  color: #10b981;
}

.stat-trend.down {
  color: #ef4444;
}

.stat-trend.flat {
  color: #64748b;
}

.stat-desc {
  margin-top: 8px;
  font-size: 12px;
  color: #64748b;
}

.chart-box {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #e2e8f0;
  height: 400px;
  display: flex;
  flex-direction: column;
}

.chart-title {
  font-weight: 700;
  margin-bottom: 14px;
  color: #1e293b;
}

.chart-instance {
  flex: 1;
  width: 100%;
}

.large {
  height: 420px;
}

.todo-box {
  height: auto;
  min-height: 240px;
}

.todo-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.todo-item {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.todo-item:hover {
  border-color: #c7d2fe;
  background: #f8faff;
}

.todo-label {
  color: #475569;
  font-size: 13px;
}

.todo-value {
  color: #1e293b;
  font-size: 20px;
  font-weight: 700;
}

.meta-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.meta-item {
  padding: 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.meta-k {
  color: #64748b;
  font-size: 13px;
}

.meta-v {
  color: #1e293b;
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 1600px) {
  .stat-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
