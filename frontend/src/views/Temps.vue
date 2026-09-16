<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;margin-bottom:14px;flex-wrap:wrap">
        <el-select v-model="filters.granaryId" placeholder="全部仓房" clearable style="width:160px">
          <el-option v-for="g in granaries" :key="g.id" :label="g.name" :value="g.id" />
        </el-select>
        <el-date-picker v-model="filters.recordDate" type="date" value-format="YYYY-MM-DD"
                        placeholder="测温日期" clearable style="width:160px" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="openCreate">登记测温</el-button>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column label="仓房" min-width="140">
          <template #default="{ row }">{{ granaryName(row.granaryId) }}</template>
        </el-table-column>
        <el-table-column prop="recordDate" label="测温日期" width="130" />
        <el-table-column prop="temperature" label="粮温(℃)" width="110" />
        <el-table-column prop="humidity" label="湿度%" width="100" />
        <el-table-column prop="recorder" label="记录人" width="110" />
        <el-table-column prop="result" label="结论" width="100">
          <template #default="{ row }">
            <el-tag :type="row.result === '超温' ? 'danger' : 'success'">{{ row.result }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" title="登记测温" width="470px">
      <el-form label-width="106px">
        <el-form-item label="仓房">
          <el-select v-model="form.granaryId" style="width:100%">
            <el-option v-for="g in granaries" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="测温日期">
          <el-date-picker v-model="form.recordDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="粮温(℃)">
          <el-input-number v-model="form.temperature" :min="-20" :max="60" :precision="1" :step="0.1" />
        </el-form-item>
        <el-form-item label="仓内湿度%">
          <el-input-number v-model="form.humidity" :min="0" :max="100" :precision="1" :step="0.1" />
        </el-form-item>
        <el-form-item label="记录人">
          <el-input v-model="form.recorder" />
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
import { tempApi, granaryApi } from '../api'

const rows = ref([])
const granaries = ref([])
const filters = ref({ granaryId: null, recordDate: null })
const visible = ref(false)
const form = ref({})

function granaryName(id) {
  const hit = granaries.value.find((g) => g.id === id)
  return hit ? hit.name : id
}

async function load() {
  try {
    rows.value = await tempApi.list({ ...filters.value })
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
  form.value = {}
  visible.value = true
}

async function save() {
  try {
    await tempApi.create(form.value)
    ElMessage.success('已登记')
    visible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  await loadGranaries()
  await load()
})
</script>
