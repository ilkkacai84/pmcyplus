<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { apiMessage, http, prepareCsrf } from '@/api/http'
import type { ProjectSummary, ResourceReport } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
const auth=useAuthStore();const report=ref<ResourceReport>();const projects=ref<ProjectSummary[]>([]);const error=ref('')
const today=new Date();const query=reactive({from:new Date(today.getFullYear(),today.getMonth(),1).toISOString().slice(0,10),to:new Date(today.getFullYear(),today.getMonth()+1,0).toISOString().slice(0,10),projectId:undefined as number|undefined})
const calendar=reactive({date:'',workday:false,standardHours:0,note:''})
async function load(){try{report.value=(await http.get('/resources',{params:query})).data;if(auth.user?.roles.includes('PROJECT_MANAGER'))projects.value=(await http.get('/projects')).data}catch(e){error.value=apiMessage(e)}}
async function saveDay(){try{await prepareCsrf();await http.put('/resources/calendar',calendar);await load()}catch(e){error.value=apiMessage(e)}}
onMounted(load)
</script>
<template><div class="page-heading"><div><span class="eyebrow">RESOURCE PLANNING</span><h1>资源负荷</h1><p>{{ report?.calculationRule }}</p></div></div>
<form class="panel form-grid" @submit.prevent="load"><label>开始日期<input v-model="query.from" type="date"></label><label>结束日期<input v-model="query.to" type="date"></label><label v-if="projects.length">项目范围<select v-model="query.projectId"><option :value="undefined">仅本人</option><option v-for="p in projects" :key="p.id" :value="p.id">{{ p.name }}</option></select></label><div class="form-actions"><button class="primary-button">刷新负荷</button></div></form>
<form v-if="auth.user?.roles.includes('ADMIN')" class="panel form-grid" @submit.prevent="saveDay"><label>节假日/调休日<input v-model="calendar.date" type="date" required></label><label class="check-label"><input v-model="calendar.workday" type="checkbox">设为工作日</label><label>标准工时<input v-model.number="calendar.standardHours" type="number" min="0" step="0.5"></label><label>说明<input v-model="calendar.note"></label><div class="form-actions span-2"><button class="secondary-button">保存日历规则</button></div></form>
<p v-if="error" class="error-message">{{ error }}</p><section class="panel table-panel"><div class="table-row resource-row table-head"><span>成员</span><span>部门</span><span>可用工时</span><span>已分配</span><span>实际工时</span><span>负荷率</span></div><div v-for="m in report?.members" :key="m.userId" class="table-row resource-row" :class="{conflict:m.conflict}"><span><strong>{{ m.userName }}</strong><small v-if="m.conflict">存在排期冲突</small></span><span>{{ m.departmentName||'未分配' }}</span><span>{{ m.capacityHours }}h</span><span>{{ m.allocatedHours }}h</span><span>{{ m.actualHours }}h</span><span><i class="status-pill" :data-status="m.conflict?'BLOCKED':'ACTIVE'">{{ m.loadRate }}%</i></span></div></section></template>
