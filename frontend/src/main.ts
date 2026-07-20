import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { installI18n } from './i18n'
import './styles.css'

const root = document.querySelector('#app')!
createApp(App).use(createPinia()).use(router).mount(root)
installI18n(root)

if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => navigator.serviceWorker.register('/sw.js'))
}
