<template>
  <el-container style="min-height: 100vh">
    <el-header class="app-header">
      <span class="app-title">粮库 · 仓房与粮情管理</span>
      <el-menu mode="horizontal" :default-active="route.path" router class="app-menu">
        <el-menu-item index="/granaries">仓房台账</el-menu-item>
        <el-menu-item index="/batches">粮食批次</el-menu-item>
        <el-menu-item index="/temps">粮情测温</el-menu-item>
        <el-menu-item index="/moves">出入库作业</el-menu-item>
      </el-menu>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>

<style>
.app-header {
  display: flex;
  align-items: center;
  gap: 28px;
  background: var(--el-color-primary-light-9);
  border-bottom: 2px solid var(--el-color-primary);
}
.app-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-color-primary-dark-2);
  white-space: nowrap;
}
.app-menu {
  flex: 1;
  border: none;
  background: transparent;
}
</style>
