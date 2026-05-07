<template>
  <div class="admin-chat-layout">
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <el-button type="warning" :icon="Service" @click="applyHuman" class="apply-btn">请求人工接入</el-button>
      </div>
      <el-scrollbar>
        <div :class="['contact-item', { active: activeType === 'AI' }]" @click="selectAI">
          <el-avatar :src="aiAvatar" size="small" />
          <div class="contact-info">
            <div class="name">AI 智能助理</div>
            <div class="status">24h 在线</div>
          </div>
        </div>
        <el-divider><span class="divider-text">人工客服记录</span></el-divider>
        <div v-for="admin in servedAdmins" :key="admin.id" :class="['contact-item', { active: activeType === 'ADMIN' && activeAdmin.id === admin.id }]" @click="selectAdmin(admin)">
          <el-avatar :src="admin.avatarUrl" size="small" />
          <div class="contact-info">
            <div class="name">客服：{{ admin.nickname }}</div>
            <div class="status">点击继续对话</div>
          </div>
        </div>
      </el-scrollbar>
    </div>

    <div class="chat-main">
      <div class="chat-header">
        <b v-if="activeType === 'AI'">正在与 AI 助理对话</b>
        <b v-else-if="activeAdmin.id">正在与客服 {{ activeAdmin.nickname }} 沟通</b>
      </div>

      <div class="message-box" ref="msgBox">
        <div v-for="m in displayMessages" :key="m.id" :class="['msg-row', m.fromUserId === account.id ? 'right' : 'left']">
          <el-avatar v-if="m.fromUserId !== account.id" :src="activeType === 'AI' ? aiAvatar : activeAdmin.avatarUrl" size="small" />
          <div :class="['bubble', { 'ai-bubble': activeType === 'AI' && m.fromUserId === 0 }]">
            <el-image v-if="m.type === '图片'" :src="m.text" :preview-src-list="[m.text]" class="chat-img" />
            <span v-else>{{ m.text }}</span>
          </div>
          <el-avatar v-if="m.fromUserId === account.id" :src="account.avatarUrl" size="small" />
        </div>

        <div v-if="activeType === 'AI' && isAiLoading" class="msg-row left">
          <el-avatar :src="aiAvatar" size="small" />
          <div class="bubble ai-bubble loading-bubble">
            <span class="dot-typing">AI 正在思考中...</span>
          </div>
        </div>
      </div>

      <div class="input-area">
        <el-upload :action="`${serverHost}/web/upload`" :show-file-list="false" :on-success="handleImgSuccess">
          <el-button :icon="Picture" circle />
        </el-upload>
        <el-input v-model="inputText" @keyup.enter="handleSend" placeholder="请输入内容..." :disabled="isAiLoading" />
        <el-button type="primary" @click="handleSend" :disabled="!inputText.trim() || isAiLoading">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick, onBeforeUnmount } from 'vue'
import { Service, Picture } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { serverHost } from '../../../config/config.default'
import { ElNotification, ElMessage } from 'element-plus'

const account = ref(localStorage.getItem('account') ? JSON.parse(localStorage.getItem('account')) : {})
const servedAdmins = ref([]);
const activeType = ref('AI');
const activeAdmin = ref({});
const aiMessages = ref([]);
const adminMessages = ref([]);
const inputText = ref('');
const msgBox = ref(null);
const aiAvatar = 'https://img.icons8.com/fluency/48/bot.png';

// 💡 状态控制：是否正在等待 AI 回复
const isAiLoading = ref(false);

let socket = null;

const displayMessages = computed(() => {
  return activeType.value === 'AI' ? aiMessages.value : adminMessages.value
})

const initSocket = () => {
  if (socket) socket.close()
  const socketUrl = `ws://localhost:9090/chatServer/${account.value.id}`
  socket = new WebSocket(socketUrl)
  socket.onmessage = (event) => {
    const data = JSON.parse(event.data)
    if (data.messageType === 'chat' && activeType.value === 'ADMIN' && data.fromUserId === activeAdmin.value.id) {
      adminMessages.value.push(data)
      scrollToBottom()
    }
  }
}

const applyHuman = () => {
  const applyMsg = {
    toUserId: 0,
    text: "用户请求人工接入",
    type: '文字',
    tp: 'uertoadmin',
    messageType: 'apply_human',
    isRead: false,
    time: new Date().toLocaleString()
  }
  request.post('/chatAdmin/save', applyMsg).then(res => {
    if (res.code === '200') {
      ElNotification.success("呼叫成功，请等待响应")
      loadServedAdmins()
      if (socket && socket.readyState === 1) socket.send(JSON.stringify(applyMsg))
    }
  })
}

// 💡 修改发送逻辑，加入 Loading 控制
const handleSend = () => {
  const content = inputText.value
  if (activeType.value === 'AI') {
    // 1. 先把用户的提问展示在页面上
    aiMessages.value.push({ fromUserId: account.value.id, text: content, type: '文字' })
    inputText.value = ''

    // 💡 2. 开启加载状态 (显示"思考中"气泡)
    isAiLoading.value = true;
    scrollToBottom(); // 确保加载气泡滚动到视野内

    // 💡 3. 向后端发起请求 (注意：建议在这里单独设置超时，防止 F12 里的 5秒超时再次发生)
    // 这里的例子假设 request 工具已经处理了超时，如果没有，请参考之前的建议在 request.post 里加上 {timeout: 60000}
    request.post('/ai/chat', { message: content }).then(res => {
      if (res.code === '200') {
        // 将后端返回的正式回复推入消息数组
        aiMessages.value.push({ fromUserId: 0, text: res.data, type: '文字' })
      } else {
        ElMessage.error(res.msg || "AI 响应异常")
      }
    }).catch(err => {
      // 捕获网络错误或超时
      console.error("AI 请求失败:", err)
      if (err.code === 'ECONNABORTED') {
        ElMessage.error("AI 思考时间较长，请稍等或重试")
      } else {
        ElMessage.error("网络异常，请重试")
      }
    }).finally(() => {
      // 💡 4. 无论成功还是失败，都要关闭加载状态 (隐藏气泡)
      isAiLoading.value = false;
      scrollToBottom();
    })
  } else {
    executeAdminSend(content, '文字')
    inputText.value = ''
  }
}

const executeAdminSend = (content, type) => {
  if (!activeAdmin.value.id) return ElNotification.warning("请先选择客服")
  const message = {
    text: content, type: type, toUserId: activeAdmin.value.id,
    fromUserId: account.value.id, tp: 'uertoadmin', messageType: 'chat',
    time: new Date().toLocaleString()
  }
  if (socket && socket.readyState === 1) socket.send(JSON.stringify(message))
  request.post('/chatAdmin/save', message).then(() => {
    adminMessages.value.push(message)
    scrollToBottom()
  })
}

const loadServedAdmins = () => request.get('/chatAdmin/getServedUsers').then(res => servedAdmins.value = res.data)

const selectAI = () => {
  activeType.value = 'AI'; activeAdmin.value = {}
  request.get('/chatAdmin/aiHistory', { params: { userId: account.value.id } }).then(res => {
    aiMessages.value = res.data; scrollToBottom()
  })
}

const selectAdmin = (admin) => {
  activeType.value = 'ADMIN'; activeAdmin.value = admin
  request.get('/chatAdmin/history', { params: { userId: account.value.id, adminId: admin.id } }).then(res => {
    adminMessages.value = res.data; scrollToBottom()
  })
}

const handleImgSuccess = (res) => {
  if (activeType.value === 'AI') aiMessages.value.push({ fromUserId: account.value.id, text: res, type: '图片' })
  else executeAdminSend(res, '图片')
}

// 💡 统一封装滚动逻辑，确保能滚到最底部
const scrollToBottom = () => nextTick(() => {
  if (msgBox.value) {
    msgBox.value.scrollTop = msgBox.value.scrollHeight
  }
})

onMounted(() => { initSocket(); selectAI(); loadServedAdmins() })
onBeforeUnmount(() => { if (socket) socket.close() })
</script>

<style scoped>
/* 原有样式 (保持不变) */
.admin-chat-layout { display: flex; width: 1000px; height: 600px; margin: 20px auto; background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); border: 1px solid #eee; }
.chat-sidebar { width: 280px; border-right: 1px solid #f0f0f0; background: #fafafa; display: flex; flex-direction: column; }
.sidebar-header { padding: 15px; border-bottom: 1px solid #eee; }
.apply-btn { width: 100%; font-weight: bold; }
.contact-item { display: flex; padding: 12px 15px; align-items: center; cursor: pointer; }
.contact-item.active { background: #f0f7ff; border-right: 4px solid #409EFF; }
.chat-main { flex: 1; display: flex; flex-direction: column; }
.message-box { flex: 1; padding: 20px; overflow-y: auto; background: #fdfdfd; }
.msg-row { display: flex; margin-bottom: 15px; gap: 8px; }
.right { justify-content: flex-end; }
.bubble { padding: 10px 14px; border-radius: 8px; font-size: 14px; max-width: 65%; background: #fff; border: 1px solid #eee; white-space: pre-wrap; word-break: break-all;}
.right .bubble { background: #95ec69; border: none; }
.ai-bubble { background: #e8f4ff; }
.input-area { padding: 15px 20px; border-top: 1px solid #eee; display: flex; gap: 10px; }

/* 💡 新增：思考中气泡的特殊样式 */
.loading-bubble {
  color: #909399; /* 灰色字体，代表临时状态 */
  font-style: italic; /* 斜体 */
  display: flex;
  align-items: center;
}

/* 💡 新增：简单的闪烁动画效果 */
.dot-typing {
  animation: blink 1.5s infinite; /* 1.5秒一次无限循环 */
}

@keyframes blink {
  0% { opacity: 0.3; } /* 最暗 */
  50% { opacity: 1; }  /* 最亮 */
  100% { opacity: 0.3; } /* 最暗 */
}
</style>