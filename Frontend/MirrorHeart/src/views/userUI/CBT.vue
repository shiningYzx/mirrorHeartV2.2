<script setup>
import MindMirror from '@/components/CBT/MindMirror.vue'
import GlimmerActionPage from '@/components/CBT/GlimmerActionPage.vue'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

defineOptions({
  name: 'CBT'
})

const router = useRouter()

const showGlimmerActionPage = ref(false)
const showMindMirrorPage = ref(false)

const openGlimmerActionPage = () => {
  showGlimmerActionPage.value = true
}

const closeGlimmerActionPage = () => {
  showGlimmerActionPage.value = false
}

const openMindMirrorPage = () => {
  showMindMirrorPage.value = true
}

const closeMindMirrorPage = () => {
  showMindMirrorPage.value = false
}

const openHeartVoicePage = () => {
  router.push('/heart-voice')
}
</script>
<template>
  <div class="container">
    <div v-if="!showGlimmerActionPage && !showMindMirrorPage" class="content">
      <div class="header">
        <h1 class="main-title">心灵疗愈空间</h1>
        <p class="sub-title">在这里，你可以找到内心的平静与力量</p>
      </div>
      <div class="cardsContainer">
        <div class="card glimmerAction" @click="openGlimmerActionPage">
          <div class="icon-wrapper">
            <div class="icon">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <circle cx="12" cy="12" r="5" />
                <line x1="12" y1="1" x2="12" y2="3" />
                <line x1="12" y1="21" x2="12" y2="23" />
                <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
                <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
                <line x1="1" y1="12" x2="3" y2="12" />
                <line x1="21" y1="12" x2="23" y2="12" />
                <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
                <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
              </svg>
            </div>
          </div>
          <div class="cardContent">
            <h2>微光行动</h2>
            <p>Glimmer Action</p>
            <p class="description">记录生活中的小确幸，点亮内心的温暖光芒</p>
          </div>
        </div>
        <div class="card mindMirror" @click="openMindMirrorPage">
          <div class="icon-wrapper">
            <div class="icon">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"
                />
              </svg>
            </div>
          </div>
          <div class="cardContent">
            <h2>解忧镜</h2>
            <p>Mind Mirror</p>
            <p class="description">与AI倾诉心事，获得温暖的陪伴与建议</p>
          </div>
        </div>
        <div class="card heartVoice" @click="openHeartVoicePage">
          <div class="icon-wrapper">
            <div class="icon">
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
          </div>
          <div class="cardContent">
            <h2>此刻心声</h2>
            <p>Heart Voice</p>
            <p class="description">把心事说给镜子听，释放内心的声音</p>
          </div>
        </div>
      </div>
    </div>
    <GlimmerActionPage
      v-if="showGlimmerActionPage"
      @close="closeGlimmerActionPage"
    />
    <MindMirror
      v-if="showMindMirrorPage"
      :drawer="true"
      @close="closeMindMirrorPage"
    />
  </div>
</template>
<style scoped>
.container {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  padding: 20px 40px;
  background: linear-gradient(135deg, #d6f7ff 0%, #fdfae8 100%);
  display: flex;
  flex-direction: column;
  height: 100%;
}

.header {
  text-align: center;
  margin-bottom: 50px;
  animation: fadeInDown 0.8s ease;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.main-title {
  font-size: 42px;
  font-weight: 800;
  margin: 0 0 15px 0;
  color: #ffffff;
  text-shadow:
    0 2px 4px rgba(0, 0, 0, 0.3),
    0 4px 8px rgba(0, 0, 0, 0.2);
  letter-spacing: 3px;
}

.sub-title {
  font-size: 18px;
  color: #ffffff;
  margin: 0;
  font-weight: 500;
  letter-spacing: 1px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.4);
}

.cardsContainer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 50px;
  flex: 1;
  animation: fadeInUp 0.8s ease 0.2s both;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.card {
  width: 380px;
  height: 280px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  position: relative;
  overflow: hidden;
}

.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0) 100%
  );
  pointer-events: none;
}

.card.glimmerAction {
  background: linear-gradient(
    135deg,
    rgba(168, 237, 234, 0.95) 0%,
    rgba(255, 212, 226, 0.95) 100%
  );
}

.card.mindMirror {
  background: linear-gradient(
    135deg,
    rgba(254, 191, 212, 0.95) 0%,
    rgba(255, 218, 185, 0.95) 100%
  );
}

.card.heartVoice {
  background: linear-gradient(
    135deg,
    rgba(255, 222, 168, 0.95) 0%,
    rgba(255, 190, 149, 0.95) 100%
  );
}

.card:hover {
  transform: translateY(-10px) scale(1.02);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.2);
}

.icon-wrapper {
  margin-bottom: 20px;
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

.icon {
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  border: 3px solid rgba(255, 255, 255, 0.5);
  transition: all 0.3s ease;
}

.card:hover .icon {
  transform: scale(1.1);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
}

.icon svg {
  width: 40px;
  height: 40px;
  color: #667eea;
}

.cardContent {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  z-index: 1;
}

.cardContent h2 {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px 0;
  color: #333;
  letter-spacing: 1px;
}

.cardContent p {
  font-size: 14px;
  color: #666;
  margin: 0 0 12px 0;
  font-weight: 500;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.cardContent .description {
  font-size: 15px;
  color: #555;
  margin: 0;
  line-height: 1.6;
  font-weight: 400;
  text-transform: none;
  max-width: 280px;
}
</style>
