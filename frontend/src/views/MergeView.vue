<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { apiMessage, http, prepareCsrf } from '@/api/http'
import type { MergeObjectType, MergePreview, MergeResult, ProjectDetails, ProjectSummary, Requirement } from '@/api/types'

interface Option { id: number; label: string }

const objectType = ref<MergeObjectType>('PROJECT')
const options = ref<Option[]>([])
const sourceIds = ref<number[]>([])
const targetId = ref<number>()
const preview = ref<MergePreview>()
const result = ref<MergeResult>()
const acceptedFields = ref<string[]>([])
const loading = ref(false)
const error = ref('')
const requiredFields = computed(() => [...new Set(preview.value?.conflicts.map(item => item.field) ?? [])])
const canExecute = computed(() => requiredFields.value.every(field => acceptedFields.value.includes(field)))

async function loadOptions() {
  error.value = ''
  preview.value = undefined
  sourceIds.value = []
  targetId.value = undefined
  try {
    if (objectType.value === 'PROJECT') {
      const data = (await http.get<ProjectSummary[]>('/projects')).data
      options.value = data.filter(item => item.status !== 'MERGED').map(item => ({ id: item.id, label: `${item.code} · ${item.name}` }))
    } else if (objectType.value === 'REQUIREMENT') {
      const data = (await http.get<Requirement[]>('/requirements')).data
      options.value = data.filter(item => item.status !== 'MERGED').map(item => ({ id: item.id, label: `${item.requirementNo} · ${item.title}` }))
    } else {
      const projects = (await http.get<ProjectSummary[]>('/projects')).data.filter(item => item.status !== 'MERGED')
      const details = await Promise.all(projects.map(item => http.get<ProjectDetails>(`/projects/${item.id}`)))
      options.value = details.flatMap(({ data }) => data.tasks.filter(task => task.status !== 'MERGED')
        .map(task => ({ id: task.id, label: `${data.project.name} · TASK-${task.id} · ${task.title}` })))
    }
  } catch (e) { error.value = apiMessage(e) }
}

async function showPreview() {
  error.value = ''
  result.value = undefined
  loading.value = true
  try {
    await prepareCsrf()
    preview.value = (await http.post<MergePreview>('/merges/preview', {
      objectType: objectType.value, sourceIds: sourceIds.value, targetId: targetId.value,
    })).data
    acceptedFields.value = []
  } catch (e) { error.value = apiMessage(e) } finally { loading.value = false }
}

async function executeMerge() {
  if (!preview.value || !window.confirm('合并后来源对象将变为只读，第一版不支持一键撤销。确认继续？')) return
  error.value = ''
  loading.value = true
  try {
    await prepareCsrf()
    result.value = (await http.post<MergeResult>('/merges/execute', {
      objectType: objectType.value, sourceIds: sourceIds.value, targetId: targetId.value,
      acceptedTargetFields: acceptedFields.value,
    })).data
    preview.value = undefined
    await loadOptions()
  } catch (e) { error.value = apiMessage(e) } finally { loading.value = false }
}

watch(objectType, () => { result.value = undefined; loadOptions() })
onMounted(loadOptions)
</script>

<template>
  <div class="page-heading"><div><span class="eyebrow">DUPLICATE GOVERNANCE</span><h1>合并管理</h1><p>管理员先核对迁移范围与字段冲突，再执行可追溯的事务性合并。</p></div></div>
  <form class="panel merge-selector" @submit.prevent="showPreview">
    <label>对象类型<select v-model="objectType"><option value="PROJECT">项目</option><option value="TASK">任务</option><option value="REQUIREMENT">需求</option></select></label>
    <label>来源对象（可多选）<select v-model="sourceIds" multiple required><option v-for="item in options" :key="item.id" :value="item.id" :disabled="item.id === targetId">{{ item.label }}</option></select></label>
    <label>目标对象<select v-model="targetId" required><option :value="undefined">请选择目标</option><option v-for="item in options" :key="item.id" :value="item.id" :disabled="sourceIds.includes(item.id)">{{ item.label }}</option></select></label>
    <button class="primary-button" :disabled="loading || !sourceIds.length || !targetId">{{ loading ? '处理中…' : '生成合并预览' }}</button>
  </form>
  <p v-if="error" class="error-message">{{ error }}</p>
  <p v-if="result" class="success-message">合并完成，映射记录 #{{ result.mergeRecordId }} 已保存。</p>

  <section v-if="preview" class="panel merge-preview">
    <div><span class="eyebrow">TARGET</span><h2>{{ preview.target.code }} · {{ preview.target.title }}</h2><p>{{ preview.policy }}</p></div>
    <div class="merge-scope"><article v-for="(count, name) in preview.migrationScope" :key="name"><strong>{{ count }}</strong><span>{{ name }}</span></article></div>
    <h3>字段冲突</h3>
    <div v-if="preview.conflicts.length" class="merge-conflicts">
      <label v-for="item in preview.conflicts" :key="`${item.sourceId}-${item.field}`">
        <input v-model="acceptedFields" type="checkbox" :value="item.field" />
        <span><strong>{{ item.field }}</strong><small>来源 {{ item.sourceId }}：{{ item.sourceValue ?? '空' }} → 保留目标：{{ item.targetValue ?? '空' }}</small></span>
      </label>
    </div>
    <p v-else>未发现字段冲突。</p>
    <button class="danger-button" :disabled="loading || !canExecute" @click="executeMerge">确认并执行合并</button>
  </section>
</template>
