import request from '@/utils/request'

export const getSessionListService = () => {
  return request({
    url: '/ai/session/list',
    method: 'GET'
  })
}

export const getMessageHistoryService = (sessionId) => {
  return request({
    url: '/ai/message/list',
    method: 'GET',
    params: { sessionId }
  })
}

export const sendStreamMessageService = (data) => {
  return request({
    url: '/ai/chat/stream',
    method: 'POST',
    data,
    responseType: 'stream'
  })
}

export const deleteSessionService = (sessionId) => {
  return request({
    url: `/ai/session/${sessionId}`,
    method: 'DELETE'
  })
}
