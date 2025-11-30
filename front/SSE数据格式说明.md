# SSE 数据格式说明

## 概述

前端通过 Server-Sent Events (SSE) 接收后端 `AgentChatResponse` 的流式数据，实现实时聊天体验。

---

## 后端返回格式

### AgentChatResponse 数据结构

后端 `AgentChatResponse` 类定义：

```java
@Data
public class AgentChatResponse {
    private String content;          // 响应内容片段
    private boolean done;            // 是否是最后一个片段
    private MessageType messageType; // 消息类型 (TEXT, TOOL_CALL等)
    private String taskId;           // 任务ID (可选)
    private String payload;          // 数据载荷 (可选)
    private Long timestamp;          // 时间戳
}
```

### SSE 传输格式

后端通过以下方式发送数据：

```java
// 发送部分响应 (流式)
tokenStream.onPartialResponse(reply -> {
    transport.sendMessage(connection, 
        AgentChatResponse.build(reply, MessageType.TEXT));
});

// 发送结束标志
tokenStream.onCompleteResponse(chatResponse -> {
    transport.sendEndMessage(connection, 
        AgentChatResponse.buildEndMessage(MessageType.TEXT));
});
```

实际传输的SSE数据格式：

```
data: {"content":"你","done":false,"messageType":"TEXT","timestamp":1701234567890}
data: {"content":"好","done":false,"messageType":"TEXT","timestamp":1701234567891}
data: {"content":"！","done":false,"messageType":"TEXT","timestamp":1701234567892}
data: {"content":"","done":true,"messageType":"TEXT","timestamp":1701234567893}
```

---

## 前端处理逻辑

### 1. 发送请求

```javascript
const response = await fetch('/api/ai/chat', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    message: userMessage,
    sessionId: currentSession.value.id
  })
})
```

### 2. 读取SSE流

```javascript
const reader = response.body.getReader()
const decoder = new TextDecoder()
let assistantMessage = {
  id: (Date.now() + 1).toString(),
  role: 'ASSISTANT',
  content: '',
  createdAt: new Date()
}
messages.value.push(assistantMessage)

while (true) {
  const { done, value } = await reader.read()
  if (done) break
  
  const chunk = decoder.decode(value)
  const lines = chunk.split('\n')
  
  // 处理每一行
  for (const line of lines) {
    if (line.startsWith('data: ')) {
      // 解析JSON数据
      const data = JSON.parse(line.slice(6))
      
      // 根据消息类型处理
      if (data.messageType === 'TOOL_CALL') {
        // 工具调用消息
        assistantMessage.content += '[工具调用] ' + data.content
        scrollToBottom()
      } else if (data.content && !data.done) {
        // 普通文本片段
        assistantMessage.content += data.content
        scrollToBottom()
      } else if (data.done) {
        // 结束消息
        if (data.content) {
          // 如果有内容，可能是错误或工具结果
          assistantMessage.content += data.content
        }
        break
      }
    }
  }
}
```

---

## 消息类型处理

### 1. TEXT (普通文本)

**场景**：AI正常回复的文本内容

**SSE数据**：
```json
{
  "content": "这是AI的回复",
  "done": false,
  "messageType": "TEXT"
}
```

**前端处理**：
```javascript
if (data.content && !data.done) {
  assistantMessage.content += data.content
  scrollToBottom()
}
```

### 2. TOOL_CALL (工具调用)

**场景**：AI调用工具（如RAG、计算等）

**SSE数据**：
```json
{
  "content": "执行工具：search",
  "done": true,
  "messageType": "TOOL_CALL"
}
```

**前端处理**：
```javascript
if (data.messageType === 'TOOL_CALL') {
  assistantMessage.content += '[工具调用] ' + data.content
  scrollToBottom()
}
```

### 3. 结束标志 (done: true)

**场景**：流式传输完成

**SSE数据**：
```json
{
  "content": "",
  "done": true,
  "messageType": "TEXT"
}
```

**前端处理**：
```javascript
else if (data.done) {
  if (data.content) {
    // 如果有内容，可能是错误消息或工具结果
    assistantMessage.content += data.content
  }
  break  // 退出循环
}
```

---

## 完整示例

### 一次完整的对话流程

**1. 用户发送消息**
```
用户输入: "你好"
```

**2. 后端返回SSE流**

```http
data: {"content":"你","done":false,"messageType":"TEXT","timestamp":1701234567890}

data: {"content":"好","done":false,"messageType":"TEXT","timestamp":1701234567891}

data: {"content":"！","done":false,"messageType":"TEXT","timestamp":1701234567892}

data: {"content":"","done":true,"messageType":"TEXT","timestamp":1701234567893}
```

**3. 前端实时渲染**

```
AI正在输入... (loading动画)
↓
AI: 你 (第1个片段)
↓
AI: 你好 (第2个片段)
↓
AI: 你好！ (第3个片段，完整消息)
```

**4. 带工具调用的对话**

```http
data: {"content":"我需要搜索一下","done":false,"messageType":"TEXT","timestamp":1701234567890}

data: {"content":"执行工具：search","done":true,"messageType":"TOOL_CALL","timestamp":1701234567891}

data: {"content":"根据搜索结果","done":false,"messageType":"TEXT","timestamp":1701234567892}

data: {"content":"","done":true,"messageType":"TEXT","timestamp":1701234567893}
```

**前端显示**：
```
AI: 我需要搜索一下
[工具调用] 执行工具：search
AI: 根据搜索结果...
```

---

## 错误处理

### 1. 网络错误

```javascript
try {
  const response = await fetch('/api/ai/chat', ...)
  if (!response.ok) {
    throw new Error('请求失败')
  }
  // 处理流
} catch (error) {
  console.error('发送消息失败:', error)
  messages.value.push({
    role: 'ASSISTANT',
    content: '抱歉，发生了错误: ' + error.message
  })
}
```

### 2. 数据解析错误

```javascript
try {
  const data = JSON.parse(line.slice(6))
  // 处理数据
} catch (e) {
  console.error('解析SSE数据失败:', e)
}
```

---

## 关键要点

1. **data: 前缀**：所有SSE数据都以 `data: ` 开头
2. **done 字段**：用于标识流式传输是否结束
3. **messageType 字段**：区分不同类型的消息
4. **content 累积**：需要将多个片段的内容累积起来
5. **实时渲染**：每次收到片段都立即更新UI
6. **自动滚动**：新内容到达时自动滚动到底部

---

## 调试技巧

### 1. 查看原始SSE数据

在浏览器开发者工具中：

```javascript
// 在Network面板中查看响应
// 或在代码中添加调试日志
console.log('原始数据:', line)
console.log('解析后:', data)
```

### 2. 监控消息流

```javascript
// 在接收数据时打印
console.log('收到片段:', {
  content: data.content,
  done: data.done,
  messageType: data.messageType
})
```

### 3. 检查累积内容

```javascript
// 在每次添加后检查
console.log('当前累积内容:', assistantMessage.content)
```

---

## 最佳实践

1. **及时释放资源**：使用 `try-finally` 确保 `isLoading` 状态正确重置
2. **错误提示友好**：向用户显示有意义的错误信息
3. **加载状态管理**：正确处理 `isLoading` 状态，避免重复发送
4. **自动滚动优化**：使用 `setTimeout` 延迟滚动，确保DOM更新完成
5. **内存管理**：避免累积过多历史消息

---

**文档版本**：v1.0  
**更新时间**：2025-11-30  
**适用后端**：`AgentChatResponse`  
**适用前端**：Vue 3 + SSE
