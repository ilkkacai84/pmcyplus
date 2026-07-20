<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { apiMessage, http, prepareCsrf } from '@/api/http'
import type { DeliveryStatus, DeliveryVersion, ProjectDetails, Task, TaskStatus, UserSummary } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()
const project = ref<ProjectDetails | null>(null)
const users = ref<UserSummary[]>([])
const showTaskForm = ref(false)
const completionTask = ref<Task | null>(null)
const reviewingTask = ref<Task | null>(null)
const error = ref('')
const form = reactive({ title: '', description: '', ownerId: undefined as number | undefined, priority: 'MEDIUM', estimatedHours: 8, plannedStartAt: '', plannedEndAt: '' })
const worklog = reactive({ hours: 0, note: '', workedOn: new Date().toISOString().slice(0, 10) })
const review = reactive({ decision: 'ACCEPTED' as DeliveryStatus, opinion: '' })
const columns: { status: TaskStatus; title: string; hint: string }[] = [
  { status: 'TODO', title: '待处理', hint: '尚未开始' },
  { status: 'IN_PROGRESS', title: '进行中', hint: '正在执行' },
  { status: 'BLOCKED', title: '阻塞', hint: '需要协调' },
  { status: 'PENDING_ACCEPTANCE', title: '待验收', hint: '等待确认' },
  { status: 'COMPLETED', title: '已完成', hint: '交付通过' },
]
const tasksByStatus = computed(() => Object.fromEntries(columns.map(column => [column.status, project.value?.tasks.filter(task => task.status === column.status) ?? []])))
const canManage = computed(() => !!project.value && (!!auth.user?.roles.includes('ADMIN') || project.value.project.managerId === auth.user?.id))
const isCustomer = computed(() => auth.user?.userType === 'CUSTOMER')

async function load() {
  const projectResult = await http.get(`/projects/${route.params.id}`)
  project.value = projectResult.data
  if (canManage.value) {
    const userResult = await http.get('/users')
    users.value = userResult.data
    if (!form.ownerId && users.value[0]) form.ownerId = users.value[0].id
  }
}
async function createTask() {
  error.value = ''
  try {
    await prepareCsrf()
    await http.post(`/projects/${route.params.id}/tasks`, { ...form, plannedStartAt: form.plannedStartAt || null, plannedEndAt: form.plannedEndAt || null })
    showTaskForm.value = false; form.title = ''; form.description = ''
    await load()
  } catch (e) { error.value = apiMessage(e) }
}
async function transition(task: Task, status: TaskStatus) {
  try {
    await prepareCsrf()
    await http.patch(`/projects/tasks/${task.id}/status`, null, { params: { status } })
    await load()
  } catch (e) { error.value = apiMessage(e) }
}
function runAction(task: Task) {
  if (task.status === 'IN_PROGRESS') {
    completionTask.value = task
    worklog.hours = Math.max(Number(task.estimatedHours) - Number(task.actualHours), 0.5)
    worklog.note = ''
    return
  }
  const action = nextAction(task)
  if (action) transition(task, action.status)
}
async function submitWorklog() {
  if (!completionTask.value) return
  error.value = ''
  try {
    await prepareCsrf()
    await http.post(`/projects/tasks/${completionTask.value.id}/complete`, worklog)
    completionTask.value = null
    await load()
  } catch (e) { error.value = apiMessage(e) }
}
function latestDelivery(taskId: number): DeliveryVersion | undefined {
  return project.value?.deliveries.find(delivery => delivery.taskId === taskId)
}
function openReview(task: Task, decision: DeliveryStatus) {
  reviewingTask.value = task
  review.decision = decision
  review.opinion = ''
}
async function submitReview() {
  if (!reviewingTask.value) return
  error.value = ''
  try {
    await prepareCsrf()
    await http.post(`/projects/tasks/${reviewingTask.value.id}/review`, review)
    reviewingTask.value = null
    await load()
  } catch (e) { error.value = apiMessage(e) }
}
function nextAction(task: Task): { status: TaskStatus; label: string } | null {
  const isOwner = task.ownerId === auth.user?.id
  if (task.status === 'TODO' && (canManage.value || isOwner)) return { status: 'IN_PROGRESS', label: '开始' }
  if (task.status === 'IN_PROGRESS' && (canManage.value || isOwner)) return { status: 'PENDING_ACCEPTANCE', label: '提交' }
  if (task.status === 'BLOCKED' && (canManage.value || isOwner)) return { status: 'IN_PROGRESS', label: '解除阻塞' }
  return null
}
onMounted(load)
</script>

<template>
  <template v-if="project">
    <div class="project-hero">
      <RouterLink to="/projects" class="back-link">← 返回项目</RouterLink>
      <div class="project-hero-row"><div><span class="code">{{ project.project.code }}</span><h1>{{ project.project.name }}</h1><p>{{ project.description || '暂无项目说明' }}</p></div><button v-if="canManage" class="primary-button" @click="showTaskForm = !showTaskForm">＋ 新建任务</button></div>
      <div class="hero-meta"><span>项目经理 <strong>{{ project.project.managerName }}</strong></span><span>状态 <strong>{{ project.project.status }}</strong></span><span>预算 <strong>¥{{ Number(project.budget).toLocaleString() }}</strong></span><span>任务 <strong>{{ project.tasks.length }}</strong></span></div>
    </div>
    <form v-if="showTaskForm" class="panel form-grid" @submit.prevent="createTask">
      <label>任务名称<input v-model="form.title" required /></label><label>负责人<select v-model="form.ownerId"><option v-for="user in users" :key="user.id" :value="user.id">{{ user.displayName }}</option></select></label>
      <label>优先级<select v-model="form.priority"><option value="LOW">低</option><option value="MEDIUM">中</option><option value="HIGH">高</option><option value="URGENT">紧急</option></select></label><label>预计工时<input v-model="form.estimatedHours" type="number" min="0" step="0.5" /></label>
      <label>计划开始<input v-model="form.plannedStartAt" type="datetime-local" /></label><label>计划结束<input v-model="form.plannedEndAt" type="datetime-local" /></label>
      <label class="span-2">说明<textarea v-model="form.description" rows="2" /></label><p v-if="error" class="error-message span-2">{{ error }}</p>
      <div class="form-actions span-2"><button type="button" class="secondary-button" @click="showTaskForm = false">取消</button><button class="primary-button">保存任务</button></div>
    </form>
    <p v-if="error && !showTaskForm" class="error-message">{{ error }}</p>
    <form v-if="completionTask" class="panel form-grid worklog-form" @submit.prevent="submitWorklog">
      <div class="span-2"><span class="eyebrow">COMPLETE TASK</span><h2>提交“{{ completionTask.title }}”的完成工时</h2></div>
      <label>实际工时<input v-model="worklog.hours" type="number" min="0.01" step="0.5" required /></label><label>工作日期<input v-model="worklog.workedOn" type="date" required /></label>
      <label class="span-2">工作说明<textarea v-model="worklog.note" rows="2" /></label>
      <div class="form-actions span-2"><button type="button" class="secondary-button" @click="completionTask = null">取消</button><button class="primary-button">提交验收</button></div>
    </form>
    <form v-if="reviewingTask" class="panel form-grid review-form" @submit.prevent="submitReview">
      <div class="span-2"><span class="eyebrow">CUSTOMER REVIEW</span><h2>验收“{{ reviewingTask.title }}” · {{ review.decision === 'ACCEPTED' ? '通过' : review.decision === 'REJECTED' ? '驳回' : '要求修改' }}</h2></div>
      <label class="span-2">验收意见<textarea v-model="review.opinion" rows="3" :required="review.decision !== 'ACCEPTED'" :placeholder="review.decision === 'ACCEPTED' ? '可填写验收说明' : '请说明需要修改的内容'" /></label>
      <div class="form-actions span-2"><button type="button" class="secondary-button" @click="reviewingTask = null">取消</button><button class="primary-button">确认验收结果</button></div>
    </form>
    <div class="board-header"><div><span class="eyebrow">KANBAN</span><h2>任务看板</h2></div><span>按状态跟踪项目交付进度</span></div>
    <div class="kanban-board">
      <section v-for="column in columns" :key="column.status" class="kanban-column">
        <header><div><strong>{{ column.title }}</strong><small>{{ column.hint }}</small></div><span>{{ tasksByStatus[column.status].length }}</span></header>
        <article v-for="task in tasksByStatus[column.status]" :key="task.id" class="task-card">
          <div class="task-priority" :data-priority="task.priority">{{ task.priority }}</div><h3>{{ task.title }}</h3><p>{{ task.description || '暂无说明' }}</p>
          <div class="task-meta"><span class="avatar small">{{ task.ownerName.slice(0, 1) }}</span><span>{{ task.ownerName }}</span><span>{{ task.estimatedHours }}h</span></div>
          <div v-if="latestDelivery(task.id)" class="delivery-note">
            <strong>交付 v{{ latestDelivery(task.id)!.versionNo }} · {{ latestDelivery(task.id)!.status }}</strong>
            <span v-if="latestDelivery(task.id)!.reviewOpinion">{{ latestDelivery(task.id)!.reviewOpinion }}</span>
          </div>
          <button v-if="nextAction(task)" class="task-action" @click="runAction(task)">{{ nextAction(task)!.label }} →</button>
          <div v-if="task.status === 'PENDING_ACCEPTANCE' && isCustomer" class="review-actions">
            <button class="task-action positive" @click="openReview(task, 'ACCEPTED')">通过</button>
            <button class="task-action danger" @click="openReview(task, 'REJECTED')">驳回</button>
            <button class="task-action" @click="openReview(task, 'CHANGES_REQUESTED')">要求修改</button>
          </div>
        </article>
        <div v-if="!tasksByStatus[column.status].length" class="column-empty">暂无任务</div>
      </section>
    </div>
  </template>
  <div v-else class="loading-state">正在加载项目…</div>
</template>
