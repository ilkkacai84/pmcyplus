<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { apiMessage, http, prepareCsrf } from '@/api/http'
import type { ApprovalInstance, ProjectSummary, Requirement, RequirementStatus, UserSummary } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const requirements = ref<Requirement[]>([])
const users = ref<UserSummary[]>([])
const projects = ref<ProjectSummary[]>([])
const showForm = ref(false)
const assigning = ref<Requirement | null>(null)
const creatingProject = ref<Requirement | null>(null)
const reviewing = ref<Requirement | null>(null)
const reviewDecision = ref<'APPROVED' | 'REJECTED'>('APPROVED')
const reviewOpinion = ref('')
const approvalHistory = ref<ApprovalInstance[]>([])
const historyRequirement = ref<Requirement | null>(null)
const error = ref('')
const form = reactive({ source: 'WEB', title: '', description: '', priority: 'MEDIUM', projectType: 'INTERNAL' })
const assignment = reactive({ assigneeId: undefined as number | undefined, projectId: undefined as number | undefined, notifyUserIds: [] as number[] })
const projectForm = reactive({ name: '', description: '', projectType: 'INTERNAL', priority: 'MEDIUM', managerId: undefined as number | undefined, plannedStartAt: '', plannedEndAt: '' })
const canAssign = computed(() => !!auth.user?.roles.some(role => role === 'ADMIN' || role === 'PROJECT_MANAGER'))
const isAdmin = computed(() => !!auth.user?.roles.includes('ADMIN'))
const internalUsers = computed(() => users.value.filter(user => user.userType === 'INTERNAL'))
const managers = computed(() => users.value.filter(user => user.roles.some(role => role === 'ADMIN' || role === 'PROJECT_MANAGER')))
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
    await prepareCsrf()
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
  assignment.notifyUserIds = []
}

async function assignRequirement() {
  if (!assigning.value || !assignment.assigneeId) return
  error.value = ''
  try {
    await prepareCsrf()
    await http.post(`/requirements/${assigning.value.id}/assign`, {
      assigneeId: assignment.assigneeId,
      projectId: assignment.projectId || null,
      notifyUserIds: assignment.notifyUserIds,
    })
    assigning.value = null
    await load()
  } catch (e) { error.value = apiMessage(e) }
}

async function transition(item: Requirement, status: RequirementStatus) {
  error.value = ''
  try {
    await prepareCsrf()
    await http.patch(`/requirements/${item.id}/status`, null, { params: { status } })
    await load()
  } catch (e) { error.value = apiMessage(e) }
}

function openProjectForm(item: Requirement) {
  creatingProject.value = item
  projectForm.name = item.title
  projectForm.description = item.description ?? ''
  projectForm.priority = item.priority
  projectForm.managerId = managers.value.some(user => user.id === item.assigneeId)
    ? item.assigneeId
    : managers.value[0]?.id
}

async function createProject() {
  if (!creatingProject.value || !projectForm.managerId) return
  error.value = ''
  try {
    await prepareCsrf()
    await http.post(`/requirements/${creatingProject.value.id}/project`, {
      ...projectForm,
      plannedStartAt: projectForm.plannedStartAt || null,
      plannedEndAt: projectForm.plannedEndAt || null,
    })
    creatingProject.value = null
    await load()
  } catch (e) { error.value = apiMessage(e) }
}

function canSubmitApproval(item: Requirement) {
  return ['REFINING', 'REJECTED'].includes(item.status) && item.assigneeId === auth.user?.id
}

async function submitApproval(item: Requirement) {
  try { await prepareCsrf(); await http.post(`/requirements/${item.id}/approval/submit`); await load() }
  catch (e) { error.value = apiMessage(e) }
}

function openReview(item: Requirement, decision: 'APPROVED' | 'REJECTED') {
  reviewing.value = item; reviewDecision.value = decision; reviewOpinion.value = ''
}

async function decideApproval() {
  if (!reviewing.value) return
  try {
    await prepareCsrf()
    await http.post(`/requirements/${reviewing.value.id}/approval/decision`, {
      decision: reviewDecision.value, opinion: reviewOpinion.value || null,
    })
    reviewing.value = null; await load()
  } catch (e) { error.value = apiMessage(e) }
}

async function withdrawApproval(item: Requirement) {
  try { await prepareCsrf(); await http.post(`/requirements/${item.id}/approval/withdraw`, { opinion: '需求负责人撤回' }); await load() }
  catch (e) { error.value = apiMessage(e) }
}

async function showHistory(item: Requirement) {
  try { approvalHistory.value = (await http.get(`/requirements/${item.id}/approvals`)).data; historyRequirement.value = item }
  catch (e) { error.value = apiMessage(e) }
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
    <label>项目类型<select v-model="form.projectType"><option value="INTERNAL">内部项目</option><option value="TEMPORARY">临时任务</option></select></label>
    <label class="span-2">详细说明<textarea v-model="form.description" rows="4" /></label>
    <div class="form-actions span-2"><button type="button" class="secondary-button" @click="showForm = false">取消</button><button class="primary-button">提交到需求池</button></div>
  </form>

  <form v-if="reviewing" class="panel form-grid" @submit.prevent="decideApproval">
    <div class="span-2"><span class="eyebrow">APPROVAL DECISION</span><h2>{{ reviewDecision === 'APPROVED' ? '批准' : '驳回' }} {{ reviewing.requirementNo }}</h2></div>
    <label class="span-2">审批意见<textarea v-model="reviewOpinion" rows="3" :required="reviewDecision === 'REJECTED'" /></label>
    <div class="form-actions span-2"><button type="button" class="secondary-button" @click="reviewing = null">取消</button><button class="primary-button">确认提交</button></div>
  </form>

  <section v-if="historyRequirement" class="panel approval-history">
    <div class="approval-config-title"><div><span class="eyebrow">APPROVAL HISTORY</span><h2>{{ historyRequirement.requirementNo }} 审批记录</h2></div><button class="inline-button" @click="historyRequirement = null">关闭</button></div>
    <article v-for="instance in approvalHistory" :key="instance.id" class="approval-history-item"><strong>第 {{ instance.id }} 次 · {{ instance.status }}</strong><small>提交人 {{ instance.submitterName }} · 当前步骤 {{ instance.currentStepName || instance.currentStep }}</small><p v-for="action in instance.actions" :key="action.id">{{ action.actorName }}：{{ action.decision }}<template v-if="action.opinion"> · {{ action.opinion }}</template></p></article>
  </section>

  <form v-if="assigning" class="panel form-grid" @submit.prevent="assignRequirement">
    <div class="span-2"><span class="eyebrow">ASSIGN REQUIREMENT</span><h2>分派 {{ assigning.requirementNo }} · {{ assigning.title }}</h2></div>
    <label>负责人<select v-model="assignment.assigneeId" required><option v-for="user in internalUsers" :key="user.id" :value="user.id">{{ user.displayName }}</option></select></label>
    <label>关联项目<select v-model="assignment.projectId"><option :value="undefined">暂不关联项目</option><option v-for="project in projects" :key="project.id" :value="project.id">{{ project.code }} · {{ project.name }}</option></select></label>
    <label class="span-2">指定通知用户<select v-model="assignment.notifyUserIds" multiple><option v-for="user in internalUsers.filter(item => item.id !== assignment.assigneeId)" :key="user.id" :value="user.id">{{ user.displayName }} · {{ user.departmentName || '未设置部门' }}</option></select></label>
    <div class="form-actions span-2"><button type="button" class="secondary-button" @click="assigning = null">取消</button><button class="primary-button">确认分派</button></div>
  </form>

  <form v-if="creatingProject" class="panel form-grid" @submit.prevent="createProject">
    <div class="span-2"><span class="eyebrow">CREATE PROJECT</span><h2>从 {{ creatingProject.requirementNo }} 创建项目</h2></div>
    <label>项目名称<input v-model="projectForm.name" required /></label>
    <label>项目经理<select v-model="projectForm.managerId" required><option v-for="user in managers" :key="user.id" :value="user.id">{{ user.displayName }}</option></select></label>
    <label>项目类型<select v-model="projectForm.projectType"><option value="INTERNAL">内部项目</option><option value="TEMPORARY">临时任务</option></select></label>
    <label>优先级<select v-model="projectForm.priority"><option value="LOW">低</option><option value="MEDIUM">中</option><option value="HIGH">高</option><option value="URGENT">紧急</option></select></label>
    <label>计划开始<input v-model="projectForm.plannedStartAt" type="datetime-local" /></label>
    <label>计划结束<input v-model="projectForm.plannedEndAt" type="datetime-local" /></label>
    <label class="span-2">项目说明<textarea v-model="projectForm.description" rows="3" /></label>
    <div class="form-actions span-2"><button type="button" class="secondary-button" @click="creatingProject = null">取消</button><button class="primary-button">创建并关联项目</button></div>
  </form>

  <p v-if="error" class="error-message page-error">{{ error }}</p>
  <section class="panel table-panel">
    <div class="table-row requirement-row table-head"><span>编号 / 需求</span><span>提交人</span><span>负责人</span><span>关联项目</span><span>状态</span><span>操作</span></div>
    <div v-for="item in requirements" :key="item.id" class="table-row requirement-row">
      <span><strong>{{ item.requirementNo }}</strong><small>{{ item.title }}</small></span>
      <span>{{ item.submitterName }}</span>
      <span>{{ item.assigneeName || '待分派' }}</span>
      <span><RouterLink v-if="item.projectId" class="text-link" :to="`/projects/${item.projectId}`">查看项目</RouterLink><template v-else>未关联</template></span>
      <span><i class="status-pill" :data-status="item.status">{{ statusText[item.status] }}</i></span>
      <span class="row-actions">
        <button v-if="canAssign && ['UNASSIGNED', 'REFINING', 'REJECTED'].includes(item.status)" class="inline-button" @click="openAssignment(item)">分派</button>
        <button v-if="canSubmitApproval(item)" class="inline-button" @click="submitApproval(item)">{{ item.status === 'REJECTED' ? '重新提交' : '提交审批' }}</button>
        <template v-if="auth.user?.userType === 'INTERNAL' && item.status === 'PENDING_APPROVAL'">
          <button class="inline-button positive" @click="openReview(item, 'APPROVED')">批准</button>
          <button class="inline-button danger" @click="openReview(item, 'REJECTED')">驳回</button>
        </template>
        <button v-if="item.status === 'PENDING_APPROVAL' && (isAdmin || item.assigneeId === auth.user?.id)" class="inline-button" @click="withdrawApproval(item)">撤回</button>
        <button v-if="item.status === 'REJECTED' && (isAdmin || item.assigneeId === auth.user?.id)" class="inline-button" @click="transition(item, 'REFINING')">重新完善</button>
        <button v-if="item.status === 'APPROVED' && !item.projectId && (isAdmin || item.assigneeId === auth.user?.id)" class="inline-button positive" @click="openProjectForm(item)">创建项目</button>
        <button v-if="item.status !== 'UNASSIGNED'" class="inline-button" @click="showHistory(item)">审批记录</button>
      </span>
    </div>
    <div v-if="!requirements.length" class="empty-state"><strong>需求池为空</strong><span>提交第一条需求，开始业务流程。</span></div>
  </section>
</template>
