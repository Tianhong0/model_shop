<template>
  <div class="coupon-list-page">
    <!-- 搜索和操作栏 -->
    <div class="search-bar">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="优惠券名称">
          <el-input v-model="searchForm.name" placeholder="请输入优惠券名称" clearable />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.type" placeholder="全部类型" clearable>
            <el-option label="满减券" :value="1" />
            <el-option label="折扣券" :value="2" />
            <el-option label="现金券" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" @click="handleAdd">新增优惠券</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="180" />
      <el-table-column prop="name" label="优惠券名称" min-width="150" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getTypeTagType(row.type)">{{ getTypeName(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="value" label="优惠值" width="100">
        <template #default="{ row }">
          {{ formatValue(row) }}
        </template>
      </el-table-column>
      <el-table-column prop="minAmount" label="门槛" width="100">
        <template #default="{ row }">
          {{ row.minAmount > 0 ? `满¥${row.minAmount}` : '无门槛' }}
        </template>
      </el-table-column>
      <el-table-column prop="pointCost" label="所需积分" width="100" />
      <el-table-column prop="stock" label="库存" width="120">
        <template #default="{ row }">
          {{ row.remainingStock }} / {{ row.totalStock }}
        </template>
      </el-table-column>
      <el-table-column prop="perUserLimit" label="限领" width="80" />
      <el-table-column prop="validDays" label="有效期" width="100">
        <template #default="{ row }">
          {{ row.validDays }}天
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button
            type="primary"
            size="small"
            link
            @click="handleEdit(row)"
          >
            编辑
          </el-button>
          <el-button
            :type="row.status === 1 ? 'warning' : 'success'"
            size="small"
            link
            @click="handleToggleStatus(row)"
          >
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button
            type="danger"
            size="small"
            link
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑优惠券' : '新增优惠券'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
        v-loading="dialogLoading"
      >
        <el-form-item label="优惠券名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入优惠券名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择类型">
            <el-option label="满减券" :value="1" />
            <el-option label="折扣券" :value="2" />
            <el-option label="现金券" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="优惠值" prop="value">
          <el-input-number
            v-model="formData.value"
            :min="0.01"
            :precision="2"
            :step="1"
            placeholder="请输入优惠值"
          />
          <div class="form-tip">
            <template v-if="formData.type === 1">满减金额，如 10 表示减 10 元</template>
            <template v-else-if="formData.type === 2">折扣比例，如 0.8 表示 8 折</template>
            <template v-else>现金金额，如 5 表示 5 元</template>
          </div>
        </el-form-item>
        <el-form-item label="最低门槛" prop="minAmount">
          <el-input-number
            v-model="formData.minAmount"
            :min="0"
            :precision="2"
            :step="10"
            placeholder="请输入最低消费门槛"
          />
          <div class="form-tip">0 表示无门槛</div>
        </el-form-item>
        <el-form-item v-if="formData.type === 2" label="最大优惠" prop="maxDiscount">
          <el-input-number
            v-model="formData.maxDiscount"
            :min="0"
            :precision="2"
            :step="10"
            placeholder="请输入最大优惠金额"
          />
          <div class="form-tip">折扣券最大优惠金额，不填则无上限</div>
        </el-form-item>
        <el-form-item label="所需积分" prop="pointCost">
          <el-input-number
            v-model="formData.pointCost"
            :min="1"
            :step="10"
            placeholder="请输入兑换所需积分"
          />
        </el-form-item>
        <el-form-item label="总库存" prop="totalStock">
          <el-input-number
            v-model="formData.totalStock"
            :min="1"
            :step="100"
            placeholder="请输入总库存"
          />
        </el-form-item>
        <el-form-item label="每人限领" prop="perUserLimit">
          <el-input-number
            v-model="formData.perUserLimit"
            :min="1"
            :max="10"
            placeholder="请输入每人限领数量"
          />
        </el-form-item>
        <el-form-item label="有效天数" prop="validDays">
          <el-input-number
            v-model="formData.validDays"
            :min="1"
            :max="365"
            placeholder="请输入有效天数"
          />
          <div class="form-tip">从用户领取日开始计算</div>
        </el-form-item>
        <el-form-item label="使用说明" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入使用说明"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getCouponTemplateList,
  getCouponTemplateDetail,
  createCouponTemplate,
  updateCouponTemplate,
  updateCouponTemplateStatus,
  deleteCouponTemplate
} from '@/api/coupon'

// 搜索表单
const searchForm = reactive({
  name: '',
  type: null,
  status: null
})

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 表格数据
const tableData = ref([])
const loading = ref(false)

// 对话框
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const isEdit = ref(false)
const editId = ref(null)

// 表单数据
const formData = reactive({
  name: '',
  type: 1,
  value: 10,
  minAmount: 0,
  maxDiscount: null,
  pointCost: 100,
  totalStock: 100,
  perUserLimit: 1,
  validDays: 30,
  description: ''
})

// 表单验证规则
const formRules = {
  name: [
    { required: true, message: '请输入优惠券名称', trigger: 'blur' },
    { max: 100, message: '优惠券名称最长100字符', trigger: 'blur' }
  ],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  value: [{ required: true, message: '请输入优惠值', trigger: 'blur' }],
  pointCost: [{ required: true, message: '请输入兑换所需积分', trigger: 'blur' }],
  totalStock: [{ required: true, message: '请输入总库存', trigger: 'blur' }]
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    }
    // 过滤空值
    Object.keys(params).forEach(key => {
      if (params[key] === '' || params[key] === null) {
        delete params[key]
      }
    })
    const res = await getCouponTemplateList(params)
    tableData.value = res?.records || []
    pagination.total = res?.total || 0
  } catch (error) {
    console.error('加载优惠券列表失败', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.name = ''
  searchForm.type = null
  searchForm.status = null
  handleSearch()
}

// 分页
const handleSizeChange = (val) => {
  pagination.pageSize = val
  loadData()
}

const handleCurrentChange = (val) => {
  pagination.pageNum = val
  loadData()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  editId.value = null
  // 重置表单
  Object.assign(formData, {
    name: '',
    type: 1,
    value: 10,
    minAmount: 0,
    maxDiscount: null,
    pointCost: 100,
    totalStock: 100,
    perUserLimit: 1,
    validDays: 30,
    description: ''
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = async (row) => {
  isEdit.value = true
  editId.value = row.id
  dialogLoading.value = true
  dialogVisible.value = true

  try {
    const detail = await getCouponTemplateDetail(row.id)
    Object.assign(formData, {
      name: detail.name || '',
      type: detail.type || 1,
      value: detail.value || 10,
      minAmount: detail.minAmount || 0,
      maxDiscount: detail.maxDiscount || null,
      pointCost: detail.pointCost || 100,
      totalStock: detail.totalStock || 100,
      perUserLimit: detail.perUserLimit || 1,
      validDays: detail.validDays || 30,
      description: detail.description || ''
    })
  } catch (error) {
    console.error('获取优惠券详情失败', error)
    ElMessage.error('获取优惠券详情失败')
    dialogVisible.value = false
  } finally {
    dialogLoading.value = false
  }
}

// 提交
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (isEdit.value) {
        await updateCouponTemplate(editId.value, formData)
        ElMessage.success('更新成功')
      } else {
        await createCouponTemplate(formData)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (error) {
      console.error(isEdit.value ? '更新优惠券失败' : '创建优惠券失败', error)
    } finally {
      submitLoading.value = false
    }
  })
}

// 切换状态
const handleToggleStatus = (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const statusText = newStatus === 1 ? '启用' : '禁用'
  ElMessageBox.confirm(`确定要${statusText}该优惠券吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await updateCouponTemplateStatus(row.id, newStatus)
      ElMessage.success(`${statusText}成功`)
      loadData()
    } catch (error) {
      console.error('更新状态失败', error)
    }
  }).catch(() => {})
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该优惠券吗？删除后无法恢复。', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteCouponTemplate(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch (error) {
      console.error('删除优惠券失败', error)
    }
  }).catch(() => {})
}

// 工具方法
const getTypeName = (type) => {
  const map = { 1: '满减券', 2: '折扣券', 3: '现金券' }
  return map[type] || '未知'
}

const getTypeTagType = (type) => {
  const map = { 1: 'primary', 2: 'warning', 3: 'success' }
  return map[type] || 'info'
}

const formatValue = (row) => {
  if (row.type === 1 || row.type === 3) {
    return `¥${row.value}`
  } else if (row.type === 2) {
    return `${row.value * 10}折`
  }
  return row.value
}

const formatTime = (time) => {
  if (!time) return '-'
  return String(time).replace('T', ' ').slice(0, 19)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.coupon-list-page {
  padding: 20px;
}

.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.coupon-list-page :deep(.el-input-number) {
  width: 200px;
}

.coupon-list-page :deep(.el-select) {
  width: 200px;
}
</style>
