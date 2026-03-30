<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="query.afterSaleSn" placeholder="售后单号" style="width: 220px" clearable>
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input v-model="query.orderSn" placeholder="订单号" style="width: 220px" clearable />
          <el-select v-model="query.status" placeholder="售后状态" clearable style="width: 160px">
            <el-option label="已申请" :value="0" />
            <el-option label="审核中" :value="1" />
            <el-option label="处理中" :value="2" />
            <el-option label="退款中" :value="3" />
            <el-option label="已完成" :value="4" />
            <el-option label="已拒绝" :value="5" />
            <el-option label="已取消" :value="6" />
          </el-select>
          <el-button type="primary" @click="loadList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-space>
      </div>

      <el-table :data="tableData" stripe border highlight-current-row style="width: 100%">
        <el-table-column prop="afterSaleSn" label="售后单号" min-width="180" />
        <el-table-column prop="orderSn" label="关联订单" min-width="160" />
        <el-table-column prop="type" label="申请类型" width="120">
          <template #default="scope">
            <el-tag>{{ typeMap[scope.row.type] || scope.row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="申请原因" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="statusMap[scope.row.status]?.type || 'info'">{{ statusMap[scope.row.status]?.label || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestedAmount" label="申请金额" width="120" />
        <el-table-column prop="approvedAmount" label="审核金额" width="120" />
        <el-table-column prop="refundStatus" label="退款状态" width="120">
          <template #default="scope">
            <el-tag :type="refundStatusMap[scope.row.refundStatus]?.type || 'info'">{{ refundStatusMap[scope.row.refundStatus]?.label || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" min-width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="scope">
            <el-button link @click="openDetail(scope.row)">详情</el-button>
            <el-button link type="primary" :disabled="!canAudit(scope.row)" @click="openAudit(scope.row)">审核</el-button>
            <el-button link type="success" :disabled="!canRefund(scope.row)" @click="openRefund(scope.row)">退款</el-button>
            <el-button link @click="openMessage(scope.row)">留言</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          :page-count="Math.max(1, Math.ceil((total || 0) / query.pageSize))"
          :hide-on-single-page="false"
          @size-change="onSizeChange"
          @current-change="onPageChange"
        />
      </div>
    </div>

    <el-dialog v-model="auditDialogVisible" title="售后审核" width="520px">
      <el-form label-width="90px">
        <el-form-item label="售后单号">
          <span>{{ currentRow?.afterSaleSn || '-' }}</span>
        </el-form-item>
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.approved">
            <el-radio :label="true">通过</el-radio>
            <el-radio :label="false">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核金额" v-if="isRefundType(currentRow?.type)">
          <el-input-number v-model="auditForm.approvedAmount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="auditForm.adminRemark" type="textarea" placeholder="请输入审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAudit">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="refundDialogVisible" title="执行退款" width="460px">
      <el-form label-width="90px">
        <el-form-item label="售后单号">
          <span>{{ currentRow?.afterSaleSn || '-' }}</span>
        </el-form-item>
        <el-form-item label="退款金额">
          <el-input-number v-model="refundForm.refundAmount" :min="0.01" :precision="2" />
        </el-form-item>
        <el-form-item label="退款原因">
          <el-input v-model="refundForm.refundReason" type="textarea" placeholder="请输入退款原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRefund">确认退款</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="messageDialogVisible" title="协商留言" width="620px">
      <el-scrollbar height="300px" class="msg-scroll">
        <div class="msg-list">
          <div class="msg-item" v-for="item in messageList" :key="item.id">
            <div class="msg-meta">{{ item.senderRole }} · {{ item.createTime }}</div>
            <div class="msg-content">{{ item.content }}</div>
          </div>
        </div>
      </el-scrollbar>
      <el-input v-model="messageForm.content" type="textarea" :rows="3" placeholder="请输入回复内容" />
      <template #footer>
        <el-button @click="messageDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="submitMessage">发送</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="售后详情" width="760px">
      <div class="detail-grid" v-if="detailData">
        <div class="detail-item"><span>售后单号：</span>{{ detailData.afterSaleSn || '-' }}</div>
        <div class="detail-item"><span>订单号：</span>{{ detailData.orderSn || '-' }}</div>
        <div class="detail-item"><span>申请类型：</span>{{ typeMap[detailData.type] || detailData.type || '-' }}</div>
        <div class="detail-item"><span>售后状态：</span>{{ statusMap[detailData.status]?.label || '-' }}</div>
        <div class="detail-item"><span>申请金额：</span>{{ detailData.requestedAmount ?? '-' }}</div>
        <div class="detail-item"><span>审核金额：</span>{{ detailData.approvedAmount ?? '-' }}</div>
      </div>
      <div class="detail-block" v-if="detailData">
        <div class="detail-label">申请原因</div>
        <div class="detail-content">{{ detailData.reason || '-' }}</div>
      </div>
      <div class="detail-block" v-if="detailData">
        <div class="detail-label">问题描述</div>
        <div class="detail-content">{{ detailData.description || '-' }}</div>
      </div>
      <div class="detail-block">
        <div class="detail-label">凭证材料</div>
        <div class="evidence-empty" v-if="!evidenceList.length">暂无凭证</div>
        <div class="evidence-list" v-else>
          <div class="evidence-item" v-for="url in evidenceList" :key="url">
            <template v-if="isVideoUrl(url)">
              <video class="evidence-video" controls preload="metadata" playsinline webkit-playsinline>
                <source :src="url" :type="getVideoMimeType(url)" />
              </video>
              <a class="evidence-link" :href="url" target="_blank" rel="noopener noreferrer">无法播放？点击下载视频</a>
            </template>
            <el-image v-else class="evidence-image" :src="url" :preview-src-list="evidenceImageList" fit="cover" />
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  auditAfterSale,
  getAfterSaleAdminDetail,
  getAfterSaleAdminList,
  getAfterSaleMessages,
  refundAfterSale,
  sendAfterSaleMessage
} from '../../api/order'

const typeMap = {
  1: '仅退款',
  2: '退货退款',
  3: '补打',
  4: '换货'
}

const statusMap = {
  0: { label: '已申请', type: 'warning' },
  1: { label: '审核中', type: 'primary' },
  2: { label: '处理中', type: 'info' },
  3: { label: '退款中', type: 'danger' },
  4: { label: '已完成', type: 'success' },
  5: { label: '已拒绝', type: 'info' },
  6: { label: '已取消', type: 'info' }
}

const refundStatusMap = {
  0: { label: '未退款', type: 'info' },
  1: { label: '退款中', type: 'warning' },
  2: { label: '已退款', type: 'success' },
  3: { label: '退款失败', type: 'danger' }
}

const query = ref({
  pageNum: 1,
  pageSize: 10,
  status: null,
  orderSn: '',
  afterSaleSn: ''
})

const tableData = ref([])
const total = ref(0)
const currentRow = ref(null)

const auditDialogVisible = ref(false)
const refundDialogVisible = ref(false)
const messageDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const detailData = ref(null)

const auditForm = ref({
  approved: true,
  approvedAmount: 0,
  adminRemark: ''
})

const refundForm = ref({
  refundAmount: 0,
  refundReason: ''
})

const messageForm = ref({ content: '' })
const messageList = ref([])
const wsInstance = ref(null)
const wsConnected = ref(false)
const wsManualClose = ref(false)

const splitEvidenceUrls = (evidenceUrls) => {
  return String(evidenceUrls || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
}

const evidenceList = ref([])
const evidenceImageList = ref([])

const isVideoUrl = (url) => {
  const lower = String(url || '').toLowerCase().split('?')[0]
  return /\.(mp4|mov|m4v|webm|ogg)$/i.test(lower) || lower.includes('/videos/')
}

const getVideoMimeType = (url) => {
  const lower = String(url || '').toLowerCase().split('?')[0]
  if (lower.endsWith('.mov')) return 'video/quicktime'
  if (lower.endsWith('.webm')) return 'video/webm'
  if (lower.endsWith('.ogg')) return 'video/ogg'
  return 'video/mp4'
}

const getWsUrl = (afterSaleId) => {
  const token = localStorage.getItem('token') || ''
  const base = 'http://localhost:9999'
  const wsBase = base.startsWith('https://') ? base.replace('https://', 'wss://') : base.replace('http://', 'ws://')
  return `${wsBase}/ws/after-sale/message?afterSaleId=${encodeURIComponent(afterSaleId)}&token=${encodeURIComponent(token)}`
}

const closeMessageWs = () => {
  wsManualClose.value = true
  if (wsInstance.value) {
    try {
      wsInstance.value.close()
    } catch (_) {
      // ignore
    }
  }
  wsInstance.value = null
  wsConnected.value = false
}

const connectMessageWs = (afterSaleId) => {
  closeMessageWs()
  wsManualClose.value = false
  const ws = new WebSocket(getWsUrl(afterSaleId))
  ws.onopen = () => {
    wsConnected.value = true
  }
  ws.onmessage = (event) => {
    if (!event?.data) return
    try {
      const payload = JSON.parse(event.data)
      if (payload?.eventType !== 'NEW_MESSAGE') return
      if (Number(payload?.afterSaleId) !== Number(currentRow.value?.id)) return
      const incoming = payload.message
      if (!incoming?.id) return
      if (messageList.value.some(item => Number(item.id) === Number(incoming.id))) return
      messageList.value.push(incoming)
    } catch (_) {
      // ignore non-json payload
    }
  }
  ws.onclose = () => {
    wsConnected.value = false
    if (!wsManualClose.value && messageDialogVisible.value && Number(currentRow.value?.id) === Number(afterSaleId)) {
      setTimeout(() => connectMessageWs(afterSaleId), 2500)
    }
  }
  ws.onerror = () => {
    wsConnected.value = false
  }
  wsInstance.value = ws
}

const isRefundType = (type) => Number(type) === 1 || Number(type) === 2
const canAudit = (row) => [0, 1].includes(Number(row?.status))
const canRefund = (row) => {
  const status = Number(row?.status)
  const refundStatus = Number(row?.refundStatus)
  if (refundStatus === 2) return false
  return status === 3
}

const loadList = async () => {
  const data = await getAfterSaleAdminList(query.value)
  tableData.value = data?.records || []
  total.value = Number(data?.total || 0)
  query.value.pageNum = Number(data?.pageNum || query.value.pageNum)
  query.value.pageSize = Number(data?.pageSize || query.value.pageSize)
}

const resetQuery = () => {
  query.value = {
    pageNum: 1,
    pageSize: 10,
    status: null,
    orderSn: '',
    afterSaleSn: ''
  }
  loadList()
}

const onPageChange = (page) => {
  query.value.pageNum = page
  loadList()
}

const onSizeChange = (size) => {
  query.value.pageSize = size
  query.value.pageNum = 1
  loadList()
}

const openAudit = (row) => {
  if (!canAudit(row)) {
    ElMessage.warning('当前状态不可审核')
    return
  }
  currentRow.value = row
  auditForm.value = {
    approved: true,
    approvedAmount: Number(row?.requestedAmount || 0),
    adminRemark: ''
  }
  auditDialogVisible.value = true
}

const openDetail = async (row) => {
  const detail = await getAfterSaleAdminDetail(row.id)
  detailData.value = detail || null
  evidenceList.value = splitEvidenceUrls(detail?.evidenceUrls)
  evidenceImageList.value = evidenceList.value.filter(url => !isVideoUrl(url))
  detailDialogVisible.value = true
}

const submitAudit = async () => {
  if (!canAudit(currentRow.value)) {
    ElMessage.warning('当前状态不可审核')
    auditDialogVisible.value = false
    await loadList()
    return
  }
  await auditAfterSale({
    afterSaleId: currentRow.value.id,
    approved: auditForm.value.approved,
    approvedAmount: isRefundType(currentRow.value?.type) ? Number(auditForm.value.approvedAmount || 0) : null,
    adminRemark: auditForm.value.adminRemark
  })
  ElMessage.success('审核成功')
  auditDialogVisible.value = false
  await loadList()
}

const openRefund = (row) => {
  if (!canRefund(row)) {
    ElMessage.warning('当前状态不可退款或已退款')
    return
  }
  currentRow.value = row
  refundForm.value = {
    refundAmount: Number(row?.approvedAmount || row?.requestedAmount || 0),
    refundReason: ''
  }
  refundDialogVisible.value = true
}

const submitRefund = async () => {
  if (!canRefund(currentRow.value)) {
    ElMessage.warning('当前状态不可退款或已退款')
    refundDialogVisible.value = false
    await loadList()
    return
  }
  await refundAfterSale({
    afterSaleId: currentRow.value.id,
    refundAmount: Number(refundForm.value.refundAmount || 0),
    refundReason: refundForm.value.refundReason
  })
  ElMessage.success('退款执行成功')
  refundDialogVisible.value = false
  await loadList()
}

const loadMessageHistory = async (afterSaleId) => {
  const data = await getAfterSaleMessages({
    afterSaleId,
    pageNum: 1,
    pageSize: 50
  })
  messageList.value = (data?.records || []).slice().reverse()
}

const openMessage = async (row) => {
  currentRow.value = row
  messageForm.value.content = ''
  await loadMessageHistory(row.id)
  connectMessageWs(row.id)
  messageDialogVisible.value = true
}

const submitMessage = async () => {
  if (!messageForm.value.content) {
    ElMessage.warning('请输入留言内容')
    return
  }
  await sendAfterSaleMessage({
    afterSaleId: currentRow.value.id,
    content: messageForm.value.content,
    messageType: 1
  })
  messageForm.value.content = ''
  if (!wsConnected.value) {
    await loadMessageHistory(currentRow.value.id)
  }
}

watch(messageDialogVisible, (visible) => {
  if (!visible) {
    closeMessageWs()
  }
})

onBeforeUnmount(() => {
  closeMessageWs()
})

onMounted(() => {
  loadList()
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
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-light);
  flex-wrap: wrap;
  gap: 16px;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border-light);
}

.msg-scroll {
  margin-bottom: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.msg-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.msg-item {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 12px;
  transition: all 0.2s ease;
}

.msg-item:hover {
  background: var(--bg-tertiary);
}

.msg-meta {
  color: var(--text-secondary);
  font-size: 12px;
  margin-bottom: 6px;
}

.msg-content {
  color: var(--text-primary);
  font-size: 14px;
  white-space: pre-wrap;
  line-height: 1.5;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 16px;
  margin-bottom: 16px;
}

.detail-item {
  color: var(--text-primary);
  font-size: 14px;
}

.detail-item span {
  color: var(--text-secondary);
}

.detail-block {
  margin-top: 16px;
}

.detail-label {
  color: var(--text-secondary);
  font-size: 13px;
  margin-bottom: 8px;
  font-weight: 500;
}

.detail-content {
  color: var(--text-primary);
  font-size: 14px;
  white-space: pre-wrap;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 12px;
  line-height: 1.6;
}

.evidence-empty {
  color: var(--text-muted);
  font-size: 13px;
  text-align: center;
  padding: 20px;
}

.evidence-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.evidence-item {
  width: 220px;
}

.evidence-image,
.evidence-video {
  width: 220px;
  height: 140px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  background: #000;
  display: block;
}

.evidence-link {
  display: block;
  margin-top: 8px;
  color: var(--primary-color);
  font-size: 12px;
  text-decoration: none;
  transition: color 0.2s ease;
}

.evidence-link:hover {
  color: var(--primary-light);
}
</style>
