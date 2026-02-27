<script lang="ts" setup>
import {
  getRecommendListService,
  getRecommendListServiceForVisitor,
  getDailyQuestionService,
  likeRecommendService
} from '@/api/homePage'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores'
import {
  ref,
  onBeforeMount,
  onMounted,
  onUnmounted,
  onActivated,
  onDeactivated,
  nextTick,
  watch
} from 'vue'
import { useRouter } from 'vue-router'
import recommendBox from '@/components/homePage/RecommendBox.vue'
import recommendBoxTop from '@/components/homePage/RecommendBoxTop.vue'
import contentDetailsBox from '@/components/homePage/ContentDetailsBox.vue'

defineOptions({
  name: 'HomePage'
})

const router = useRouter()
const userStore = useUserStore()

const token = userStore.basicUser.token

//查询的日期
const date = ref('')

//设置lec文件
const lrcDoc = ref()

//设置歌词
const lyrics = ref([])
//解析歌词
function parseLrc(lrcContent) {
  const regex = /^\[(\d{2}:\d{2})\]\s*(.*)$/gm
  const lyricsArray = []

  let match
  while ((match = regex.exec(lrcContent)) !== null) {
    const timestamp = match[1]
    const text = match[2].trim()

    const minutes = parseInt(timestamp.split(':')[0], 10)
    const seconds = parseInt(timestamp.split(':')[1], 10)
    const time = minutes * 60 + seconds

    lyricsArray.push({ time, text })
  }

  return lyricsArray
}

//设置歌词索引
const lyricIndex = ref(0)
const lyricsContainerRef = ref(null) // 获取歌词容器的引用
const isPageActive = ref(true) // 记录当前页面是否处于前台

//记录唱片的旋转角度
const rotationAngle = ref(0)

// 获取音乐播放器的播放按钮
const audioRef = ref(null)
const audioWrapperRef = ref(null) // 🌟 新增：音频包裹容器，用于实现DOM漂移

//记录音乐播放器的定时器
const musicTimer = ref(null)

//获取音乐唱片容器
const musicImg = ref(null)

//创建一个储存每日推荐的返回值的对象
const recommendList = ref(null)

//创建一个储存每日一问的返回值的对象
const dailyQuestion = ref(null)

//创建一个储存每日一问点赞状态的变量
const dailyQuestionLiked = ref(false)

//从items数组中获取各类型的推荐
const getRecommendByType = (type) => {
  if (!recommendList.value || !recommendList.value.items) {
    return null
  }
  return recommendList.value.items.find((item) => item.type === type)
}

// 创建一个新的 Date 对象
const currentDate = new Date()

// 获取当前日
const day = currentDate.getDate()

// 获取当前年份的英文表示
const year = currentDate.toLocaleString('en-US', { year: 'numeric' })

// 获取当前月份的英文表示
const month = currentDate.toLocaleString('en-US', { month: 'long' })

// 获取当前月份（注意：月份从 0 开始，所以需要加 1）
const nowMonth = currentDate.getMonth() + 1

// 获取今日格式化日期，用于向后端请求数据
const nowDate = `${year}-${nowMonth.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`

const articleContent = ref('') // 定义一个 ref 来存储文章内容
const parsedContent = ref('') // 定义一个 ref 来存储解析后的文章内容

//一进页面就获取当天的推荐
const getRecommendList = async () => {
  try {
    if (!userStore.basicUser.token) {
      const res = await getRecommendListServiceForVisitor()
      console.log('访客推荐数据:', res.data.data)
      recommendList.value = res.data.data
      if (recommendList.value && recommendList.value.items) {
        const musicItem = getRecommendByType('MUSIC')
        const articleItem = getRecommendByType('ARTICLE')
        if (musicItem) {
          lrcDoc.value = musicItem.specificData?.lyric || ''
          lyrics.value = parseLrc(lrcDoc.value)
          clearInterval(musicTimer.value)
          musicTimer.value = null
          rotationAngle.value = 0
          if (musicImg.value) {
            musicImg.value.style.transform = `rotate(${rotationAngle.value}deg)`
          }
        }
        if (articleItem) {
          articleContent.value = articleItem.summary || ''
          updateParsedContent()
        }
      }
    } else {
      const res = await getRecommendListService(nowDate)
      console.log('登录用户推荐数据:', res.data.data)
      recommendList.value = res.data.data
      date.value = nowDate
      if (recommendList.value && recommendList.value.items) {
        const musicItem = getRecommendByType('MUSIC')
        const articleItem = getRecommendByType('ARTICLE')
        if (musicItem) {
          lrcDoc.value = musicItem.specificData?.lyric || ''
          lyrics.value = parseLrc(lrcDoc.value)
          clearInterval(musicTimer.value)
          musicTimer.value = null
          rotationAngle.value = 0
          if (musicImg.value) {
            musicImg.value.style.transform = `rotate(${rotationAngle.value}deg)`
          }
        }
        if (articleItem) {
          articleContent.value = articleItem.summary || ''
          updateParsedContent()
        }
      }
    }
    const questionRes = await getDailyQuestionService()
    console.log('每日一问数据:', questionRes.data.data)
    dailyQuestion.value = questionRes.data.data
  } catch (error) {
    console.error('获取推荐数据失败:', error)
    ElMessage.error('获取推荐数据失败，请稍后重试')
  }
}

onBeforeMount(() => {
  getRecommendList()
})

// 设置解析文章内容的函数
const parseContent = (content) => {
  return content
    .split('\n')
    .map((item) => {
      return `<p>${item.trim()}</p>`
    })
    .join('')
}

const updateParsedContent = () => {
  parsedContent.value = parseContent(articleContent.value)
}

const disabledDate = (time) => {
  return time.getTime() > Date.now()
}

const searchRecommendList = async () => {
  if (!userStore.basicUser.token) {
    ElMessage.error('登录后才能查看历史推荐哦！')
    userStore.setIsLogin()
    return
  } else if (date.value === '') {
    ElMessage.error('请先选择日期再查看哦！')
    return
  } else {
    try {
      const res = await getRecommendListService(date.value)
      console.log('历史推荐数据:', res.data.data)
      recommendList.value = res.data.data
      ElMessage.success('成功获取历史每日推荐')
      if (recommendList.value && recommendList.value.items) {
        const musicItem = getRecommendByType('MUSIC')
        const articleItem = getRecommendByType('ARTICLE')
        if (musicItem) {
          lrcDoc.value = musicItem.specificData?.lyric || ''
          lyrics.value = parseLrc(lrcDoc.value)
          clearInterval(musicTimer.value)
          musicTimer.value = null
          rotationAngle.value = 0
          if (musicImg.value) {
            musicImg.value.style.transform = `rotate(${rotationAngle.value}deg)`
          }
        }
        if (articleItem) {
          articleContent.value = articleItem.summary || ''
          updateParsedContent()
        }
      }
    } catch (error) {
      console.error('获取历史推荐失败:', error)
      ElMessage.error('获取历史推荐失败，请稍后重试')
    }
  }
}

// 处理播放事件 (提升转动流畅度)
const handlePlay = () => {
  if (!musicTimer.value) {
    musicTimer.value = setInterval(() => {
      rotationAngle.value += 1
      if (musicImg.value) {
        musicImg.value.style.transform = `rotate(${rotationAngle.value}deg)`
      }
    }, 50)
  }
}

// 处理暂停事件
const handlePause = () => {
  clearInterval(musicTimer.value)
  musicTimer.value = null
}

// 监听 recommendList 的变化绑定事件
watch(recommendList, (newValue) => {
  if (newValue) {
    nextTick(() => {
      if (
        audioRef.value &&
        typeof audioRef.value.addEventListener === 'function'
      ) {
        // 先移除旧事件防止重复触发
        audioRef.value.removeEventListener('play', handlePlay)
        audioRef.value.removeEventListener('pause', handlePause)
        audioRef.value.addEventListener('play', handlePlay)
        audioRef.value.addEventListener('pause', handlePause)
      }
    })
  }
})

// 组件挂载时添加事件监听器
onMounted(() => {
  if (recommendList.value) {
    nextTick(() => {
      if (
        audioRef.value &&
        typeof audioRef.value.addEventListener === 'function'
      ) {
        audioRef.value.addEventListener('play', handlePlay)
        audioRef.value.addEventListener('pause', handlePause)
      }
    })
  }
})

// 🌟 自定义歌词滚动算法：将高光歌词精准固定在靠上方位置
const scrollToActiveLyric = () => {
  // 如果页面不在前台展示，停止 DOM 滚动，防止影响性能或报错
  if (!isPageActive.value || !lyricsContainerRef.value) return

  const activeLyric = lyricsContainerRef.value.querySelector('.active')
  if (activeLyric) {
    // 目标高度：减去固定值 60，让高光歌词锁定在容器偏上方的位置
    const targetScrollTop = activeLyric.offsetTop - 60

    lyricsContainerRef.value.scrollTo({
      top: targetScrollTop,
      behavior: 'smooth'
    })
  }
}

// 组件激活时（从keep-alive缓存中恢复）重新绑定DOM及动画
onActivated(() => {
  isPageActive.value = true // 标记页面已进入前台

  nextTick(() => {
    // 🌟 DOM漂移恢复：如果 audio 之前在后台被移走了，现在把它移回控制面板
    if (
      audioRef.value &&
      audioWrapperRef.value &&
      audioRef.value.parentNode !== audioWrapperRef.value
    ) {
      audioWrapperRef.value.appendChild(audioRef.value)
      // 恢复正常的显示样式
      audioRef.value.style.position = 'static'
      audioRef.value.style.visibility = 'visible'
    }

    if (recommendList.value && audioRef.value) {
      // 确立事件监听器
      audioRef.value.removeEventListener('play', handlePlay)
      audioRef.value.removeEventListener('pause', handlePause)
      audioRef.value.addEventListener('play', handlePlay)
      audioRef.value.addEventListener('pause', handlePause)

      // 恢复唱片旋转角度并检查是否需要继续转动
      if (musicImg.value) {
        musicImg.value.style.transform = `rotate(${rotationAngle.value}deg)`
      }
      if (!audioRef.value.paused) {
        handlePlay()
      }
    }
    // 切回页面时，立即同步一次歌词位置
    scrollToActiveLyric()
  })
})

// 组件停用时（切换到其他页面）暂停动画但【突破浏览器限制保持音乐播放】
onDeactivated(() => {
  isPageActive.value = false // 标记页面已切入后台

  if (audioRef.value) {
    // 🌟 核心：如果音乐正在播放，为了防止 keep-alive 剥离 DOM 导致浏览器强行暂停
    // 我们将 audio 节点偷偷移到 document.body 里面保护起来！
    if (!audioRef.value.paused) {
      document.body.appendChild(audioRef.value)
      // 使用绝对定位隐藏它，防止破坏其他页面布局 (不能用 display:none)
      audioRef.value.style.position = 'absolute'
      audioRef.value.style.top = '-9999px'
      audioRef.value.style.visibility = 'hidden'
    }

    // 在后台时为了节省性能，停止转动唱片
    if (musicTimer.value) {
      clearInterval(musicTimer.value)
      musicTimer.value = null
    }
  }
})

// 组件卸载时彻底清理
onUnmounted(() => {
  isPageActive.value = false
  if (
    audioRef.value &&
    typeof audioRef.value.removeEventListener === 'function'
  ) {
    audioRef.value.removeEventListener('play', handlePlay)
    audioRef.value.removeEventListener('pause', handlePause)
    // 确保清理残留在 body 中的音频节点
    if (audioRef.value.parentNode === document.body) {
      document.body.removeChild(audioRef.value)
    }
  }
  if (musicTimer.value) {
    clearInterval(musicTimer.value)
    musicTimer.value = null
  }
})

//二分法查找歌词
const binarySearchClosest = (arr, target) => {
  let left = 0
  let right = arr.length - 1
  let closestTime = null

  if (arr.length > 0 && arr[0].time === 0 && target >= 0) {
    closestTime = 0
  }

  while (left <= right) {
    const mid = Math.floor((left + right) / 2)
    if (arr[mid].time === target) {
      return arr[mid].time
    } else if (arr[mid].time < target) {
      if (closestTime === null || arr[mid].time > closestTime) {
        closestTime = arr[mid].time
      }
      left = mid + 1
    } else {
      if (mid > 0 && arr[mid - 1].time <= target) {
        closestTime = arr[mid - 1].time
      }
      right = mid - 1
    }
  }
  return closestTime
}

//获取歌词并更新 index
const handleTimeUpdate = (event) => {
  const currentTime = event.target.currentTime
  const closestTime = binarySearchClosest(lyrics.value, currentTime)
  const currentLyric = lyrics.value.find((item) => item.time === closestTime)

  if (currentLyric) {
    lyricIndex.value = lyrics.value.indexOf(currentLyric)
  }
}

// 监听歌词变化，并触发精确定位
watch(lyricIndex, (newValue) => {
  if (newValue !== null) {
    nextTick(() => {
      scrollToActiveLyric()
    })
  }
})

const message = ref({}) //设置抽屉的内容
const DetailsBox = ref() //获取抽屉组件
const handleContextDetails = (type) => {
  if (type === 1) {
    if (dailyQuestion.value) {
      router.push({
        path: '/daily-question',
        query: { data: JSON.stringify(dailyQuestion.value) }
      })
    }
  } else {
    const typeMap = {
      2: 'ARTICLE',
      3: 'BOOK',
      4: 'MOVIE',
      5: 'PAINTING'
    }
    const item = getRecommendByType(typeMap[type])
    if (item) {
      message.value = item
      DetailsBox.value.openDrawer()
    }
  }
}

const handleLike = async (type, event) => {
  event.stopPropagation()
  if (!token) {
    ElMessage.error('请先登录')
    userStore.setIsLogin()
    return
  }
  if (type === 'QUESTION') {
    try {
      await likeRecommendService(dailyQuestion.value?.recordId, 'DAILY_ANSWER')
      dailyQuestionLiked.value = !dailyQuestionLiked.value
      ElMessage.success(dailyQuestionLiked.value ? '点赞成功' : '取消点赞成功')
    } catch (error) {
      ElMessage.error('操作失败，请稍后重试')
    }
  } else {
    const typeMap = {
      QUOTE: 'QUOTE',
      MUSIC: 'MUSIC',
      ARTICLE: 'ARTICLE',
      BOOK: 'BOOK',
      MOVIE: 'MOVIE',
      PAINTING: 'PAINTING'
    }
    const item = getRecommendByType(typeMap[type])
    if (item) {
      try {
        await likeRecommendService(item.id, 'CONTENT')
        item.isLiked = !item.isLiked
        ElMessage.success(item.isLiked ? '点赞成功' : '取消点赞成功')
      } catch (error) {
        ElMessage.error('操作失败，请稍后重试')
      }
    }
  }
}

const isLiked = (type) => {
  if (type === 'QUESTION') {
    return dailyQuestionLiked.value
  }
  const typeMap = {
    QUOTE: 'QUOTE',
    MUSIC: 'MUSIC',
    ARTICLE: 'ARTICLE',
    BOOK: 'BOOK',
    MOVIE: 'MOVIE',
    PAINTING: 'PAINTING'
  }
  const item = getRecommendByType(typeMap[type])
  return item?.isLiked || false
}
</script>

<template>
  <div class="container">
    <!-- Bento 顶部区域 -->
    <div class="bento-grid-top">
      <!-- 模块1：日期与历史搜索 -->
      <div class="bento-card date-search-box">
        <div class="date-display">
          <div class="dayBox">{{ day }}</div>
          <div class="yearBox">
            <span class="month-text">{{ month }}</span>
            <span class="year-text">{{ year }}</span>
          </div>
        </div>
        <div class="search-history">
          <span class="search-title">漫游历史推荐</span>
          <el-date-picker
            v-model="date"
            type="date"
            placeholder="选择日期"
            :disabled-date="disabledDate"
            size="large"
            value-format="YYYY-MM-DD"
            style="width: 100%; margin: 10px 0"
          />
          <el-button
            type="primary"
            size="large"
            class="search-btn"
            @click="searchRecommendList"
            >时光穿梭</el-button
          >
        </div>
      </div>

      <!-- 模块2：每日一问与金句 -->
      <div class="bento-column">
        <!-- 每日一问 -->
        <el-tooltip
          effect="dark"
          content="点击可查看每日一问详情哦！"
          placement="top"
        >
          <div
            class="bento-card questionBox"
            v-if="dailyQuestion"
            @click="handleContextDetails(1)"
          >
            <div class="question-icon">💡</div>
            <div class="questionText">
              {{ dailyQuestion.text || '' }}
            </div>
            <div
              class="like-button"
              @click.stop="handleLike('QUESTION', $event)"
            >
              {{ isLiked('QUESTION') ? '❤️' : '🤍' }}
            </div>
          </div>
        </el-tooltip>

        <!-- 句 -->
        <div class="bento-card sentenceBox">
          <recommendBoxTop>
            <template #title><p class="card-title">句</p></template>
            <template #like>
              <span class="like-button" @click="handleLike('QUOTE', $event)">
                {{ isLiked('QUOTE') ? '❤️' : '🤍' }}
              </span>
            </template>
          </recommendBoxTop>
          <div class="sentenceBoxBottom">
            <div class="quote-marks">"</div>
            <div v-if="getRecommendByType('QUOTE')" class="sentence-content">
              {{ getRecommendByType('QUOTE')?.title || '' }}
            </div>
            <div v-if="getRecommendByType('QUOTE')" class="quote-author">
              — {{ getRecommendByType('QUOTE')?.source || '' }}
            </div>
          </div>
        </div>
      </div>

      <!-- 模块3：乐 - 留声机 -->
      <div class="bento-card music-player-card">
        <recommendBoxTop class="music-header">
          <template #title><p class="card-title">乐</p></template>
          <template #like>
            <span class="like-button" @click="handleLike('MUSIC', $event)">
              {{ isLiked('MUSIC') ? '❤️' : '🤍' }}
            </span>
          </template>
        </recommendBoxTop>

        <div class="music-body">
          <div class="vinyl-container">
            <div class="vinyl-record" ref="musicImg">
              <img
                class="vinyl-cover"
                v-if="getRecommendByType('MUSIC')"
                :src="getRecommendByType('MUSIC')?.coverUrl || ''"
                alt="album cover"
              />
              <div class="vinyl-center"></div>
            </div>
          </div>

          <div class="music-controls">
            <div class="music-info">
              <div class="music-title" v-if="getRecommendByType('MUSIC')">
                {{ getRecommendByType('MUSIC')?.title || '' }}
              </div>
              <div class="music-artist" v-if="getRecommendByType('MUSIC')">
                {{ getRecommendByType('MUSIC')?.source || '' }}
              </div>
            </div>

            <!-- 🌟 歌词展示区域 -->
            <div class="lyrics-container" ref="lyricsContainerRef">
              <p
                v-for="(lyric, index) in lyrics"
                :key="index"
                :class="{ active: index === lyricIndex }"
              >
                {{ lyric.text }}
              </p>
            </div>

            <!-- 🌟 音频容器：增加 ref 解决 DOM 漂移 -->
            <div class="audio-wrapper" ref="audioWrapperRef">
              <!-- 将 v-if 修改为 v-show，保证 DOM 实例的连贯性 -->
              <audio
                ref="audioRef"
                class="custom-audio"
                controls
                v-show="getRecommendByType('MUSIC')"
                :src="getRecommendByType('MUSIC')?.specificData?.audioUrl || ''"
                @timeupdate="handleTimeUpdate"
              ></audio>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Bento 底部区域 -->
    <div class="bento-grid-bottom">
      <!-- 模块4：文 -->
      <el-tooltip
        effect="dark"
        content="点击可查看文章详情哦！"
        placement="top"
      >
        <div class="bento-card articleBox" @click="handleContextDetails(2)">
          <recommendBoxTop>
            <template #title><p class="card-title">文</p></template>
            <template #like>
              <span
                class="like-button"
                @click.stop="handleLike('ARTICLE', $event)"
              >
                {{ isLiked('ARTICLE') ? '❤️' : '🤍' }}
              </span>
            </template>
          </recommendBoxTop>
          <div class="articleBoxBottom">
            <h3 class="article-title" v-if="getRecommendByType('ARTICLE')">
              {{ getRecommendByType('ARTICLE')?.title || '' }}
            </h3>
            <div
              class="article-content"
              v-if="getRecommendByType('ARTICLE')"
              v-html="parsedContent"
            ></div>
            <div v-if="getRecommendByType('ARTICLE')" class="article-author">
              — {{ getRecommendByType('ARTICLE')?.source || '' }}
            </div>
          </div>
        </div>
      </el-tooltip>

      <!-- 模块5：书 -->
      <el-tooltip
        effect="dark"
        content="点击可查看书籍详情哦！"
        placement="top"
      >
        <recommendBox @click="handleContextDetails(3)">
          <template #title><p class="card-title">书</p></template>
          <template #like>
            <span class="like-button" @click.stop="handleLike('BOOK', $event)">
              {{ isLiked('BOOK') ? '❤️' : '🤍' }}
            </span>
          </template>
          <template #body>
            <img
              class="media-cover"
              v-if="getRecommendByType('BOOK')"
              :src="getRecommendByType('BOOK')?.coverUrl || ''"
            />
            <div class="media-title" v-if="getRecommendByType('BOOK')">
              《{{ getRecommendByType('BOOK')?.title || '' }}》
            </div>
            <div class="media-author" v-if="getRecommendByType('BOOK')">
              {{ getRecommendByType('BOOK')?.source || '' }}
            </div>
          </template>
        </recommendBox>
      </el-tooltip>

      <!-- 模块6：影 -->
      <el-tooltip
        effect="dark"
        content="点击可查看电影详情哦！"
        placement="top"
      >
        <recommendBox @click="handleContextDetails(4)">
          <template #title><p class="card-title">影</p></template>
          <template #like>
            <span class="like-button" @click.stop="handleLike('MOVIE', $event)">
              {{ isLiked('MOVIE') ? '❤️' : '🤍' }}
            </span>
          </template>
          <template #body>
            <img
              class="media-cover"
              v-if="getRecommendByType('MOVIE')"
              :src="getRecommendByType('MOVIE')?.coverUrl || ''"
            />
            <div class="media-title" v-if="getRecommendByType('MOVIE')">
              《{{ getRecommendByType('MOVIE')?.title || '' }}》
            </div>
          </template>
        </recommendBox>
      </el-tooltip>

      <!-- 模块7：画 -->
      <el-tooltip content="点击可欣赏放大画像哦！" placement="top">
        <recommendBox @click="handleContextDetails(5)">
          <template #title><p class="card-title">画</p></template>
          <template #like>
            <span
              class="like-button"
              @click.stop="handleLike('PAINTING', $event)"
            >
              {{ isLiked('PAINTING') ? '❤️' : '🤍' }}
            </span>
          </template>
          <template #body>
            <img
              class="media-cover"
              v-if="getRecommendByType('PAINTING')"
              :src="getRecommendByType('PAINTING')?.coverUrl || ''"
            />
            <div class="media-title" v-if="getRecommendByType('PAINTING')">
              《{{ getRecommendByType('PAINTING')?.title || '' }}》
            </div>
          </template>
        </recommendBox>
      </el-tooltip>
    </div>

    <contentDetailsBox :message="message" ref="DetailsBox"></contentDetailsBox>
  </div>
</template>

<style scoped>
/* 全局容器背景 */
.container {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  padding: 30px 40px;
  background: linear-gradient(135deg, #e0f7fa 0%, #f9fbe7 100%);
  display: flex;
  flex-direction: column;
  gap: 24px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
    'Helvetica Neue', Arial, sans-serif;
  overflow-y: auto;
  overflow-x: hidden;
}

/* --- Bento Grid 系统 --- */
.bento-grid-top {
  display: grid;
  grid-template-columns: 5fr 9fr 10fr;
  gap: 20px;
  min-height: 310px; /* 紧凑的高度 */
  flex-shrink: 0;
}

.bento-grid-bottom {
  display: grid;
  grid-template-columns: 8fr 5fr 5fr 5fr;
  gap: 15px;
  min-height: 450px;
  padding-bottom: 20px;
}

.bento-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
}

/* --- 通用 Bento 卡片基础样式 (无悬浮状态) --- */
.bento-card {
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.04);
  padding: 0px 20px 20px 20px;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.card-title {
  font-size: 24px;
  font-weight: 800;
  color: #2c3e50;
  margin: 0;
}

/* 点赞按钮 */
.like-button {
  cursor: pointer;
  font-size: 20px;
  transition:
    transform 0.2s,
    background-color 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.like-button:hover {
  transform: scale(1.15);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
.like-button:active {
  transform: scale(0.95);
}

/* ================== 交互区域优化 ================== */

/* 1. 时光穿梭按钮 */
.search-btn {
  width: 100%;
  font-weight: bold;
  border-radius: 8px;
  background: linear-gradient(135deg, #50d287 0%, #48c9b0 100%);
  border: none;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.search-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(80, 210, 135, 0.4);
  background: linear-gradient(135deg, #5ee097 0%, #56d8be 100%);
}

.search-btn:active {
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(80, 210, 135, 0.3);
}

/* 2. 每日一问专属悬浮效果 */
.questionBox {
  flex-direction: row;
  align-items: center;
  gap: 15px;
  background: linear-gradient(
    135deg,
    rgba(255, 205, 160, 0.9) 0%,
    rgba(255, 182, 155, 0.9) 100%
  );
  padding: 16px 20px;
  cursor: pointer;
  flex: none;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  position: relative;
  overflow: hidden;
  border-radius: 32px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.questionBox::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 100%;
  height: 100%;
  background: radial-gradient(
    circle,
    rgba(255, 255, 255, 0.3) 0%,
    transparent 70%
  );
  pointer-events: none;
}

.questionBox:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.08);
  /* 悬浮时变得更加柔和、明亮透气 */
  background: linear-gradient(
    135deg,
    rgba(255, 208, 164, 0.95) 0%,
    rgba(255, 159, 121, 0.95) 100%
  );
}

/* 3. 文 - 卡片专属悬浮效果 */
.articleBox {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.articleBox:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.08);
  /* 悬浮时增加一层极简高级的淡蓝柔光背景 */
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.95),
    rgba(240, 248, 255, 0.95)
  );
}

/* 4. 书、影、画 (RecommendBox) 深度覆盖悬浮效果 */
:deep(.recommendBox) {
  border-radius: 20px !important;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.04) !important;
  border: 1px solid rgba(255, 255, 255, 0.8) !important;
  background: rgba(255, 255, 255, 0.65) !important;
  backdrop-filter: blur(16px) !important;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1) !important;
  height: 100% !important;
  max-height: none !important;
  min-height: 380px !important;
}

:deep(.recommendBox:hover) {
  transform: translateY(-6px) !important;
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.08) !important;
  /* 悬浮时和 "文" 保持一致的高级柔光背景 */
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.95),
    rgba(240, 248, 255, 0.95)
  ) !important;
}

:deep(.recommendBox:hover) .media-cover {
  transform: scale(1.05) translateY(-5px);
}

/* ================== 静态展示区域 ================== */

/* --- 模块1：日期与搜索 --- */
.date-search-box {
  justify-content: space-between;
  padding: 24px;
}
.date-display {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  gap: 12px;
  margin-bottom: 12px;
}
.dayBox {
  font-size: 80px;
  font-weight: 900;
  line-height: 0.9;
  background: linear-gradient(135deg, #2c3e50, #3498db);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.yearBox {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-top: 5px;
}
.month-text {
  font-size: 20px;
  font-weight: 700;
  color: #34495e;
  text-transform: uppercase;
}
.year-text {
  font-size: 16px;
  color: #7f8c8d;
}
.search-history {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: rgba(255, 255, 255, 0.5);
  padding: 16px;
  border-radius: 16px;
}
.search-title {
  font-size: 14px;
  font-weight: 600;
  color: #555;
}

/* --- 每日一问的文案 --- */
.question-icon {
  font-size: 24px;
}
.questionText {
  flex: 1;
  font-size: 16px;
  font-weight: 600;
  color: #8c5a45;
  line-height: 1.5;
}

/* --- 模块2：句 --- */
.sentenceBox {
  flex: 1;
}
.sentenceBoxBottom {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}
.quote-marks {
  position: absolute;
  top: -15px;
  left: 5px;
  font-size: 100px;
  color: rgba(64, 158, 255, 0.1);
  font-family: serif;
  line-height: 1;
}
.sentence-content {
  font-size: 18px;
  line-height: 1.8;
  color: #34495e;
  text-align: justify;
  font-family: 'Songti SC', 'SimSun', serif;
  z-index: 1;
}

.quote-author {
  font-size: 14px;
  color: #7f8c8d;
  text-align: right;
  margin-top: 10px;
  font-style: italic;
  font-family: 'Songti SC', 'SimSun', serif;
}

/* --- 模块3：乐 (居中同心圆光盘设计) --- */
.music-player-card {
  padding: 0;
}
.music-header {
  padding: 20px 20px 0 20px;
}
.music-body {
  display: flex;
  flex: 1;
  align-items: center;
  padding: 0 16px 16px 16px;
  gap: 20px;
}

/* 外层固定尺寸容器 */
.vinyl-container {
  flex-shrink: 0;
  width: 150px;
  height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 光盘本体 */
.vinyl-record {
  position: relative;
  width: 100%;
  height: 100%;
  background: #111;
  border-radius: 50%;
  box-shadow:
    0 8px 16px rgba(0, 0, 0, 0.3),
    inset 0 0 12px rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  will-change: transform;
}

/* 黑胶的细腻纹理边缘 */
.vinyl-record::before {
  content: '';
  position: absolute;
  width: 88%;
  height: 88%;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.05);
  pointer-events: none;
}

/* 封面贴纸 */
.vinyl-cover {
  width: 75%;
  height: 75%;
  border-radius: 50%;
  object-fit: cover;
  z-index: 1;
}

/* 核心圆孔 */
.vinyl-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 28px;
  height: 28px;
  background: #fdfae8;
  border-radius: 50%;
  border: 2px solid #222;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
  z-index: 2;
}

.music-controls {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  height: 100%;
}

.music-info {
  text-align: center;
  margin-bottom: 10px;
}

.music-title {
  font-size: 16px;
  font-weight: 600;
  color: #34495e;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.music-artist {
  font-size: 14px;
  color: #7f8c8d;
  font-style: italic;
}

/* 🌟 优化歌词滚动区域 */
.lyrics-container {
  flex: 1;
  max-height: 120px;
  overflow-y: auto;
  padding-right: 10px;
  padding-top: 40px;
  padding-bottom: 40px;
  position: relative;
  mask-image: linear-gradient(
    to bottom,
    transparent,
    black 15%,
    black 85%,
    transparent
  );
  -webkit-mask-image: linear-gradient(
    to bottom,
    transparent,
    black 15%,
    black 85%,
    transparent
  );
}
.lyrics-container::-webkit-scrollbar {
  display: none;
}
.lyrics-container p {
  font-size: 14px;
  color: #7f8c8d;
  margin: 12px 0;
  transition: all 0.3s;
  text-align: center;
}
.lyrics-container p.active {
  font-size: 17px;
  color: #409eff;
  font-weight: bold;
  transform: scale(1.05);
}

.audio-wrapper {
  width: 100%;
}

.custom-audio {
  width: 100%;
  margin-top: 10px;
  height: 36px;
}

/* --- 模块4：文 --- */
.articleBoxBottom {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
  padding-top: 20px;
}
.article-title {
  font-size: 22px;
  font-weight: 800;
  color: #2c3e50;
  margin: 20px 0 0 0;
  flex-shrink: 0;
}
.article-content {
  font-size: 16px;
  color: #555;
  line-height: 1.8;
  font-family: 'Songti SC', 'SimSun', serif;
  text-align: justify;
  margin-top: auto;
  margin-bottom: auto;
  max-height: 100%;
  overflow: hidden;
}

.article-author {
  font-size: 14px;
  color: #7f8c8d;
  text-align: right;
  margin-top: 10px;
  font-style: italic;
  font-family: 'Songti SC', 'SimSun', serif;
}

/* --- 媒体卡片 (书、影、画) 内容样式 --- */
.media-cover {
  width: 70%;
  aspect-ratio: 2 / 3;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.15);
  transition: transform 0.4s ease;
  margin-bottom: 12px;
  margin-top: 20px;
  max-height: 250px;
}
.media-title {
  font-size: 16px;
  font-weight: 600;
  color: #34495e;
  text-align: center;
  width: 100%;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  text-overflow: ellipsis;
}

.media-author {
  font-size: 14px;
  color: #7f8c8d;
  text-align: center;
  margin-top: 8px;
  font-style: italic;
  font-family: 'Songti SC', 'SimSun', serif;
}
</style>
