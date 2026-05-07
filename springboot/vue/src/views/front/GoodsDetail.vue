<script setup>
import {onMounted, ref} from 'vue'
import {Camera, Star, Clock, Guide, Ticket, LocationInformation, ArrowLeft, StarFilled} from '@element-plus/icons-vue'
import request from '@/utils/request.js'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'

const route = useRoute()
const router = useRouter()

const goods = ref({})
const id = route.query.id
const load = () => {
  request.get("/goods/" + id).then(res => {
    goods.value = res.data
    goods.value.imgList = goods.value.img + ',' + goods.value.imgList
  })
}

const currentRoomImg = ref('')
const changeImg = (img) => {
  goods.value.img = img
}

// 获取表格中显示的图片列表
const getImageList = (imgString) => {
  if (!imgString) return [];
  return imgString.split(',');
};

const users = ref([]);
const loadUser = () => {
  request.get('/user').then(res => {
    users.value = res.data;
  });
};

const buy = (id) => {
  if (account.value.id === null) {
    ElMessage.warning("请登录");
    return;
  }

  router.push(`/front/confirm?id=${id}`);
};


const storedAccount = localStorage.getItem('account')
const account = ref(storedAccount ? JSON.parse(storedAccount) : {})
const collect = (id) => {
  if (account.value.id == null) {
    ElMessage.warning("请登录")
    return;
  }
  let data = {
    itemId: id,
    userId: account.value.id
  }
  request.post("/collect", data).then(res => {
    if (res.code === '200') {
      goods.value.isCollected = true
      ElMessage.success("收藏成功")
    } else {
      goods.value.isCollected = false
      ElMessage.error(res.msg)
    }
  })
}

onMounted(() => {
  load()
  loadUser()
})
</script>

<template>
  <div class="goods-detail">
    <div class="seller-header">
      <div class="seller-info" @click="router.push('/front/user?id='+goods.userId)">
        <el-avatar :src="users.find(i => i.id === goods.userId)?.avatarUrl " :size="50"/>
        <div class="seller-details">
          <h3>{{ users.find(i => i.id === goods.userId)?.nickname }}</h3>
          <div class="seller-stats">
            <span>{{ goods.place }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="main-content">
      <div class="image-section">
        <div class="thumbnail-list">
          <div v-for="(img, index) in getImageList(goods.imgList)"
               :key="index"
               class="thumbnail"
               :class="{ active: currentRoomImg === img }"
               @click="changeImg(img)">
            <img :src="img" :alt="`Thumbnail ${index + 1}`" width="80" height="80">
          </div>
        </div>
        <div class="main-image">
          <el-image :src="goods.img"/>
          <!-- 已售出标签 -->
        </div>
      </div>

      <div class="product-info">
        <div class="quality-badge">{{ goods.shipment }}</div>

        <div class="price-section">
          <div class="current-price">
            <span class="currency">¥</span>
            <span class="amount">{{ goods.price }}</span>
            <span class="shipment">
              <el-text tag="del">行业常规参考价¥{{ goods.rePrice }}</el-text>
            </span>
          </div>
          <div class="view-stats">
            {{ goods.num }}浏览
          </div>
        </div>
        <h3>{{ goods.name }}</h3>
        <div class="product-title" v-html="goods.content"></div>

        <div class="product-specs">
          <div class="spec-item">
            <span class="label">地区：</span>
            <span class="value">{{ goods.place }}</span>
          </div>
          <div class="spec-item">
            <span class="label">技能水平：</span>
            <span class="value">{{ goods.quality }}</span>
          </div>
        </div>

        <div class="action-buttons" v-if="account.id!==goods.userId">
          <el-button
              class="chat-button"
              size="large"
              @click="router.push('/front/chat?userId='+goods.userId)"
              v-if="goods.status==='已上架'"
          >
            <el-icon>
              <ChatDotRound/>
            </el-icon>
            聊一聊
          </el-button>
          <el-button
              class="buy-button"
              size="large"
              type="primary"
              @click="buy(goods.id)"
              v-if="goods.status==='已上架'"
          >
            立即购买
          </el-button>
          <el-button
              class="favorite-button"
              size="large"
              @click="collect(goods.id)"
              v-if="goods.status==='已上架'"
          >
            <el-icon>
              <StarFilled v-if="goods.isCollected"/>
              <Star v-else/>
            </el-icon>
            {{ goods.isCollected ? '已收藏' : '收藏' }}
          </el-button>
          <el-button
              size="large"
              type="info"
              v-if="goods.status==='已售出'"
              disabled
          >
            已售出
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.goods-detail {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  background: #f5f5f5;
}


.seller-header {
  background: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 15px;
  cursor: pointer;
}

.seller-details h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.seller-stats {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
}

.seller-stats .divider {
  color: #ddd;
}


.main-content {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}


.image-section {
  flex: 1;
  display: flex;
  gap: 10px;
  background: white;
  padding: 20px;
  border-radius: 8px;
}

.thumbnail-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.thumbnail {
  width: 80px;
  height: 80px;
  border: 2px solid transparent;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.thumbnail:hover,
.thumbnail.active {
  border-color: #ff6f00;
}

.thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.main-image {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  border-radius: 8px;
  overflow: hidden;
  max-height: 600px;
}

.main-image img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}


.sold-badge img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}


.product-info {
  width: 500px;
  background: white;
  padding: 30px;
  border-radius: 8px;
  position: relative;
}

.quality-badge {
  position: absolute;
  top: 20px;
  right: 20px;
  background: #fff3e0;
  color: #ff6f00;
  padding: 6px 16px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
}

.price-section {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.current-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}

.current-price .currency {
  color: #ff6f00;
  font-size: 24px;
  font-weight: 600;
}

.current-price .amount {
  color: #ff6f00;
  font-size: 48px;
  font-weight: 700;
  line-height: 1;
}

.current-price .shipment {
  color: #ff6f00;
  font-size: 16px;
  padding: 4px 8px;
  background: #fff3e0;
  border-radius: 4px;
}

.view-stats {
  color: #999;
  font-size: 13px;
}

.product-title {
  font-size: 16px;
  line-height: 1.6;
  color: #333;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.product-specs {
  margin-bottom: 30px;
}

.spec-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.spec-item .label {
  color: #999;
  width: 100px;
  font-size: 14px;
}

.spec-item .value {
  color: #333;
  font-size: 14px;
  flex: 1;
}

/* Action Buttons */
.action-buttons {
  display: flex;
  gap: 10px;
}

.chat-button {
  flex: 1;
  background: #ffd54f;
  border: none;
  color: #333;
  font-size: 16px;
  font-weight: 600;
  height: 50px;
}

.chat-button:hover {
  background: #ffca28;
}

.buy-button {
  flex: 1;
  background: #424242;
  border: none;
  color: white;
  font-size: 16px;
  font-weight: 600;
  height: 50px;
}

.buy-button:hover {
  background: #303030;
}

.favorite-button {
  width: 70px;
  height: 50px;
  border: 1px solid #ddd;
  background: white;
  color: #666;
}

.favorite-button.active {
  color: #ff6f00;
  border-color: #ff6f00;
}

.favorite-button:hover {
  border-color: #ff6f00;
  color: #ff6f00;
}


</style>
