<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { apiMessage, http } from '@/api/http'
import type { ProjectSummary, UserSummary } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const projects = ref<ProjectSummary[]>([])
const users = ref<UserSummary[]>([])
const showForm = ref(false)
const error = ref('')
const form = reactive({ name: '', description: '', projectType: 'INTERNAL', priority: 'MEDIUM', managerId: undefined as number | undefined, plannedStartAt: '', plannedEndAt: '' })
const managers = computed(() => users.value.filter(u => u.roles.includes('PROJECT_MANAGER') || u.roles.includes('ADMIN')))
const canCreate = computed(() => !!auth.user?.roles.some(role => role === 'ADMIN' || role === 'PROJECT_MANAGER'))

async function load() {
  const projectResult = await http.get('/projects')
  projects.value = projectResult.data
  if (canCreate.value) {
    const userResult = await http.get('/users')
    users.value = userResult.data
    if (!form.managerId && managers.value[0]) form.managerId = managers.value[0].id
  }
}
async function createProject() {
  error.value = ''
  try {
    await http.get('/auth/csrf')
    await http.post('/projects', { ...form, plannedStartAt: form.plannedStartAt || null, plannedEndAt: form.plannedEndAt || null })
    showForm.value = false
    form.name = ''; form.description = ''
    await load()
  } catch (e) { error.value = apiMessage(e) }
}
onMounted(load)

const statusText: Record<string, string> = { DRAFT: '草稿', ACTIVE: '进行中', BLOCKED: '阻塞', COMPLETED: '已完成', CANCELLED: '已取消', MERGED: '已合并' }
</script>

<template>
  <div class="page-heading"><div><span class="eyebrow">PROJECTS</span><h1>项目中心</h1><p>统一查看项目健康度、负责人和计划节点。</p></div><button v-if="canCreate" class="primary-button" @click="showForm = !showForm">＋ 创建项目</button></div>
  <form v-if="showForm" class="panel form-grid" @submit.prevent="createProject">
    <label>项目名称<input v-model="form.name" required placeholder="例如：客户门户交付" /></label>
    <label>项目类型<select v-model="form.projectType"><option value="INTERNAL">内部项目</option><option value="TEMPORARY">临时任务</option></select></label>
    <label>优先级<select v-model="form.priority"><option value="LOW">低</option><option value="MEDIUM">中</option><option value="HIGH">高</option><option value="URGENT">紧急</option></select></label>
    <label>项目经理<select v-model="form.managerId"><option v-for="user in managers" :key="user.id" :value="user.id">{{ user.displayName }}</option></select></label>
    <label>计划开始<input v-model="form.plannedStartAt" type="datetime-local" /></label>
    <label>计划结束<input v-model="form.plannedEndAt" type="datetime-local" /></label>
    <label class="span-2">说明<textarea v-model="form.description" rows="3" /></label>
    <p v-if="error" class="error-message span-2">{{ error }}</p>
    <div class="form-actions span-2"><button type="button" class="secondary-button" @click="showForm = false">取消</button><button class="primary-button">保存项目</button></div>
  </form>
  <div class="project-grid">
    <RouterLink v-for="project in projects" :key="project.id" :to="`/projects/${project.id}`" class="project-card">
      <div class="project-card-top"><span class="code">{{ project.code }}</span><span class="status-pill" :data-status="project.status">{{ statusText[project.status] }}</span></div>
      <h2>{{ project.name }}</h2><p>{{ project.projectType === 'INTERNAL' ? '内部项目' : '临时任务' }} · {{ project.managerName }}</p>
      <div class="project-footer"><span>优先级 {{ project.priority }}</span><span>{{ project.plannedEndAt ? new Date(project.plannedEndAt).toLocaleDateString() : '未设截止时间' }}</span></div>
    </RouterLink>
    <div v-if="!projects.length" class="empty-state"><strong>还没有项目</strong><span>创建第一个项目，开始拆分里程碑和任务。</span></div>
  </div>
</template>
