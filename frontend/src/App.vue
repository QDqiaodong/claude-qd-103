<template>
  <el-container style="min-height: 100vh">
    <el-header style="display:flex;align-items:center;gap:28px;background:#f5f7fa">
      <strong style="font-size:16px">粮库 · 仓房与粮情管理</strong>
      <el-menu mode="horizontal" :default-active="route.path" router style="flex:1;border:none;background:transparent">
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
