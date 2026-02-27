<script setup>
import { ref, reactive, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getSessionListService,
  getMessageHistoryService,
  deleteSessionService
} from '@/api/ai'
import { ArrowLeft, ChatDotRound, Delete } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores'

const props = defineProps({
  drawer: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])

const drawerVisible = ref(false)
const showSessionList = ref(false)
const currentSessionId = ref(null)
const inputText = ref('')
const messages = ref([])
const chatList = ref(null)
const inputRef = ref(null)
const sessionList = ref([])
const loading = ref(false)
const isStreaming = ref(false)

const open = () => {
  drawerVisible.value = true
  loadSessionList()
}

const close = () => {
  drawerVisible.value = false
  emit('close')
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatList.value) {
      chatList.value.scrollTop = chatList.value.scrollHeight
    }
  })
}

const loadSessionList = async () => {
  try {
    console.log('开始加载会话列表...')
    const res = await getSessionListService()
    console.log('会话列表响应:', res)

    // 🌟 修复：必须通过 res.data.code 判断，数据在 res.data.data 中
    if (res.data.code === 0) {
      sessionList.value = res.data.data
      console.log('会话列表已设置:', sessionList.value)
    }
  } catch (error) {
    console.error('加载会话列表失败:', error)
    ElMessage.error('加载会话列表失败')
  }
}

const loadMessageHistory = async (sessionId) => {
  loading.value = true
  try {
    const res = await getMessageHistoryService(sessionId)
    if (res.data.code === 0) {
      messages.value = res.data.data.map((msg) => {
        let content = msg.content || ''

        // 🌟 核心修复：如果是 AI 的回复，用正则表达式去掉 <think>...</think> 及其内部的所有内容
        if (msg.role !== 'user') {
          content = content.replace(/<think>[\s\S]*?<\/think>/gi, '').trim()
        }

        return {
          text: content,
          isMine: msg.role === 'user'
        }
      })
      currentSessionId.value = sessionId
      showSessionList.value = false
      scrollToBottom()
    }
  } catch (error) {
    ElMessage.error('加载历史消息失败')
  } finally {
    loading.value = false
  }
}

const createNewSession = () => {
  currentSessionId.value = null
  messages.value = []
  showSessionList.value = false
  nextTick(() => {
    if (inputRef.value && typeof inputRef.value.focus === 'function') {
      inputRef.value.focus()
    }
  })
}

const deleteSession = async (sessionId, event) => {
  event.stopPropagation()
  try {
    const res = await deleteSessionService(sessionId)
    if (res.data.code === 0) {
      ElMessage.success('删除成功')
      await loadSessionList()
      if (currentSessionId.value === sessionId) {
        createNewSession()
      }
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const toggleSessionList = async () => {
  showSessionList.value = !showSessionList.value
  if (showSessionList.value && sessionList.value.length === 0) {
    await loadSessionList()
  }
}

const formatTime = (timeStr) => {
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) {
    return '刚刚'
  } else if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  } else if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  } else {
    return date.toLocaleDateString('zh-CN', {
      month: '2-digit',
      day: '2-digit'
    })
  }
}

const sendMessage = async () => {
  if (isStreaming.value) {
    ElMessage.warning('请等待回答完成')
    return
  }
  if (inputText.value.trim()) {
    isStreaming.value = true
    const userMessage = {
      text: inputText.value,
      isMine: true
    }
    messages.value.push(userMessage)
    inputText.value = ''
    scrollToBottom()

    try {
      await simulateServerResponse(userMessage.text)
    } catch (error) {
      ElMessage.error('发送失败')
    } finally {
      isStreaming.value = false
    }
  }
}

const simulateServerResponse = async (userInput) => {
  let assistantMessage = null

  const userStore = useUserStore()
  const token = userStore.basicUser.token
  const tokenHead = userStore.basicUser.tokenHead || 'Bearer'

  if (!token) {
    ElMessage.error('请先登录')
    return
  }

  try {
    const response = await fetch(
      'http://localhost:8080/api/v1/ai/chat/stream',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `${tokenHead} ${token}`
        },
        body: JSON.stringify({
          sessionId: currentSessionId.value || null,
          content: userInput,
          thinking: 'auto'
        })
      }
    )

    if (!response.ok) {
      const errorText = await response.text()
      console.error('Response error:', errorText)
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let currentEvent = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value, { stream: true })
      buffer += chunk

      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (!line.trim()) continue

        if (line.startsWith('event:')) {
          currentEvent = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          const data = line.slice(5).trim()

          if (data === '[DONE]') {
            if (assistantMessage) {
              assistantMessage.isThinking = false
              assistantMessage.thinkingText = ''
            }
            scrollToBottom()
            continue
          }

          if (currentEvent === 'session_id' && data) {
            currentSessionId.value = parseInt(data)
          } else if (currentEvent === 'reasoning' && data) {
            if (!assistantMessage) {
              assistantMessage = reactive({
                text: '',
                isMine: false,
                isThinking: true,
                thinkingText: ''
              })
              messages.value.push(assistantMessage)
            }
            assistantMessage.thinkingText += data
            scrollToBottom()
          } else if (currentEvent === 'content' && data) {
            if (!assistantMessage) {
              assistantMessage = reactive({
                text: '',
                isMine: false,
                isThinking: false,
                thinkingText: ''
              })
              messages.value.push(assistantMessage)
            }
            assistantMessage.thinkingText = ''
            assistantMessage.isThinking = false
            assistantMessage.text += data
            nextTick(() => scrollToBottom())
          } else if (currentEvent === 'done') {
            if (assistantMessage) {
              assistantMessage.isThinking = false
              assistantMessage.thinkingText = ''
            }
            scrollToBottom()
          }
        }
      }
    }

    if (assistantMessage) {
      assistantMessage.isThinking = false
      assistantMessage.thinkingText = ''
      scrollToBottom()
    }
  } catch (error) {
    console.error('流式对话错误:', error)

    const errorMessage = error.message || ''

    if (
      errorMessage.includes('ERR_INCOMPLETE_CHUNKED_ENCODING') ||
      errorMessage.includes('network error')
    ) {
      if (assistantMessage && assistantMessage.text.length > 0) {
        console.log('SSE流正常结束，已收到完整回复')
        assistantMessage.isThinking = false
        assistantMessage.thinkingText = ''
        scrollToBottom()
        return
      }
    }

    if (assistantMessage && assistantMessage.text.length > 0) {
      ElMessage.warning('连接中断，但已收到部分回复')
    } else {
      ElMessage.error('连接失败，请检查后端服务是否启动')
      if (!assistantMessage) {
        assistantMessage = reactive({
          text: '抱歉，连接失败了。请稍后重试。',
          isMine: false,
          isThinking: false,
          thinkingText: ''
        })
        messages.value.push(assistantMessage)
      } else {
        assistantMessage.text = '抱歉，连接失败了。请稍后重试。'
      }
    }
    if (assistantMessage) {
      assistantMessage.isThinking = false
      assistantMessage.thinkingText = ''
    }
    scrollToBottom()
  }
}

onMounted(() => {
  if (props.drawer) {
    loadSessionList()
  }
})

defineExpose({
  open
})
</script>

<template>
  <el-drawer
    v-if="!props.drawer"
    v-model="drawerVisible"
    :with-header="false"
    size="40%"
  >
    <div class="container">
      <div class="header">
        <el-button class="backButton" @click="close" circle>
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div class="title">
          <h2>解忧镜</h2>
          <p>Mind Mirror</p>
        </div>
        <el-button
          v-if="!showSessionList"
          class="sessionButton"
          @click="toggleSessionList"
        >
          <el-icon><ChatDotRound /></el-icon>
        </el-button>
      </div>

      <div v-if="showSessionList" class="sessionList">
        <div class="sessionHeader">
          <h3>历史会话</h3>
          <el-button type="primary" size="small" @click="createNewSession"
            >新建会话</el-button
          >
        </div>
        <div v-loading="loading" class="sessionItems">
          <div v-if="sessionList.length === 0" class="empty">
            <el-empty description="暂无会话记录" />
          </div>
          <div
            v-for="session in sessionList"
            :key="session.id"
            class="sessionItem"
            @click="loadMessageHistory(session.id)"
          >
            <div class="sessionTitle">{{ session.title || '未命名会话' }}</div>
            <div class="sessionTime">{{ formatTime(session.updatedAt) }}</div>
          </div>
        </div>
      </div>

      <div v-else class="chatContent">
        <div class="intro">
          <p>
            嘘——先把外界的嘈杂关在门外吧。这面镜子只属于你。现在，你心里最乱的那根线头在哪里？
          </p>
        </div>

        <div class="chat" ref="chatList">
          <div class="chat_list">
            <li class="left">
              <img src="@/assets/logo.png" alt="接收者" />
              <span class="leftChat">倾诉吧，我在听。</span>
            </li>
            <template v-for="(message, index) in messages" :key="index">
              <li v-if="message.isMine" class="right">
                <img src="@/assets/logo.png" alt="发送者" />
                <span class="rightChat">{{ message.text }}</span>
              </li>
              <li v-else class="left">
                <img src="@/assets/logo.png" alt="接收者" />
                <div class="messageContent">
                  <div
                    v-if="message.isThinking && message.thinkingText"
                    class="thinkingText"
                  >
                    {{ message.thinkingText }}
                  </div>
                  <span v-if="message.text" class="leftChat">{{
                    message.text
                  }}</span>
                </div>
              </li>
            </template>
          </div>
        </div>

        <div class="bottom_div">
          <div class="send_box">
            <div class="input_bg">
              <input
                v-model="inputText"
                class="chat_input"
                ref="inputRef"
                type="text"
                placeholder="输入文字后麦克风变成提交按钮"
                @keyup.enter="sendMessage"
                :disabled="isStreaming"
              />
            </div>
            <img
              class="send_img"
              src="@/assets/send.png"
              alt="发送"
              @click="sendMessage"
            />
          </div>
          <div class="black_border">
            <span></span>
          </div>
        </div>
      </div>
    </div>
  </el-drawer>

  <div v-else class="container pageMode">
    <div class="main-layout">
      <div class="left-panel">
        <div class="panel-header">
          <el-button
            v-if="props.drawer"
            class="back-btn"
            :icon="ArrowLeft"
            circle
            size="small"
            @click="close"
          />
          <h3>历史会话</h3>
          <el-button
            type="primary"
            size="small"
            :icon="Plus"
            @click="createNewSession"
            >新建会话</el-button
          >
        </div>
        <div v-loading="loading" class="session-list">
          <div v-if="sessionList.length === 0" class="empty-state">
            <el-icon class="empty-icon"><ChatDotRound /></el-icon>
            <p>暂无会话记录</p>
            <p class="empty-desc">开始你的第一次对话吧</p>
          </div>
          <div
            v-for="session in sessionList"
            :key="session.id"
            :class="[
              'session-card',
              { active: currentSessionId === session.id }
            ]"
            @click="loadMessageHistory(session.id)"
          >
            <div class="session-card-header">
              <div class="session-card-title">
                {{ session.title || '未命名会话' }}
              </div>
              <el-button
                type="danger"
                size="small"
                :icon="Delete"
                circle
                class="delete-btn"
                @click="deleteSession(session.id, $event)"
              />
            </div>
            <div class="session-card-time">
              {{ formatTime(session.updatedAt) }}
            </div>
          </div>
        </div>
      </div>

      <div class="right-panel">
        <div class="chat-container">
          <div class="chat-messages" ref="chatList">
            <div class="message-list">
              <div class="chat-welcome">
                <div class="welcome-icon">🪞</div>
                <h2>解忧镜</h2>
                <p>Mind Mirror</p>
                <p class="welcome-text">
                  嘘——先把外界的嘈杂关在门外吧。这面镜子只属于你。
                </p>
              </div>

              <div class="message ai-message">
                <div class="avatar ai-avatar">
                  <img src="@/assets/logo.png" alt="AI" />
                </div>
                <div class="message-content">
                  <div class="message-bubble ai-bubble">倾诉吧，我在听。</div>
                </div>
              </div>
              <template v-for="(message, index) in messages" :key="index">
                <div
                  :class="[
                    'message',
                    message.isMine ? 'user-message' : 'ai-message'
                  ]"
                >
                  <div
                    :class="[
                      'avatar',
                      message.isMine ? 'user-avatar' : 'ai-avatar'
                    ]"
                  >
                    <img src="@/assets/logo.png" alt="Avatar" />
                  </div>
                  <div class="message-content">
                    <div
                      v-if="message.isThinking && message.thinkingText"
                      class="thinking-content"
                    >
                      <div class="thinking-icon">💭</div>
                      <div class="thinking-text">
                        {{ message.thinkingText }}
                      </div>
                    </div>
                    <div
                      v-if="message.text"
                      :class="[
                        'message-bubble',
                        message.isMine ? 'user-bubble' : 'ai-bubble'
                      ]"
                    >
                      {{ message.text }}
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </div>

          <div class="chat-input-area">
            <div class="input-container">
              <input
                v-model="inputText"
                class="chat-input"
                ref="inputRef"
                type="text"
                placeholder="在这里写下你的想法..."
                @keyup.enter="sendMessage"
                :disabled="isStreaming"
              />
              <button
                :class="[
                  'send-button',
                  { disabled: isStreaming || !inputText.trim() }
                ]"
                @click="sendMessage"
                :disabled="isStreaming || !inputText.trim()"
              >
                <el-icon><ChatDotRound /></el-icon>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
li {
  list-style: none;
}

.container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(#d6f7ff, #fdfae8);
  box-sizing: border-box;
  padding: 0px;
  border-radius: 8px;
}

.container.pageMode {
  border-radius: 0;
  padding: 0;
}

.main-layout {
  display: flex;
  height: 100%;
  gap: 20px;
  padding: 20px;
}

.left-panel {
  width: 320px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid rgba(0, 0, 0, 0.05);
}

.panel-header h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: #2c3e50;
}

.back-btn {
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(0, 0, 0, 0.1);
  color: #666;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: rgba(102, 126, 234, 0.1);
  border-color: rgba(102, 126, 234, 0.3);
  color: #667eea;
  transform: translateX(-2px);
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 5px;
}

.session-list::-webkit-scrollbar {
  width: 4px;
}

.session-list::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 2px;
}

.session-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 2px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 15px;
  opacity: 0.5;
}

.empty-state p {
  margin: 5px 0;
  font-size: 14px;
}

.empty-desc {
  font-size: 12px;
  opacity: 0.7;
}

.session-card {
  background: rgba(255, 255, 255, 0.9);
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.session-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 1);
}

.session-card.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
}

.session-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.session-card-title {
  font-size: 14px;
  font-weight: 500;
  flex: 1;
  margin-right: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
}

.session-card.active .session-card-title {
  color: white;
}

.delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;
}

.session-card:hover .delete-btn {
  opacity: 1;
}

.session-card.active .delete-btn {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
}

.session-card-time {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.session-card.active .session-card-time {
  color: rgba(255, 255, 255, 0.8);
  border-top-color: rgba(255, 255, 255, 0.2);
}

.right-panel {
  flex: 1;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 20px;
}

.chat-welcome {
  text-align: center;
  padding: 30px 20px;
  background: linear-gradient(
    135deg,
    rgba(102, 126, 234, 0.1) 0%,
    rgba(118, 75, 162, 0.1) 100%
  );
  border-radius: 16px;
  animation: fadeInDown 0.6s ease-out;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.welcome-icon {
  font-size: 64px;
  margin-bottom: 15px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.chat-welcome h2 {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 5px 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.chat-welcome p {
  font-size: 14px;
  color: #666;
  margin: 5px 0;
}

.welcome-text {
  font-size: 16px;
  color: #888;
  margin-top: 10px;
  font-style: italic;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 16px;
}

.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 3px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message {
  display: flex;
  gap: 12px;
  animation: messageSlideIn 0.3s ease-out;
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message.user-message {
  flex-direction: row-reverse;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 70%;
}

.thinking-content {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  border-left: 3px solid #667eea;
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.thinking-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.thinking-text {
  flex: 1;
  font-style: italic;
}

.message-bubble {
  padding: 14px 18px;
  border-radius: 16px;
  font-size: 15px;
  line-height: 1.6;
  word-wrap: break-word;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  animation: bubblePop 0.3s ease-out;
}

@keyframes bubblePop {
  0% {
    transform: scale(0.9);
    opacity: 0;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.ai-bubble {
  background: linear-gradient(135deg, #93a7ff 0%, #d0a1ff 100%);
  color: white;
  border-radius: 16px 16px 16px 4px;
}

.user-bubble {
  background: white;
  color: #2c3e50;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 16px 16px 4px 16px;
}

.chat-input-area {
  padding: 10px 0;
}

.input-container {
  display: flex;
  gap: 12px;
  align-items: center;
  background: rgba(255, 255, 255, 0.9);
  padding: 8px 16px;
  border-radius: 28px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border: 2px solid rgba(102, 126, 234, 0.1);
  transition: all 0.3s ease;
}

.input-container:focus-within {
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.2);
  border-color: rgba(102, 126, 234, 0.3);
}

.chat-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  color: #2c3e50;
  padding: 8px 0;
}

.chat-input::placeholder {
  color: #999;
}

.chat-input:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.send-button {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.send-button:hover:not(.disabled) {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.send-button:active:not(.disabled) {
  transform: scale(0.95);
}

.send-button.disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none;
}

.send-button .el-icon {
  font-size: 20px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 15px;
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
  display: flex;
  justify-content: flex-end;
}

.title {
  flex: 1;
  text-align: center;
}

.title h2 {
  font-size: 28px;
  font-weight: bold;
  margin: 0 0 5px 0;
  color: #333;
}

.title p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.sessionButton {
  margin-left: 10px;
}

.sessionList {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sessionHeader {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 15px;
  position: relative;
}

.sessionHeader h3 {
  font-size: 18px;
  font-weight: bold;
  margin: 0;
  color: #333;
}

.sessionHeader .el-button {
  position: absolute;
  right: 0;
}

/* 🌟 网格布局核心设置 */
.sessionItems {
  flex: 1;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(
    auto-fill,
    minmax(220px, 1fr)
  ); /* 自适应列宽，最小220px */
  gap: 15px;
  align-content: start; /* 让内容从顶部开始排列 */
  padding: 5px; /* 留出阴影空间 */
}

.empty {
  grid-column: 1 / -1; /* 占据所有列 */
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

/* 卡片样式 */
.sessionItem {
  background: linear-gradient(135deg, #ffffff 0%, #f0f0f0 100%);
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 0; /* 移除旧的 margin，使用 gap 控制间距 */
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.6);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100%; /* 保证同一行的卡片高度一致 */
  box-sizing: border-box;
}

.sessionItem:hover {
  background: linear-gradient(135deg, #ffffff 0%, #e8f4f8 100%);
  transform: translateY(-4px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

.sessionHeader {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.sessionTitle {
  font-size: 15px;
  font-weight: bold;
  color: #333;
  flex: 1;
  margin-right: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.sessionTime {
  font-size: 12px;
  color: #999;
  margin-top: auto;
  border-top: 1px dashed #e4e4e4;
  padding-top: 10px;
}

.chatContent {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.intro {
  background-color: rgba(255, 255, 255, 0.8);
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 15px;
  line-height: 1.6;
  color: #555;
  text-align: center;
}

.chat {
  width: 100%;
  flex: 1;
  overflow-y: auto;
  box-sizing: border-box;
  padding: 10px 0;
  overflow-y: scroll;
}

.chat::-webkit-scrollbar {
  display: none;
}

.chat_list li {
  display: flex;
  margin-top: 15px;
  align-items: flex-start;
}

.chat_list li.right {
  flex-direction: row-reverse;
}

.chat_list li.left {
  flex-direction: row;
}

.thinkingContainer {
  display: flex;
  justify-content: center;
  margin: 15px 0;
  padding: 0 20px;
}

.chat_list li img {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  border: 2px #fff solid;
  flex-shrink: 0;
}

.messageContent {
  display: flex;
  flex-direction: column;
  max-width: 70%;
}

.thinkingText {
  font-size: 13px;
  color: #666;
  font-style: italic;
  line-height: 1.6;
  background-color: rgba(255, 255, 255, 0.6);
  padding: 12px 20px;
  border-radius: 20px;
  border-left: 3px solid #50d287;
  margin-bottom: 8px;
}

.leftChat {
  margin-left: 10px;
  border-radius: 1px 10px 1px 10px;
  display: inline-block;
  padding: 12px 16px;
  background-image: linear-gradient(180deg, #b1e393 0%, #50d287 100%);
  box-shadow: 2px 2px 10px 0px rgba(201, 201, 201, 0.1);
  color: #ffffff;
  word-wrap: break-word;
}

.rightChat {
  margin-right: 10px;
  border-radius: 1px 10px 1px 10px;
  display: inline-block;
  padding: 12px 16px;
  background: #ffffff;
  border: 1px solid rgba(247, 247, 247, 1);
  color: #000000;
  max-width: 70%;
  word-wrap: break-word;
}

.bottom_div {
  width: 100%;
  height: 65px;
  margin-top: 25px;
  background-color: #fff;
  box-shadow: 0px -5px 7px 0px rgba(168, 168, 168, 0.05);
  border-radius: 25px 25px 25px 25px;
  padding: 15px 15px 0px 15px;
  box-sizing: border-box;
}

.send_box {
  display: flex;
  align-items: center;
}

.send_box img {
  width: 34px;
  height: 34px;
  background-color: transparent;
  cursor: pointer;
}

.input_bg {
  height: 35px;
  background: #f3f3f3;
  border-radius: 18px;
  padding-left: 17px;
  flex: 1;
  margin-right: 15px;
  display: flex;
  align-items: center;
}

.input_bg input {
  border: 0;
  outline: 0;
  background-color: transparent;
  display: inline-block;
  width: 100%;
  height: 100%;
}

.input_bg input:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.black_border {
  margin-top: 10px;
  height: 34px;
  text-align: center;
}

.black_border span {
  display: inline-block;
  background-color: #000;
  width: 105px;
  height: 4px;
  border-radius: 50px;
}
</style>
