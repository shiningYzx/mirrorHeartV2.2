<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const drawer = ref(false)
const customAction = ref('')
const actions = ref([
  {
    id: 1,
    text: '去阳台或楼下，找出一株你叫不出名字的植物。',
    completed: false
  },
  { id: 2, text: '去便利店买一样自己想吃的小零食。', completed: false },
  {
    id: 3,
    text: '给好久没联系的朋友发一个可爱的表情包，不强求聊天。',
    completed: false
  },
  { id: 4, text: '在纸上写下 3 件今天做得还不错的小事。', completed: false },
  {
    id: 5,
    text: '听一首自己最喜欢的、时长不超过 3 分钟的歌。',
    completed: false
  }
])

const historyDrawer = ref(false)
const historyList = ref([
  {
    time: '2026-02-20 15:30',
    action: '去阳台或楼下，找出一株你叫不出名字的植物。'
  },
  {
    time: '2026-02-20 10:20',
    action: '听一首自己最喜欢的、时长不超过 3 分钟的歌。'
  }
])

const refreshActions = () => {
  const newActions = [
    { id: Date.now(), text: '给家里的植物浇一点水。', completed: false },
    { id: Date.now() + 1, text: '拍一张天空的照片。', completed: false },
    { id: Date.now() + 2, text: '深呼吸 10 次。', completed: false },
    { id: Date.now() + 3, text: '整理一下桌面。', completed: false },
    { id: Date.now() + 4, text: '喝一杯温水。', completed: false }
  ]
  actions.value = newActions
  ElMessage.success('已刷新行动推荐')
}

const toggleAction = (action) => {
  action.completed = !action.completed
  if (action.completed) {
    ElMessageBox.confirm('恭喜你完成了一项微光行动！', '完成确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'success'
    })
      .then(() => {
        const now = new Date()
        const timeStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
        historyList.value.unshift({ time: timeStr, action: action.text })
      })
      .catch(() => {
        action.completed = false
      })
  }
}

const addCustomAction = () => {
  if (customAction.value.trim()) {
    const newAction = {
      id: Date.now(),
      text: customAction.value.trim(),
      completed: false
    }
    actions.value.unshift(newAction)
    customAction.value = ''
    ElMessage.success('已添加你的专属微光')
  }
}

const open = () => {
  drawer.value = true
}

const openHistory = () => {
  historyDrawer.value = true
}

defineExpose({
  open
})
</script>

<template>
  <el-drawer v-model="drawer" :with-header="false" size="40%">
    <div class="container">
      <div class="header">
        <h2>微光行动</h2>
        <p>Glimmer Action</p>
      </div>

      <div class="intro">
        <p>
          每个人点亮心里的方式都不同。对你来说，哪一件小事能让你感到'我还在生活'？也许是闻一下咖啡香，或者是摸摸猫。把它记下来，作为你的专属微光吧。
        </p>
      </div>

      <div class="section">
        <div class="sectionHeader">
          <h3>行动推荐</h3>
          <el-button type="primary" size="small" @click="refreshActions"
            >刷新</el-button
          >
        </div>
        <div class="actionsList">
          <div
            v-for="action in actions"
            :key="action.id"
            class="actionItem"
            :class="{ completed: action.completed }"
          >
            <el-checkbox
              v-model="action.completed"
              @change="toggleAction(action)"
            >
              {{ action.text }}
            </el-checkbox>
          </div>
        </div>
      </div>

      <div class="section">
        <h3>你的行动</h3>
        <div class="customAction">
          <el-input
            v-model="customAction"
            placeholder="添加你的专属微光..."
            @keyup.enter="addCustomAction"
          />
          <el-button type="primary" @click="addCustomAction">添加</el-button>
        </div>
      </div>

      <div class="section">
        <div class="sectionHeader">
          <h3>每周日晚，AI 总结：</h3>
        </div>
        <div class="aiSummary">
          <p>"这周你亲手点亮了 12 颗微光，你的星空已经初具规模了。"</p>
          <p>每一个微小的行动，都是你与自己温柔相处的证明。继续发光吧！</p>
        </div>
      </div>

      <div class="footer">
        <el-button @click="openHistory">历史行动</el-button>
      </div>
    </div>
  </el-drawer>

  <el-drawer
    v-model="historyDrawer"
    title="历史行动"
    :with-header="true"
    size="30%"
  >
    <div class="historyContainer">
      <div
        v-for="(item, index) in historyList"
        :key="index"
        class="historyItem"
      >
        <div class="historyTime">{{ item.time }}</div>
        <div class="historyAction">{{ item.action }}</div>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped>
.container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(#d6f7ff, #fdfae8);
  box-sizing: border-box;
  padding: 20px;
  border-radius: 8px;
  overflow-y: auto;
}

.header {
  text-align: center;
  margin-bottom: 20px;
}

.header h2 {
  font-size: 28px;
  font-weight: bold;
  margin: 0 0 5px 0;
  color: #333;
}

.header p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.intro {
  background-color: rgba(255, 255, 255, 0.8);
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
  line-height: 1.6;
  color: #555;
}

.section {
  margin-bottom: 20px;
}

.sectionHeader {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.section h3 {
  font-size: 18px;
  font-weight: bold;
  margin: 0 0 10px 0;
  color: #333;
}

.actionsList {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.actionItem {
  background-color: rgba(255, 255, 255, 0.9);
  padding: 12px;
  border-radius: 8px;
  transition: all 0.3s;
}

.actionItem.completed {
  background-color: rgba(177, 227, 147, 0.3);
  text-decoration: line-through;
  opacity: 0.7;
}

.customAction {
  display: flex;
  gap: 10px;
}

.customAction .el-input {
  flex: 1;
}

.aiSummary {
  background-color: rgba(255, 255, 255, 0.9);
  padding: 15px;
  border-radius: 8px;
  line-height: 1.6;
  color: #555;
}

.aiSummary p {
  margin: 5px 0;
}

.footer {
  margin-top: auto;
  text-align: center;
}

.historyContainer {
  padding: 10px;
}

.historyItem {
  background-color: #f5f5f5;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 10px;
}

.historyTime {
  font-size: 12px;
  color: #999;
  margin-bottom: 5px;
}

.historyAction {
  font-size: 14px;
  color: #333;
  line-height: 1.4;
}
</style>
