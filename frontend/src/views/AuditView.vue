<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiMessage, http } from '@/api/http'
import type { AuditLog } from '@/api/types'

const logs = ref<AuditLog[]>([])
const error = ref('')
const actionNames: Record<string, string> = {
  USER_CREATED: '创建账号', DEPARTMENT_CREATED: '创建部门', DEPARTMENT_UPDATED: '更新部门',
  REQUIREMENT_CREATED: '创建需求', REQUIREMENT_ASSIGNED: '分派需求',
  REQUIREMENT_STATUS_CHANGED: '变更需求状态', REQUIREMENT_LINKED_TO_PROJECT: '需求关联项目',
  PROJECT_CREATED: '创建项目', PROJECT_STATUS_CHANGED: '变更项目状态',
  MILESTONE_CREATED: '创建里程碑', TASK_CREATED: '创建任务', TASK_STATUS_CHANGED: '变更任务状态',
  TASK_DELIVERED: '提交任务交付', DELIVERY_REVIEWED: '客户验收交付',
}

function detail(value?: string) {
  if (!value) return '—'
  try {
    return Object.entries(JSON.parse(value)).map(([key, item]) => `${key}: ${item}`).join(' · ')
  } catch { return value }
}

async function load() {
  error.value = ''
  try { logs.value = (await http.get('/audit-logs')).data }
  catch (e) { error.value = apiMessage(e) }
}

onMounted(load)
</script>

<template>
  <div class="page-heading"><div><span class="eyebrow">AUDIT TRAIL</span><h1>审计日志</h1><p>集中追溯权限、需求、项目、任务和验收等关键操作。</p></div></div>
  <p v-if="error" class="error-message page-error">{{ error }}</p>
  <section class="panel table-panel">
    <div class="table-row audit-row table-head"><span>操作时间</span><span>操作人</span><span>动作</span><span>业务对象</span><span>变更明细</span></div>
    <div v-for="log in logs" :key="log.id" class="table-row audit-row">
      <span>{{ new Date(log.createdAt).toLocaleString() }}</span>
      <span><strong>{{ log.actorName }}</strong></span>
      <span>{{ actionNames[log.action] || log.action }}</span>
      <span>{{ log.objectType }} #{{ log.objectId || '—' }}</span>
      <span class="audit-detail">{{ detail(log.detailJson) }}</span>
    </div>
    <div v-if="!logs.length" class="empty-state"><strong>暂无审计记录</strong><span>关键业务操作会自动记录在这里。</span></div>
  </section>
</template>
