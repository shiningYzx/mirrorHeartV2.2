<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowLeft,
  Microphone,
  Loading,
  VideoPause,
  Delete,
  Refresh
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

const isRecording = ref(false)
const isPaused = ref(false)
const isSubmitting = ref(false)
const recordingTime = ref(0)
const audioUrl = ref('')
const audioBlob = ref(null)
let mediaRecorder = null
let audioChunks = []
let timer = null

const handleBack = () => {
  cleanup()
  router.back()
}

const cleanup = () => {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
    mediaRecorder.stream.getTracks().forEach((track) => track.stop())
  }

  if (timer) {
    clearInterval(timer)
    timer = null
  }

  if (audioUrl.value) {
    URL.revokeObjectURL(audioUrl.value)
    audioUrl.value = ''
  }

  audioBlob.value = null
  isRecording.value = false
  isPaused.value = false
  recordingTime.value = 0
}

const startRecording = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaRecorder = new MediaRecorder(stream)
    audioChunks = []

    mediaRecorder.ondataavailable = (event) => {
      audioChunks.push(event.data)
    }

    mediaRecorder.onstop = () => {
      const blob = new Blob(audioChunks, { type: 'audio/webm' })
      audioBlob.value = blob
      audioUrl.value = URL.createObjectURL(blob)
    }

    mediaRecorder.start()
    isRecording.value = true
    isPaused.value = false
    recordingTime.value = 0

    timer = setInterval(() => {
      if (!isPaused.value) {
        recordingTime.value++
      }
    }, 1000)

    ElMessage.success('开始录音，请说出你的心声...')
  } catch (error) {
    console.error('录音失败:', error)
    ElMessage.error('无法访问麦克风，请检查权限设置')
  }
}

const pauseRecording = () => {
  if (mediaRecorder && mediaRecorder.state === 'recording') {
    mediaRecorder.pause()
    isPaused.value = true
    ElMessage.info('录音已暂停')
  }
}

const resumeRecording = () => {
  if (mediaRecorder && mediaRecorder.state === 'paused') {
    mediaRecorder.resume()
    isPaused.value = false
    ElMessage.info('继续录音...')
  }
}

const stopRecording = () => {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
    mediaRecorder.stream.getTracks().forEach((track) => track.stop())
  }

  if (timer) {
    clearInterval(timer)
    timer = null
  }

  isRecording.value = false
  isPaused.value = false
}

const deleteRecording = () => {
  if (audioUrl.value) {
    URL.revokeObjectURL(audioUrl.value)
  }
  audioUrl.value = ''
  audioBlob.value = null
  recordingTime.value = 0
  ElMessage.info('已删除录音')
}

const resetRecording = () => {
  deleteRecording()
  ElMessage.info('可以重新录制了')
}

const submitAudio = async () => {
  if (!audioBlob.value) {
    ElMessage.warning('请先录制语音')
    return
  }

  isSubmitting.value = true
  try {
    await new Promise((resolve) => setTimeout(resolve, 1000))
    ElMessage.success('心声已记录，感谢你的分享')
    deleteRecording()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    isSubmitting.value = false
  }
}

const formatTime = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

onMounted(() => {
  window.scrollTo(0, 0)
})

onUnmounted(() => {
  cleanup()
})
</script>

<template>
  <div class="heart-voice-page">
    <div class="page-header">
      <el-button class="back-button" @click="handleBack" circle>
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <div class="header-title">
        <h2>此刻心声</h2>
        <p>Heart Voice</p>
      </div>
    </div>

    <div class="main-layout">
      <div class="left-panel">
        <div class="mirror-frame">
          <div class="mirror-surface">
            <div class="mirror-reflection"></div>
            <div class="mirror-shine"></div>
          </div>
        </div>
      </div>

      <div class="right-panel">
        <div class="voice-container">
          <div class="voice-intro">
            <div class="intro-icon">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
                />
              </svg>
            </div>
            <h3 class="intro-title">把心事说给镜子听</h3>
            <p class="intro-text">
              如果此刻的心情有形状，它是轻盈的还是沉重的？别关在心里，试着把它说出来。
            </p>
          </div>

          <div class="voice-input-area">
            <div v-if="!audioUrl" class="recording-controls">
              <el-tooltip
                :content="
                  isRecording
                    ? isPaused
                      ? '点击继续录音'
                      : '点击暂停录音'
                    : '点击开始录音'
                "
                placement="top"
                effect="light"
              >
                <button
                  class="record-button"
                  @click="
                    isRecording
                      ? isPaused
                        ? resumeRecording()
                        : pauseRecording()
                      : startRecording()
                  "
                  :class="{
                    recording: isRecording && !isPaused,
                    paused: isPaused
                  }"
                >
                  <el-icon class="mic-icon">
                    <Microphone />
                  </el-icon>
                  <span class="record-text">
                    {{
                      isRecording
                        ? isPaused
                          ? '继续录音'
                          : formatTime(recordingTime)
                        : '点击录音'
                    }}
                  </span>
                </button>
              </el-tooltip>

              <div v-if="isRecording && !isPaused" class="pause-hint-external">
                <el-icon><VideoPause /></el-icon>
                <span>再次点击可以暂停录音</span>
              </div>

              <div v-if="isRecording" class="control-buttons">
                <button class="control-btn stop-btn" @click="stopRecording">
                  <el-icon><VideoPause /></el-icon>
                  <span>结束</span>
                </button>
                <button class="control-btn cancel-btn" @click="resetRecording">
                  <el-icon><Delete /></el-icon>
                  <span>取消</span>
                </button>
              </div>

              <div class="recording-hint">
                {{
                  isRecording
                    ? isPaused
                      ? '录音已暂停，点击按钮继续'
                      : '正在录音...'
                    : '点击上方按钮开始录制你的心声'
                }}
              </div>
            </div>

            <div v-else class="recorded-audio">
              <div class="audio-player-wrapper">
                <audio :src="audioUrl" controls class="audio-player"></audio>
              </div>
              <div class="audio-actions">
                <button
                  class="action-btn re-record-btn"
                  @click="resetRecording"
                >
                  <el-icon><Refresh /></el-icon>
                  <span>重新录制</span>
                </button>
                <button class="action-btn delete-btn" @click="deleteRecording">
                  <el-icon><Delete /></el-icon>
                  <span>删除</span>
                </button>
                <button
                  class="action-btn submit-btn"
                  @click="submitAudio"
                  :disabled="isSubmitting"
                >
                  <el-icon v-if="isSubmitting" class="is-loading"
                    ><Loading
                  /></el-icon>
                  <span>{{ isSubmitting ? '提交中...' : '提交心声' }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.heart-voice-page {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  padding: 20px 40px;
  background: linear-gradient(135deg, #ffeef8 0%, #fff5e6 100%);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.page-header {
  display: flex;
  align-items: center;
  padding-top: 10px;
  position: relative;
}

.back-button {
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

.header-title {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  text-align: center;
}

.header-title h2 {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 4px 0;
  color: #333;
  letter-spacing: 2px;
}

.header-title p {
  font-size: 14px;
  color: #999;
  margin: 0;
  font-weight: 500;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.main-layout {
  display: flex;
  gap: 40px;
  flex: 1;
  min-height: 0;
}

.left-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeInLeft 0.8s ease;
}

@keyframes fadeInLeft {
  from {
    opacity: 0;
    transform: translateX(-30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.mirror-frame {
  width: 320px;
  height: 420px;
  background: linear-gradient(135deg, #f5f5f5 0%, #e0e0e0 100%);
  border-radius: 160px 160px 20px 20px;
  padding: 20px;
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.15),
    inset 0 2px 10px rgba(255, 255, 255, 0.8);
  position: relative;
}

.mirror-frame::before {
  content: '';
  position: absolute;
  top: 10px;
  left: 10px;
  right: 10px;
  bottom: 10px;
  background: linear-gradient(135deg, #d4d4d4 0%, #a8a8a8 100%);
  border-radius: 150px 150px 15px 15px;
  z-index: 0;
}

.mirror-surface {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #ffffff 0%, #f0f0f0 100%);
  border-radius: 140px 140px 15px 15px;
  position: relative;
  overflow: hidden;
  box-shadow: inset 0 0 30px rgba(0, 0, 0, 0.05);
}

.mirror-reflection {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.4) 0%,
    transparent 50%
  );
  border-radius: 140px 140px 15px 15px;
}

.mirror-shine {
  position: absolute;
  top: 30px;
  left: 40px;
  width: 80px;
  height: 120px;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.6) 0%,
    transparent 100%
  );
  border-radius: 40px;
  transform: rotate(-15deg);
  animation: shine 3s ease-in-out infinite;
}

@keyframes shine {
  0%,
  100% {
    opacity: 0.3;
  }
  50% {
    opacity: 0.7;
  }
}

.right-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeInRight 0.8s ease 0.2s both;
}

@keyframes fadeInRight {
  from {
    opacity: 0;
    transform: translateX(30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.voice-container {
  width: 100%;
  max-width: 500px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 40px 40px 20px 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.voice-intro {
  text-align: center;
  margin-bottom: 30px;
}

.intro-icon {
  width: 70px;
  height: 70px;
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  box-shadow: 0 8px 24px rgba(255, 154, 158, 0.3);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.intro-icon svg {
  width: 35px;
  height: 35px;
  color: white;
}

.intro-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 12px 0;
  color: #333;
  letter-spacing: 1px;
}

.intro-text {
  font-size: 16px;
  color: #666;
  margin: 0;
  line-height: 1.8;
  font-weight: 400;
}

.voice-input-area {
  position: relative;
  margin-bottom: 24px;
  text-align: center;
}

.recording-controls {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.record-button {
  width: 100%;
  height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  border: 3px dashed #e8e8e8;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  padding: 20px;
}

.record-button:hover:not(:disabled) {
  border-color: #ff9a9e;
  background: rgba(255, 154, 158, 0.05);
  transform: scale(1.02);
}

.record-button.recording {
  border-color: #ff9a9e;
  background: linear-gradient(
    135deg,
    rgba(255, 154, 158, 0.1) 0%,
    rgba(254, 207, 239, 0.1) 100%
  );
  animation: pulse-border 2s ease-in-out infinite;
}

@keyframes pulse-border {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(255, 154, 158, 0.4);
  }
  50% {
    box-shadow: 0 0 0 10px rgba(255, 154, 158, 0);
  }
}

.record-button.submitting {
  opacity: 0.7;
  cursor: not-allowed;
}

.record-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.mic-icon {
  font-size: 52px;
  color: #ff9a9e;
  transition: all 0.3s ease;
}

.mic-icon.recording {
  color: #ff6b6b;
  animation: pulse-icon 1s ease-in-out infinite;
}

@keyframes pulse-icon {
  0%,
  100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
}

.record-text {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  transition: all 0.3s ease;
  min-height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.record-button.recording .record-text {
  color: #ff6b6b;
  font-family: 'Courier New', monospace;
  font-size: 40px;
  font-weight: 700;
  min-height: 48px;
}

.pause-hint-external {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 24px;
  background: rgba(255, 107, 107, 0.12);
  border-radius: 24px;
  font-size: 14px;
  font-weight: 600;
  color: #ff6b6b;
  animation: fadeInUp 0.3s ease;
  margin-top: 8px;
}

.pause-hint-external .el-icon {
  font-size: 16px;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.record-button.paused {
  border-color: #ffa726;
  background: linear-gradient(
    135deg,
    rgba(255, 167, 38, 0.1) 0%,
    rgba(255, 193, 7, 0.1) 100%
  );
}

.record-button.paused .mic-icon {
  color: #ffa726;
}

.record-button.paused .record-text {
  color: #ffa726;
}

.control-buttons {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  justify-content: center;
  width: 100%;
}

.control-btn {
  flex: 1;
  max-width: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px 20px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.control-btn .el-icon {
  font-size: 18px;
}

.stop-btn {
  background: linear-gradient(135deg, #ff6b6b 0%, #ff8e8e 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
}

.stop-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 107, 107, 0.4);
}

.cancel-btn {
  background: linear-gradient(135deg, #909399 0%, #b1b3b8 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(144, 147, 153, 0.3);
}

.cancel-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(144, 147, 153, 0.4);
}

.recording-hint {
  margin-top: 16px;
  font-size: 14px;
  color: #999;
  font-weight: 500;
}

.recorded-audio {
  animation: fadeInUp 0.5s ease;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.audio-player-wrapper {
  background: rgba(245, 247, 250, 0.8);
  border-radius: 20px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border: 2px solid rgba(255, 154, 158, 0.2);
}

.audio-player {
  width: 100%;
  height: 48px;
}

.audio-player::-webkit-media-controls-panel {
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
  border-radius: 24px;
}

.audio-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: none;
  border-radius: 16px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 100px;
  justify-content: center;
}

.action-btn .el-icon {
  font-size: 18px;
}

.re-record-btn {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.re-record-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.4);
}

.delete-btn {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(245, 108, 108, 0.3);
}

.delete-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(245, 108, 108, 0.4);
}

.submit-btn {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(103, 194, 58, 0.4);
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.is-loading {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
