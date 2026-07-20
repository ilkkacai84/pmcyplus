<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { apiMessage, http } from '@/api/http'
import type { ProjectSummary, Requirement, RequirementStatus, UserSummary } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const requirements = ref<Requirement[]>([])
const users = ref<UserSummary[]>([])
const projects = ref<ProjectSummary[]>([])
const showForm = ref(false)
const assigning = ref<Requirement | null>(null)
const error = ref('')
const form = reactive({ source: 'WEB', title: '', description: '', priority: 'MEDIUM' })
const assignment = reactive({ assigneeId: undefined as number | undefined, projectId: undefined as number | undefined })
const canAssign = computed(() => !!auth.user?.roles.some(role => role === 'ADMIN' || role === 'PROJECT_MANAGER'))
const isAdmin = computed(() => !!auth.user?.roles.includes('ADMIN'))
const internalUsers = computed(() => users.value.filter(user => user.userType === 'INTERNAL'))
const statusText: Record<RequirementStatus, string> = {
  UNASSIGNED: '待分派', REFINING: '完善中', PENDING_APPROVAL: '待审批', APPROVED: '已批准',
  REJECTED: '已驳回', DELIVERED: '已交付', ACCEPTED: '已验收', MERGED: '已合并',
}

async function load() {
  error.value = ''
  try {
    requirements.value = (await http.get('/requirements')).data
    if (canAssign.value) {
      const [userResult, projectResult] = await Promise.all([http.get('/users'), http.get('/projects')])
      users.value = userResult.data
      projects.value = projectResult.data
    }
  } catch (e) { error.value = apiMessage(e) }
}

async function createRequirement() {
  error.value = ''
  try {
    await http.get('/auth/csrf')
    await http.post('/requirements', form)
    showForm.value = false
    form.title = ''; form.description = ''
    await load()
  } catch (e) { error.value = apiMessage(e) }
}

function openAssignment(item: Requirement) {
  assigning.value = item
  assignment.assigneeId = item.assigneeId ?? internalUsers.value[0]?.id
  assignment.projectId = item.projectId
}

async function assignRequirement() {
  if (!assigning.value || !assignment.assigneeId) return
  error.value = ''
  try {
    await http.get('/auth/csrf')
    await http.post(`/requirements/${assigning.value.id}/assign`, {
      assigneeId: assignment.assigneeId,
      projectId: assignment.projectId || null,
    })
    assigning.value = null
    await load()
  } catch (e) { error.value = apiMessage(e) }
}

async function transition(item: Requirement, status: RequirementStatus) {
  error.value = ''
  try {
    await http.get('/auth/csrf')
    await http.patch(`/requirements/${item.id}/status`, null, { params: { status } })
    await load()
  } catch (e) { error.value = apiMessage(e) }
}

function canSubmitApproval(item: Requirement) {
  return item.status === 'REFINING' && item.assigneeId === auth.user?.id
}

onMounted(load)
</script>

<template>
  <div class="page-heading">
    <div><span class="eyebrow">REQUIREMENTS</span><h1>需求中心</h1><p>所有入口汇总到统一需求池，再进入项目执行。</p></div>
    <button class="primary-button" @click="showForm = !showForm">＋ 提交需求</button>
  </div>

  <form v-if="showForm" class="panel form-grid" @submit.prevent="createRequirement">
    <label>需求标题<input v-model="form.title" required /></label>
    <label>来源<select v-model="form.source"><option value="WEB">系统表单</option><option value="EMAIL">邮件</option><option value="WECHAT">企业微信</option><option value="MOBILE">移动端</option></select></label>
    <label>优先级<select v-model="form.priority"><option value="LOW">低</option><option value="MEDIUM">中</option><option value="HIGH">高</option><option value="URGENT">紧急</option></select></label>
    <label class="span-2">详细说明<textarea v-model="form.description" rows="4" /></label>
    <div class="form-actions span-2"><button type="button" class="secondary-button" @click="showForm = false">取消</button><button class="primary-button">提交到需求池</button></div>
  </form>

  <form v-if="assigning" class="panel form-grid" @submit.prevent="assignRequirement">
    <div class="span-2"><span class="eyebrow">ASSIGN REQUIREMENT</span><h2>分派 {{ assigning.requirementNo }} · {{ assigning.title }}</h2></div>
    <label>负责人<select v-model="assignment.assigneeId" required><option v-for="user in internalUsers" :key="user.id" :value="user.id">{{ user.displayName }}</option></select></label>
    <label>关联项目<select v-model="assignment.projectId"><option :value="undefined">暂不关联项目</option><option v-for="project in projects" :key="project.id" :value="project.id">{{ project.code }} · {{ project.name }}</option></select></label>
    <div class="form-actions span-2"><button type="button" class="secondary-button" @click="assigning = null">取消</button><button class="primary-button">确认分派</button></div>
  </form>

  <p v-if="error" class="error-message page-error">{{ error }}</p>
  <section class="panel table-panel">
    <div class="table-row requirement-row table-head"><span>编号 / 需求</span><span>提交人</span><span>负责人</span><span>关联项目</span><span>状态</span><span>操作</span></div>
    <div v-for="item in requirements" :key="item.id" class="table-row requirement-row">
      <span><strong>{{ item.requirementNo }}</strong><small>{{ item.title }}</small></span>
      <span>{{ item.submitterName }}</span>
      <span>{{ item.assigneeName || '待分派' }}</span>
      <span>{{ item.projectId ? `#${item.projectId}` : '未关联' }}</span>
      <span><i class="status-pill" :data-status="item.status">{{ statusText[item.status] }}</i></span>
      <span class="row-actions">
        <button v-if="canAssign && ['UNASSIGNED', 'REFINING', 'REJECTED'].includes(item.status)" class="inline-button" @click="openAssignment(item)">分派</button>
        <button v-if="canSubmitApproval(item)" class="inline-button" @click="transition(item, 'PENDING_APPROVAL')">提交审批</button>
        <template v-if="isAdmin && item.status === 'PENDING_APPROVAL'">
          <button class="inline-button positive" @click="transition(item, 'APPROVED')">批准</button>
          <button class="inline-button danger" @click="transition(item, 'REJECTED')">驳回</button>
        </template>
        <button v-if="item.status === 'REJECTED' && (isAdmin || item.assigneeId === auth.user?.id)" class="inline-button" @click="transition(item, 'REFINING')">重新完善</button>
      </span>
    </div>
    <div v-if="!requirements.length" class="empty-state"><strong>需求池为空</strong><span>提交第一条需求，开始业务流程。</span></div>
  </section>
</template>
