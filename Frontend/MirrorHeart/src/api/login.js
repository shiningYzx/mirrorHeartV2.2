import request from '@/utils/request'

export const userGetRegisterCodeService = ({ email }) =>
  request.post('/auth/send-code', { email, scene: 'REGISTER' })

export const userRegisterService = ({ email, password, code, nickname }) =>
  request.post('/auth/register', { email, password, code, nickname })

export const userCodeLoginService = ({ email, password }) =>
  request.post('/auth/login', { email, password })

export const userGetLoginCodeService = ({ email }) =>
  request.post('/auth/send-code', { email, scene: 'LOGIN' })

export const userMsgLoginService = ({ email, code }) =>
  request.post('/auth/login-by-code', { email, code })

export const userGetForgetPwdCodeService = ({ email }) =>
  request.post('/auth/send-code', { email, scene: 'RESET_PASSWORD' })

export const userResetPwdService = ({ email, newPassword, code }) =>
  request.post('/auth/reset-password', {
    email,
    newPassword,
    code
  })

export const userLogoutService = () => {
  return request.post('/auth/logout')
}

export const refreshTokenService = (refreshToken) => {
  return request.post('/auth/refresh', { refreshToken })
}
