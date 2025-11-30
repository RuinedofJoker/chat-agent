# AI Agent 聊天前端

基于 Vue3 构建的类似 ChatGPT 的聊天界面。

## 技术栈

- Vue 3
- Vite
- Axios
- ChatGPT 风格 UI 设计

## 项目结构

```
front/
├── src/
│   ├── components/
│   │   └── NewChatModal.vue    # 新建聊天弹窗组件
│   ├── api/
│   │   └── index.js            # API 接口封装
│   ├── App.vue                 # 主应用组件
│   ├── main.js                 # 应用入口
│   └── style.css               # 全局样式
├── index.html
├── vite.config.js              # Vite 配置
├── package.json
└── README.md
```

## 功能特性

- ✨ 左侧历史聊天列表，支持按时间分组（今天、过去7天）
- ✨ 右侧聊天消息区域，支持流式响应
- ✨ 底部消息输入框，支持 Enter 发送
- ✨ 新建聊天弹窗表单，支持配置模型参数
- ✨ ChatGPT 风格的深色主题 UI
- ✨ 响应式设计，适配不同屏幕尺寸

## API 接口

前端调用以下后端接口：

- `GET /ai/queryAllSessions` - 查询所有会话
- `GET /ai/queryHistoryMessages/{sessionId}` - 查询会话历史消息
- `POST /ai/createSession` - 创建新会话
- `POST /ai/chat` - 发送聊天消息（支持 SSE）

## 使用方法

### 安装依赖

```bash
cd front
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 构建生产版本

```bash
npm run build
```

### 预览生产版本

```bash
npm run preview
```

## 配置说明

### Vite 代理配置

在 `vite.config.js` 中，已配置将 `/ai` 路径代理到 `http://localhost:8080`，请确保后端服务运行在 8080 端口。

### 后端 API

请确保后端启动在 http://localhost:8080，且暴露以下接口：

- `POST /ai/chat` - SSE 流式聊天
- `POST /ai/createSession` - 创建会话
- `GET /ai/queryAllSessions` - 查询所有会话
- `GET /ai/queryHistoryMessages/{sessionId}` - 查询历史消息

## 创建新聊天

点击左侧 "新建聊天" 按钮，会弹出表单，需要填写：

**必填项：**
- 欢迎消息
- 系统提示词
- Agent 模型配置（API Key、Base URL、模型ID、模型端点、协议等）

**可选项：**
- Embedding 模型配置
- 会话标题
- 多模态支持（当前版本建议保持关闭）

## 注意事项

1. 当前版本未实现文件上传功能，multiModal 建议保持关闭
2. SSE 功能可能需要根据实际后端实现进行调整
3. 样式参考 ChatGPT 官网，采用深色主题

## 开发者

本项目使用现代前端技术栈，遵循 Vue 3 Composition API 最佳实践。
