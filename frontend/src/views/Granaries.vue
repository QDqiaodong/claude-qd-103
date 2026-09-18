<template>
  <div>
    <div class="top">
      <input v-model="keyword" class="s" placeholder="搜仓房编号 / 名称" />
      <select v-model="statusFilter" class="m">
        <option value="">全部状态</option>
        <option value="空仓">空仓</option>
        <option value="在储">在储</option>
        <option value="维修">维修</option>
      </select>
      <span class="grow" />
      <span class="add" @click="openCreate">＋ 新增仓房</span>
    </div>

    <div class="grid">
      <div
        v-for="g in shown"
        :key="g.id"
        class="cell"
        :class="{ repair: g.status === '维修', sealing: sealedIdSet.has(g.id) }"
      >
        <div class="ring" :style="ringStyle(g)">
          <div class="hole">
            <b>{{ pct(g) }}<i>%</i></b>
            <span>占用</span>
          </div>
        </div>

        <div class="info">
          <div class="nm">
            {{ g.name }}
            <em>{{ g.code }}</em>
          </div>
          <div class="kv"><span>仓容</span><b>{{ g.capacity }}</b>吨</div>
          <div class="kv"><span>在储</span><b class="hl">{{ stored(g.id) }}</b>吨</div>
          <div class="kv"><span>还可入</span><b>{{ g.capacity - stored(g.id) }}</b>吨</div>
          <div v-if="sealedIdSet.has(g.id)" class="st st-密闭">密闭中（熏蒸）</div>
          <div v-else class="st" :class="'st-' + g.status">{{ g.status }}</div>
        </div>

        <span class="edit" @click="openEdit(g)">改</span>
      </div>
    </div>

    <el-dialog v-model="visible" :title="form.id ? '编辑仓房' : '新增仓房'" width="440px">
      <el-form label-width="96px">
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
            <el-option
              label="维修"
              value="维修"
              :disabled="sealedIdSet.has(form.id)"
            />
          </el-select>
          <div v-if="sealedIdSet.has(form.id)" class="form-warn">
            这间仓正在熏蒸密闭中，散气复检合格前不能改成维修
          </div>
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { granaryApi, batchApi, fumigationApi } from '../api'

const rows = ref([])
const batches = ref([])
const fumigations = ref([])
const keyword = ref('')
const statusFilter = ref('')
const visible = ref(false)
const form = ref({})

const sealedIdSet = computed(
  () => new Set(fumigations.value.filter((f) => f.status === '密闭中').map((f) => f.granaryId))
)

const shown = computed(() => {
  const k = keyword.value.trim()
  return rows.value.filter(
    (g) =>
      (!k || (g.name || '').includes(k) || (g.code || '').includes(k)) &&
      (!statusFilter.value || g.status === statusFilter.value)
  )
})

function stored(id) {
  return batches.value
    .filter((b) => b.granaryId === id && b.status === '在储')
    .reduce((s, b) => s + (b.quantity || 0), 0)
}

function pct(g) {
  if (!g.capacity) return 0
  return Math.min(100, Math.round((stored(g.id) / g.capacity) * 100))
}

function ringStyle(g) {
  const p = pct(g)
  const color = p >= 95 ? '#f56c6c' : p >= 60 ? '#e6a23c' : 'var(--el-color-primary)'
  return {
    background: `conic-gradient(${color} 0 ${p}%, #eef1f5 ${p}% 100%)`
  }
}

async function load() {
  try {
    rows.value = await granaryApi.list({})
    batches.value = await batchApi.list({})
    fumigations.value = await fumigationApi.list({})
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

<style scoped>
.top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.s {
  width: 220px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  padding: 8px 11px;
  font-size: 13px;
  outline: none;
}
.m {
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  padding: 8px 10px;
  font-size: 13px;
  outline: none;
  background: #fff;
}
.grow {
  flex: 1;
}
.add {
  font-size: 13px;
  color: var(--el-color-primary);
  cursor: pointer;
  user-select: none;
}
.add:hover {
  text-decoration: underline;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(330px, 1fr));
  gap: 14px;
}
.cell {
  position: relative;
  display: flex;
  align-items: center;
  gap: 18px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px 18px;
  transition: box-shadow 0.15s;
}
.cell:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.07);
}
.cell.repair {
  opacity: 0.6;
}
.ring {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
}
.hole {
  width: 66px;
  height: 66px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.hole b {
  font-size: 17px;
  line-height: 1;
  font-family: monospace;
  color: #303133;
}
.hole b i {
  font-size: 10px;
  font-style: normal;
  color: #909399;
}
.hole span {
  font-size: 10px;
  color: #909399;
  margin-top: 3px;
}
.info {
  flex: 1;
  min-width: 0;
}
.nm {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 7px;
}
.nm em {
  font-style: normal;
  font-size: 12px;
  color: #a8abb2;
  font-family: monospace;
  margin-left: 6px;
}
.kv {
  font-size: 12px;
  color: #909399;
  line-height: 1.9;
}
.kv span {
  display: inline-block;
  width: 48px;
}
.kv b {
  font-family: monospace;
  font-size: 13px;
  color: #303133;
}
.kv b.hl {
  color: var(--el-color-primary);
}
.st {
  display: inline-block;
  margin-top: 6px;
  font-size: 11px;
  border-radius: 9px;
  padding: 1px 9px;
  background: #f4f4f5;
  color: #606266;
}
.st-在储 {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}
.st-维修 {
  background: #fef0f0;
  color: #c45656;
}
.st-密闭 {
  background: #fdf6ec;
  color: #b88230;
  border: 1px solid #f5dab1;
}
.cell.sealing {
  border-color: #f5dab1;
}
.form-warn {
  font-size: 12px;
  color: #b88230;
  margin-top: 6px;
  line-height: 1.5;
}
.edit {
  position: absolute;
  right: 14px;
  bottom: 12px;
  font-size: 12px;
  color: var(--el-color-primary);
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s;
}
.cell:hover .edit {
  opacity: 1;
}
</style>
