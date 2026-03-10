<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="query.keyword" clearable placeholder="订单号/商品标题" style="width: 240px" />
          <el-input-number v-model="query.buyerId" :min="1" controls-position="right" placeholder="买家ID" style="width: 160px" />
          <el-input-number v-model="query.sellerId" :min="1" controls-position="right" placeholder="卖家ID" style="width: 160px" />
          <el-select v-model="query.status" clearable placeholder="订单状态" style="width: 150px">
            <el-option label="待支付" :value="0" />
            <el-option label="待发货" :value="1" />
            <el-option label="待收货" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已取消" :value="4" />
            <el-option label="售后中" :value="5" />
          </el-select>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-space>
      </div>

      <el-table v-loading="loading" :data="records" stripe border highlight-current-row>
        <el-table-column prop="id" label="订单ID" width="150" />
        <el-table-column prop="orderSn" label="订单号" width="210" />
        <el-table-column prop="listingTitle" label="商品标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="buyerNickname" label="买家" width="140" />
        <el-table-column prop="sellerNickname" label="卖家" width="140" />
        <el-table-column label="金额" width="120">
          <template #default="scope">￥{{ Number(scope.row.orderAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope"><el-tag :type="statusType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="showDetail(scope.row)">详情</el-button>
            <el-button v-if="scope.row.status === 1" link type="primary" @click="openShip(scope.row)">代发货</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          :page-count="Math.max(1, Math.ceil((total || 0) / query.pageSize))"
          :hide-on-single-page="false"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="二手订单详情" width="900px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="订单号">{{ detail.orderSn }}</el-descriptions-item>
        <el-descriptions-item label="商品标题">{{ detail.listingTitle }}</el-descriptions-item>
        <el-descriptions-item label="买家">{{ detail.buyerNickname }}</el-descriptions-item>
        <el-descriptions-item label="卖家">{{ detail.sellerNickname }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">￥{{ Number(detail.orderAmount || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ detail.receiverName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">{{ detail.receiverAddress }}</el-descriptions-item>
        <el-descriptions-item label="物流公司">{{ detail.deliveryCompany || '-' }}</el-descriptions-item>
        <el-descriptions-item label="物流单号">{{ detail.deliverySn || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ detail.payTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider v-if="detail?.afterSale" />
      <el-descriptions v-if="detail?.afterSale" :column="2" border title="售后信息">
        <el-descriptions-item label="售后单号">{{ detail.afterSale.afterSaleSn }}</el-descriptions-item>
        <el-descriptions-item label="售后状态">{{ afterSaleText(detail.afterSale.status) }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ afterSaleType(detail.afterSale.type) }}</el-descriptions-item>
        <el-descriptions-item label="申请金额">￥{{ Number(detail.afterSale.requestedAmount || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="原因" :span="2">{{ detail.afterSale.reason }}</el-descriptions-item>
        <el-descriptions-item label="卖家备注" :span="2">{{ detail.afterSale.sellerRemark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="shipVisible" title="平台代发货" width="520px">
      <el-form :model="shipForm" label-width="90px">
        <el-form-item label="订单号"><span>{{ shipForm.orderSn }}</span></el-form-item>
        <el-form-item label="物流公司"><el-input v-model="shipForm.deliveryCompany" /></el-form-item>
        <el-form-item label="物流单号"><el-input v-model="shipForm.deliverySn" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipLoading" @click="submitShip">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminShipUsedOrder, getAdminUsedOrderDetail, getAdminUsedOrderPage } from '../../api/used'

const loading = ref(false)
const total = ref(0)
const records = ref([])
const detailVisible = ref(false)
const detail = ref(null)
const shipVisible = ref(false)
const shipLoading = ref(false)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  buyerId: null,
  sellerId: null,
  status: null
})

const shipForm = reactive({
  orderId: null,
  orderSn: '',
  deliveryCompany: '',
  deliverySn: ''
})

const statusText = (status) => ({ 0: '待支付', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消', 5: '售后中' }[Number(status)] || '未知')
const statusType = (status) => ({ 0: 'warning', 1: 'primary', 2: 'info', 3: 'success', 4: 'danger', 5: 'warning' }[Number(status)] || '')
const afterSaleText = (status) => ({ 0: '待处理', 1: '已同意', 2: '已拒绝', 3: '平台介入', 4: '已退款', 5: '已关闭' }[Number(status)] || '未知')
const afterSaleType = (type) => ({ 1: '仅退款', 2: '退货退款', 3: '协商补偿' }[Number(type)] || '未知')

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getAdminUsedOrderPage({ ...query })
    records.value = res.records || []
    total.value = Number(res.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '获取订单失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  Object.assign(query, { pageNum: 1, pageSize: 10, keyword: '', buyerId: null, sellerId: null, status: null })
  fetchList()
}

const handlePageChange = (pageNum) => {
  query.pageNum = pageNum
  fetchList()
}

const handleSizeChange = (pageSize) => {
  query.pageSize = pageSize
  query.pageNum = 1
  fetchList()
}

const showDetail = async (row) => {
  detail.value = await getAdminUsedOrderDetail(row.id)
  detailVisible.value = true
}

const openShip = (row) => {
  shipForm.orderId = row.id
  shipForm.orderSn = row.orderSn
  shipForm.deliveryCompany = ''
  shipForm.deliverySn = ''
  shipVisible.value = true
}

const submitShip = async () => {
  shipLoading.value = true
  try {
    await adminShipUsedOrder({ orderId: shipForm.orderId, deliveryCompany: shipForm.deliveryCompany, deliverySn: shipForm.deliverySn })
    ElMessage.success('发货成功')
    shipVisible.value = false
    fetchList()
  } catch (error) {
    ElMessage.error(error.message || '发货失败')
  } finally {
    shipLoading.value = false
  }
}

onMounted(fetchList)
</script>

<style scoped>
.page-container { padding: 0; }
.table-card { background: #fff; padding: 24px; border-radius: 16px; border: 1px solid #e2e8f0; }
.header-actions { display: flex; justify-content: space-between; margin-bottom: 20px; }
.pagination-container { display: flex; justify-content: flex-end; margin-top: 20px; }
</style>
