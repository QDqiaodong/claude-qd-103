<template>
  <div>
    <el-steps :active="step" align-center finish-status="success" class="wiz">
      <el-step title="选批次" description="要动哪一批粮" />
      <el-step title="填数量" description="入多少 / 出多少" />
      <el-step title="确认开单" description="核对后提交" />
    </el-steps>

    <div class="stage">
      <div v-if="step === 0" class="picks">
        <div
          v-for="b in batches"
          :key="b.id"
          class="pick"
          :class="{
            on: form.batchId === b.id,
            out: b.status === '已出库',
            sealed: sealedIdSet.has(b.granaryId)
          }"
          @click="choose(b)"
        >
          <div class="p-code">{{ b.code }}</div>
          <div class="p-var">{{ b.variety }}</div>
          <div class="p-qty">{{ b.quantity }} <i>吨</i></div>
          <div class="p-st">
            <span v-if="sealedIdSet.has(b.granaryId)" class="sealmark">{{ granaryName(b.granaryId) }} · 密闭中</span>
            <span v-else>{{ b.status }}</span>
          </div>
        </div>
      </div>

      <div v-else-if="step === 1" class="formbox">
        <div class="frow">
          <label>作业类型</label>
          <div class="segs">
            <span :class="{ on: form.moveType === '入库' }" @click="form.moveType = '入库'">入库</span>
            <span :class="{ on: form.moveType === '出库' }" @click="form.moveType = '出库'">出库</span>
          </div>
        </div>
        <div class="frow">
          <label>吨数</label>
          <input v-model.number="form.quantity" type="number" min="1" />
          <span class="unit">吨</span>
        </div>
        <div class="frow">
          <label>作业日期</label>
          <input v-model="form.moveDate" type="date" />
        </div>
        <div class="frow">
          <label>经办人</label>
          <input v-model="form.operator" placeholder="如 李保管" />
        </div>
        <div class="frow">
          <label>作业单号</label>
          <input v-model="form.code" class="mono" />
          <span class="unit dim">自动生成，可改</span>
        </div>
      </div>

      <div v-else class="confirm">
        <div class="ck">
          <span>批次</span>
          <b>{{ pickedInfo.code }} · {{ pickedInfo.variety }}（当前 {{ pickedInfo.quantity }} 吨）</b>
        </div>
        <div class="ck">
          <span>动作</span>
          <b class="big" :class="form.moveType === '入库' ? 'in' : 'out'">{{ form.moveType }}</b>
        </div>
        <div class="ck"><span>吨数</span><b class="mono">{{ form.quantity }} 吨</b></div>
        <div class="ck"><span>日期</span><b class="mono">{{ form.moveDate }}</b></div>
        <div class="ck"><span>经办人</span><b>{{ form.operator }}</b></div>
        <div class="ck"><span>单号</span><b class="mono">{{ form.code }}</b></div>
        <div class="warn">
          提交后这张单子先落在「待执行」，真正加减在库吨数是在点「执行」那一刻 ——
          执行时才会校验出库不超在库、入库不破仓容。
        </div>
      </div>
    </div>

    <div class="wiz-foot">
      <button v-if="step > 0" class="ghost" @click="step -= 1">← 上一步</button>
      <span class="grow" />
      <span v-if="tipText" class="tip">{{ tipText }}</span>
      <button v-if="step < 2" class="main" :disabled="!canNext" @click="next">下一步 →</button>
      <button v-else class="main" @click="submit">提交开单</button>
    </div>

    <div class="existing">
      <div class="ex-cap">
        已开的作业单
        <span class="dim">（点「执行」才真正加减在库吨数）</span>
      </div>
      <table>
        <thead>
          <tr>
            <th style="width:120px">单号</th>
            <th style="width:110px">批次</th>
            <th style="width:150px">仓房</th>
            <th style="width:90px">类型</th>
            <th style="width:90px">吨数</th>
            <th style="width:120px">日期</th>
            <th style="width:100px">经办</th>
            <th style="width:110px">状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in moves" :key="m.id">
            <td class="mono">{{ m.code }}</td>
            <td class="mono dim">{{ batchCode(m.batchId) }}</td>
            <td class="dim">
              {{ granaryName(batchGranary(m.batchId)) }}
              <span v-if="sealedIdSet.has(batchGranary(m.batchId))" class="sealtag">密闭中</span>
            </td>
            <td>
              <span class="pill" :class="m.moveType === '入库' ? 'in' : 'out'">{{ m.moveType }}</span>
            </td>
            <td class="mono">{{ m.quantity }}</td>
            <td class="mono dim">{{ m.moveDate }}</td>
            <td class="dim">{{ m.operator }}</td>
            <td>
              <span class="pill" :class="m.status === '已完成' ? 'done' : 'wait'">{{ m.status }}</span>
            </td>
            <td>
              <template v-if="m.status === '待执行'">
                <span v-if="sealedIdSet.has(batchGranary(m.batchId))" class="sealblock">
                  密闭中，禁执行
                </span>
                <span v-else class="lnk" @click="execute(m)">执行</span>
              </template>
              <span v-else class="dim">已执行</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { moveApi, batchApi, granaryApi, fumigationApi } from '../api'

const moves = ref([])
const batches = ref([])
const granaries = ref([])
const fumigations = ref([])
const step = ref(0)
const form = ref({})

const sealedIdSet = computed(
  () => new Set(fumigations.value.filter((f) => f.status === '密闭中').map((f) => f.granaryId))
)

const pickedInfo = computed(() => batches.value.find((b) => b.id === form.value.batchId) || {})

const canNext = computed(() => {
  if (step.value === 0) return !!form.value.batchId
  if (step.value === 1) {
    return (
      form.value.quantity > 0 &&
      form.value.moveDate &&
      form.value.operator &&
      form.value.code &&
      form.value.moveType
    )
  }
  return true
})

const tipText = computed(() => {
  if (step.value === 0 && !form.value.batchId) return '先挑一批粮'
  if (step.value === 1) {
    if (!form.value.quantity) return '填一下吨数'
    if (!form.value.moveDate) return '选一下作业日期'
    if (!form.value.operator) return '填一下经办人'
  }
  return ''
})

function batchCode(id) {
  const hit = batches.value.find((b) => b.id === id)
  return hit ? hit.code : id
}

function batchGranary(id) {
  const hit = batches.value.find((b) => b.id === id)
  return hit ? hit.granaryId : null
}

function granaryName(id) {
  const hit = granaries.value.find((g) => g.id === id)
  return hit ? hit.name : '—'
}

function nextCode() {
  const seq = moves.value
    .map((m) => parseInt(String(m.code).replace(/\D/g, ''), 10) || 0)
    .reduce((a, b) => Math.max(a, b), 0)
  return 'SM-' + String(seq + 1).padStart(4, '0')
}

function choose(b) {
  if (sealedIdSet.has(b.granaryId)) {
    ElMessage.warning(`${granaryName(b.granaryId)} 正在熏蒸密闭中，散气复检合格前禁止出入库`)
    return
  }
  form.value.batchId = b.id
}

async function load() {
  try {
    moves.value = await moveApi.list({})
    batches.value = await batchApi.list({})
    granaries.value = await granaryApi.list({})
    fumigations.value = await fumigationApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function next() {
  if (!canNext.value) {
    ElMessage.warning(tipText.value || '还有没填完的')
    return
  }
  if (step.value === 1 && !form.value.code) form.value.code = nextCode()
  step.value += 1
}

async function submit() {
  try {
    await moveApi.create({ ...form.value })
    ElMessage.success('已开单（状态：待执行）')
    await load()
    step.value = 0
    form.value = {}
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function execute(m) {
  try {
    await moveApi.execute(m.id)
    ElMessage.success('已执行，在库吨数已更新')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>

<style scoped>
.wiz {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 22px 30px 12px;
  margin-bottom: 16px;
}
.stage {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 22px 26px;
  min-height: 170px;
}
.picks {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 12px;
}
.pick {
  border: 1px solid #e4e7ed;
  border-radius: 7px;
  padding: 13px 15px;
  cursor: pointer;
  transition: all 0.15s;
}
.pick:hover {
  border-color: var(--el-color-primary-light-5);
  background: #fafcff;
}
.pick.on {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-8);
}
.pick.out {
  opacity: 0.45;
}
.pick.sealed {
  border-color: #f5dab1;
  background: #fdf8ec;
  cursor: not-allowed;
}
.sealmark {
  color: #b88230;
  font-weight: 600;
}
.sealblock {
  color: #b88230;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 3px;
  padding: 1px 8px;
  font-size: 12px;
  white-space: nowrap;
}
.sealtag {
  font-size: 11px;
  color: #b88230;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 3px;
  padding: 0 6px;
  margin-left: 5px;
}
.p-code {
  font-family: monospace;
  font-size: 12px;
  color: #909399;
}
.p-var {
  font-size: 15px;
  font-weight: 600;
  margin: 3px 0 6px;
}
.p-qty {
  font-family: monospace;
  font-size: 19px;
  color: var(--el-color-primary);
  line-height: 1;
}
.p-qty i {
  font-style: normal;
  font-size: 11px;
  color: #909399;
}
.p-st {
  margin-top: 7px;
  font-size: 11px;
  color: #909399;
}
.formbox {
  max-width: 470px;
}
.frow {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 15px;
}
.frow label {
  width: 82px;
  font-size: 13px;
  color: #606266;
  text-align: right;
}
.frow input {
  flex: 1;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  padding: 8px 11px;
  font-size: 13px;
  outline: none;
}
.frow input:focus {
  border-color: var(--el-color-primary);
}
.mono {
  font-family: monospace;
}
.unit {
  font-size: 12px;
  color: #909399;
}
.unit.dim {
  color: #c0c4cc;
}
.segs {
  display: flex;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  overflow: hidden;
}
.segs span {
  padding: 8px 26px;
  font-size: 13px;
  cursor: pointer;
  background: #fff;
  color: #606266;
}
.segs span.on {
  background: var(--el-color-primary);
  color: #fff;
}
.confirm .ck {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 9px 0;
  border-bottom: 1px dashed #f0f2f5;
  font-size: 13px;
}
.confirm .ck > span:first-child {
  width: 76px;
  color: #909399;
}
.confirm .ck b {
  font-weight: 600;
}
.confirm .ck b.big {
  font-size: 15px;
}
.confirm b.in {
  color: #67c23a;
}
.confirm b.out {
  color: #e6a23c;
}
.warn {
  margin-top: 16px;
  font-size: 12px;
  color: #b88230;
  background: #fdf6ec;
  border-radius: 5px;
  padding: 10px 14px;
  line-height: 1.7;
}
.wiz-foot {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 4px 4px;
}
.grow {
  flex: 1;
}
.tip {
  font-size: 12px;
  color: #e6a23c;
}
.ghost {
  border: 1px solid var(--el-border-color);
  background: #fff;
  border-radius: 4px;
  padding: 9px 18px;
  font-size: 13px;
  cursor: pointer;
  color: #606266;
}
.main {
  border: none;
  background: var(--el-color-primary);
  color: #fff;
  border-radius: 4px;
  padding: 9px 24px;
  font-size: 13px;
  cursor: pointer;
}
.main:disabled {
  background: #c8d6e5;
  cursor: not-allowed;
}
.existing {
  margin-top: 18px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 15px 18px;
}
.ex-cap {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
}
.ex-cap .dim {
  font-weight: 400;
  font-size: 12px;
  color: #909399;
  margin-left: 6px;
}
.existing table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.existing th {
  text-align: left;
  font-size: 12px;
  color: #909399;
  font-weight: 500;
  padding: 7px 9px;
  border-bottom: 1px solid #ebeef5;
}
.existing td {
  padding: 9px;
  border-bottom: 1px solid #f5f7fa;
}
.dim {
  color: #a8abb2;
  font-size: 12px;
}
.pill {
  font-size: 12px;
  border-radius: 3px;
  padding: 1px 9px;
}
.pill.in {
  background: #f0f9eb;
  color: #529b2e;
}
.pill.out {
  background: #fdf6ec;
  color: #b88230;
}
.pill.done {
  background: #f0f9eb;
  color: #529b2e;
}
.pill.wait {
  background: #f4f4f5;
  color: #606266;
}
.lnk {
  color: var(--el-color-primary);
  cursor: pointer;
  font-size: 12px;
}
.lnk:hover {
  text-decoration: underline;
}
</style>
