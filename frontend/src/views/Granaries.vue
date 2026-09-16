<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;margin-bottom:14px;flex-wrap:wrap">
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:140px">
          <el-option label="空仓" value="空仓" />
          <el-option label="在储" value="在储" />
          <el-option label="维修" value="维修" />
        </el-select>
        <el-input v-model="filters.keyword" placeholder="编号或名称" clearable style="width:190px" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="openCreate">新增仓房</el-button>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="code" label="编号" width="105" />
        <el-table-column prop="name" label="仓房" min-width="150" />
        <el-table-column label="仓容（吨）" width="120">
          <template #default="{ row }">{{ row.capacity }}</template>
        </el-table-column>
        <el-table-column label="当前在储（吨）" width="140">
          <template #default="{ row }">
            <span :style="stored(row.id) > row.capacity ? 'color:#f56c6c;font-weight:500' : ''">
              {{ stored(row.id) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '维修' ? 'danger' : row.status === '在储' ? 'warning' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="form.id ? '编辑仓房' : '新增仓房'" width="460px">
      <el-form label-width="106px">
        <el-form-item label="编号">
          <el-input v-model="form.code" :disabled="!!form.id" placeholder="如 G-05" />
        </el-form-item>
        <el-form-item label="仓房名称">
          <el-input v-model="form.name" placeholder="如 五号仓" />
        </el-form-item>
        <el-form-item label="仓容（吨）">
          <el-input-number v-model="form.capacity" :min="1" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="空仓" value="空仓" />
            <el-option label="在储" value="在储" />
            <el-option label="维修" value="维修" />
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
import { granaryApi, batchApi } from '../api'

const rows = ref([])
const allBatches = ref([])
const filters = ref({ status: '', keyword: '' })
const visible = ref(false)
const form = ref({})

function stored(granaryId) {
  return allBatches.value
    .filter((b) => b.granaryId === granaryId && b.status === '在储')
    .reduce((sum, b) => sum + b.quantity, 0)
}

async function load() {
  try {
    rows.value = await granaryApi.list({ ...filters.value })
    allBatches.value = await batchApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openCreate() {
  form.value = { capacity: 300, status: '空仓' }
  visible.value = true
}

function openEdit(row) {
  form.value = { ...row }
  visible.value = true
}

async function save() {
  try {
    if (form.value.id) {
      await granaryApi.update(form.value.id, form.value)
    } else {
      await granaryApi.create(form.value)
    }
    ElMessage.success('已保存')
    visible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>
