<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { apiMessage, http } from '@/api/http'
import type { DepartmentScope, UserSummary } from '@/api/types'

const scope = ref<DepartmentScope>({ tasks: [] })
const members = ref<UserSummary[]>([])
const error = ref('')
const openTasks = computed(() => scope.value.tasks.filter(task => !['COMPLETED', 'CANCELLED', 'MERGED'].includes(task.status)))
const overdue = computed(() => openTasks.value.filter(task => task.plannedEndAt && new Date(task.plannedEndAt) < new Date()))

async function load() {
  error.value = ''
  try {
    const [scopeResult, userResult] = await Promise.all([http.get('/departments/my-scope'), http.get('/users')])
    scope.value = scopeResult.data
    members.value = userResult.data
  } catch (e) { error.value = apiMessage(e) }
}

onMounted(load)
</script>

<template>
  <div class="page-heading"><div><span class="eyebrow">DEPARTMENT SCOPE</span><h1>{{ scope.department?.name || '部门视图' }}</h1><p>仅展示本部门成员与任务，不扩展完整项目访问权。</p></div></div>
  <p v-if="error" class="error-message page-error">{{ error }}</p>
  <div class="metric-grid">
    <article class="metric-card green"><span>部门成员</span><strong>{{ members.length }}</strong><small>当前部门账号</small></article>
    <article class="metric-card sand"><span>进行中任务</span><strong>{{ openTasks.length }}</strong><small>未完成、取消或合并</small></article>
    <article class="metric-card coral"><span>逾期任务</span><strong>{{ overdue.length }}</strong><small>需要协调排期</small></article>
  </div>
  <section class="panel table-panel">
    <div class="table-row department-task-row table-head"><span>任务</span><span>所属项目</span><span>负责人</span><span>截止时间</span><span>状态</span></div>
    <div v-for="task in scope.tasks" :key="task.id" class="table-row department-task-row"><span><strong>{{ task.title }}</strong></span><span>{{ task.projectName }}</span><span>{{ task.ownerName }}</span><span>{{ task.plannedEndAt ? new Date(task.plannedEndAt).toLocaleString() : '未设置' }}</span><span><i class="status-pill" :data-status="task.status">{{ task.status }}</i></span></div>
    <div v-if="!scope.tasks.length" class="empty-state"><strong>本部门暂无任务</strong><span>项目经理分配任务后将在这里显示。</span></div>
  </section>
</template>
