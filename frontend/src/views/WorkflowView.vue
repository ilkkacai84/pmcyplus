<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiMessage, http, prepareCsrf } from '@/api/http'
import type { Role, WorkflowApprovalStep, WorkflowTemplate, WorkflowTransition } from '@/api/types'

const templates = ref<WorkflowTemplate[]>([])
const error = ref('')
const savedId = ref<number>()
const roles: { value: Role; label: string }[] = [
  { value: 'ADMIN', label: '管理员' }, { value: 'PROJECT_MANAGER', label: '项目经理' },
  { value: 'MEMBER', label: '项目成员' }, { value: 'DEPARTMENT_MANAGER', label: '部门负责人' },
  { value: 'CUSTOMER', label: '客户' },
]

async function load() {
  error.value = ''
  try { templates.value = (await http.get('/workflows')).data }
  catch (e) { error.value = apiMessage(e) }
}

function toggleRole(transition: WorkflowTransition, role: Role, checked: boolean) {
  transition.allowedRoles = checked
    ? [...new Set([...transition.allowedRoles, role])]
    : transition.allowedRoles.filter(item => item !== role)
}

async function save(transition: WorkflowTransition) {
  error.value = ''
  try {
    await prepareCsrf()
    await http.put(`/workflows/transitions/${transition.id}`, {
      allowedRoles: transition.allowedRoles,
      enabled: transition.enabled,
      requiresReason: transition.requiresReason,
      notificationEvent: transition.notificationEvent || null,
    })
    savedId.value = transition.id
    window.setTimeout(() => { if (savedId.value === transition.id) savedId.value = undefined }, 1500)
  } catch (e) { error.value = apiMessage(e) }
}

async function saveStep(step: WorkflowApprovalStep) {
  error.value = ''
  try {
    await prepareCsrf()
    await http.put(`/workflows/approval-steps/${step.id}`, {
      name: step.name, approverRole: step.approverRole, active: step.active,
    })
    savedId.value = step.id
  } catch (e) { error.value = apiMessage(e) }
}

async function addStep(template: WorkflowTemplate) {
  try {
    await prepareCsrf()
    await http.post(`/workflows/${template.id}/approval-steps`, {
      name: `第 ${template.approvalSteps.length + 1} 级审批`, approverRole: 'ADMIN', active: true,
    })
    await load()
  } catch (e) { error.value = apiMessage(e) }
}

onMounted(load)
</script>

<template>
  <div class="page-heading"><div><span class="eyebrow">WORKFLOW CONFIG</span><h1>流程模板</h1><p>内部项目与临时任务分别维护状态迁移、操作角色、原因和通知事件。</p></div></div>
  <p v-if="error" class="error-message page-error">{{ error }}</p>
  <section v-for="template in templates" :key="template.id" class="panel workflow-panel">
    <div class="workflow-title"><div><span class="eyebrow">{{ template.projectType }}</span><h2>{{ template.name }}</h2></div><i class="status-pill" data-status="ACTIVE">{{ template.active ? '启用' : '停用' }}</i></div>
    <div class="approval-config">
      <div class="approval-config-title"><strong>顺序审批步骤</strong><button class="inline-button" @click="addStep(template)">＋ 增加步骤</button></div>
      <div v-for="step in template.approvalSteps" :key="step.id" class="approval-step-row">
        <span class="step-number">{{ step.stepOrder }}</span><input v-model="step.name" class="compact-input">
        <select v-model="step.approverRole" class="compact-input"><option v-for="role in roles" :key="role.value" :value="role.value">{{ role.label }}</option></select>
        <label><input v-model="step.active" type="checkbox">启用</label>
        <button class="inline-button positive" @click="saveStep(step)">{{ savedId === step.id ? '已保存' : '保存' }}</button>
      </div>
    </div>
    <div class="workflow-transition workflow-head"><span>状态迁移</span><span>允许角色</span><span>规则</span><span>通知事件</span><span></span></div>
    <div v-for="transition in template.transitions" :key="transition.id" class="workflow-transition">
      <span><strong>{{ transition.fromStatus }} → {{ transition.toStatus }}</strong><small>{{ transition.objectType }}</small></span>
      <span class="role-options"><label v-for="role in roles" :key="role.value"><input type="checkbox" :checked="transition.allowedRoles.includes(role.value)" @change="toggleRole(transition, role.value, ($event.target as HTMLInputElement).checked)">{{ role.label }}</label></span>
      <span class="rule-options"><label><input v-model="transition.enabled" type="checkbox">启用</label><label><input v-model="transition.requiresReason" type="checkbox">必填原因</label></span>
      <span><input v-model="transition.notificationEvent" class="compact-input" placeholder="通知事件代码"></span>
      <span><button class="inline-button positive" :disabled="!transition.allowedRoles.length" @click="save(transition)">{{ savedId === transition.id ? '已保存' : '保存' }}</button></span>
    </div>
  </section>
</template>
