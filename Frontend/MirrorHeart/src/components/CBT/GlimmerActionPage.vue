<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Refresh,
  MagicStick,
  EditPen
} from '@element-plus/icons-vue'
import {
  addTodoService,
  getTodoListService,
  updateTodoService,
  deleteTodoService,
  getRecommendTodoService
} from '@/api/todo'

const emit = defineEmits(['close'])

const newTodoTitle = ref('')
const todoList = ref([])
const filterStatus = ref(-1)
const loading = ref(false)
const recommendList = ref([])
const selectedRecommend = ref([])
const recommendLoading = ref(false)

const loadTodoList = async () => {
  loading.value = true
  try {
    const params = filterStatus.value === -1 ? undefined : filterStatus.value
    const res = await getTodoListService(params)
    if (res.data.code === 0) {
      todoList.value = res.data.data
    }
  } catch (error) {
    ElMessage.error('加载待办事项失败')
  } finally {
    loading.value = false
  }
}

const addTodo = async () => {
  if (!newTodoTitle.value.trim()) {
    ElMessage.warning('请输入待办事项内容')
    return
  }
  try {
    const res = await addTodoService(newTodoTitle.value.trim())
    if (res.data.code === 0) {
      ElMessage.success('添加成功')
      newTodoTitle.value = ''
      await loadTodoList()
    }
  } catch (error) {
    ElMessage.error('添加失败')
  }
}

const updateTodo = async (todo) => {
  try {
    const res = await updateTodoService(todo.id, todo.title, todo.status)
    if (res.data.code === 0) {
      ElMessage.success(todo.status === 1 ? '已完成' : '已恢复')
      await loadTodoList()
    }
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

const deleteTodo = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个待办事项吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await deleteTodoService(id)
    if (res.data.code === 0) {
      ElMessage.success('删除成功')
      await loadTodoList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const startEdit = (todo) => {
  todo.isEditing = true
  todo.editTitle = todo.title
}

const cancelEdit = (todo) => {
  todo.isEditing = false
  todo.editTitle = ''
}

const saveEdit = async (todo) => {
  if (!todo.editTitle.trim()) {
    ElMessage.warning('请输入待办事项内容')
    return
  }
  try {
    const res = await updateTodoService(
      todo.id,
      todo.editTitle.trim(),
      todo.status
    )
    if (res.data.code === 0) {
      ElMessage.success('修改成功')
      todo.isEditing = false
      await loadTodoList()
    }
  } catch (error) {
    ElMessage.error('修改失败')
  }
}

const loadRecommendTodo = async () => {
  recommendLoading.value = true
  selectedRecommend.value = []
  try {
    const res = await getRecommendTodoService(5) // 左侧竖向空间充足，可以请求更多灵感
    if (res.data.code === 0) {
      recommendList.value = res.data.data
    }
  } catch (error) {
    ElMessage.error('加载推荐待办失败')
  } finally {
    recommendLoading.value = false
  }
}

// 切换推荐项的选择状态
const toggleRecommend = (item) => {
  const index = selectedRecommend.value.indexOf(item)
  if (index > -1) {
    selectedRecommend.value.splice(index, 1)
  } else {
    selectedRecommend.value.push(item)
  }
}

const addSelectedRecommend = async () => {
  if (selectedRecommend.value.length === 0) {
    ElMessage.warning('请先选择要添加的灵感')
    return
  }
  try {
    for (const title of selectedRecommend.value) {
      await addTodoService(title)
    }
    ElMessage.success(
      `成功采纳 ${selectedRecommend.value.length} 个灵感，已加入待办`
    )
    selectedRecommend.value = []
    await loadTodoList()
  } catch (error) {
    ElMessage.error('添加失败')
  }
}

onMounted(() => {
  loadTodoList()
  loadRecommendTodo()
})
</script>

<template>
  <div class="container pageMode">
    <!-- 头部区域 -->
    <div class="header">
      <el-button class="backButton" @click="emit('close')" circle>
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <div class="title">
        <h2>微光行动</h2>
        <p>Glimmer Action</p>
      </div>
      <div class="headerRight"></div>
    </div>

    <!-- 🌟 核心布局：左右分栏 -->
    <div class="main-layout">
      <!-- ================= 左侧面板 ================= -->
      <div class="left-panel">
        <!-- 介绍区域 -->
        <div class="intro">
          <p>
            ✨每个人点亮心里的方式都不同。对你来说，哪一件小事能让你感到<strong
              class="highlight-text"
              >“我还在生活”</strong
            >？也许是闻一下咖啡香，或者是摸摸猫。把它记下来，作为你的专属微光吧。
          </p>
        </div>

        <!-- 推荐灵感区域 -->
        <div class="recommendSection" v-loading="recommendLoading">
          <div class="recommendHeader">
            <h3>
              <el-icon><MagicStick /></el-icon> 灵感微光
            </h3>
            <el-button
              size="small"
              :icon="Refresh"
              @click="loadRecommendTodo"
              text
              bg
              class="refresh-btn"
              >换一批</el-button
            >
          </div>

          <div v-if="recommendList.length === 0" class="emptyRecommend">
            <span class="emptyText">暂无推荐灵感</span>
          </div>
          <div v-else class="recommendList">
            <div
              v-for="(item, index) in recommendList"
              :key="index"
              class="recommendCard"
              :class="{ active: selectedRecommend.includes(item) }"
              @click="toggleRecommend(item)"
            >
              {{ item }}
            </div>
          </div>

          <div class="recommendActions" v-if="selectedRecommend.length > 0">
            <el-button
              type="primary"
              round
              @click="addSelectedRecommend"
              class="addBtn"
            >
              采纳选中的 {{ selectedRecommend.length }} 个微光
            </el-button>
          </div>
        </div>
      </div>

      <!-- ================= 右侧面板 ================= -->
      <div class="right-panel">
        <!-- 美化后的自定义添加区域 -->
        <div class="addSection">
          <el-input
            v-model="newTodoTitle"
            placeholder="记下你的专属微光..."
            @keyup.enter="addTodo"
            size="large"
            class="beauty-input"
          >
            <template #prefix>
              <el-icon class="input-icon"><EditPen /></el-icon>
            </template>
            <template #append>
              <div class="beauty-append-btn" @click="addTodo">点亮它</div>
            </template>
          </el-input>
        </div>

        <!-- 列表控制栏 -->
        <div class="listControlBar">
          <h3>我的微光清单</h3>
          <el-radio-group
            v-model="filterStatus"
            @change="loadTodoList"
            size="small"
            class="custom-radio"
          >
            <el-radio-button :label="-1">全部</el-radio-button>
            <el-radio-button :label="0">未点亮</el-radio-button>
            <el-radio-button :label="1">已点亮</el-radio-button>
          </el-radio-group>
        </div>

        <!-- 待办网格列表 -->
        <div class="todoList" v-loading="loading">
          <div v-if="todoList.length === 0" class="empty">
            <el-empty
              description="空空如也，快去捕捉一些微光吧"
              :image-size="120"
            />
          </div>

          <div
            v-for="todo in todoList"
            :key="todo.id"
            class="todoItem"
            :class="{ completed: todo.status === 1 }"
          >
            <div class="todoTop">
              <el-checkbox
                v-model="todo.status"
                :true-label="1"
                :false-label="0"
                @change="updateTodo(todo)"
                size="large"
                class="custom-checkbox"
              />
              <div class="todoContent">
                <el-input
                  v-if="todo.isEditing"
                  v-model="todo.editTitle"
                  size="small"
                  @keyup.enter="saveEdit(todo)"
                  @blur="saveEdit(todo)"
                  autofocus
                />
                <span v-else class="todoTitle">{{ todo.title }}</span>
              </div>
            </div>

            <div class="todoBottom">
              <span class="todoTime">{{ todo.createdAt }}</span>
              <div class="todoActions">
                <el-button
                  v-if="!todo.isEditing"
                  link
                  type="primary"
                  size="small"
                  @click="startEdit(todo)"
                  >编辑</el-button
                >
                <el-button
                  v-else
                  link
                  type="info"
                  size="small"
                  @click="cancelEdit(todo)"
                  >取消</el-button
                >
                <el-button
                  link
                  type="danger"
                  size="small"
                  @click="deleteTodo(todo.id)"
                  >删除</el-button
                >
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #e0f7fa 0%, #fdfae8 100%);
  box-sizing: border-box;
  overflow: hidden; /* 防止最外层出现双滚动条 */
}

/* 页面模式的内边距 */
.pageMode {
  padding: 0px 40px 20px 40px;
}

/* --- 头部 --- */
.header {
  display: flex;
  align-items: center;
  margin-bottom: 25px;
  flex-shrink: 0;
}

.backButton {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  z-index: 10;
}

.backButton:hover {
  background: rgba(255, 255, 255, 1);
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

.headerRight {
  width: 80px;
}

.title {
  flex: 1;
  text-align: center;
}

.title h2 {
  font-size: 26px;
  font-weight: 800;
  margin: 0 0 4px 0;
  color: #2c3e50;
  letter-spacing: 1px;
}

.title p {
  font-size: 13px;
  color: #7f8c8d;
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 2px;
}

/* ================= 🌟 左右分栏核心布局 ================= */
.main-layout {
  display: flex;
  flex: 1;
  gap: 30px;
  overflow: hidden; /* 让左右两侧各自内部滚动 */
}

/* --- 左侧面板 --- */
.left-panel {
  width: 32%; /* 左侧占比 */
  min-width: 300px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: auto; /* 左侧独立滚动 */
  padding-right: 10px;
}
.left-panel::-webkit-scrollbar {
  width: 4px;
}
.left-panel::-webkit-scrollbar-thumb {
  background-color: #dcdfe6;
  border-radius: 4px;
}

/* --- 右侧面板 --- */
.right-panel {
  flex: 1; /* 右侧占据剩余空间 */
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 响应式：屏幕较小时变为上下堆叠 */
@media (max-width: 900px) {
  .main-layout {
    flex-direction: column;
    overflow-y: auto;
  }
  .left-panel,
  .right-panel {
    width: 100%;
    overflow-y: visible;
    padding-right: 0;
  }
  .right-panel {
    margin-top: 10px;
  }
}

/* ================= 左侧组件样式 ================= */

/* 介绍卡片 */
.intro {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(15px);
  padding: 24px 20px;
  border-radius: 20px;
  line-height: 1.8;
  color: #555;
  font-size: 14.5px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.03);
  position: relative;
}

.intro-icon {
  font-size: 28px;
  margin-bottom: 10px;
}

.highlight-text {
  color: #67c23a;
  background: rgba(103, 194, 58, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
}

/* 灵感推荐区块 */
.recommendSection {
  flex: 1; /* 占据左侧剩余空间 */
  padding: 20px;
  background: linear-gradient(
    135deg,
    rgba(255, 249, 230, 0.8) 0%,
    rgba(255, 232, 204, 0.8) 100%
  );
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(230, 162, 60, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
}

.recommendHeader {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.recommendHeader h3 {
  margin: 0;
  font-size: 17px;
  color: #d48806;
  display: flex;
  align-items: center;
  gap: 8px;
}

.refresh-btn {
  color: #d48806 !important;
  background-color: rgba(255, 255, 255, 0.5) !important;
  border-radius: 12px;
}

.recommendList {
  display: flex;
  flex-direction: column; /* 竖向排列 */
  gap: 12px;
  margin-bottom: 15px;
}

.recommendCard {
  background: #ffffff;
  padding: 14px 18px;
  border-radius: 14px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  border: 1px solid #e4e7ed;
  user-select: none;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
  line-height: 1.5;
}

.recommendCard:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.06);
  border-color: #dcdfe6;
}

.recommendCard.active {
  background-color: #f0f9eb;
  border-color: #67c23a;
  color: #67c23a;
  font-weight: bold;
}

.recommendActions {
  margin-top: auto; /* 按钮推到底部 */
  display: flex;
  justify-content: center;
  animation: fadeIn 0.3s ease-in-out;
}

.addBtn {
  width: 100%;
  background: linear-gradient(135deg, #85ce61 0%, #67c23a 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
  height: 40px;
  font-weight: bold;
}

.addBtn:hover {
  box-shadow: 0 6px 16px rgba(103, 194, 58, 0.4);
  transform: translateY(-1px);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ================= 右侧组件样式 ================= */

/* 🌟 深度美化输入框 */
.addSection {
  margin-bottom: 25px;
  position: relative;
  z-index: 10;
}

.beauty-input :deep(.el-input__wrapper) {
  border-radius: 30px 0 0 30px;
  padding: 8px 15px 8px 25px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.04);
  border: 1px solid transparent;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.beauty-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 8px 24px rgba(103, 194, 58, 0.15) !important;
  background: #ffffff;
}

/* 消除 element 默认 focus 的蓝色边框 */
.beauty-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: none;
}
.beauty-input :deep(.el-input__inner:focus) {
  outline: none;
}

.input-icon {
  color: #909399;
  font-size: 18px;
  margin-right: 5px;
}

.beauty-input :deep(.el-input-group__append) {
  border-radius: 0 30px 30px 0;
  background: transparent; /* 去除默认底色 */
  border: none;
  padding: 0;
  overflow: hidden;
}

/* 渐变发光按钮 */
.beauty-append-btn {
  background: linear-gradient(135deg, #a8edea 0%, #b1e393 100%);
  color: #2c3e50;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 30px;
  font-weight: bold;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: -2px 0 10px rgba(177, 227, 147, 0.2);
}

.beauty-append-btn:hover {
  background: linear-gradient(135deg, #b1e393 0%, #a8edea 100%);
  color: #1a252f;
}

.beauty-append-btn:active {
  transform: scale(0.98);
}

/* 列表控制栏 */
.listControlBar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding: 0 5px;
}

.listControlBar h3 {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.custom-radio :deep(.el-radio-button__inner) {
  border-radius: 20px;
  border: none;
  background: rgba(255, 255, 255, 0.6);
  margin-left: 5px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02) !important;
}
.custom-radio
  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: #67c23a;
  color: white;
  box-shadow: 0 4px 10px rgba(103, 194, 58, 0.3) !important;
}

/* 网格布局 */
.todoList {
  flex: 1;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  align-content: start;
  padding: 5px 10px 20px 5px; /* 右侧留白给滚动条 */
}

.todoList::-webkit-scrollbar {
  width: 6px;
}
.todoList::-webkit-scrollbar-thumb {
  background-color: #dcdfe6;
  border-radius: 4px;
}

.empty {
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 250px;
}

/* 待办卡片样式 */
.todoItem {
  display: flex;
  flex-direction: column;
  background-color: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(5px);
  padding: 16px 16px 12px 16px;
  border-radius: 16px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  border: 1px solid rgba(255, 255, 255, 1);
  height: 100%;
  box-sizing: border-box;
}

.todoItem:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.todoItem.completed {
  background-color: rgba(240, 249, 235, 0.6);
  border-color: rgba(103, 194, 58, 0.2);
}

.todoTop {
  display: flex;
  align-items: flex-start;
  margin-bottom: 15px;
}

/* 增大选框点击区域和圆润度 */
.custom-checkbox :deep(.el-checkbox__inner) {
  width: 18px;
  height: 18px;
  border-radius: 50%;
}
.custom-checkbox :deep(.el-checkbox__inner::after) {
  top: 3px;
  left: 6px;
}

.todoContent {
  flex: 1;
  margin-left: 12px;
  display: flex;
  flex-direction: column;
}

.todoTitle {
  font-size: 15px;
  color: #303133;
  line-height: 1.5;
  word-wrap: break-word;
  white-space: pre-wrap;
  margin-top: 2px;
  transition: color 0.3s;
}

.todoItem.completed .todoTitle {
  text-decoration: line-through;
  color: #a8abb2;
}

/* 底部区域 */
.todoBottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.04);
}

.todoTime {
  font-size: 12px;
  color: #c0c4cc;
}

.todoActions {
  display: flex;
  gap: 8px;
  opacity: 0.6;
  transition: opacity 0.3s;
}

.todoItem:hover .todoActions {
  opacity: 1;
}
</style>
