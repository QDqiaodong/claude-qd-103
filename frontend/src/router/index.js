import { createRouter, createWebHistory } from 'vue-router'
import Granaries from '../views/Granaries.vue'
import Batches from '../views/Batches.vue'
import Temps from '../views/Temps.vue'
import Moves from '../views/Moves.vue'
import Fumigations from '../views/Fumigations.vue'

const routes = [
  { path: '/', redirect: '/granaries' },
  { path: '/granaries', component: Granaries, meta: { title: '仓房台账' } },
  { path: '/batches', component: Batches, meta: { title: '粮食批次' } },
  { path: '/temps', component: Temps, meta: { title: '粮情测温' } },
  { path: '/fumigations', component: Fumigations, meta: { title: '熏蒸作业' } },
  { path: '/moves', component: Moves, meta: { title: '出入库作业' } }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
