<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;margin-bottom:14px;flex-wrap:wrap">
        <el-select v-model="filters.batchId" placeholder="全部批次" clearable style="width:190px">
          <el-option v-for="b in batches" :key="b.id" :label="`${b.code}（${b.quantity}吨）`" :value="b.id" />
        </el-select>
        <el-select v-model="filters.moveType" placeholder="全部类型" clearable style="width:130px">
          <el-option label="入库" value="入库" />
          <el-option label="出库" value="出库" />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:130px">
          <el-option label="待执行" value="待执行" />
          <el-option label="已完成" value="已完成" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="openCreate">开作业单</el-button>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="code" label="作业单号" width="130" />
        <el-table-column label="批次" min-width="170">
          <template #default="{ row }">{{ batchName(row.batchId) }}</template>
        </el-table-column>
        <el-table-column prop="moveType" label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.moveType === '入库' ? 'success' : 'warning'">{{ row.moveType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="吨数" width="90" />
        <el-table-column prop="moveDate" label="作业日期" width="120" />
        <el-table-column prop="operator" label="经办人" width="110" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '已完成' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="row.status === '待执行'" link type="primary" @click="execute(row)">
              执行
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" title="开作业单" width="480px">
      <el-form label-width="106px">
        <el-form-item label="作业单号">
          <el-input v-model="form.code" placeholder="如 SM-0003" />
        </el-form-item>
        <el-form-item label="批次">
          <el-select v-model="form.batchId" style="width:100%">
            <el-option v-for="b in batches" :key="b.id" :label="`${b.code}（在库 ${b.quantity} 吨）`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.moveType" style="width:100%">
            <el-option label="入库" value="入库" />
            <el-option label="出库" value="出库" />
          </el-select>
        </el-form-item>
        <el-form-item label="吨数">
          <el-input-number v-model="form.quantity" :min="1" />
        </el-form-item>
        <el-form-item label="作业日期">
          <el-date-picker v-model="form.moveDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="经办人">
          <el-input v-model="form.operator" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { moveApi, batchApi } from '../api'

const rows = ref([])
const batches = ref([])
const filters = ref({ batchId: null, moveType: '', status: '' })
const visible = ref(false)
const form = ref({})

function batchName(id) {
  const hit = batches.value.find((b) => b.id === id)
  return hit ? `${hit.code} ${hit.variety}` : id
}

async function load() {
  try {
    rows.value = await moveApi.list({ ...filters.value })
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function loadBatches() {
  try {
    batches.value = await batchApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openCreate() {
  form.value = { moveType: '出库' }
  visible.value = true
}

async function save() {
  try {
    await moveApi.create(form.value)
    ElMessage.success('已开单')
    visible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function execute(row) {
  try {
    await moveApi.execute(row.id)
    ElMessage.success('已执行')
    await load()
    await loadBatches()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  await loadBatches()
  await load()
})
</script>
