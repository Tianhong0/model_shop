<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <h3 style="margin: 0;">批量打印折扣配置</h3>
        <el-space>
          <el-button type="primary" @click="addRule">添加规则</el-button>
          <el-button type="success" :loading="saving" @click="saveRules">保存配置</el-button>
        </el-space>
      </div>

      <el-alert
        title="配置说明"
        type="info"
        :closable="false"
        style="margin-bottom: 20px;"
      >
        <template #default>
          <p>• 根据用户购买数量自动计算折扣优惠</p>
          <p>• 数量区间不能重叠，系统会自动匹配最优惠的规则</p>
          <p>• 折扣百分比：100表示无折扣，90表示9折，80表示8折</p>
        </template>
      </el-alert>

      <el-table v-loading="loading" :data="rules" border stripe>
        <el-table-column label="数量区间" min-width="200">
          <template #default="scope">
            <el-space>
              <el-input-number v-model="scope.row.minQuantity" :min="1" size="small" style="width: 100px" />
              <span>~</span>
              <el-input-number v-model="scope.row.maxQuantity" :min="0" size="small" style="width: 100px" placeholder="不限" />
            </el-space>
          </template>
        </el-table-column>
        <el-table-column label="折扣百分比" width="180">
          <template #default="scope">
            <el-input-number v-model="scope.row.discountPercent" :min="1" :max="100" size="small" style="width: 120px" />
            <span style="margin-left: 8px;">%</span>
          </template>
        </el-table-column>
        <el-table-column label="描述" min-width="200">
          <template #default="scope">
            <el-input v-model="scope.row.description" placeholder="如：2-4件 95折" />
          </template>
        </el-table-column>
        <el-table-column label="排序" width="100">
          <template #default="scope">
            <el-input-number v-model="scope.row.sortOrder" :min="0" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-switch v-model="scope.row.isActive" :active-value="1" :inactive-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="scope">
            <el-button link type="danger" @click="removeRule(scope.$index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="preview-section">
        <h4>价格计算示例</h4>
        <div class="preview-form">
          <el-input-number v-model="previewQuantity" :min="1" placeholder="输入数量" style="width: 150px" />
          <el-input-number v-model="previewPrice" :min="0" :precision="2" placeholder="输入单价" style="width: 150px" />
          <el-button type="primary" @click="calculatePreview">计算</el-button>
        </div>
        <div v-if="previewResult" class="preview-result">
          <p>原价：¥{{ previewResult.originalTotal }}</p>
          <p>折扣：{{ previewResult.discountPercent }}%</p>
          <p>优惠后总价：¥{{ previewResult.totalAmount }}</p>
          <p>节省：¥{{ previewResult.savedAmount }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getBatchDiscountListApi, saveBatchDiscountApi } from '../../api/groupBuy'

const loading = ref(false)
const saving = ref(false)
const rules = ref([])

const previewQuantity = ref(1)
const previewPrice = ref(100)
const previewResult = ref(null)

const fetchRules = async () => {
  loading.value = true
  try {
    const res = await getBatchDiscountListApi()
    rules.value = (res || []).map(r => ({
      ...r,
      isActive: r.isActive ?? 1
    }))
    if (rules.value.length === 0) {
      // 初始化默认规则
      rules.value = [
        { minQuantity: 2, maxQuantity: 4, discountPercent: 95, description: '2-4件 95折', isActive: 1, sortOrder: 1 },
        { minQuantity: 5, maxQuantity: 9, discountPercent: 90, description: '5-9件 9折', isActive: 1, sortOrder: 2 },
        { minQuantity: 10, maxQuantity: 19, discountPercent: 85, description: '10-19件 85折', isActive: 1, sortOrder: 3 },
        { minQuantity: 20, maxQuantity: null, discountPercent: 80, description: '20件以上 8折', isActive: 1, sortOrder: 4 }
      ]
    }
  } finally {
    loading.value = false
  }
}

const addRule = () => {
  rules.value.push({
    minQuantity: 1,
    maxQuantity: null,
    discountPercent: 100,
    description: '',
    isActive: 1,
    sortOrder: rules.value.length + 1
  })
}

const removeRule = (index) => {
  rules.value.splice(index, 1)
}

const validateRules = () => {
  for (let i = 0; i < rules.value.length; i++) {
    const rule = rules.value[i]
    if (!rule.minQuantity || rule.minQuantity < 1) {
      ElMessage.warning(`第${i + 1}条规则的最小数量必须大于0`)
      return false
    }
    if (rule.maxQuantity !== null && rule.maxQuantity < rule.minQuantity) {
      ElMessage.warning(`第${i + 1}条规则的最大数量不能小于最小数量`)
      return false
    }
    if (!rule.discountPercent || rule.discountPercent < 1 || rule.discountPercent > 100) {
      ElMessage.warning(`第${i + 1}条规则的折扣百分比必须在1-100之间`)
      return false
    }
  }
  return true
}

const saveRules = async () => {
  if (!validateRules()) return

  saving.value = true
  try {
    await saveBatchDiscountApi(rules.value)
    ElMessage.success('保存成功')
    fetchRules()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const calculatePreview = () => {
  const quantity = previewQuantity.value
  const price = previewPrice.value

  if (!quantity || !price) {
    ElMessage.warning('请输入数量和单价')
    return
  }

  // 找到匹配的折扣规则
  let matchedRule = null
  for (const rule of rules.value) {
    if (!rule.isActive) continue
    if (quantity >= rule.minQuantity) {
      if (rule.maxQuantity === null || quantity <= rule.maxQuantity) {
        matchedRule = rule
        break
      }
    }
  }

  const discountPercent = matchedRule ? matchedRule.discountPercent : 100
  const originalTotal = price * quantity
  const totalAmount = originalTotal * discountPercent / 100
  const savedAmount = originalTotal - totalAmount

  previewResult.value = {
    originalTotal: originalTotal.toFixed(2),
    discountPercent,
    totalAmount: totalAmount.toFixed(2),
    savedAmount: savedAmount.toFixed(2)
  }
}

onMounted(fetchRules)
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
}

.preview-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid var(--border-light);
}

.preview-section h4 {
  margin-bottom: 16px;
  color: #333;
}

.preview-form {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.preview-result {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
}

.preview-result p {
  margin: 8px 0;
  color: #666;
}

.preview-result p:first-child {
  margin-top: 0;
}

.preview-result p:last-child {
  margin-bottom: 0;
}
</style>
