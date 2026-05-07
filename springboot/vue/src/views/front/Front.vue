<script setup>
import {ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {projectName} from '../../config/config.default'
import {ElMessage} from 'element-plus'
import {
  Search,
  Star,
  ChatDotRound,
  User,
  Lock,
  SwitchButton,
  Plus,
  Document
} from '@element-plus/icons-vue'
// 路由实例
const router = useRouter()
const route = useRoute()

// 用户信息
const account = ref(
    localStorage.getItem('account') ? JSON.parse(localStorage.getItem('account')) : {}
)

// 退出登录
const logout = () => {
  localStorage.removeItem('account')
  ElMessage.success('退出成功')
  router.push('/login')
}

const handleUpdateAccount = (updatedAccount) => {
  // 更新父组件中的用户信息
  account.value = updatedAccount
}


const keyword = ref('')

const search = () => {
  location.href = '/front/search?keyword=' + keyword.value + '&typeId=0';
}

const goToHome = () => {
  router.push('/front/home')
}

const isActive = (path) => {
  return route.path === path
}








// Front.vue 中的方法
const transferToHuman = () => {
  request.get("/ai/getAdmin").then(res => {
    if (res.code === '200') {
      const admin = res.data
      // 关键：发送一条特殊的 text 消息给管理员
      const applyMsg = {
        fromUserId: account.value.id,
        toUserId: admin.id,
        text: "REQUEST_HUMAN_SERVICE", // 特殊标识
        type: "文字",
        time: new Date().toLocaleString(),
        isRead: false
      }
      request.post("/chat", applyMsg).then(chatRes => {
        aiChatVisible.value = false
        ElMessage.success("已发起申请，正在为您转接人工...")
        router.push('/front/chat') // 跳转到用户的聊天页等待
      })
    }
  })
}













</script>

<template>
  <div class="front-container">
    <!-- 顶部导航栏 -->
    <header class="header-nav">
      <div class="header-left-warp">
        <div class="logo-warp" @click="goToHome" style="cursor: pointer">
          <div class="logo">
            <img src="../../config/logo.svg" alt="Logo"/>
          </div>
          <!--          <div class="logo-text">{{ projectName }}</div>-->
        </div>

        <div class="search-warp">
          <div class="search-container">
            <input
                v-model="keyword"
                placeholder="搜索你想要的闲置商品..."
                class="search-input"
            />
            <el-button
                class="search-btn"
                type="info"
                @click="search"
            >
              <el-icon class="search-icon">
                <Search/>
              </el-icon>
              搜索
            </el-button>
          </div>
        </div>
      </div>

      <div class="user-warp">
        <!-- 未登录状态显示登录注册按钮 -->
        <template v-if="!account.id">
          <div class="btn-login">
            <el-button @click="router.push('/login')">登录</el-button>
          </div>
          <div class="btn-login" style="margin-left: 10px">
            <el-button @click="router.push('/register')">注册</el-button>
          </div>
        </template>

        <!-- 已登录状态显示用户头像和下拉菜单 -->
        <el-dropdown v-else class="custom-dropdown">
          <div class="user-avatar">
            <img :src="account.avatarUrl"/>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>{{ account.nickname }}</el-dropdown-item>
              <el-dropdown-item>
                <router-link to="/front/person" class="dropdown-link">
                  <el-icon>
                    <User/>
                  </el-icon>
                  <span>个人信息</span>
                </router-link>
              </el-dropdown-item>
              <el-dropdown-item>
                <router-link to="/front/password" class="dropdown-link">
                  <el-icon>
                    <Lock/>
                  </el-icon>
                  <span>修改密码</span>
                </router-link>
              </el-dropdown-item>
              <el-dropdown-item>
                <div @click="logout" class="dropdown-link">
                  <el-icon>
                    <SwitchButton/>
                  </el-icon>
                  <span>退出登录</span>
                </div>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 主内容区域 -->
    <div class="main-content">
      <router-view @update-account="handleUpdateAccount"></router-view>
    </div>

    <!-- 为每个按钮添加active类判断 -->
    <div class="floating-sidebar">
      <div
          class="floating-btn publish-btn"
          :class="{ active: isActive('/front/publish') }"
          @click="router.push('/front/publish')"
      >
        <el-icon :size="24">
          <Plus/>
        </el-icon>
        <span class="btn-text">发布</span>
      </div>

      <div class="divider"></div>

      <div
          class="floating-btn"
          :class="{ active: isActive('/front/chat') }"
          @click="router.push('/front/chat')"
      >
        <el-icon :size="22">
          <ChatDotRound/>
        </el-icon>
        <span class="btn-text">消息</span>
      </div>

      <div class="divider"></div>

      <div
          class="floating-btn"
          :class="{ active: isActive('/front/orders') }"
          @click="router.push('/front/orders')"
      >
        <el-icon :size="22">
          <Document/>
        </el-icon>
        <span class="btn-text">订单</span>
      </div>

      <div class="divider"></div>

      <div
          class="floating-btn"
          :class="{ active: isActive('/front/collect') }"
          @click="router.push('/front/collect')"
      >
        <el-icon :size="22">
          <Star/>
        </el-icon>
        <span class="btn-text">收藏</span>
      </div>

      <div class="divider"></div>

      <div
          class="floating-btn"
          :class="{ active: isActive('/front/user') }"
          @click="router.push('/front/user?id='+account.id)"
      >
        <el-icon :size="22">
          <User/>
        </el-icon>
        <span class="btn-text">我的</span>
      </div>

      <div class="divider"></div>

    </div>

    <!-- 页脚 -->
    <footer class="front-footer">
      <p>© {{ new Date().getFullYear() }} {{ projectName }}. 保留所有权利</p>
    </footer>
  </div>
</template>

<style lang="scss" scoped>

/*定义前台头部 背景 主题色*/
$front-back-color: #ffe610;

/*定义前台头部 字体 主题色*/
$front-font-color: #151111;

.front-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header-nav {
  z-index: 1800;
  position: sticky;
  top: 0;
  height: 70px;
  background-color: $front-back-color;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 40px;
  box-shadow: 0 2px 10px 0 rgba(0, 0, 0, 0.1);
  overflow: visible;

  .header-left-warp {
    display: flex;
    align-items: center;
    height: 100%;
    flex: 1;
    justify-content: space-between;

    .logo-warp {
      display: flex;
      align-items: center;
      margin-left: 20px;
      transition: opacity 0.3s;
      min-width: 200px;

      &:hover {
        opacity: 0.8;
      }

      .logo {
        width: 70px;
        height: 70px;
        margin-right: 10px;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
      }

      .logo-text {
        font-size: 22px;
        font-weight: 500;
        color: $front-font-color;
      }

    }

    .search-warp {
      position: absolute;
      left: 50%;
      transform: translateX(-50%);
      width: 500px;
      max-width: 500px;
      margin: 0;

      .search-container {
        position: relative;
        display: flex;
        align-items: center;
        background: #ffffff;
        border-radius: 24px;
        padding: 8px 12px;
        transition: all 0.3s ease;
        border: 2px solid transparent;

        &:hover {
          background: #fff;
          border-color: #151111;
          box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
        }

        &:focus-within {
          background: #fff;
          border-color: $front-font-color;
          box-shadow: 0 2px 16px rgba(64, 132, 217, 0.15);
        }

        .search-icon {
          font-size: 18px;
          color: #151111;
          margin-right: 8px;
          transition: color 0.3s;
        }

        .search-input {
          flex: 1;
          border: none;
          outline: none;
          background: transparent;
          font-size: 14px;
          color: #303133;
          padding: 4px 8px;

          &::placeholder {
            color: #a8abb2;
          }
        }

        .search-btn {
          margin-left: 8px;
          border-radius: 18px;
          padding: 8px 24px;
          font-weight: 500;
          background: $front-back-color;
          color: black;
          border: none;
          transition: all 0.3s;

          &:hover {
            transform: translateY(-1px);
            box-shadow: 0 4px 12px rgba(64, 132, 217, 0.3);
          }

          &:active {
            transform: translateY(0);
          }
        }
      }
    }
  }

  .user-warp {
    display: flex;
    align-items: center;
    margin-right: 20px;
    height: 100%;
    min-width: 200px;
    justify-content: flex-end;

    .btn-login {
      margin-top: 0;
    }

    .user-avatar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      overflow: hidden;
      border: 1px solid $front-font-color;
      padding: 2px;
      cursor: pointer;
      outline: none !important;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        border-radius: 50%;
      }

    }

    .dropdown-link {
      display: flex;
      align-items: center;
      color: inherit;
      text-decoration: none;

      .el-icon {
        margin-right: 8px;
      }
    }

  }
}

.main-content {
  flex: 1;
  background-color: #fff;
}

.floating-sidebar {
  position: fixed;
  right: 10px;
  bottom: 25%;
  z-index: 1000;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.08);
  padding: 12px 0;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(64, 132, 217, 0.1);


  .floating-btn {
    position: relative;
    width: 80px;
    height: 70px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    color: #606266;

    .btn-text {
      font-size: 13px;
      margin-top: 6px;
      font-weight: 500;
    }

    &:hover {
      color: $front-font-color;
      background: $front-back-color;
    }

    &:active {
      transform: scale(0.95);
    }

    &.publish-btn {
      color: $front-font-color;
      font-weight: 600;
    }

    &.active {
      color: $front-font-color;
      background: $front-back-color;
      font-weight: 600;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 4px;
        height: 40px;
        background: $front-font-color;
        border-radius: 0 4px 4px 0;
      }
    }
  }

  .divider {
    height: 1px;
    background: linear-gradient(to right, transparent, #e4e7ed 20%, #e4e7ed 80%, transparent);
    margin: 0 12px;
  }
}

.front-footer {
  padding: 16px 24px;
  text-align: center;
  background-color: #fff;
  color: #666;
  font-size: 12px;
  border-top: 1px solid #eee;
}

</style>
