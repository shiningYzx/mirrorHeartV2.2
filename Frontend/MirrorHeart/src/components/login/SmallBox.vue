<script lang="ts" setup>
import { ArrowRightBold } from '@element-plus/icons-vue'
</script>
<template>
  <div class="containerBox">
    <div class="userIco">
      <slot></slot>
    </div>
    <el-icon><ArrowRightBold /></el-icon>
  </div>
</template>
<style>
.containerBox {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 30px;
  width: 220px;
  border-radius: 8px;
  margin: 1px 20px;
  padding: 0 10px;
  cursor: pointer;
  transition: all 0.3s;
}

.containerBox:hover {
  background-color: #e3e5e7;
}

.containerBox .userIco {
  display: flex;
  width: 90px;
  height: 25px;
  justify-content: space-between;
  align-items: center;
  box-sizing: border-box;
}

.containerBox .userIco img {
  height: 20px;
}

.containerBox .userIco span {
  font-size: 16x;
}
</style>
