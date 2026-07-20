<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { http } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

interface Dashboard { openTasks: number; overdueTasks: number; unassignedRequirements: number }
const data = ref<Dashboard>({ openTasks: 0, overdueTasks: 0, unassignedRequirements: 0 })
const auth = useAuthStore()
onMounted(async () => { data.value = (await http.get<Dashboard>('/dashboard')).data })
</script>

<template>
  <div class="page-heading">
    <div><span class="eyebrow">TODAY</span><h1>早上好，{{ auth.user?.displayName }}</h1><p>这里是需要你关注的项目动态。</p></div>
    <RouterLink class="primary-button" to="/requirements">＋ 新建需求</RouterLink>
  </div>
  <div class="metric-grid">
    <article class="metric-card green"><span>我的进行中任务</span><strong>{{ data.openTasks }}</strong><small>保持更新，让团队掌握真实进度</small></article>
    <article class="metric-card coral"><span>已逾期任务</span><strong>{{ data.overdueTasks }}</strong><small>需要优先处理或调整计划</small></article>
    <article class="metric-card sand"><span>待分派需求</span><strong>{{ data.unassignedRequirements }}</strong><small>尚未进入项目执行链路</small></article>
  </div>
  <div class="dashboard-grid">
    <section class="panel action-panel">
      <div class="panel-title"><div><span class="eyebrow">QUICK START</span><h2>常用操作</h2></div></div>
      <div class="quick-actions">
        <RouterLink to="/requirements"><span>◇</span><strong>提交需求</strong><small>内部与客户需求统一进入需求池</small></RouterLink>
        <RouterLink to="/projects"><span>▦</span><strong>查看项目</strong><small>跟踪里程碑和任务看板</small></RouterLink>
      </div>
    </section>
    <section class="panel focus-panel">
      <span class="eyebrow">WORKING AGREEMENT</span><h2>协作原则</h2>
      <ol><li><span>01</span>任务必须有明确负责人和截止时间</li><li><span>02</span>完成时填报实际工时并提交验收</li><li><span>03</span>客户只访问与自己关联的项目数据</li></ol>
    </section>
  </div>
</template>
