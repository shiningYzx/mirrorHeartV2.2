import request from '@/utils/request'

export const getRecommendListService = (selectedDay) => {
  return request.get('/recommend/date', {
    params: {
      date: selectedDay
    }
  })
}

export const getRecommendListServiceForVisitor = () => {
  return request.get('/recommend/today')
}

export const getDailyQuestionService = () => {
  return request.get('/question/today')
}

export const submitDailyQuestionService = (
  questionId,
  text,
  audioUrl,
  durationMs,
  visibility
) => {
  return request.post('/question/answer', {
    questionId,
    text,
    audioUrl,
    durationMs,
    visibility
  })
}

export const likeRecommendService = (targetId, targetType) => {
  return request.post('/interaction/like', {
    targetId,
    targetType
  })
}

export const getRecommendHistoryService = (pageNo, pageSize) => {
  return request.get('/recommend/history', {
    params: { pageNo, pageSize }
  })
}

export const getTagListService = (group) => {
  return request.get('/tag/list', {
    params: { group }
  })
}

export const addTagService = (name, groupName) => {
  return request.post('/tag/add', { name, groupName })
}

export const getContentListService = (pageNo, pageSize, type) => {
  return request.get('/content/list', {
    params: { pageNo, pageSize, type }
  })
}

export const getContentDetailService = (id) => {
  return request.get(`/content/${id}`)
}

export const addQuestionService = (text, topic) => {
  return request.post('/question/add', { text, topic })
}

export const getQuestionAnswersService = (questionId, pageNo, pageSize) => {
  return request.get(`/question/userDailyRecord/${questionId}/answers`, {
    params: { pageNo, pageSize }
  })
}
