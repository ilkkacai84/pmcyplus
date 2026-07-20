<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiMessage, http, prepareCsrf } from '@/api/http'
import type { NotificationInbox } from '@/api/types'

const inbox = ref<NotificationInbox>({ unread: 0, items: [] })
const error = ref('')
async function load() { try { inbox.value = (await http.get('/notifications')).data } catch (e) { error.value = apiMessage(e) } }
async function read(id: number) { try { await prepareCsrf(); await http.post(`/notifications/${id}/read`); await load() } catch (e) { error.value = apiMessage(e) } }
onMounted(load)
</script>

<template>
  <div class="page-heading"><div><span class="eyebrow">INBOX</span><h1>消息通知</h1><p>{{ inbox.unread }} 条未读；邮件、企业微信和 Teams 同步进入待投递队列。</p></div></div>
  <p v-if="error" class="error-message page-error">{{ error }}</p>
  <section class="notification-list">
    <article v-for="item in inbox.items" :key="item.id" class="panel notification-card" :class="{ unread: item.status === 'SENT' }">
      <div><span class="eyebrow">{{ item.eventType }}<template v-if="item.escalationLevel"> · L{{ item.escalationLevel }}</template></span><h2>{{ item.title }}</h2><p>{{ item.content }}</p><small>{{ new Date(item.createdAt).toLocaleString() }}</small></div>
      <button v-if="item.status === 'SENT'" class="inline-button" @click="read(item.id)">标为已读</button>
    </article>
    <div v-if="!inbox.items.length" class="empty-state"><strong>暂无通知</strong><span>需求、任务、审批和验收动态会显示在这里。</span></div>
  </section>
</template>
