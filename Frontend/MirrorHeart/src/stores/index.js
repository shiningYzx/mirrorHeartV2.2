import { createPinia } from 'pinia'
import piniaPluginPersist from 'pinia-plugin-persist'

const pinia = createPinia()
pinia.use(piniaPluginPersist)
export default pinia

export * from './modules/user'
export * from './modules/recommendList'
