<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">
      <div class="modal-header">
        <h2>创建新聊天</h2>
        <button class="btn-close" @click="$emit('close')">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <path d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"/>
          </svg>
        </button>
      </div>

      <div class="modal-body">
        <form @submit.prevent="handleSubmit">
          <!-- 必填字段 -->
          <div class="form-group">
            <label class="form-label required">欢迎消息</label>
            <textarea
              v-model="formData.welcomeMessage"
              class="input"
              rows="3"
              placeholder="输入欢迎消息"
              required
            ></textarea>
          </div>

          <div class="form-group">
            <label class="form-label required">系统提示词</label>
            <textarea
              v-model="formData.systemPrompt"
              class="input"
              rows="4"
              placeholder="输入系统提示词"
              required
            ></textarea>
          </div>

          <div class="form-group">
            <label class="form-label required">Agent模型配置</label>
            <div class="config-section">
              <div class="config-row">
                <input
                  v-model="formData.agentModelConfig.apiKey"
                  type="password"
                  class="input"
                  placeholder="API Key"
                  required
                />
              </div>
              <div class="config-row">
                <input
                  v-model="formData.agentModelConfig.baseUrl"
                  type="text"
                  class="input"
                  placeholder="Base URL"
                  required
                />
              </div>
              <div class="config-row">
                <input
                  v-model="formData.agentModelConfig.modelId"
                  type="text"
                  class="input"
                  placeholder="模型ID"
                  required
                />
              </div>
              <div class="config-row">
                <input
                  v-model="formData.agentModelConfig.modelEndpoint"
                  type="text"
                  class="input"
                  placeholder="模型端点"
                  required
                />
              </div>
              <div class="config-row">
                <select v-model="formData.agentModelConfig.protocol" class="input" required>
                  <option value="">选择协议</option>
                  <option value="OPENAI">OpenAI</option>
                  <option value="ANTHROPIC">Anthropic</option>
                  <option value="AZURE">Azure</option>
                  <option value="CUSTOM">Custom</option>
                </select>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Embedding模型配置（可选）</label>
            <div class="config-section">
              <div class="config-row">
                <input
                  v-model="formData.embeddingModelConfig.apiKey"
                  type="password"
                  class="input"
                  placeholder="API Key"
                />
              </div>
              <div class="config-row">
                <input
                  v-model="formData.embeddingModelConfig.baseUrl"
                  type="text"
                  class="input"
                  placeholder="Base URL"
                />
              </div>
              <div class="config-row">
                <input
                  v-model="formData.embeddingModelConfig.modelId"
                  type="text"
                  class="input"
                  placeholder="模型ID"
                />
              </div>
              <div class="config-row">
                <input
                  v-model="formData.embeddingModelConfig.modelEndpoint"
                  type="text"
                  class="input"
                  placeholder="模型端点"
                />
              </div>
              <div class="config-row">
                <select v-model="formData.embeddingModelConfig.protocol" class="input">
                  <option value="">选择协议</option>
                  <option value="OPENAI">OpenAI</option>
                  <option value="ANTHROPIC">Anthropic</option>
                  <option value="AZURE">Azure</option>
                  <option value="CUSTOM">Custom</option>
                </select>
              </div>
            </div>
          </div>

          <div class="form-group" style="display: none;">
            <label class="form-label required">支持多模态</label>
            <div class="checkbox-group">
              <input
                id="multimodal"
                v-model="formData.multiModal"
                type="checkbox"
                class="checkbox"
              />
              <label for="multimodal" class="checkbox-label">
                启用多模态支持（可上传图片等文件）
              </label>
            </div>
            <small class="form-help">注意：当前版本未实现文件上传功能，建议保持关闭状态</small>
          </div>

          <div class="form-group">
            <label class="form-label">会话标题（可选）</label>
            <input
              v-model="formData.title"
              type="text"
              class="input"
              placeholder="输入会话标题"
            />
          </div>
        </form>
      </div>

      <div class="modal-footer">
        <button class="btn btn-secondary" @click="$emit('close')">取消</button>
        <button class="btn btn-primary" @click="handleSubmit">创建</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['close', 'create'])

const formData = ref({
  welcomeMessage: '有什么可以帮忙的？',
  systemPrompt: '你是一个智能聊天助手，可以回答各种生活、科技、历史、学习类问题。\n请用简洁明了的语言解释，不使用生硬术语。',
  agentModelConfig: {
    apiKey: '',
    baseUrl: 'https://api.deepseek.com',
    modelId: 'deepseek-chat',
    modelEndpoint: 'deepseek-chat',
    protocol: 'OPENAI'
  },
  embeddingModelConfig: {
    apiKey: '',
    baseUrl: '',
    modelId: '',
    modelEndpoint: '',
    protocol: 'OPENAI'
  },
  multiModal: false,
  title: ''
})

const handleSubmit = () => {
  // 表单验证
  if (!formData.value.welcomeMessage.trim()) {
    alert('请输入欢迎消息')
    return
  }

  if (!formData.value.systemPrompt.trim()) {
    alert('请输入系统提示词')
    return
  }

  const agentConfig = formData.value.agentModelConfig
  if (!agentConfig.apiKey || !agentConfig.baseUrl || !agentConfig.modelId) {
    alert('请填写完整的Agent模型配置')
    return
  }

  emit('create', formData.value)
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-content {
  background-color: #202123;
  border-radius: 12px;
  width: 90%;
  max-width: 700px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    transform: translateY(-20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #3E3F4B;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #ECECF1;
  margin: 0;
}

.btn-close {
  background: none;
  border: none;
  color: #8E8EA0;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.btn-close:hover {
  background-color: #2A2B32;
  color: #ECECF1;
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #ECECF1;
  margin-bottom: 8px;
}

.form-label.required::after {
  content: ' *';
  color: #E53935;
}

.config-section {
  border: 1px solid #3E3F4B;
  border-radius: 8px;
  padding: 16px;
  background-color: #2A2B32;
}

.config-row {
  margin-bottom: 12px;
}

.config-row:last-child {
  margin-bottom: 0;
}

.checkbox-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.checkbox-label {
  font-size: 14px;
  color: #ECECF1;
  cursor: pointer;
}

.form-help {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: #8E8EA0;
}

.modal-footer {
  padding: 20px 24px;
  border-top: 1px solid #3E3F4B;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
