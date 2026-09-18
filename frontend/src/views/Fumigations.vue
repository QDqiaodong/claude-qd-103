<template>
  <div>
    <div class="top">
      <span class="ttl">熏蒸作业</span>
      <span class="hint">投药密闭期间作业全停；散气后残气复检合格才恢复出入库</span>
      <span class="grow" />
      <select v-model="statusFilter" class="m">
        <option value="">全部状态</option>
        <option value="待密闭">待密闭</option>
        <option value="密闭中">密闭中</option>
        <option value="已放行">已放行</option>
      </select>
      <span class="add" @click="openCreate">＋ 开熏蒸单</span>
    </div>

    <div class="rulebar">
      <span><i class="r-dot"></i>密闭中：该仓待执行出入库不许执行、新开出入库/新批次全挡住、仓房不能转维修；测温仍可登记，但记为「密闭期测温」。</span>
    </div>

    <div class="panel">
      <table>
        <thead>
          <tr>
            <th style="width:104px">熏蒸单号</th>
            <th style="width:120px">仓房</th>
            <th style="width:110px">药剂</th>
            <th style="width:90px">投药量</th>
            <th style="width:110px">计划密闭起</th>
            <th style="width:100px">计划散气</th>
            <th style="width:80px">投药人</th>
            <th style="width:90px">状态</th>
            <th style="width:200px">残气复检</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="f in shown" :key="f.id" :class="{ sealing: f.status === '密闭中' }">
            <td class="mono">{{ f.code }}</td>
            <td>{{ granaryName(f.granaryId) }}</td>
            <td>{{ f.chemical }}</td>
            <td class="mono">{{ f.dosage }} kg</td>
            <td class="mono dim">{{ f.planSealDate }}<i v-if="f.sealDate" class="real">实际 {{ f.sealDate }}</i></td>
            <td class="mono dim">{{ f.planAirDate }}</td>
            <td class="dim">{{ f.operator }}</td>
            <td>
              <span class="pill" :class="pillClass(f.status)">{{ f.status }}</span>
            </td>
            <td class="ck-cell">
              <template v-if="f.releaseResult">
                <div>
                  <span class="pill" :class="f.releaseResult === '合格' ? 'pass' : 'fail'">
                    {{ f.releaseResult }}
                  </span>
                  <span class="dim">{{ f.releaseInspector }} · {{ f.releaseDate }}</span>
                </div>
                <div v-if="f.releaseRemark" class="dim rmk">{{ f.releaseRemark }}</div>
              </template>
              <span v-else class="dim">—</span>
            </td>
            <td>
              <span v-if="f.status === '待密闭'" class="lnk" @click="start(f)">开始密闭</span>
              <span v-else-if="f.status === '密闭中'">
                <span
                  class="lnk"
                  :class="{ disabled: !canRelease(f) }"
                  @click="canRelease(f) && openRelease(f)"
                >复检放行</span>
                <span v-if="!canRelease(f)" class="dim soon">散气日 {{ f.planAirDate }} 后可复检</span>
              </span>
              <span v-else class="dim">已放行</span>
            </td>
          </tr>
          <tr v-if="!shown.length">
            <td colspan="10" class="none">没有熏蒸单，虫情来了点右上角「开熏蒸单」</td>
          </tr>
        </tbody>
      </table>
    </div>

    <el-dialog v-model="createVisible" title="开熏蒸单" width="500px">
      <el-form label-width="124px">
        <el-form-item label="熏蒸仓房">
          <el-select v-model="form.granaryId" style="width:100%" placeholder="只能选在储的仓">
            <el-option
              v-for="g in granaries"
              :key="g.id"
              :label="`${g.name}（${g.code}）${sealedIdSet.has(g.id) ? ' · 密闭中' : g.status !== '在储' ? ' · ' + g.status : ''}`"
              :value="g.id"
              :disabled="g.status !== '在储' || sealedIdSet.has(g.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="药剂名称">
          <el-input v-model="form.chemical" placeholder="如 磷化铝" />
        </el-form-item>
        <el-form-item label="投药量（千克）">
          <el-input-number v-model="form.dosage" :min="0.1" :precision="1" :step="1" />
        </el-form-item>
        <el-form-item label="计划密闭起始日">
          <el-date-picker v-model="form.planSealDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="计划散气日">
          <el-date-picker v-model="form.planAirDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="投药人">
          <el-input v-model="form.operator" placeholder="如 李保管" />
        </el-form-item>
        <el-form-item label="熏蒸单号">
          <el-input v-model="form.code" class="mono" />
          <span class="dlg-dim">自动生成，可改</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">提交开单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="releaseVisible" title="散气后残气复检" width="480px">
      <el-form label-width="110px">
        <el-form-item label="仓房">
          <b>{{ releaseForm.granaryLabel }}</b>
        </el-form-item>
        <el-form-item label="计划散气日">
          <span class="mono">{{ releaseForm.planAirDate }}</span>
          <span v-if="!releaseReachable" class="warn-inline">（还没到，不能复检）</span>
        </el-form-item>
        <el-form-item label="残气检测人">
          <el-input v-model="releaseForm.releaseInspector" placeholder="如 赵安监" />
        </el-form-item>
        <el-form-item label="复检结论">
          <el-radio-group v-model="releaseForm.releaseResult">
            <el-radio label="合格">合格，解除密闭</el-radio>
            <el-radio label="不合格">不合格，继续密闭</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="releaseForm.releaseRemark" type="textarea" :rows="2" placeholder="可填残气浓度、处置情况（选填）" />
        </el-form-item>
      </el-form>
      <div class="dlg-tip">
        不合格：单子仍停在「密闭中」，出入库继续停；合格：这间仓才重新允许出入库和转维修。
      </div>
      <template #footer>
        <el-button @click="releaseVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!releaseForm.releaseResult || !releaseReachable" @click="submitRelease">
          提交复检
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fumigationApi, granaryApi } from '../api'

const rows = ref([])
const granaries = ref([])
const statusFilter = ref('')
const createVisible = ref(false)
const releaseVisible = ref(false)
const form = ref({})
const releaseForm = ref({})

const today = () => {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

const shown = computed(() =>
  rows.value.filter((f) => !statusFilter.value || f.status === statusFilter.value)
)

const sealedIdSet = computed(
  () => new Set(rows.value.filter((f) => f.status === '密闭中').map((f) => f.granaryId))
)

function granaryName(id) {
  const g = granaries.value.find((x) => x.id === id)
  return g ? `${g.name}（${g.code}）` : id
}

function pillClass(status) {
  if (status === '密闭中') return 'sealing'
  if (status === '已放行') return 'pass'
  return 'wait'
}

function canRelease(f) {
  return f.status === '密闭中' && today() >= f.planAirDate
}

function nextCode() {
  const seq = rows.value
    .map((f) => parseInt(String(f.code).replace(/\D/g, ''), 10) || 0)
    .reduce((a, b) => Math.max(a, b), 0)
  return 'FM-' + String(seq + 1).padStart(4, '0')
}

async function load() {
  try {
    rows.value = await fumigationApi.list({})
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
  form.value = {
    code: nextCode(),
    planSealDate: today(),
    dosage: 10,
    chemical: '',
    operator: '',
    granaryId: null,
    planAirDate: ''
  }
  createVisible.value = true
}

async function submitCreate() {
  try {
    await fumigationApi.create({ ...form.value })
    ElMessage.success('熏蒸单已开（状态：待密闭）')
    createVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function start(f) {
  try {
    await ElMessageBox.confirm(
      `确认仓房 ${granaryName(f.granaryId)} 现在投药、正式开始密闭？密闭期间该仓出入库/新批次/转维修全停。`,
      '开始密闭',
      { confirmButtonText: '投药并开始密闭', cancelButtonText: '再等等', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await fumigationApi.start(f.id)
    ElMessage.success('已开始密闭，该仓作业全停，散气复检合格后恢复')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
    await load()
  }
}

const releaseReachable = computed(
  () => releaseForm.value.planAirDate && today() >= releaseForm.value.planAirDate
)

function openRelease(f) {
  releaseForm.value = {
    id: f.id,
    granaryLabel: granaryName(f.granaryId),
    planAirDate: f.planAirDate,
    releaseInspector: '',
    releaseResult: '合格',
    releaseRemark: ''
  }
  releaseVisible.value = true
}

async function submitRelease() {
  try {
    await fumigationApi.release(releaseForm.value.id, {
      releaseInspector: releaseForm.value.releaseInspector,
      releaseResult: releaseForm.value.releaseResult,
      releaseRemark: releaseForm.value.releaseRemark
    })
    if (releaseForm.value.releaseResult === '合格') {
      ElMessage.success('残气复检合格，密闭解除，仓房恢复出入库')
    } else {
      ElMessage.warning('复检不合格，继续密闭，作业仍然全停')
    }
    releaseVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
    await load()
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
  margin-bottom: 12px;
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
}
.add:hover {
  text-decoration: underline;
}
.rulebar {
  background: #fdf6ec;
  border: 1px solid #faecd8;
  color: #b88230;
  font-size: 12px;
  border-radius: 6px;
  padding: 9px 14px;
  margin-bottom: 14px;
  line-height: 1.6;
}
.r-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e6a23c;
  margin-right: 7px;
}
.panel {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px 16px 16px;
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
  vertical-align: top;
}
tr.sealing td {
  background: #fdf8ec;
}
.mono {
  font-family: monospace;
}
.dim {
  color: #a8abb2;
  font-size: 12px;
}
.real {
  display: block;
  font-style: normal;
  font-size: 11px;
  color: #e6a23c;
  margin-top: 2px;
}
.pill {
  font-size: 12px;
  border-radius: 3px;
  padding: 1px 9px;
  white-space: nowrap;
}
.pill.sealing {
  background: #fdf6ec;
  color: #b88230;
  border: 1px solid #f5dab1;
  font-weight: 600;
}
.pill.pass {
  background: #f0f9eb;
  color: #529b2e;
}
.pill.fail {
  background: #fef0f0;
  color: #c45656;
}
.pill.wait {
  background: #f4f4f5;
  color: #606266;
}
.ck-cell .rmk {
  margin-top: 3px;
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
.lnk.disabled {
  color: #c0c4cc;
  cursor: not-allowed;
  text-decoration: none;
}
.soon {
  margin-left: 8px;
  white-space: nowrap;
}
.none {
  text-align: center;
  color: #c0c4cc;
  padding: 34px 0;
}
.dlg-dim {
  font-size: 12px;
  color: #909399;
  margin-left: 10px;
}
.warn-inline {
  color: #e6a23c;
  font-size: 12px;
  margin-left: 6px;
}
.dlg-tip {
  font-size: 12px;
  color: #909399;
  background: #fafafa;
  border-radius: 5px;
  padding: 9px 12px;
  line-height: 1.7;
}
</style>
