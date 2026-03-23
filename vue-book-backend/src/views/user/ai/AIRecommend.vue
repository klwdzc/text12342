<template>
  <div class="ai-container">
    <el-row :gutter="20">
      <!-- 左侧聊天对话框 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>AI 智能推荐</span>
            </div>
          </template>

          <!-- 聊天记录展示区 -->
          <div class="chat-box" ref="chatBoxRef">
            <div v-for="(msg, index) in chatHistory" :key="index" class="message-item">
              <div :class="['message', msg.role === 'user' ? 'message-user' : 'message-ai']">
                <div class="message-avatar">
                  <el-icon v-if="msg.role === 'user'"><User /></el-icon>
                  <el-icon v-else><ChatDotRound /></el-icon>
                </div>
                <div class="message-content">
                  {{ msg.content }}
                </div>
              </div>
            </div>
            <div v-if="loading" class="message-item">
              <div class="message message-ai">
                <div class="message-avatar">
                  <el-icon><ChatDotRound /></el-icon>
                </div>
                <div class="message-content">
                  <el-tag type="info">AI 正在思考中...</el-tag>
                </div>
              </div>
            </div>
          </div>

          <!-- 输入框 -->
          <div class="input-box">
            <el-input
              v-model="userInput"
              placeholder="请输入您想了解的图书类型或主题，例如：推荐一些 Java 编程的书籍"
              :disabled="loading"
              @keyup.enter="handleSend"
            >
              <template #append>
                <el-button type="primary" @click="handleSend" :loading="loading">
                  <el-icon><Promotion /></el-icon>
                  发送
                </el-button>
              </template>
            </el-input>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧历史记录 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>最近咨询记录</span>
              <el-button size="small" @click="loadHistory">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>

          <el-timeline>
            <el-timeline-item
              v-for="(item, index) in historyList"
              :key="index"
              :timestamp="item.createTime || ''"
              placement="top"
            >
              <el-card shadow="hover">
                <p>{{ item.question }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>

          <el-empty v-if="historyList.length === 0" description="暂无咨询记录" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { aiRecommend, getAiHistory } from '@/api/aiIntelligent'

const store = useUserStore()
const chatBoxRef = ref(null)
const loading = ref(false)
const userInput = ref('')

const chatHistory = ref([
  {
    role: 'ai',
    content: '您好！我是 AI 图书推荐助手，请告诉我您感兴趣的图书类型或主题，我会为您推荐相关书籍。',
  },
])

const historyList = ref([])

// 获取用户 ID
const getUserId = () => {
  return localStorage.getItem('userId') || ''
}

// 发送消息
const handleSend = async () => {
  if (!userInput.value.trim()) {
    ElMessage.warning('请输入内容')
    return
  }

  const question = userInput.value.trim()
  
  // 添加用户消息到聊天历史
  chatHistory.value.push({
    role: 'user',
    content: question,
  })

  userInput.value = ''
  loading.value = true

  // 滚动到底部
  await nextTick()
  scrollToBottom()

  try {
    const userId = getUserId()
    const res = await aiRecommend({
      userId: userId,
      question: question,
    })

    if (res.code === 200) {
      chatHistory.value.push({
        role: 'ai',
        content: res.data || '抱歉，暂时无法为您提供推荐。',
      })
    } else {
      chatHistory.value.push({
        role: 'ai',
        content: res.msg || '推荐失败，请稍后重试。',
      })
    }
  } catch (error) {
    console.error('AI 推荐失败:', error)
    chatHistory.value.push({
      role: 'ai',
      content: '请求失败，请稍后重试。',
    })
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (chatBoxRef.value) {
    chatBoxRef.value.scrollTop = chatBoxRef.value.scrollHeight
  }
}

// 加载历史记录
const loadHistory = async () => {
  try {
    const userId = getUserId()
    const res = await getAiHistory(userId)
    
    if (res.code === 200 && res.data) {
      historyList.value = res.data
    } else {
      historyList.value = []
    }
  } catch (error) {
    console.error('加载历史记录失败:', error)
    historyList.value = []
  }
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.ai-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.chat-box {
  height: 500px;
  overflow-y: auto;
  margin-bottom: 20px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.message-item {
  margin-bottom: 20px;
}

.message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.message-user {
  flex-direction: row-reverse;
}

.message-ai .message-content {
  background-color: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 10px;
  max-width: 80%;
}

.message-user .message-content {
  background-color: #409EFF;
  color: #fff;
  border-radius: 4px;
  padding: 10px;
  max-width: 80%;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #ecf5ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #409EFF;
  flex-shrink: 0;
}

.input-box {
  margin-top: 20px;
}
</style>
