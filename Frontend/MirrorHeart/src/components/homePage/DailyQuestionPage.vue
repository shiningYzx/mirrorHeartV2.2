<script setup>
import { ref, watch, onUnmounted, onMounted } from 'vue'
import {
  getDailyQuestionService,
  submitDailyQuestionService,
  getQuestionAnswersService
} from '@/api/homePage'
import { uploadFileService } from '@/api/userCenter'
import { likeRecommendService } from '@/api/homePage'
import {
  Clock,
  ArrowLeft,
  ChatLineRound,
  Microphone,
  VideoPause,
  View,
  Delete
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const token = userStore.basicUser.token

const message = ref({})

onMounted(() => {
  if (route.query.data) {
    try {
      message.value = JSON.parse(route.query.data)
      if (message.value.myAnswerText) {
        textarea.value = message.value.myAnswerText
      }
      if (message.value.myAudioUrl) {
        localAudioUrl.value = message.value.myAudioUrl
      }
    } catch (error) {
      console.error('解析每日一问数据失败:', error)
    }
  }
})

// === 沉浸作答区状态 ===
const textarea = ref('')
const minutes = ref(0)
const seconds = ref(0)
const timer = ref(null)
const radio = ref(null)

watch(
  () => radio.value,
  (val) => {
    if (val) {
      minutes.value = val
    }
  }
)

const startCountDown = () => {
  if (minutes.value > 120 || minutes.value < 1 || minutes.value % 1 !== 0) {
    ElMessage.error('倒计时时间范围为 1 至 120 分钟的整数')
    return
  }
  if (!timer.value) {
    timer.value = setInterval(() => {
      if (minutes.value === 0 && seconds.value === 0) {
        clearInterval(timer.value)
        timer.value = null
        radio.value = null
        ElMessage.success('专注时光结束，辛苦了')
        return
      }
      if (seconds.value === 0) {
        seconds.value = 60
        minutes.value--
      }
      seconds.value--
    }, 1000)
  }
}

// ==========================================
// 🌟 语音录制逻辑 (完全独立，随时可用)
// ==========================================
const mediaRecorder = ref(null)
const audioChunks = ref([])
const isRecording = ref(false)
const recordingTime = ref(0)
const recordTimer = ref(null)
const localAudioUrl = ref('')
const localAudioBlob = ref(null)
const audioDuration = ref(0)
const visibility = ref(0)

const formatDuration = (seconds) => {
  const m = Math.floor(seconds / 60)
    .toString()
    .padStart(2, '0')
  const s = (seconds % 60).toString().padStart(2, '0')
  return `${m}:${s}`
}

const startRecording = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaRecorder.value = new MediaRecorder(stream)
    audioChunks.value = []

    mediaRecorder.value.ondataavailable = (e) => {
      if (e.data.size > 0) audioChunks.value.push(e.data)
    }

    mediaRecorder.value.onstop = () => {
      const blob = new Blob(audioChunks.value, { type: 'audio/webm' })
      localAudioBlob.value = blob
      localAudioUrl.value = URL.createObjectURL(blob)
      audioDuration.value = recordingTime.value * 1000
      stream.getTracks().forEach((track) => track.stop())
    }

    mediaRecorder.value.start()
    isRecording.value = true
    recordingTime.value = 0
    recordTimer.value = setInterval(() => {
      recordingTime.value++
    }, 1000)
  } catch (err) {
    ElMessage.error('无法访问麦克风，请检查浏览器权限')
  }
}

const stopRecording = () => {
  if (mediaRecorder.value && isRecording.value) {
    mediaRecorder.value.stop()
    isRecording.value = false
    clearInterval(recordTimer.value)
  }
}

const deleteRecording = () => {
  localAudioBlob.value = null
  localAudioUrl.value = ''
  recordingTime.value = 0
  audioDuration.value = 0
}

// ==========================================
// 🌟 数据保存与更新
// ==========================================
const getDailyQuestion = async () => {
  const res = await getDailyQuestionService()
  textarea.value = res.data.data.myAnswerText || ''
  localAudioUrl.value = res.data.data.myAudioUrl || ''
  localAudioBlob.value = null
}

const handleSaveAnswer = async () => {
  if (!token) {
    ElMessage.warning('登录后才能回答每日一问哦！')
    userStore.setIsLogin(true)
    return
  }

  const answer = textarea.value ? textarea.value.trim() : ''

  if (answer === '' && !localAudioBlob.value && !localAudioUrl.value) {
    ElMessage.warning('请至少写下一点思绪，或录制一段语音吧~')
    return
  }

  if (isRecording.value) {
    ElMessage.warning('请先停止录音再保存')
    return
  }

  // 🌟 提前结束倒计时的温柔提示
  if (timer.value) {
    try {
      await ElMessageBox.confirm(
        '专注倒计时还没结束，确定要提前封存答案吗？',
        '提示',
        {
          confirmButtonText: '确定封存',
          cancelButtonText: '继续专注',
          type: 'info'
        }
      )
      clearInterval(timer.value)
      timer.value = null
      seconds.value = 0
      radio.value = null
    } catch {
      return // 用户点击了取消，继续计时
    }
  }

  try {
    let uploadedAudioUrl = localAudioUrl.value

    if (localAudioBlob.value) {
      const uploadRes = await uploadFileService(localAudioBlob.value, 'AUDIO')
      if (uploadRes.data.code === 0) {
        uploadedAudioUrl = uploadRes.data.data
      } else {
        throw new Error('语音上传失败')
      }
    }

    await submitDailyQuestionService(
      message.value.questionId,
      answer,
      uploadedAudioUrl,
      audioDuration.value,
      visibility.value
    )

    ElMessage.success('答案已妥善封存')
    message.value.isAnswered = true
    getDailyQuestion()

    if (showPlaza.value) {
      loadPlazaData(true)
    }
  } catch (error) {
    ElMessage.error(error.message || '封存失败，请重试')
  }
}

const handleBack = () => {
  if (timer.value) {
    clearInterval(timer.value)
    timer.value = null
  }
  stopRecording()
  router.push('/homePage')
}

onUnmounted(() => {
  if (timer.value) clearInterval(timer.value)
  if (recordTimer.value) clearInterval(recordTimer.value)
  stopRecording()
})

// ==========================================
// 🌟 灵魂回音（他人回答广场）相关逻辑
// ==========================================
const showPlaza = ref(false)
const plazaList = ref([])
const plazaPage = ref(1)
const plazaLoading = ref(false)
const plazaNoMore = ref(false)

const openPlaza = () => {
  showPlaza.value = true
  if (plazaList.value.length === 0) {
    loadPlazaData(true)
  }
}

const loadPlazaData = async (isRefresh = false) => {
  if (plazaLoading.value || (plazaNoMore.value && !isRefresh)) return

  if (isRefresh) {
    plazaPage.value = 1
    plazaList.value = []
    plazaNoMore.value = false
  }

  plazaLoading.value = true
  try {
    const res = await getQuestionAnswersService(
      message.value.questionId,
      plazaPage.value,
      10
    )

    const records = res.data.data.records || []
    if (records.length < 10) plazaNoMore.value = true
    plazaList.value.push(...records)
    plazaPage.value++
  } catch (error) {
    console.error('获取他人回答失败', error)
  } finally {
    plazaLoading.value = false
  }
}

const toggleLike = async (item) => {
  try {
    const res = await likeRecommendService(item.id, 'DAILY_ANSWER')
    item.isLiked = res.data.data
    item.likeCount += item.isLiked ? 1 : -1
  } catch (error) {
    ElMessage.error('操作太快了，休息一下吧')
  }
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}
</script>

<template>
  <div class="daily-question-page">
    <div class="ambient-glow glow-1"></div>
    <div class="ambient-glow glow-2"></div>

    <header class="page-header"></header>

    <main class="main-layout">
      <!-- 左侧：灵魂发问展示区 -->
      <section class="left-panel">
        <el-button class="back-button" circle @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>

        <div class="question-display">
          <div class="quote-mark top-quote">"</div>
          <h1 class="question-text">
            {{ message.text || '聆听内心的声音...' }}
          </h1>
          <div class="quote-mark bottom-quote">"</div>

          <div class="explore-plaza-wrapper">
            <button class="explore-btn" @click="openPlaza">
              <el-icon class="explore-icon"><ChatLineRound /></el-icon>
              <span>倾听他人的回音</span>
            </button>
            <p class="explore-hint">已有许多人在这里留下了他们的思绪</p>
          </div>
        </div>
      </section>

      <!-- 右侧：玻璃态沉浸作答区 -->
      <section class="right-panel">
        <div class="glass-card" :class="{ 'is-focused': timer }">
          <!-- 🌟 修改：非强制的输入区，默认直接可用 -->
          <div class="answer-wrapper">
            <div class="textarea-mask">
              <el-input
                v-model="textarea"
                class="zen-textarea"
                resize="none"
                :autosize="{ minRows: 3, maxRows: 3 }"
                type="textarea"
                :placeholder="
                  message.isAnswered
                    ? '你已经留下了足迹。你还可以继续修改...'
                    : '随时写下心里的答案吧...'
                "
              />

              <!-- 语音录制交互栏 -->
              <div class="audio-record-section">
                <!-- 录音中状态 -->
                <div v-if="isRecording" class="recording-active">
                  <div class="pulsing-dot"></div>
                  <span>正在倾诉... {{ formatDuration(recordingTime) }}</span>
                  <el-button
                    type="danger"
                    size="small"
                    circle
                    @click="stopRecording"
                    title="停止录制"
                  >
                    <el-icon><VideoPause /></el-icon>
                  </el-button>
                </div>

                <!-- 录音完成/已有录音状态 -->
                <div v-else-if="localAudioUrl" class="recorded-audio">
                  <div class="audio-player-wrapper">
                    <audio
                      :src="localAudioUrl"
                      controls
                      class="custom-audio"
                    ></audio>
                  </div>
                  <el-button
                    type="danger"
                    link
                    @click="deleteRecording"
                    class="del-audio-btn"
                  >
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-button>
                </div>

                <!-- 未录音初始状态 -->
                <div v-else class="start-record">
                  <el-button round size="small" @click="startRecording">
                    <el-icon><Microphone /></el-icon>
                    录制语音
                  </el-button>
                </div>
              </div>
            </div>
          </div>

          <!-- 🌟 修改：变为可选的专注模式区域 -->
          <div class="focus-tool-section">
            <div class="focus-header">
              <el-icon><Clock /></el-icon>
              <span>可选：开启专注时光</span>
            </div>

            <div class="timer-flex-row">
              <div class="clock-display" :class="{ 'is-active': timer }">
                <div class="clock-time">
                  {{
                    radio || timer
                      ? `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
                      : '-- : --'
                  }}
                </div>
              </div>

              <div class="timer-selector" :class="{ 'is-disabled': timer }">
                <el-radio-group v-model="radio" class="time-grid">
                  <el-radio :value="5" border class="time-radio">5 分</el-radio>
                  <el-radio :value="15" border class="time-radio"
                    >15 分</el-radio
                  >
                  <el-radio :value="30" border class="time-radio"
                    >30 分</el-radio
                  >
                </el-radio-group>
              </div>

              <el-button
                class="action-btn start-btn"
                @click="startCountDown"
                :disabled="!!timer || !radio"
              >
                {{ timer ? '专注中' : '开始' }}
              </el-button>
            </div>
          </div>

          <!-- 权限选择器 -->
          <div class="visibility-selector">
            <div class="visibility-header">
              <el-icon><View /></el-icon>
              <span>选择回音可见范围</span>
            </div>
            <div class="visibility-options">
              <div
                class="visibility-option"
                :class="{ active: visibility === 0 }"
                @click="visibility = 0"
              >
                <div class="option-icon">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <circle cx="12" cy="12" r="10" />
                    <path d="M2 12h20" />
                    <path
                      d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-10-4 15.3 15.3 0 0 1-10 4 15.3 15.3 0 0 1 10 4 15.3 15.3 0 0 1 4 10"
                    />
                  </svg>
                </div>
                <div class="option-content">
                  <div class="option-title">公开回音</div>
                  <div class="option-desc">所有人可见</div>
                </div>
              </div>
              <div
                class="visibility-option"
                :class="{ active: visibility === 1 }"
                @click="visibility = 1"
              >
                <div class="option-icon">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                    <circle cx="9" cy="7" r="4" />
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                    <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                  </svg>
                </div>
                <div class="option-content">
                  <div class="option-title">粉丝可见</div>
                  <div class="option-desc">仅粉丝可见</div>
                </div>
              </div>
              <div
                class="visibility-option"
                :class="{ active: visibility === 2 }"
                @click="visibility = 2"
              >
                <div class="option-icon">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                  </svg>
                </div>
                <div class="option-content">
                  <div class="option-title">仅自己可见</div>
                  <div class="option-desc">私密回音</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 提交按钮 -->
          <div class="submit-action-area">
            <el-button
              class="action-btn save-btn"
              round
              @click="handleSaveAnswer"
            >
              妥善封存答案
            </el-button>
          </div>
        </div>
      </section>
    </main>

    <!-- 灵魂回音抽屉 -->
    <el-drawer
      v-model="showPlaza"
      size="480px"
      class="plaza-drawer"
      :with-header="false"
    >
      <div class="plaza-container">
        <div class="plaza-header">
          <div class="header-decoration">
            <div class="decoration-circle circle-1"></div>
            <div class="decoration-circle circle-2"></div>
            <div class="decoration-circle circle-3"></div>
          </div>
          <div class="header-content">
            <h2 class="plaza-title">灵魂回音</h2>
            <p class="plaza-subtitle">那些与你同样在寻找答案的人</p>
          </div>
        </div>

        <div
          class="plaza-scroll-area"
          v-infinite-scroll="loadPlazaData"
          :infinite-scroll-disabled="plazaLoading || plazaNoMore || !showPlaza"
          :infinite-scroll-distance="50"
          :infinite-scroll-immediate="false"
        >
          <div
            v-if="plazaList.length === 0 && !plazaLoading"
            class="empty-plaza"
          >
            <div class="empty-animation">
              <div class="empty-leaf">🍃</div>
              <div class="empty-dots">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
            <p class="empty-text">
              这里还很安静<br />去成为第一个留下回音的人吧
            </p>
          </div>

          <div class="plaza-item" v-for="item in plazaList" :key="item.id">
            <div class="item-decoration"></div>
            <div class="author-row">
              <div class="avatar-wrapper">
                <img
                  :src="
                    item.author?.avatarUrl ||
                    'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                  "
                  class="author-avatar"
                />
                <div class="avatar-ring"></div>
              </div>
              <div class="author-info">
                <span class="author-name">{{
                  item.author?.nickname || '匿名灵魂'
                }}</span>
                <span class="author-time">{{
                  formatTime(item.createdAt)
                }}</span>
              </div>
            </div>

            <div class="answer-content" v-if="item.content">
              {{ item.content }}
            </div>

            <div class="audio-content" v-if="item.audioUrl">
              <div class="audio-wrapper">
                <audio
                  :src="item.audioUrl"
                  controls
                  class="custom-audio"
                ></audio>
              </div>
            </div>

            <div class="interaction-row">
              <div
                class="like-btn"
                :class="{ 'is-liked': item.isLiked }"
                @click="toggleLike(item)"
              >
                <svg
                  viewBox="0 0 24 24"
                  width="20"
                  height="20"
                  stroke="currentColor"
                  :fill="item.isLiked ? 'currentColor' : 'none'"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path
                    d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
                  ></path>
                </svg>
                <span class="like-count">{{ item.likeCount || 0 }}</span>
              </div>
            </div>
          </div>

          <div v-if="plazaLoading" class="loading-state">
            <div class="loading-spinner"></div>
            <span>回音加载中...</span>
          </div>
          <div v-if="plazaNoMore && plazaList.length > 0" class="nomore-state">
            <span>已经看完了所有的回音</span>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
/* 极光背景保留 */
.daily-question-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #e0f7fa 0%, #f9fbe7 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
    'Helvetica Neue', Arial, sans-serif;
  z-index: 1;
}
.ambient-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  z-index: -1;
  opacity: 0.6;
  pointer-events: none;
  animation: float 14s infinite alternate ease-in-out;
}
.glow-1 {
  width: 600px;
  height: 600px;
  background: #a8edea;
  top: -100px;
  left: 5%;
}
.glow-2 {
  width: 700px;
  height: 700px;
  background: #fed6e3;
  bottom: -150px;
  right: 0%;
  animation-delay: -7s;
}
@keyframes float {
  0% {
    transform: translateY(0) scale(1);
  }
  100% {
    transform: translateY(50px) scale(1.15);
  }
}

/* 头部 */
.page-header {
  display: flex;
  align-items: center;
  padding-top: 27px;
  padding-left: 50px;
  background: transparent;
  z-index: 10;
  width: fit-content;
}

.main-layout {
  display: flex;
  flex: 1;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  padding: 0px 40px 40px;
  gap: 60px;
  box-sizing: border-box;
}

/* 左侧发问区 */
.left-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.back-button {
  position: absolute;
  top: 0;
  left: 0;
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

.back-button:hover {
  background: rgba(255, 255, 255, 1);
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}
.question-display {
  text-align: center;
  max-width: 500px;
  position: relative;
}
.quote-mark {
  font-size: 100px;
  color: rgba(64, 158, 255, 0.15);
  font-family: Georgia, serif;
  line-height: 0.5;
  position: absolute;
}
.top-quote {
  top: -40px;
  left: -40px;
}
.bottom-quote {
  bottom: -60px;
  right: -40px;
  transform: rotate(180deg);
}
.question-text {
  font-size: 34px;
  font-weight: 700;
  color: #2c3e50;
  line-height: 1.7;
  font-family: 'Songti SC', 'SimSun', serif;
  margin: 0;
  position: relative;
  z-index: 2;
  text-shadow: 0 2px 4px rgba(255, 255, 255, 0.5);
}

/* 去往广场按钮 */
.explore-plaza-wrapper {
  margin-top: 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  animation: fadeIn 1.5s ease-in-out;
}
.explore-btn {
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  padding: 12px 30px;
  border-radius: 30px;
  color: #2c3e50;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);
}
.explore-btn:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(64, 158, 255, 0.15);
  color: #409eff;
}
.explore-icon {
  font-size: 20px;
}
.explore-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 12px;
  letter-spacing: 1px;
}

@keyframes fadeIn {
  0% {
    opacity: 0;
    transform: translateY(10px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 右侧工作区 */
.right-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.glass-card {
  width: 100%;
  max-width: 550px;
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(25px);
  border-radius: 24px;
  padding: 30px;
  box-shadow:
    0 10px 40px rgba(0, 0, 0, 0.05),
    inset 0 0 0 1px rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.8);
  transition: all 0.5s ease;
}
.glass-card.is-focused {
  box-shadow:
    0 15px 50px rgba(64, 158, 255, 0.15),
    inset 0 0 0 1px rgba(255, 255, 255, 0.8);
  transform: scale(1.02);
}

/* 输入区 (默认解锁) */
.answer-wrapper {
  width: 100%;
  margin-bottom: 25px;
}
.textarea-mask {
  border-radius: 16px;
  padding: 2px;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.8) 0%,
    rgba(255, 255, 255, 0.4) 100%
  );
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
}
:deep(.zen-textarea .el-textarea__inner) {
  background: rgba(255, 255, 255, 0.85);
  border: none;
  border-radius: 14px 14px 0 0;
  padding: 20px;
  font-size: 16px;
  line-height: 1.8;
  color: #333;
  font-family: 'Songti SC', 'SimSun', serif;
  box-shadow: none;
  transition: background 0.3s;
}
:deep(.zen-textarea .el-textarea__inner:focus) {
  background: #ffffff;
}

/* 语音条 */
.audio-record-section {
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 0 0 14px 14px;
  border-top: 1px dashed rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 56px;
  box-sizing: border-box;
}
.recording-active {
  display: flex;
  align-items: center;
  gap: 15px;
  color: #f56c6c;
  font-weight: 600;
  font-size: 14px;
}
.pulsing-dot {
  width: 10px;
  height: 10px;
  background-color: #f56c6c;
  border-radius: 50%;
  animation: pulse-red 1.5s infinite;
}
@keyframes pulse-red {
  0% {
    box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.7);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(245, 108, 108, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(245, 108, 108, 0);
  }
}
.recorded-audio {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 16px;
  background: rgba(245, 247, 250, 0.8);
  border-radius: 16px;
  border: 2px solid rgba(64, 158, 255, 0.1);
  transition: all 0.3s ease;
}

.recorded-audio:hover {
  border-color: rgba(64, 158, 255, 0.3);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.1);
}

.audio-player-wrapper {
  flex: 1;
  position: relative;
}

.recorded-audio audio {
  height: 40px;
  width: 100%;
  border-radius: 20px;
}

.recorded-audio audio::-webkit-media-controls-panel {
  background: linear-gradient(135deg, #60baff 0%, #94f4ff 100%);
  border-radius: 20px;
}

.del-audio-btn {
  font-size: 13px;
  font-weight: 600;
  padding: 8px 16px;
  border-radius: 12px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 4px;
}

.del-audio-btn:hover {
  background: rgba(245, 108, 108, 0.1);
  transform: scale(1.05);
}

/* 🌟 可选的专注模式 */
.focus-tool-section {
  background: rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  padding: 15px;
  margin-bottom: 25px;
  border: 1px dashed rgba(0, 0, 0, 0.1);
}
.focus-header {
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}
.timer-flex-row {
  display: flex;
  align-items: center;
  gap: 15px;
}

.clock-display {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  padding: 10px 0;
  text-align: center;
  border: 1px solid #ebeef5;
  transition: all 0.3s;
}
.clock-display.is-active {
  background: #ecf5ff;
  border-color: #b3d8ff;
}
.clock-time {
  font-size: 24px;
  font-weight: 800;
  color: #606266;
  font-family: 'Courier New', Courier, monospace;
}
.clock-display.is-active .clock-time {
  color: #409eff;
}

.timer-selector {
  flex: 2;
}
.timer-selector.is-disabled {
  opacity: 0.5;
  pointer-events: none;
}
.time-grid {
  display: flex;
  gap: 8px;
  width: 100%;
}
:deep(.el-radio.time-radio) {
  margin: 0;
  flex: 1;
  height: 42px;
  border-radius: 8px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
:deep(.el-radio.time-radio.is-checked) {
  background: #ecf5ff;
  border-color: #409eff;
}
:deep(.el-radio__input) {
  display: none;
}
:deep(.el-radio__label) {
  padding-left: 0;
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}
:deep(.el-radio.is-checked .el-radio__label) {
  color: #409eff;
}

.start-btn {
  height: 42px;
  padding: 0 20px;
  border-radius: 8px;
  background: #f4f4f5;
  color: #909399;
  border: none;
  font-weight: 600;
}
.start-btn:not(:disabled) {
  background: #409eff;
  color: white;
  cursor: pointer;
}
.start-btn:not(:disabled):hover {
  background: #66b1ff;
}

/* 提交按钮区域 */
.submit-action-area {
  width: 100%;
}
.save-btn {
  width: 100%;
  padding: 14px 0;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #67c23a 0%, #5daf34 100%);
  color: white;
  border: none;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
  box-shadow: 0 4px 15px rgba(103, 194, 58, 0.3);
}
.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(103, 194, 58, 0.4);
}

/* 权限选择器样式 */
.visibility-selector {
  width: 100%;
  margin-bottom: 25px;
}
.visibility-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 15px;
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}
.visibility-header .el-icon {
  font-size: 18px;
  color: #409eff;
}
.visibility-options {
  display: flex;
  gap: 12px;
}
.visibility-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 16px 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.6);
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}
.visibility-option::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    135deg,
    rgba(64, 158, 255, 0.1) 0%,
    rgba(64, 158, 255, 0.05) 100%
  );
  opacity: 0;
  transition: opacity 0.3s ease;
}
.visibility-option:hover::before {
  opacity: 1;
}
.visibility-option.active {
  border-color: #409eff;
  background: rgba(64, 158, 255, 0.08);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}
.option-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.8);
  color: #909399;
  transition: all 0.3s ease;
}
.visibility-option.active .option-icon {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: white;
  transform: scale(1.1);
}
.option-icon svg {
  width: 20px;
  height: 20px;
}
.option-content {
  text-align: center;
}
.option-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 3px;
}
.option-desc {
  font-size: 11px;
  color: #909399;
}

/* 抽屉样式优化 */
:deep(.plaza-drawer) {
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(250, 250, 252, 0.95) 100%
  ) !important;
  backdrop-filter: blur(30px) !important;
}
:deep(.plaza-drawer .el-drawer__body) {
  padding: 0;
}
.plaza-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
}
.plaza-header {
  padding: 35px 30px 25px;
  background: linear-gradient(
    135deg,
    rgba(64, 158, 255, 0.05) 0%,
    rgba(103, 194, 58, 0.05) 100%
  );
  position: relative;
  overflow: hidden;
}
.header-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}
.decoration-circle {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(
    135deg,
    rgba(64, 158, 255, 0.1) 0%,
    rgba(103, 194, 58, 0.1) 100%
  );
  animation: float-circle 6s ease-in-out infinite;
}
.circle-1 {
  width: 80px;
  height: 80px;
  top: -20px;
  right: -10px;
  animation-delay: 0s;
}
.circle-2 {
  width: 60px;
  height: 60px;
  bottom: -15px;
  left: 20px;
  animation-delay: -2s;
}
.circle-3 {
  width: 40px;
  height: 40px;
  top: 30px;
  left: 50%;
  animation-delay: -4s;
}
@keyframes float-circle {
  0%,
  100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-10px) scale(1.05);
  }
}
.header-content {
  position: relative;
  z-index: 1;
}
.plaza-title {
  font-size: 28px;
  color: #2c3e50;
  margin: 0 0 8px 0;
  font-weight: 800;
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.plaza-subtitle {
  color: #909399;
  font-size: 14px;
  margin: 0;
  font-weight: 500;
}
.plaza-scroll-area {
  flex: 1;
  overflow-y: auto;
  padding: 0 25px 30px;
}
.empty-plaza {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 50%;
  color: #a8abb2;
  text-align: center;
}
.empty-animation {
  position: relative;
  margin-bottom: 20px;
}
.empty-leaf {
  font-size: 48px;
  animation: leaf-float 3s ease-in-out infinite;
}
@keyframes leaf-float {
  0%,
  100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-10px) rotate(5deg);
  }
}
.empty-dots {
  display: flex;
  gap: 6px;
  margin-top: 10px;
}
.empty-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c0c4cc;
  animation: dot-pulse 1.5s ease-in-out infinite;
}
.empty-dots span:nth-child(1) {
  animation-delay: 0s;
}
.empty-dots span:nth-child(2) {
  animation-delay: 0.3s;
}
.empty-dots span:nth-child(3) {
  animation-delay: 0.6s;
}
@keyframes dot-pulse {
  0%,
  100% {
    opacity: 0.3;
    transform: scale(0.8);
  }
  50% {
    opacity: 1;
    transform: scale(1);
  }
}
.empty-text {
  font-size: 14px;
  color: #909399;
  line-height: 1.8;
  margin: 0;
}
.plaza-item {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  padding: 24px;
  margin-bottom: 18px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}
.plaza-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #409eff 0%, #67c23a 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}
.plaza-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
}
.plaza-item:hover::before {
  opacity: 1;
}
.item-decoration {
  position: absolute;
  top: -10px;
  right: -10px;
  width: 60px;
  height: 60px;
  background: radial-gradient(
    circle,
    rgba(64, 158, 255, 0.1) 0%,
    transparent 70%
  );
  border-radius: 50%;
  pointer-events: none;
}
.author-row {
  display: flex;
  align-items: center;
  margin-bottom: 18px;
}
.avatar-wrapper {
  position: relative;
  margin-right: 14px;
}
.author-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.avatar-ring {
  position: absolute;
  top: -3px;
  left: -3px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 2px solid transparent;
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%) border-box;
  -webkit-mask:
    linear-gradient(#fff 0 0) padding-box,
    linear-gradient(#fff 0 0);
  mask:
    linear-gradient(#fff 0 0) padding-box,
    linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  animation: ring-pulse 2s ease-in-out infinite;
}
@keyframes ring-pulse {
  0%,
  100% {
    opacity: 0.6;
  }
  50% {
    opacity: 1;
  }
}
.author-info {
  display: flex;
  flex-direction: column;
}
.author-name {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 3px;
}
.author-time {
  font-size: 12px;
  color: #a8abb2;
  font-weight: 500;
}
.answer-content {
  font-size: 15px;
  color: #444;
  line-height: 1.8;
  margin-bottom: 18px;
  word-wrap: break-word;
  white-space: pre-wrap;
  text-align: justify;
  padding: 16px;
  background: rgba(245, 247, 250, 0.5);
  border-radius: 12px;
  border-left: 3px solid #409eff;
}
.audio-content {
  margin-bottom: 18px;
}
.audio-wrapper {
  background: rgba(245, 247, 250, 0.8);
  border-radius: 24px;
  padding: 12px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.custom-audio {
  width: 100%;
  height: 40px;
  border-radius: 20px;
}
.interaction-row {
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #f0f2f5;
  padding-top: 14px;
}
.like-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  user-select: none;
  padding: 8px 16px;
  border-radius: 20px;
  background: rgba(245, 247, 250, 0.5);
}
.like-btn:hover {
  color: #f56c6c;
  background: rgba(245, 108, 108, 0.1);
  transform: scale(1.05);
}
.like-btn.is-liked {
  color: #f56c6c;
  background: rgba(245, 108, 108, 0.15);
}
.like-btn.is-liked:hover {
  background: rgba(245, 108, 108, 0.25);
}
.like-count {
  font-weight: 600;
}
.loading-state,
.nomore-state {
  text-align: center;
  padding: 20px 0;
  color: #c0c4cc;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}
.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid #e4e7ed;
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
