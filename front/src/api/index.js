import axios from 'axios'

const api = axios.create({
  baseURL: '/api/ai',
  timeout: 30000
})

// 查询所有会话
export const queryAllSessions = async () => {
  const response = await api.get('/queryAllSessions')
  return response.data
}

// 查询历史消息
export const queryHistoryMessages = async (sessionId) => {
  const response = await api.get(`/queryHistoryMessages/${sessionId}`)
  return response.data
}

// 创建新会话
export const createSession = async (sessionData) => {
  if (sessionData.embeddingModelConfig != null) {
    if (!sessionData.embeddingModelConfig.apiKey || !sessionData.embeddingModelConfig.baseUrl) {
      sessionData.embeddingModelConfig = null
    }
  }
  const response = await api.post('/createSession', sessionData)
  return response.data
}

// 发送聊天消息（SSE）
// 使用 POST-GET 模式：
// 1. POST /chat - 创建连接并发送消息
// 2. GET /stream/{sessionId} - 通过 EventSource 接收流数据
export const chat = async (chatRequest) => {
  // POST 到 /chat 发送消息，后端会创建 SseEmitter
  await api.post('/chat', chatRequest)
  // 不返回任何数据
}






export default api
