<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;margin-bottom:14px;flex-wrap:wrap">
        <el-select v-model="filters.granaryId" placeholder="全部仓房" clearable style="width:160px">
          <el-option v-for="g in granaries" :key="g.id" :label="g.name" :value="g.id" />
        </el-select>
        <el-select v-model="filters.variety" placeholder="全部品种" clearable style="width:130px">
          <el-option v-for="v in varieties" :key="v" :label="v" :value="v" />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:130px">
          <el-option label="在储" value="在储" />
          <el-option label="已出库" value="已出库" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="openCreate">入库新批次</el-button>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="code" label="批次号" width="130" />
        <el-table-column prop="variety" label="品种" width="90" />
        <el-table-column label="所在仓房" width="120">
          <template #default="{ row }">{{ granaryName(row.granaryId) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="在库（吨）" width="110" />
        <el-table-column prop="inDate" label="入库日期" width="120" />
        <el-table-column prop="moisture" label="水分%" width="90" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '在储' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="form.id ? '编辑批次' : '入库新批次'" width="480px">
      <el-form label-width="106px">
        <el-form-item label="批次号">
          <el-input v-model="form.code" :disabled="!!form.id" placeholder="如 GB-2026-05" />
        </el-form-item>
        <el-form-item label="品种">
          <el-select v-model="form.variety" style="width:100%">
            <el-option v-for="v in varieties" :key="v" :label="v" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!form.id" label="入哪个仓">
          <el-select v-model="form.granaryId" style="width:100%">
            <el-option v-for="g in granaries" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!form.id" label="入库吨数">
          <el-input-number v-model="form.quantity" :min="1" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="入库日期">
          <el-date-picker v-model="form.inDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="水分%">
          <el-input-number v-model="form.moisture" :min="0" :max="30" :precision="1" :step="0.1" />
        </el-form-item>
        <el-form-item v-if="form.id" label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="在储" value="在储" />
            <el-option label="已出库" value="已出库" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { batchApi, granaryApi } from '../api'

const varieties = ['小麦', '玉米', '稻谷', '大豆']
const rows = ref([])
const granaries = ref([])
const filters = ref({ granaryId: null, variety: '', status: '' })
const visible = ref(false)
const form = ref({})

function granaryName(id) {
  const hit = granaries.value.find((g) => g.id === id)
  return hit ? hit.name : id
}

async function load() {
  try {
    rows.value = await batchApi.list({ ...filters.value })
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function loadGranaries() {
  try {
    granaries.value = await granaryApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openCreate() {
  form.value = { variety: '小麦', moisture: 12.0 }
  visible.value = true
}

function openEdit(row) {
  form.value = { ...row }
  visible.value = true
}

async function save() {
  try {
    if (form.value.id) {
      await batchApi.update(form.value.id, form.value)
    } else {
      await batchApi.create(form.value)
    }
    ElMessage.success('已保存')
    visible.value = false
    await load()
    await loadGranaries()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  await loadGranaries()
  await load()
})
</script>
