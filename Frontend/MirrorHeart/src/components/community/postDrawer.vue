<script setup>
import { ref, onBeforeUnmount } from 'vue'
import { Plus, Microphone, VideoPause, Delete } from '@element-plus/icons-vue'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import { submitPostService, uploadFileService } from '@/api/userCommunity.js'
// import { useUserStore } from '@/stores'
import { ElMessage } from 'element-plus'
// const userStore = useUserStore()

const props = defineProps({
  message: Number
})

const emit = defineEmits(['refresh'])

// const token = userStore.basicUser.token

// 配图的上传
const fileList = ref([])

// uploadFile 上传的文件
const uploadFileList = ref([])

const mediaRecorder = ref(null)
const audioChunks = ref([])
const isRecording = ref(false)
const recordingTime = ref(0)
const recordTimer = ref(null)
const localAudioUrl = ref('')
const localAudioBlob = ref(null)

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
      stream.getTracks().forEach((track) => track.stop())
    }

    mediaRecorder.value.start()
    isRecording.value = true
    recordingTime.value = 0
    recordTimer.value = setInterval(() => {
      recordingTime.value++
    }, 1000)
    ElMessage.success('开始录音...')
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
}

// 上传图片前的检查
const beforeFileUpload = (file) => {
  if (fileList.value.length > 9) {
    ElMessage.warning('最多上传9张图片')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.warning('上传图片大小不能超过 5MB!')
    return false
  }
  return true
}

//图片预览
const onUploadMultipleFile = (uploadFile) => {
  fileList.value.push({
    name: uploadFile.raw.name,
    url: URL.createObjectURL(uploadFile.raw)
  })
  uploadFileList.value.push(uploadFile.raw)
}

// 上传图片超过9张
const handleExceed = () => {
  ElMessage.warning('最多上传9张图片')
  return false
}

const dialogImageUrl = ref('')

const dialogVisible = ref(false)

const handleRemove = () => {
  uploadFileList.value.splice(-1, 1)
}

const handlePictureCardPreview = (file) => {
  dialogImageUrl.value = file.url
  dialogVisible.value = true
}

//设置抽屉的显示与隐藏
const isPostDrawer = ref(false)

const openPostDrawer = () => {
  isPostDrawer.value = true
}

const title = ref('') // 标题

const content = ref('') // 内容

const uploadType = ref(0) // 上传类型

const blockType = ref(0) // 屏蔽类型

// 发布帖子
const handleSubPost = async (number) => {
  if (!title.value || !content.value) {
    ElMessage.warning('请填写完整信息再提交哦！')
    return
  }

  uploadType.value = number

  try {
    ElMessage.info('正在上传文件...')

    // 1. 上传正文配图
    const imageUrls = []
    if (uploadFileList.value.length > 0) {
      for (const file of uploadFileList.value) {
        const imgRes = await uploadFileService(file, 'POST_IMG')
        imageUrls.push(imgRes.data.data)
      }
    }

    // 2. 上传音频文件
    let audioUrl = null
    let audioDurationMs = 0
    if (localAudioBlob.value) {
      const audioFile = new File([localAudioBlob.value], 'recording.webm', {
        type: 'audio/webm'
      })
      const audioRes = await uploadFileService(audioFile, 'AUDIO')
      audioUrl = audioRes.data.data
      audioDurationMs = recordingTime.value * 1000 // 转换为毫秒
    }

    ElMessage.info('正在发布帖子...')

    // 3. 构建发布帖子请求数据
    const postData = {
      title: title.value,
      text: content.value,
      visibility: blockType.value,
      imageUrls: imageUrls,
      audioUrl: audioUrl,
      audioDurationMs: audioDurationMs,
      tagIds: []
    }

    const res = await submitPostService(postData)
    ElMessage.success(res.data.message || '发布成功')
    isPostDrawer.value = false

    // 清空表单
    resetForm()

    // 刷新社区列表
    emit('refresh')
  } catch (error) {
    console.error('发布失败:', error)
    ElMessage.error(error.response?.data?.message || '发布失败，请稍后重试')
  }
}

// 重置表单
const resetForm = () => {
  title.value = ''
  content.value = ''
  fileList.value = []
  uploadFileList.value = []
  localAudioBlob.value = null
  localAudioUrl.value = ''
  recordingTime.value = 0
  blockType.value = 0
}

defineExpose({
  openPostDrawer
})

onBeforeUnmount(() => {
  if (recordTimer.value) {
    clearInterval(recordTimer.value)
  }
  if (mediaRecorder.value && isRecording.value) {
    mediaRecorder.value.stop()
  }
})
</script>

<template>
  <el-drawer
    v-model="isPostDrawer"
    :with-header="false"
    size="45%"
    class="custom-drawer"
  >
    <div class="drawer-container">
      <header class="drawer-header">
        <h2>{{ props.message === 1 ? '发布新图文' : '记录日记' }}</h2>
        <p class="subtitle">与世界分享这一刻的灵感与思考</p>
      </header>

      <div class="form-wrapper">
        <el-form :model="postFormModel" ref="formRef" label-position="top">
          <el-form-item
            :label="props.message === 1 ? '标题' : '日记标题'"
            prop="title"
          >
            <el-input
              v-model="title"
              size="large"
              placeholder="填写一个吸引人的标题吧..."
              class="custom-input"
            >
            </el-input>
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12" v-if="props.message === 1">
              <el-form-item label="可见范围" prop="blockType">
                <el-select
                  v-model.number="blockType"
                  placeholder="请选择屏蔽类型"
                  size="large"
                  class="custom-select"
                >
                  <el-option label="全部可见" :value="0" />
                  <el-option label="仅订阅可见" :value="1" />
                  <el-option label="仅订阅不可见" :value="2" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12" v-if="props.message === 2">
              <el-form-item label="可见范围" prop="blockType">
                <el-select
                  v-model.number="blockType"
                  placeholder="请选择屏蔽类型"
                  size="large"
                  class="custom-select"
                >
                  <el-option label="全部可见" :value="0" />
                  <el-option label="仅订阅可见" :value="1" />
                  <el-option label="仅订阅不可见" :value="2" />
                  <el-option label="仅自己可见" :value="3" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="语音附件" prop="audio">
            <div class="audio-recorder">
              <div v-if="isRecording" class="recording-status">
                <div class="recording-indicator"></div>
                <span class="recording-time">{{
                  formatDuration(recordingTime)
                }}</span>
                <el-button
                  type="danger"
                  size="small"
                  circle
                  @click="stopRecording"
                >
                  <el-icon><VideoPause /></el-icon>
                </el-button>
              </div>
              <div v-else-if="localAudioUrl" class="recorded-audio">
                <audio
                  :src="localAudioUrl"
                  controls
                  class="custom-audio"
                ></audio>
                <el-button
                  type="danger"
                  link
                  @click="deleteRecording"
                  class="delete-audio-btn"
                >
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
              <div v-else class="start-record">
                <el-button round @click="startRecording">
                  <el-icon><Microphone /></el-icon>
                  <span>录制语音</span>
                </el-button>
              </div>
            </div>
          </el-form-item>

          <el-form-item
            :label="props.message === 1 ? '正文配图 (最多9张)' : '日记配图'"
            prop="pictures"
          >
            <el-upload
              v-model:file-list="fileList"
              list-type="picture-card"
              :on-preview="handlePictureCardPreview"
              :on-remove="handleRemove"
              :before-upload="beforeFileUpload"
              :on-change="onUploadMultipleFile"
              :limit="9"
              :on-exceed="handleExceed"
              accept="image/*"
              :auto-upload="false"
              class="picture-uploader"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>

            <el-dialog v-model="dialogVisible" width="50%" align-center>
              <img
                style="width: 100%; border-radius: 12px"
                :src="dialogImageUrl"
                alt="Preview Image"
              />
            </el-dialog>
          </el-form-item>

          <el-form-item
            :label="props.message === 1 ? '正文内容' : '日记内容'"
            prop="content"
          >
            <div class="editor-container">
              <quill-editor
                theme="snow"
                v-model:content="content"
                contentType="html"
                placeholder="写下你想说的..."
              ></quill-editor>
            </div>
          </el-form-item>

          <div class="action-footer">
            <el-button
              round
              class="btn-draft"
              @click="handleSubPost(props.message === 1 ? 1 : 3)"
            >
              存为草稿
            </el-button>
            <el-button
              round
              type="primary"
              class="btn-publish"
              @click="handleSubPost(props.message === 1 ? 0 : 2)"
            >
              立即发布
            </el-button>
          </div>
        </el-form>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped>
/* 抽屉样式穿透覆盖 */
:deep(.el-drawer) {
  background: linear-gradient(135deg, #f5f7fa 0%, #e4efe9 100%);
}
:deep(.el-drawer__body) {
  padding: 0;
}

.drawer-container {
  display: flex;
  border-radius: 24px;
  flex-direction: column;
  min-height: 100%;
  width: 100%;
  box-sizing: border-box;
  padding: 40px;
  background: linear-gradient(135deg, #e0f7fa 0%, #f9fbe7 100%);
  overflow-y: auto;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
    'Helvetica Neue', Arial, sans-serif;
}

.drawer-header {
  margin-bottom: 30px;
}

.drawer-header h2 {
  font-size: 28px;
  font-weight: 800;
  color: #2c3e50;
  margin: 0 0 8px 0;
}

.subtitle {
  font-size: 14px;
  color: #7f8c8d;
  margin: 0;
}

.form-wrapper {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

:deep(.el-form-item__label) {
  font-size: 15px;
  font-weight: 600;
  color: #34495e;
  padding-bottom: 8px;
}

/* 封面上传器美化 */
.avatar-uploader {
  width: 100%;
}
:deep(.avatar-uploader .el-upload) {
  width: 100%;
  height: 200px;
  border: 2px dashed #dcdfe6;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}
:deep(.avatar-uploader .el-upload:hover) {
  border-color: #409eff;
  background: rgba(64, 158, 255, 0.05);
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #8c939d;
}
.upload-icon {
  font-size: 32px;
  margin-bottom: 10px;
}
.uploaded-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 语音录制器样式 */
.audio-recorder {
  width: 100%;
}

.recording-status {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: rgba(255, 107, 107, 0.1);
  border: 2px solid rgba(255, 107, 107, 0.3);
  border-radius: 12px;
}

.recording-indicator {
  width: 12px;
  height: 12px;
  background: #ff6b6b;
  border-radius: 50%;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.7;
  }
}

.recording-time {
  flex: 1;
  font-size: 16px;
  font-weight: 600;
  color: #ff6b6b;
  font-family: 'Courier New', monospace;
}

.recorded-audio {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
}

.custom-audio {
  flex: 1;
  height: 36px;
}

.delete-audio-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #ff6b6b;
  font-size: 14px;
}

.delete-audio-btn:hover {
  color: #ff4757;
}

.start-record {
  display: flex;
  align-items: center;
}

.start-record .el-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
  font-weight: 600;
  transition: all 0.3s ease;
}

.start-record .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

/* 富文本编辑器美化 */
.editor-container {
  width: 100%;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
}
:deep(.ql-toolbar.ql-snow) {
  border: none;
  border-bottom: 1px solid #ebeef5;
  background: #f8f9fa;
}
:deep(.ql-container.ql-snow) {
  border: none;
  min-height: 200px;
  font-size: 15px;
  font-family: inherit;
}

/* 底部操作区 */
.action-footer {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  margin-top: 40px;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.btn-draft {
  padding: 12px 24px;
  border: none;
  background: #f0f2f5;
  color: #606266;
  font-weight: 600;
}
.btn-draft:hover {
  background: #e4e7ed;
  color: #303133;
}

.btn-publish {
  padding: 12px 30px;
  font-weight: 600;
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}
.btn-publish:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.4);
}
</style>
