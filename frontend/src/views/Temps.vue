<template>
  <div>
    <div class="top">
      <span class="ttl">粮情温度表</span>
      <span class="hint">行是仓房、列是日期，格子里的数字是粮温（℃）；超 26℃ 标红</span>
      <span class="grow" />
      <span class="add" @click="openCreate">＋ 登记测温</span>
    </div>

    <div class="matrix">
      <table>
        <thead>
          <tr>
            <th class="corner">仓房 \ 日期</th>
            <th v-for="d in dates" :key="d" :class="{ today: d === lastDate }">
              {{ d.slice(5) }}
              <i v-if="d === lastDate">最新</i>
            </th>
            <th class="avg">平均</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="g in granaries" :key="g.id">
            <th class="rowhead">
              {{ g.name }}
              <em>{{ g.code }}</em>
            </th>
            <td
              v-for="d in dates"
              :key="d"
              :class="{ hot: isHot(g.id, d), empty: !cell(g.id, d) }"
              :title="tip(g.id, d)"
            >
              <span v-if="cell(g.id, d)">{{ cell(g.id, d).temperature }}</span>
              <span v-else>—</span>
            </td>
            <td class="avg">{{ avg(g.id) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="legend">
      <span><i class="dot normal"></i>正常（＜26℃）</span>
      <span><i class="dot hot"></i>超温（≥26℃）</span>
      <span><i class="dot none"></i>当天没测</span>
      <span class="grow" />
      <span class="note">鼠标停在格子上能看到湿度和记录人</span>
    </div>

    <el-dialog v-model="visible" title="登记测温" width="450px">
      <el-form label-width="106px">
        <el-form-item label="仓房">
          <el-select v-model="form.granaryId" style="width:100%">
            <el-option v-for="g in granaries" :key="g.id" :label="`${g.name}（${g.code}）`" :value="g.id" />
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
          <el-input v-model="form.recorder" placeholder="如 李保管" />
        </el-form-item>
      </el-form>
      <div class="dlg-tip">登记后结论（正常 / 超温）由服务端自动判定，不用手填。</div>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { tempApi, granaryApi } from '../api'

const rows = ref([])
const granaries = ref([])
const visible = ref(false)
const form = ref({})

const dates = computed(() => [...new Set(rows.value.map((r) => r.recordDate))].sort())
const lastDate = computed(() => dates.value[dates.value.length - 1] || '')

function cell(granaryId, date) {
  return rows.value.find((r) => r.granaryId === granaryId && r.recordDate === date) || null
}

function isHot(granaryId, date) {
  const c = cell(granaryId, date)
  return !!c && c.temperature >= 26
}

function tip(granaryId, date) {
  const c = cell(granaryId, date)
  if (!c) return '这天还没测'
  return `湿度 ${c.humidity}%　${c.recorder}　${c.result}`
}

function avg(granaryId) {
  const list = rows.value.filter((r) => r.granaryId === granaryId).map((r) => r.temperature)
  if (!list.length) return '—'
  return (list.reduce((a, b) => a + b, 0) / list.length).toFixed(1)
}

async function load() {
  try {
    rows.value = await tempApi.list({})
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

<style scoped>
.top {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin-bottom: 16px;
}
.ttl {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
}
.hint {
  font-size: 12px;
  color: #909399;
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
.matrix {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 6px 8px;
  overflow-x: auto;
}
table {
  border-collapse: collapse;
  width: 100%;
  font-size: 13px;
}
th,
td {
  border: 1px solid #f0f2f5;
  padding: 10px 12px;
  text-align: center;
}
thead th {
  background: #fafbfc;
  font-weight: 500;
  color: #606266;
  font-size: 12px;
  font-family: monospace;
}
thead th.today {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 700;
}
thead th.today i {
  font-style: normal;
  font-size: 10px;
  margin-left: 4px;
  background: var(--el-color-primary);
  color: #fff;
  border-radius: 8px;
  padding: 0 6px;
}
.corner {
  text-align: left;
  color: #c0c4cc !important;
  font-family: inherit !important;
}
.rowhead {
  text-align: left;
  font-weight: 600;
  background: #fafbfc;
  white-space: nowrap;
}
.rowhead em {
  font-style: normal;
  font-size: 11px;
  color: #a8abb2;
  font-family: monospace;
  margin-left: 6px;
  font-weight: 400;
}
td {
  font-family: monospace;
  color: #303133;
}
td.empty {
  color: #dcdfe6;
}
td.hot {
  background: #fef0f0;
  color: #f56c6c;
  font-weight: 700;
}
td.avg,
th.avg {
  background: #fafbfc;
  color: var(--el-color-primary);
  font-weight: 600;
  font-family: monospace;
}
.legend {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}
.legend .dot {
  display: inline-block;
  width: 9px;
  height: 9px;
  border-radius: 2px;
  margin-right: 6px;
}
.dot.normal {
  background: #fff;
  border: 1px solid #dcdfe6;
}
.dot.hot {
  background: #fef0f0;
  border: 1px solid #f56c6c;
}
.dot.none {
  background: #f5f7fa;
  border: 1px solid #ebeef5;
}
.legend .grow {
  flex: 1;
}
.note {
  color: #c0c4cc;
}
.dlg-tip {
  font-size: 12px;
  color: #909399;
  padding-left: 106px;
  margin-top: -6px;
}
</style>
