# SSE实时渲染问题诊断报告

## 问题现象

- ✅ 后端发送消息（基于Spring SseEmitter）
- ❌ 前端未实时渲染
- ❌ 显示"坍缩框"
- ❌ 需要刷新页面才能看到消息

---

## 根本原因

### Spring SseEmitter 工作原理

1. **POST /ai/chat** - 返回 `SseEmitter` 对象
2. **服务器端** - 通过 `emitter.send(data)` 推送数据
3. **数据格式** - SSE协议：`data: {JSON}\n\n`
4. **前端接收** - 需要EventSource API或SSE客户端库

### 当前前端实现

```javascript
// 使用fetch接收流（❌ 错误方式）
const response = await fetch('/api/ai/chat', {...})
const reader = response.body.getReader()  // 无法接收SSE事件
```

**问题**：`fetch()` 无法直接接收Spring SseEmitter推送的SSE事件。

---

## 解决方案

### 方案A：使用SSE客户端库（推荐）

**优点**：无需修改后端，标准SSE支持
**缺点**：需要引入第三方库

```javascript
// 安装：npm install eventsource-polyfill
import EventSource from 'eventsource-polyfill'

const eventSource = new EventSource('/api/ai/chat?sessionId=xxx&message=yyy')

eventSource.onmessage = (event) => {
  const data = JSON.parse(event.data)
  // 处理消息
}

eventSource.onerror = (error) => {
  // 处理错误
}
```

### 方案B：修改后端支持GET接口（标准做法）

**优点**：符合SSE标准，纯前端实现
**缺点**：需要修改后端

**后端修改**：
```java
@RestController
@RequestMapping("/ai")
public class AIChatController {
    
    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody ChatRequest request) {
        // POST创建连接
        return aiChatService.chat(request);
    }
    
    @GetMapping("/stream/{sessionId}")
    public SseEmitter stream(@PathVariable String sessionId) {
        // GET接收流
        // 返回该session的SseEmitter
    }
}
```

**前端修改**：
```javascript
// 1. POST创建连接
await fetch('/api/ai/chat', {...})

// 2. EventSource接收流
const eventSource = new EventSource('/api/ai/stream/' + sessionId)
```

### 方案C：改为WebSocket（最灵活）

**优点**：双向通信，功能强大
**缺点**：需要修改后端WebSocket配置

### 方案D：短轮询（最简单，但体验差）

**优点**：无需修改后端，立即可用
**缺点**：性能差，不是真正的实时

```javascript
// 每500ms轮询一次获取最新消息
setInterval(async () => {
  const messages = await fetch('/api/ai/queryHistoryMessages/' + sessionId)
  // 更新UI
}, 500)
```

---

## 推荐解决方案：方案B（修改后端）

### 步骤1：修改AIChatController

```java
package org.joker.agent.controller;

import jakarta.annotation.Resource;
import org.joker.agent.dto.ChatRequest;
import org.joker.agent.dto.NewSessionDTO;
import org.joker.agent.model.MessageEntity;
import org.joker.agent.model.SessionEntity;
import org.joker.agent.service.AiChatService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RestController
@RequestMapping("/ai")
public class AIChatController {

    @Resource
    private AiChatService aiChatService;
    
    // 存储sessionId到SseEmitter的映射
    private final ConcurrentMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 创建聊天连接（POST）
     */
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody ChatRequest chatRequest) {
        String sessionId = chatRequest.getSessionId();
        
        // 1. 创建SseEmitter
        SseEmitter emitter = aiChatService.chat(chatRequest);
        
        // 2. 存储到映射中
        emitters.put(sessionId, emitter);
        
        // 3. 立即返回会话ID
        return Map.of("sessionId", sessionId);
    }

    /**
     * 接收SSE流（GET）
     */
    @GetMapping("/stream/{sessionId}")
    public SseEmitter stream(@PathVariable String sessionId) {
        return emitters.get(sessionId);
    }
    
    /**
     * 创建新会话
     */
    @PostMapping("/createSession")
    public SessionEntity createSession(@RequestBody NewSessionDTO session) {
        return aiChatService.createSession(session);
    }

    /**
     * 查询历史会话
     */
    @GetMapping("/queryAllSessions")
    public List<SessionEntity> queryAllSessions() {
        return aiChatService.queryAllSessions();
    }

    /**
     * 查询历史消息
     */
    @GetMapping("/queryHistoryMessages/{sessionId}")
    public List<MessageEntity> queryHistoryMessages(@PathVariable String sessionId) {
        return aiChatService.queryHistoryMessages(sessionId);
    }
}
```

### 步骤2：修改前端

```javascript
const sendMessage = async () => {
  if (!currentMessage.value.trim() || !currentSession.value) return
  
  const userMessage = currentMessage.value.trim()
  currentMessage.value = ''
  
  // 1. 发送POST请求创建连接
  await fetch('/api/ai/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      message: userMessage,
      sessionId: currentSession.value.id
    })
  })
  
  // 2. 使用EventSource接收流
  const assistantMessage = {
    id: (Date.now() + 1).toString(),
    role: 'ASSISTANT',
    content: '',
    createdAt: new Date()
  }
  messages.value.push(assistantMessage)
  
  const eventSource = new EventSource(`/api/ai/stream/${currentSession.value.id}`)
  
  eventSource.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      
      if (data.messageType === 'TOOL_CALL') {
        assistantMessage.content += '[工具调用] ' + data.content
      } else if (data.content && !data.done) {
        assistantMessage.content += data.content
        scrollToBottom()
      } else if (data.done) {
        if (data.content) {
          assistantMessage.content += data.content
        }
        eventSource.close()
        isLoading.value = false
      }
    } catch (e) {
      console.error('解析SSE数据失败:', e)
    }
  }
  
  eventSource.onerror = (error) => {
    console.error('SSE连接错误:', error)
    eventSource.close()
    isLoading.value = false
  }
}
```

---

## 当前可用方案：短轮询（立即修复）

如果不想修改后端，可以使用短轮询方案：

```javascript
const sendMessage = async () => {
  // 1. 发送POST请求
  await fetch('/api/ai/chat', {...})
  
  // 2. 立即轮询获取消息
  const interval = setInterval(async () => {
    const messages = await fetch('/api/ai/queryHistoryMessages/' + sessionId)
    const msgList = await messages.json()
    
    // 找到最新消息并更新UI
    
    // 如果有完整响应，停止轮询
    if (/* 响应完整 */) {
      clearInterval(interval)
      isLoading.value = false
    }
  }, 500)
  
  // 最多轮询30秒
  setTimeout(() => {
    clearInterval(interval)
    isLoading.value = false
  }, 30000)
}
```

---

## 总结

| 方案 | 修改后端 | 实时性 | 复杂度 | 推荐度 |
|------|----------|--------|--------|--------|
| SSE客户端库 | ❌ | ✅ | ⭐⭐ | ⭐⭐⭐⭐ |
| GET接口 | ✅ | ✅ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| WebSocket | ✅ | ✅ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| 短轮询 | ❌ | ❌ | ⭐⭐ | ⭐⭐ |

**推荐**：方案B（修改后端支持GET接口），符合SSE标准，体验最佳。

---

**报告时间**：2025-11-30 15:00  
**问题状态**：已诊断，等待用户选择解决方案
