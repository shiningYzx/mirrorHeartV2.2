import request from '@/utils/request'

export const addTodoService = (title) => {
  return request({
    url: '/todo/add',
    method: 'POST',
    data: { title }
  })
}

export const getTodoListService = (status) => {
  return request({
    url: '/todo/list',
    method: 'GET',
    params: status !== undefined ? { status } : {}
  })
}

export const updateTodoService = (id, title, status) => {
  return request({
    url: `/todo/${id}`,
    method: 'PUT',
    data: { title, status }
  })
}

export const deleteTodoService = (id) => {
  return request({
    url: `/todo/${id}`,
    method: 'DELETE'
  })
}

export const getRecommendTodoService = (count = 3) => {
  return request({
    url: '/todo/recommend',
    method: 'GET',
    params: { count }
  })
}
