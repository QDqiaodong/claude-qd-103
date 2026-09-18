<template>
  <div>
    <div class="top">
      <span class="ttl">熏蒸作业</span>
      <span class="hint">
        开单一次写齐仓房、药剂、投药量、密闭起止日和投药人；密闭期间出入库 / 入粮 / 转维修全停，
        散气复检合格后才恢复
      </span>
      <span class="grow" />
      <select v-model="statusFilter" class="m">
        <option value="">全部状态</option>
        <option value="待密闭">待密闭</option>
        <option value="密闭中">密闭中</option>
        <option value="已完成">已完成</option>
      </select>
      <span class="add" @click="openCreate">＋ 开熏蒸单</span>
    </div>

    <div class="panel">
      <table>
        <thead>
          <tr>
            <th style="width:110px">熏蒸单号</th>
            <th style="width:130px">仓房</th>
            <th style="width:110px">药剂</th>
            <th style="width:90px">投药量</th>
            <th style="width:115px">密闭起始日</th>
            <th style="width:110px">计划散气日</th>
            <th style="width:80px">投药人</th>
            <th style="width:90px">状态</th>
            <th>复检记录</th>
            <th style="width:150px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="f in shown" :key="f.id">
            <td class="mono">{{ f.code }}</td>
            <td>{{ granaryName(f.granaryId) }} <em class="mono dim">{{ granaryCode(f.granaryId) }}</em></td>
            <td>{{ f.chemicalName }}</td>
            <td class="mono">{{ f.dosage }} 公斤</td>
            <td class="mono dim">{{ f.sealDate }}</td>
            <td class="mono dim">{{ f.aerationDate }}</td>
            <td class="dim">{{ f.applicator }}</td>
            <td><span class="pill" :class="pillClass(f.status)">{{ f.status }}</span></td>
            <td class="dim">
              <template v-if="f.checkResult">
                {{ f.checkDate }}　{{ f.checker }}　结论：
                <b :class="f.checkResult === '合格' ? 'pass' : 'fail'">{{ f.checkResult }}</b>
              </template>
              <span v-else>—</span>
            </td>
            <td>
              <span v-if="f.status === '待密闭'" class="lnk" @click="start(f)">开始密闭</span>
              <template v-else-if="f.status === '密闭中'">
                <span
                  v-if="canRelease(f)"
                  class="lnk"
                  @click="openRelease(f)"
                >复检放行</span>
                <span v-else class="dim lock" title="散气日没到，复检放行点不了">
                  🔒 {{ f.aerationDate }} 后可复检
                </span>
              </template>
              <span v-else class="dim">已放行</span>
            </td>
          </tr>
          <tr v-if="!shown.length">
            <td colspan="10" class="none">没有熏蒸单，虫情来了点右上角「开熏蒸单」</td>
          </tr>
        </tbody>
      </table>
    </div>

    <el-dialog v-model="createVisible" title="开熏蒸单" width="520px">
      <el-form label-width="120px">
        <el-form-item label="熏蒸仓房">
          <el-select v-model="form.granaryId" style="width:100%" placeholder="选一间在储仓">
            <el-option
              v-for="g in granaryOptions"
              :key="g.id"
              :label="`${g.name}（${g.code}）· 在储 ${stored(g.id)} 吨`"
              :value="g.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="药剂名称">
          <el-input v-model="form.chemicalName" placeholder="如 磷化铝" />
        </el-form-item>
        <el-form-item label="投药量">
          <el-input-number v-model="form.dosage" :min="0.1" :precision="1" :step="0.5" />
          <span class="unit">公斤</span>
        </el-form-item>
        <el-form-item label="计划密闭起始日">
          <el-date-picker v-model="form.sealDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="计划散气日">
          <el-date-picker v-model="form.aerationDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="投药人">
          <el-input v-model="form.applicator" placeholder="如 李保管" />
        </el-form-item>
        <el-form-item label="熏蒸单号">
          <el-input v-model="form.code" class="mono" />
          <span class="dlg-note">自动生成，可改</span>
        </el-form-item>
      </el-form>
      <div class="dlg-tip">
        提交后先落「待密闭」；点「开始密闭」那一刻起，这间仓的出入库、新建在储批次、转维修全部锁死，
        测温仍可登记并自动记为「密闭测温」。
      </div>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">提交开单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="releaseVisible" title="散气复检放行" width="460px">
      <el-form label-width="110px">
        <el-form-item label="熏蒸单">
          <span class="mono">{{ releaseForm.code }}</span>
          <span style="margin-left:10px">{{ granaryName(releaseForm.granaryId) }}</span>
        </el-form-item>
        <el-form-item label="计划散气日">
          <span class="mono">{{ releaseForm.aerationDate }}</span>
          <span class="dlg-note">今天 {{ today }}</span>
        </el-form-item>
        <el-form-item label="残气检测人">
          <el-input v-model="releaseForm.checker" placeholder="谁检测谁签字" />
        </el-form-item>
        <el-form-item label="复检结论">
          <el-radio-group v-model="releaseForm.checkResult">
            <el-radio value="合格">合格，恢复作业</el-radio>
            <el-radio value="不合格">不合格，继续密闭</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div class="dlg-tip">
        不合格：单子仍停在「密闭中」，作业继续全停，改日重新复检；合格：熏蒸单完结，这间仓恢复出入库和维修。
        复检只能放行一次。
      </div>
      <template #footer>
        <el-button @click="releaseVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRelease">提交复检</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fumigationApi, granaryApi, batchApi } from '../api'

const rows = ref([])
const granaries = ref([])
const batches = ref([])
const statusFilter = ref('')
const createVisible = ref(false)
const releaseVisible = ref(false)
const form = ref({})
const releaseForm = ref({})

const today = computed(() => {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
})

const shown = computed(() =>
  rows.value.filter((f) => !statusFilter.value || f.status === statusFilter.value)
)

// 只能给不在维修的仓开熏蒸；已经密闭中的仓不放进下拉（后端也会再挡一道）。
const granaryOptions = computed(() =>
  granaries.value.filter((g) => g.status !== '维修' && !isSealed(g.id))
)

function isSealed(granaryId) {
  return rows.value.some((f) => f.granaryId === granaryId && f.status === '密闭中')
}

function stored(id) {
  return batches.value
    .filter((b) => b.granaryId === id && b.status === '在储')
    .reduce((s, b) => s + (b.quantity || 0), 0)
}

function granaryName(id) {
  const hit = granaries.value.find((g) => g.id === id)
  return hit ? hit.name : id
}

function granaryCode(id) {
  const hit = granaries.value.find((g) => g.id === id)
  return hit ? hit.code : ''
}

function pillClass(status) {
  if (status === '密闭中') return 'sealed'
  if (status === '已完成') return 'done'
  return 'wait'
}

function canRelease(f) {
  return f.status === '密闭中' && today.value >= f.aerationDate
}

function nextCode() {
  const seq = rows.value
    .map((f) => parseInt(String(f.code).replace(/\D/g, ''), 10) || 0)
    .reduce((a, b) => Math.max(a, b), 0)
  return 'FM-' + String(seq + 1).padStart(4, '0')
}

async function load() {
  try {
    const [f, g, b] = await Promise.all([
      fumigationApi.list({}),
      granaryApi.list({}),
      batchApi.list({})
    ])
    rows.value = f
    granaries.value = g
    batches.value = b
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openCreate() {
  form.value = {
    dosage: 5,
    sealDate: today.value,
    aerationDate: today.value,
    code: nextCode()
  }
  createVisible.value = true
}

async function submitCreate() {
  const f = form.value
  if (!f.granaryId) return ElMessage.warning('请选择熏蒸仓房')
  if (!f.chemicalName) return ElMessage.warning('请填药剂名称')
  if (!(f.dosage > 0)) return ElMessage.warning('投药量要大于 0')
  if (!f.sealDate) return ElMessage.warning('请填计划密闭起始日')
  if (!f.aerationDate) return ElMessage.warning('请填计划散气日')
  if (f.aerationDate < f.sealDate) return ElMessage.warning('计划散气日不能早于密闭起始日')
  if (!f.applicator) return ElMessage.warning('请填投药人')
  try {
    await fumigationApi.create({ ...f })
    ElMessage.success('熏蒸单已开（待密闭）')
    createVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function start(f) {
  try {
    await ElMessageBox.confirm(
      `确认现在对 ${granaryName(f.granaryId)} 开始密闭？开始后这间仓出入库、入粮、转维修全停。`,
      '开始密闭',
      { type: 'warning', confirmButtonText: '开始密闭', cancelButtonText: '再等等' }
    )
  } catch {
    return
  }
  try {
    await fumigationApi.start(f.id)
    ElMessage.success('已开始密闭，作业全停，等散气复检')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
    await load()
  }
}

function openRelease(f) {
  releaseForm.value = { id: f.id, code: f.code, granaryId: f.granaryId, aerationDate: f.aerationDate }
  releaseVisible.value = true
}

async function submitRelease() {
  const r = releaseForm.value
  if (!r.checker) return ElMessage.warning('请填残气检测人')
  if (!r.checkResult) return ElMessage.warning('请选复检结论')
  try {
    await fumigationApi.release(r.id, { checker: r.checker, checkResult: r.checkResult })
    ElMessage.success(r.checkResult === '合格' ? '复检合格，熏蒸放行，作业已恢复' : '复检不合格，继续密闭停作业')
    releaseVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
    await load()
  }
}

onMounted(load)
</script>

<style scoped>
.top {
  display: flex;
  align-items: center;
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
.m {
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  padding: 8px 10px;
  font-size: 13px;
  outline: none;
  background: #fff;
}
.add {
  font-size: 13px;
  color: var(--el-color-primary);
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
}
.add:hover {
  text-decoration: underline;
}
.panel {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px 16px;
  overflow-x: auto;
}
table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
th {
  text-align: left;
  font-size: 12px;
  color: #909399;
  font-weight: 500;
  padding: 9px;
  border-bottom: 1px solid #ebeef5;
  white-space: nowrap;
}
td {
  padding: 10px 9px;
  border-bottom: 1px solid #f5f7fa;
  vertical-align: middle;
}
.mono {
  font-family: monospace;
}
.dim {
  color: #a8abb2;
  font-size: 12px;
}
.none {
  text-align: center;
  color: #c0c4cc;
  padding: 34px 0;
}
.pill {
  font-size: 12px;
  border-radius: 3px;
  padding: 2px 9px;
  white-space: nowrap;
}
.pill.sealed {
  background: #fef0f0;
  color: #c45656;
  font-weight: 600;
}
.pill.done {
  background: #f0f9eb;
  color: #529b2e;
}
.pill.wait {
  background: #f4f4f5;
  color: #606266;
}
.pass {
  color: #529b2e;
}
.fail {
  color: #c45656;
}
.lnk {
  color: var(--el-color-primary);
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
}
.lnk:hover {
  text-decoration: underline;
}
.lock {
  white-space: nowrap;
  cursor: not-allowed;
}
.unit {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}
.dlg-note {
  margin-left: 10px;
  font-size: 12px;
  color: #c0c4cc;
}
.dlg-tip {
  font-size: 12px;
  color: #b88230;
  background: #fdf6ec;
  border-radius: 5px;
  padding: 10px 14px;
  line-height: 1.7;
}
</style>
