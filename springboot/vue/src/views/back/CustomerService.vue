<template>
  <div class="customer-service-container">
    <div class="sidebar">
      <div class="sidebar-header">
        <h3>客服管理中心</h3>
        <el-button :icon="Refresh" circle size="small" @click="refreshAll" />
      </div>

      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane name="new">
          <template #label>
            <el-badge :value="applyList.length" :hidden="applyList.length === 0" type="danger">待接入</el-badge>
          </template>
          <div class="op-bar">
            <el-button link type="primary" :icon="Refresh" @click="loadApplyList">刷新申请</el-button>
          </div>
          <el-scrollbar>
            <div v-for="item in applyList" :key="item.id" class="user-item apply-card" @click="handleAccept(item)">
              <div class="user-info">
                <div class="user-name">用户 ID: {{ item.fromUserId }}</div>
                <div class="apply-text">{{ item.text }}</div>
              </div>
              <el-button type="primary" size="small" round>接入</el-button>
            </div>
            <el-empty v-if="applyList.length === 0" description="暂无新申请" :image-size="60" />
          </el-scrollbar>
        </el-tab-pane>

        <el-tab-pane label="服务中" name="active">
          <el-scrollbar>
            <div v-for="user in recentUsers" :key="user.id"
                 class="user-item" :class="{ 'active-user': activeUser.id === user.id }"
                 @click="selectUser(user)">
              <el-avatar :src="user.avatarUrl" size="default" />
              <div class="user-info">
                <div class="user-name">{{ user.nickname }}</div>
                <div class="last-msg">点击进入对话</div>
              </div>
            </div>
          </el-scrollbar>
        </el-tab-pane>
      </el-tabs>
    </div>

    <div class="chat-main">
      <template v-if="activeUser.id">
        <div class="chat-header">正在服务：<b>{{ activeUser.nickname }}</b></div>

        <div class="message-container" ref="msgContainer">
          <div v-for="(msg, index) in messages" :key="index"
               class="msg-row" :class="{ 'msg-self': msg.fromUserId === account.id }">

            <el-avatar v-if="msg.fromUserId !== account.id" :src="activeUser.avatarUrl" size="small" />

            <div class="msg-content">
              <div class="bubble">
                <el-image
                    v-if="msg.type === '图片'"
                    :src="msg.text"
                    :preview-src-list="[msg.text]"
                    class="chat-img"
                    fit="cover"
                />
                <span v-else>{{ msg.text }}</span>
              </div>
              <div class="msg-time">{{ msg.time }}</div>
            </div>

            <el-avatar v-if="msg.fromUserId === account.id" :src="account.avatarUrl" size="small" />
          </div>
        </div>

        <div class="input-area">
          <div class="toolbar" style="margin-bottom: 8px;">
            <el-upload
                :action="`${serverHost}/web/upload`"
                :show-file-list="false"
                :on-success="handleImgSuccess"
            >
              <el-button :icon="Picture" circle title="发送图片" />
            </el-upload>
          </div>
          <el-input
              v-model="inputText"
              type="textarea"
              :rows="3"
              @keyup.enter="sendMsg"
              placeholder="请输入回复内容..."
              resize="none"
          />
          <div class="actions">
            <el-button type="primary" @click="sendMsg" :disabled="!inputText.trim()">发送消息</el-button>
          </div>
        </div>
      </template>

      <div v-else class="empty-chat">
        <el-icon :size="60" color="#ddd"><Service /></el-icon>
        <p>请选择一个待接入申请或客户开始工作</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue'
import request from "@/utils/request"
import { Refresh, Service, Picture } from '@element-plus/icons-vue'
import { ElNotification } from 'element-plus'
import { serverHost } from '../../../config/config.default'// 💡 确保路径正确

const account = ref(JSON.parse(localStorage.getItem('account') || '{}'))
const activeTab = ref('new')
const applyList = ref([])
const recentUsers = ref([])
const activeUser = ref({})
const messages = ref([])
const inputText = ref('')
const msgContainer = ref(null)

let socket = null

// 💡 初始化 WebSocket
const initSocket = () => {
  if (socket) socket.close()
  const url = `ws://localhost:9090/chatServer/${account.value.id}`
  socket = new WebSocket(url)

  socket.onmessage = (event) => {
    const data = JSON.parse(event.data)

    // 监听实时呼叫信号
    if (data.messageType === 'apply_human') {
      const exists = applyList.value.some(a => a.fromUserId === data.fromUserId)
      if (!exists) {
        applyList.value.unshift(data)
        ElNotification.info({ title: '新人工申请', message: `用户 ${data.fromUserId} 请求接入` })
      }
      return
    }

    // 监听当前对话人的实时消息
    if (data.messageType === 'chat' && activeUser.value.id === data.fromUserId) {
      messages.value.push(data)
      scrollToBottom()
    }
  }
}

const loadApplyList = () => request.get("/chatAdmin/getApplyList").then(res => applyList.value = res.data)
const loadServedUsers = () => request.get("/chatAdmin/getServedUsers").then(res => recentUsers.value = res.data)

// 💡 切换用户并清除已读状态
const selectUser = (user) => {
  activeUser.value = user
  request.get("/chatAdmin/clearApply", { params: { userId: user.id } }).then(() => {
    loadApplyList()
    loadMessages(user.id)
  })
}

// 💡 接入新用户
const handleAccept = (item) => {
  request.get("/chat/user/" + item.fromUserId).then(res => {
    const user = res.data
    const welcome = {
      toUserId: user.id,
      fromUserId: account.value.id,
      text: "您好，人工客服已接入，请问有什么可以帮您？",
      tp: 'admintouser',
      messageType: 'chat',
      type: '文字',
      isRead: false
    }

    // 通过 Socket 告知用户端已接入
    if (socket && socket.readyState === 1) socket.send(JSON.stringify(welcome))

    request.post("/chatAdmin/save", welcome).then(() => {
      activeTab.value = 'active'
      selectUser(user)
    })
  })
}

const loadMessages = (targetId) => request.get('/chatAdmin/history', { params: { userId: account.value.id, adminId: targetId } }).then(res => {
  messages.value = res.data
  scrollToBottom()
})

// 💡 文字发送
const sendMsg = () => {
  if (!inputText.value.trim()) return
  const msgData = {
    toUserId: activeUser.value.id,
    fromUserId: account.value.id,
    text: inputText.value,
    type: '文字',
    tp: 'admintouser',
    messageType: 'chat',
    isRead: false
  }

  if (socket && socket.readyState === 1) socket.send(JSON.stringify(msgData))

  request.post("/chatAdmin/save", msgData).then(() => {
    messages.value.push({ ...msgData, time: new Date().toLocaleString() })
    inputText.value = ''
    scrollToBottom()
  })
}

// 💡 核心新增：管理员图片上传成功处理
const handleImgSuccess = (res) => {
  const msgData = {
    toUserId: activeUser.value.id,
    fromUserId: account.value.id,
    text: res,
    type: '图片', // 💡 必须标记为图片
    tp: 'admintouser',
    messageType: 'chat',
    isRead: false
  }

  if (socket && socket.readyState === 1) socket.send(JSON.stringify(msgData))

  request.post("/chatAdmin/save", msgData).then(() => {
    messages.value.push({ ...msgData, time: new Date().toLocaleString() })
    scrollToBottom()
  })
}

const refreshAll = () => { loadApplyList(); loadServedUsers(); }
const scrollToBottom = () => nextTick(() => { if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight })

onMounted(() => {
  initSocket()
  refreshAll()
})

onBeforeUnmount(() => {
  if (socket) socket.close()
})
</script>

<style scoped>
.customer-service-container { display: flex; height: 85vh; background: #fff; border: 1px solid #eee; margin: 10px; border-radius: 8px; overflow: hidden; }
.sidebar { width: 300px; border-right: 1px solid #f0f0f0; display: flex; flex-direction: column; background: #fafafa; }
.sidebar-header { padding: 15px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #eee; background: #fff; }
.user-item { padding: 15px; display: flex; align-items: center; cursor: pointer; border-bottom: 1px solid #f9f9f9; background: #fff; }
.active-user { background: #e6f7ff !important; border-right: 4px solid #1890ff; }
.apply-card { background: #fffbe6; }
.chat-main { flex: 1; display: flex; flex-direction: column; background: #fff; }
.message-container { flex: 1; padding: 20px; overflow-y: auto; background: #f8f9fb; }
.msg-row { display: flex; margin-bottom: 20px; gap: 10px; }
.msg-self { justify-content: flex-end; }
.bubble { padding: 10px 14px; background: #fff; border-radius: 6px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); font-size: 14px; line-height: 1.5; }
.msg-self .bubble { background: #95ec69; }
.chat-img { max-width: 200px; border-radius: 4px; display: block; margin-top: 2px; cursor: pointer; }
.msg-time { font-size: 11px; color: #bbb; margin-top: 6px; }
.input-area { padding: 15px; border-top: 1px solid #eee; background: #fff; }
.actions { text-align: right; margin-top: 10px; }
.empty-chat { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #ccc; }
</style>