<template>
  <div class="app">
    <!-- 左侧边栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <button class="new-chat-btn" @click="showNewChatModal = true">
          + 新建聊天
        </button>
      </div>
      <div class="sidebar-content">
        <div v-if="sessions.length === 0" class="empty-state">
          暂无聊天记录
        </div>
        <div v-else class="chat-list">
          <div
            v-for="session in sessions"
            :key="session.id"
            :class="['chat-item', { active: currentSessionId === session.id }]"
            @click="selectSession(session.id)"
          >
            <div class="chat-item-title">
              {{ session.name || '未命名会话' }}
            </div>
            <div class="chat-item-time">
              {{ formatTime(session.createTime) }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧主内容区 -->
    <div class="main-content">
      <div v-if="!currentSessionId" class="welcome-screen">
        <h1>欢迎使用 AI Agent</h1>
        <p>开始一个新的对话或选择历史对话</p>
      </div>

      <div v-else class="chat-container">
        <!-- 聊天消息区域 -->
        <div class="messages" ref="messagesContainer">
          <div
            v-for="message in currentMessages"
            :key="message.id"
            :class="['message', message.role]"
          >
            <div class="message-avatar">
              {{ message.role === 'user' ? 'U' : 'AI' }}
            </div>
            <div class="message-content">
              <div class="message-text">{{ message.content }}</div>
              <div class="message-time">{{ formatTime(message.createTime) }}</div>
            </div>
          </div>

          <!-- 加载指示器 -->
          <div v-if="isTyping" class="message assistant">
            <div class="message-avatar">AI</div>
            <div class="message-content">
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area">
          <div class="input-container">
            <textarea
              v-model="inputMessage"
              @keydown="handleKeyDown"
              placeholder="输入消息..."
              rows="1"
              ref="messageInput"
            ></textarea>
            <button
              class="send-btn"
              @click="sendMessage"
              :disabled="!inputMessage.trim() || isTyping"
            >
              发送
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 新建聊天模态框 -->
    <NewChatModal
      v-if="showNewChatModal"
      @close="showNewChatModal = false"
      @create="handleSessionCreated"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import EventSource from 'eventsource-polyfill'
import { queryAllSessions, queryHistoryMessages, createSession, chat } from './api'
import NewChatModal from './components/NewChatModal.vue'

// 响应式数据
const sessions = ref([])
const currentSessionId = ref(null)
const currentMessages = ref([])
const inputMessage = ref('')
const isTyping = ref(false)
const showNewChatModal = ref(false)

// DOM 引用
const messagesContainer = ref(null)
const messageInput = ref(null)

// 加载会话列表
const loadSessions = async () => {
  try {
    sessions.value = await queryAllSessions()
  } catch (error) {
    console.error('加载会话列表失败:', error)
  }
}

// 加载历史消息
const loadHistoryMessages = async (sessionId) => {
  try {
    currentMessages.value = await queryHistoryMessages(sessionId)
    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('加载历史消息失败:', error)
  }
}

// 选择会话
const selectSession = (sessionId) => {
  currentSessionId.value = sessionId
  loadHistoryMessages(sessionId)
}

// 创建新会话
const handleSessionCreated = async (sessionData) => {
  try {
    showNewChatModal.value = false

    // 调用后端API创建会话
    const newSession = await createSession(sessionData)

    // 重新加载会话列表
    await loadSessions()

    // 设置当前会话
    currentSessionId.value = newSession.id
    currentMessages.value = []

    // 显示欢迎消息
    if (newSession.welcomeMessage) {
      currentMessages.value.push({
        id: Date.now(),
        role: 'assistant',
        content: newSession.welcomeMessage,
        createTime: new Date().toISOString()
      })
    }

    await nextTick()
    scrollToBottom()
    messageInput.value?.focus()
  } catch (error) {
    console.error('创建会话失败:', error)
    alert('创建会话失败，请重试')
  }
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || !currentSessionId.value || isTyping.value) {
    return
  }

  const userMessage = inputMessage.value.trim()
  inputMessage.value = ''
  isTyping.value = true

  // 添加用户消息到界面
  currentMessages.value.push({
    id: Date.now(),
    role: 'user',
    content: userMessage,
    createTime: new Date().toISOString()
  })

  await nextTick()
  scrollToBottom()

  try {
    // 使用 POST-GET 模式
    const sessionId = currentSessionId.value

    // 步骤1: POST /chat 创建连接并发送消息
    await chat({
      sessionId: sessionId,
      message: userMessage
    })

    // 步骤2: GET /stream/{sessionId} 建立 EventSource 连接接收流数据
    const eventSource = new EventSource(`/api/ai/stream/${sessionId}`)

    let assistantMessage = {
      id: Date.now() + 1,
      role: 'assistant',
      content: '',
      createTime: new Date().toISOString()
    }

    // 立即添加assistant消息到列表
    currentMessages.value.push(assistantMessage)

    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.content) {
          assistantMessage.content += data.content
          // 更新最后一条消息（现在它一定存在）
          const lastIndex = currentMessages.value.length - 1
          currentMessages.value[lastIndex] = { ...assistantMessage }
          scrollToBottom()
        }
        // 只有在done为true时才结束loading
        if (data.done) {
          isTyping.value = false
        }
      } catch (error) {
        console.error('解析消息失败:', error)
      }
    }

    eventSource.onerror = (error) => {
      console.error('SSE错误:', error)
      eventSource.close()
      isTyping.value = false
    }

    eventSource.oncomplete = () => {
      eventSource.close()
      isTyping.value = false
    }

  } catch (error) {
    console.error('发送消息失败:', error)
    isTyping.value = false
  }
}

// 键盘事件
const handleKeyDown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 格式化时间
const formatTime = (timeStr) => {
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) {
    return '刚刚'
  } else if (diff < 3600000) {
    return `${Math.floor(diff / 60000)} 分钟前`
  } else if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)} 小时前`
  } else {
    return date.toLocaleDateString('zh-CN', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }
}

// 组件挂载
onMounted(() => {
  loadSessions()
})
</script>

<style scoped>
.app {
  display: flex;
  height: 100vh;
  background: #343541;
  color: #ECECF1;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 左侧边栏 */
.sidebar {
  width: 280px;
  background: #202123;
  border-right: 1px solid #3E3F4B;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 1rem;
  border-bottom: 1px solid #3E3F4B;
}

.new-chat-btn {
  width: 100%;
  padding: 0.75rem 1rem;
  background: #4A4B53;
  color: #ECECF1;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}

.new-chat-btn:hover {
  background: #5A5B63;
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 0.5rem;
}

.empty-state {
  padding: 2rem 1rem;
  text-align: center;
  color: #8E8EA0;
  font-size: 14px;
}

.chat-list {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.chat-item {
  padding: 0.75rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.chat-item:hover {
  background: #2A2B32;
}

.chat-item.active {
  background: #4A4B53;
}

.chat-item-title {
  font-size: 14px;
  margin-bottom: 0.25rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-item-time {
  font-size: 12px;
  color: #8E8EA0;
}

/* 右侧主内容区 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.welcome-screen {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
}

.welcome-screen h1 {
  font-size: 2rem;
  margin-bottom: 1rem;
  color: #ECECF1;
}

.welcome-screen p {
  font-size: 1rem;
  color: #8E8EA0;
}

/* 聊天容器 */
.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  max-height: 100vh;
}

/* 消息区域 */
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 2rem 1rem;
}

.message {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;
}

.message.user {
  background: #343541;
}

.message.assistant {
  background: #444654;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
}

.message.user .message-avatar {
  background: #5436DA;
}

.message.assistant .message-avatar {
  background: #10A37F;
}

.message-content {
  flex: 1;
}

.message-text {
  font-size: 15px;
  line-height: 1.6;
  margin-bottom: 0.5rem;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.message-time {
  font-size: 12px;
  color: #8E8EA0;
}

/* 加载指示器 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 0.5rem 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #8E8EA0;
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) {
  animation-delay: 0s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-10px);
  }
}

/* 输入区域 */
.input-area {
  border-top: 1px solid #3E3F4B;
  padding: 1.5rem;
  background: #343541;
}

.input-container {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  gap: 0.75rem;
  align-items: flex-end;
}

textarea {
  flex: 1;
  padding: 0.75rem 1rem;
  background: #40414F;
  color: #ECECF1;
  border: 1px solid #565869;
  border-radius: 6px;
  font-size: 15px;
  line-height: 1.5;
  resize: none;
  max-height: 200px;
  font-family: inherit;
}

textarea:focus {
  outline: none;
  border-color: #10A37F;
}

.send-btn {
  padding: 0.75rem 1.5rem;
  background: #10A37F;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: background 0.2s;
}

.send-btn:hover:not(:disabled) {
  background: #0D8F6F;
}

.send-btn:disabled {
  background: #565869;
  cursor: not-allowed;
  opacity: 0.5;
}

/* 滚动条样式 */
.messages::-webkit-scrollbar,
.sidebar-content::-webkit-scrollbar {
  width: 8px;
}

.messages::-webkit-scrollbar-track,
.sidebar-content::-webkit-scrollbar-track {
  background: transparent;
}

.messages::-webkit-scrollbar-thumb,
.sidebar-content::-webkit-scrollbar-thumb {
  background: #565869;
  border-radius: 4px;
}

.messages::-webkit-scrollbar-thumb:hover,
.sidebar-content::-webkit-scrollbar-thumb:hover {
  background: #6E6F80;
}
</style>
