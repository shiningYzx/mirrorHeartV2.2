<script setup>
import { ref, watch } from 'vue'

const drawerIsShow = ref(false)

const openDrawer = () => {
  drawerIsShow.value = true
}

const props = defineProps({
  message: {
    type: Object,
    required: true
  }
})

const parsedContent = ref('')

const parseContent = (content) => {
  return content
    .split('\n')
    .map((item) => {
      return `<p>${item.trim()}</p>`
    })
    .join('')
}

const updateParsedContent = () => {
  const articleContent = parseContent(
    props.message.content || props.message.summary || ''
  )
  parsedContent.value = articleContent
}

watch(
  () => props.message.content,
  (newContent, oldContent) => {
    if (newContent !== oldContent) {
      updateParsedContent()
    }
  }
)

watch(
  () => props.message.summary,
  (newContent, oldContent) => {
    if (newContent !== oldContent) {
      updateParsedContent()
    }
  }
)

defineExpose({
  openDrawer
})
</script>

<template>
  <el-drawer
    v-model="drawerIsShow"
    :with-header="false"
    size="45%"
    class="custom-drawer"
  >
    <div class="content-container">
      <header class="media-header">
        <div class="media-cover-wrapper" v-if="message.coverUrl">
          <img class="media-cover" :src="message.coverUrl" alt="cover" />
        </div>

        <div class="media-info">
          <h2 class="media-title" v-if="message.title">
            《{{ message.title }}》
          </h2>
          <p class="media-author" v-if="message.summary && !message.text">
            —— {{ message.summary }}
          </p>
        </div>
      </header>

      <main class="reading-area">
        <div
          class="reading-content brief-content"
          v-if="message.summary && !message.text && !message.title"
          v-html="parsedContent"
        ></div>
        <div
          class="reading-content main-content"
          v-if="message.content"
          v-html="parsedContent"
        ></div>
      </main>
    </div>
  </el-drawer>
</template>

<style scoped>
li {
  list-style: none;
}

:deep(.el-drawer) {
  background: linear-gradient(135deg, #f5f7fa 0%, #e4efe9 100%);
}
:deep(.el-drawer__body) {
  padding: 0;
}

.content-container {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  width: 100%;
  box-sizing: border-box;
  padding: 40px 50px;
  background: linear-gradient(135deg, #e0f7fa 0%, #f9fbe7 100%);
  overflow-y: auto;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
    'Songti SC', 'SimSun', serif;
}

.media-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 30px;
  text-align: center;
}

.media-cover-wrapper {
  margin-bottom: 25px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.15);
  transition: transform 0.3s ease;
}
.media-cover-wrapper:hover {
  transform: translateY(-5px);
}

.media-cover {
  width: 220px;
  max-height: 320px;
  object-fit: cover;
  display: block;
}

.media-info {
  margin-top: 10px;
}

.media-title {
  font-size: 26px;
  font-weight: 800;
  color: #2c3e50;
  margin: 0 0 10px 0;
  letter-spacing: 1px;
}

.media-author {
  font-size: 16px;
  color: #7f8c8d;
  margin: 0;
}

.reading-area {
  margin-bottom: 30px;
}

.reading-content {
  font-size: 18px;
  color: #34495e;
  line-height: 2;
  text-align: justify;
  letter-spacing: 0.5px;
  background: rgba(255, 255, 255, 0.5);
  padding: 30px;
  border-radius: 16px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
}

:deep(.reading-content p) {
  margin-bottom: 1em;
  text-indent: 2em;
}
</style>
