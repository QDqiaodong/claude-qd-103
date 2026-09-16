<template>
  <el-row :gutter="16">
    <el-col :span="10">
      <div class="panel">
        <div class="cap">
          粮食批次
          <span class="newlink" @click="openCreate">＋ 入库新批次</span>
        </div>
        <input v-model="keyword" class="s" placeholder="搜批次号 / 品种" />
        <div class="list">
          <div
            v-for="b in shown"
            :key="b.id"
            class="item"
            :class="{ on: form.id === b.id, out: b.status === '已出库' }"
            @click="pick(b)"
          >
            <span class="vd" :class="'v-' + b.variety">{{ b.variety }}</span>
            <span class="code">{{ b.code }}</span>
            <span class="qty">{{ b.quantity }}<i>吨</i></span>
          </div>
          <div v-if="!shown.length" class="none">没有匹配的批次</div>
        </div>
      </div>
    </el-col>

    <el-col :span="14">
      <div class="panel">
        <div class="cap">{{ form.id ? '批次详情' : '入库新批次' }}</div>

        <el-empty
          v-if="!form.id && !form.code"
          description="从左边选一批粮，或点「入库新批次」"
          :image-size="80"
        />

        <template v-else>
          <el-form label-width="96px">
            <el-form-item label="批次号">
              <el-input v-model="form.code" :disabled="!!form.id" placeholder="如 GB-2026-05" />
            </el-form-item>
            <el-form-item label="品种">
              <el-select v-model="form.variety" style="width:100%">
                <el-option v-for="v in varieties" :key="v" :label="v" :value="v" />
              </el-select>
            </el-form-item>

            <template v-if="!form.id">
              <el-form-item label="入哪个仓">
                <el-select v-model="form.granaryId" style="width:100%">
                  <el-option v-for="g in granaries" :key="g.id" :label="`${g.name}（${g.code}）`" :value="g.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="入库吨数">
                <el-input-number v-model="form.quantity" :min="1" />
              </el-form-item>
              <el-form-item label="入库日期">
                <el-date-picker v-model="form.inDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
              </el-form-item>
            </template>

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

          <div v-if="form.id" class="meta">
            <span>所在仓房：<b>{{ granaryName(form.granaryId) }}</b></span>
            <span>在库：<b>{{ form.quantity }}</b> 吨</span>
            <span>入库：<b class="mono">{{ form.inDate }}</b></span>
          </div>

          <div class="foot">
            <el-button @click="form = {}">清空</el-button>
            <el-button type="primary" @click="save">保存</el-button>
          </div>
        </template>
      </div>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { batchApi, granaryApi } from '../api'

const varieties = ['小麦', '玉米', '稻谷', '大豆']
const rows = ref([])
const granaries = ref([])
const keyword = ref('')
const form = ref({})

const shown = computed(() => {
  const k = keyword.value.trim()
  return rows.value.filter(
    (b) => !k || (b.code || '').includes(k) || (b.variety || '').includes(k)
  )
})

function granaryName(id) {
  const hit = granaries.value.find((g) => g.id === id)
  return hit ? hit.name : id
}

async function load() {
  try {
    rows.value = await batchApi.list({})
    if (!form.value.id && rows.value.length) form.value = { ...rows.value[0] }
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

function pick(row) {
  form.value = { ...row }
}

function openCreate() {
  form.value = { variety: '小麦', moisture: 12.0 }
}

async function save() {
  try {
    if (form.value.id) {
      await batchApi.update(form.value.id, form.value)
    } else {
      await batchApi.create(form.value)
    }
    ElMessage.success('已保存')
    form.value = {}
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

<style scoped>
.panel {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 15px 17px;
}
.cap {
  display: flex;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}
.newlink {
  margin-left: auto;
  font-size: 12px;
  color: var(--el-color-primary);
  font-weight: 400;
  cursor: pointer;
}
.newlink:hover {
  text-decoration: underline;
}
.s {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  padding: 8px 11px;
  font-size: 13px;
  outline: none;
  margin-bottom: 10px;
}
.s:focus {
  border-color: var(--el-color-primary);
}
.list {
  max-height: 520px;
  overflow-y: auto;
}
.item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 10px;
  border-radius: 6px;
  cursor: pointer;
  border-bottom: 1px solid #f5f7fa;
}
.item:hover {
  background: var(--el-color-primary-light-9);
}
.item.on {
  background: var(--el-color-primary-light-8);
}
.item.out {
  opacity: 0.5;
}
.vd {
  font-size: 11px;
  border-radius: 3px;
  padding: 1px 7px;
  color: #fff;
  flex: none;
}
.v-小麦 {
  background: #e6a23c;
}
.v-玉米 {
  background: #f0b429;
}
.v-稻谷 {
  background: #67c23a;
}
.v-大豆 {
  background: #8d6e63;
}
.code {
  font-family: monospace;
  font-size: 13px;
  color: #303133;
}
.qty {
  margin-left: auto;
  font-family: monospace;
  font-size: 14px;
  color: var(--el-color-primary);
}
.qty i {
  font-style: normal;
  font-size: 11px;
  color: #909399;
  margin-left: 2px;
}
.none {
  text-align: center;
  color: #c0c4cc;
  font-size: 12px;
  padding: 30px 0;
}
.meta {
  display: flex;
  gap: 22px;
  font-size: 12px;
  color: #909399;
  background: #fafafa;
  border-radius: 5px;
  padding: 10px 14px;
  margin-bottom: 12px;
}
.meta b {
  color: #303133;
}
.mono {
  font-family: monospace;
}
.foot {
  text-align: right;
}
</style>
