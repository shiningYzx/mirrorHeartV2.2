import request from '@/utils/request'

export const getUserBasicInfoService = () => {
  return request.get('/user/me')
}

export const postUserBasicInfoService = (data) => {
  return request.put('/user/update', data)
}

export const postLogoutService = (email, code, reason) => {
  return request.post('/auth/cancel-account', {
    email,
    code,
    reason
  })
}

export const updateAvatarService = (key) => {
  return request.put('/user/avatar', { key })
}

export const updateBirthdayService = (birthday) => {
  return request.put('/user/birthday', { birthday })
}

export const uploadFileService = (file, scene) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('scene', scene)
  return request.post('/system/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const getUserPostsService = (userId, pageNo, pageSize) => {
  return request.get(`/post/user/${userId}`, {
    params: { pageNo, pageSize }
  })
}

export const getUserFavoritesService = (userId, pageNo, pageSize) => {
  return request.get(`/post/user/${userId}/favorites`, {
    params: { pageNo, pageSize }
  })
}

export const getPostHistoryService = (pageNo, pageSize, userId) => {
  return request.get('/post/history', {
    params: { pageNo, pageSize, userId }
  })
}

export const getUnreadNotificationCountService = () => {
  return request.get('/system/notification/unread-count')
}

export const getNotificationListService = (pageNo, pageSize) => {
  return request.get('/system/notification/list', {
    params: { pageNo, pageSize }
  })
}

export const submitReportService = (targetType, targetId, reason) => {
  return request.post('/system/report/submit', {
    targetType,
    targetId,
    reason
  })
}

export const getMyReportsService = (pageNo, pageSize) => {
  return request.get('/system/report/my-list', {
    params: { pageNo, pageSize }
  })
}
