<script setup>
import {onMounted, ref} from 'vue'
import request from '@/utils/request.js'
import {useRouter} from 'vue-router'

const router = useRouter()

const banners = ref([])
const loadBanner = () => {
  request.get("/banner").then(res => {
    banners.value = res.data
  })
}

const notices = ref([])
const loadNotice = () => {
  request.get("/notice").then(res => {
    notices.value = res.data
  })
}

const types = ref([])
const loadType = () => {
  request.get("/type").then(res => {
    types.value = res.data
  })
}


const hotTypes = ref([])
const loadhotTypes = () => {
  request.get("/type/hot").then(res => {
    hotTypes.value = res.data
  })
}


const recommendTypes = ref([])
const loadRecommendType = () => {
  request.get("/type/front").then(res => {
    recommendTypes.value = res.data
  })
}

const goods = ref([])
const loadGoods = () => {
  request.get("/goods/front").then(res => {
    goods.value = res.data
  })
}

const users = ref([]);
const loadUser = () => {
  request.get('/user').then(res => {
    users.value = res.data;
  });
};

const categoryColors = [
  'linear-gradient(135deg, #ff6b35 0%, #ff8c42 100%)',
  'linear-gradient(135deg, #ffd93d 0%, #ffe66d 100%)',
  'linear-gradient(135deg, #6bcf7f 0%, #8fe99e 100%)',
  'linear-gradient(135deg, #ff6b9d 0%, #ffa5c5 100%)'
]

const getProvince = (place) => {
  if (!place) return ''
  return place.split('/')[0]
}


const showNotice = ref(true)
const closeNotice = () => {
  showNotice.value = false
}

onMounted(() => {
  loadType()
  loadBanner()
  loadNotice()
  loadGoods()
  loadUser()
  loadRecommendType()
})
</script>

<template>
  <div class="home-page">
    <div v-if="showNotice && notices.length > 0" class="notice-section">
      <div class="container-full">
        <div class="notice-wrapper">
          <el-carousel
              :interval="3000"
              arrow="never"
              indicator-position="none"
              height="40px"
              class="notice-carousel"
              direction="vertical"
          >
            <el-carousel-item v-for="notice in notices" :key="notice.id">
              <div class="notice-item">
                <span class="notice-content">{{ notice.name }}:{{notice.info}}</span>
              </div>
            </el-carousel-item>
          </el-carousel>
          <button class="notice-close" @click="closeNotice">✕</button>
        </div>
      </div>
    </div>

    <div class="top-categories-section">
      <div class="container-full">
        <div class="categories-wrapper">
          <div class="all-categories-sidebar">
            <div
                v-for="type in types"
                :key="type.id"
                class="category-item"
                @click="router.push('/front/search?keyword=&typeId=' + type.id)"
            >
              <el-image class="category-icon" :src="type.icon"></el-image>
              <span class="category-text">{{ type.name }}</span>
            </div>
          </div>
          <div class="banner-section">
            <el-carousel
                :interval="5000"
                arrow="hover"
                height="440px"
                class="banner-carousel"
            >
              <el-carousel-item v-for="banner in banners" :key="banner.id">
                <div class="banner-item">
                  <img :src="banner.img" :alt="banner.title"/>
                  <div class="banner-overlay">
                    <h2 class="banner-title">{{ banner.name }}</h2>
                  </div>
                </div>
              </el-carousel-item>
            </el-carousel>
          </div>
          <div class="hot-categories-grid">
            <div
                v-for="(type, index) in recommendTypes"
                :key="type.id"
                class="hot-category-card"
                :style="{ background: categoryColors[index % 4] }"
            >
              <div class="card-content">
                <div class="category-info"
                     @click="router.push('/front/search?keyword=&typeId=' + type.id)">
                  <h3 class="category-title">{{ type.name }}</h3>
                  <p class="category-desc">{{ type.info }}</p>
                  <div class="category-image">
                    <img v-if="type.img" :src="type.img" :alt="type.name"/>
                  </div>
                </div>
                <div class="category-goods">
                  <div
                      v-for="item in type.goodsList"
                      :key="item.id"
                      class="goods-mini-card"
                      @click="router.push('/front/goodsDetail?id=' + item.id)"
                  >
                    <div class="goods-mini-image">
                      <img :src="item.img" :alt="item.name"/>
                    </div>
                    <div class="goods-mini-price">¥{{ item.price }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="goods-section">
      <div class="container">
        <div class="section-header">
          <h2 class="section-title">精选服务</h2>
          <el-button link type="primary" @click="router.push('/front/search?keyword=&typeId=0')">查看全部 →</el-button>
        </div>
        <el-row :gutter="24" class="goods-grid">
          <el-col
              v-for="item in goods"
              :key="item.id"
              :xs="24"
              :sm="12"
              :md="8"
              :lg="4"
              class="goods-col"
          >
            <el-card
                shadow="hover"
                class="goods-card"
                :body-style="{ padding: '0' }"
            >
              <div @click="router.push('/front/goodsDetail?id=' + item.id)">
                <div class="goods-image">
                  <img :src="item.img" :alt="item.name"/>
                </div>
                <div class="goods-info">
                  <h3 class="goods-name">
                    <span class="good-shipment">{{ item.shipment }}</span>
                    {{ item.name }}
                  </h3>
                  <div class="goods-footer">
                    <span class="goods-price">¥{{ item.price }}</span>
                    <el-tag size="small" effect="plain">{{ getProvince(item.place) }}</el-tag>
                  </div>
                  <div class="goods-user">
                    <el-avatar
                        :size="24"
                        :src="users.find(i => i.id === item.userId)?.avatarUrl"
                        class="user-avatar"
                    >
                    </el-avatar>
                    <span class="user-nickname">
                      {{ users.find(i => i.id === item.userId)?.nickname }}
                    </span>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </div>
  </div>
</template>

<style scoped>
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

.home-page {
  min-height: 100vh;
  background: #fafafa;
}


.notice-section {
  background: #ffffff;
  padding: 0;
}

.notice-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  position: relative;
}

.notice-carousel {
  flex: 1;
}

.notice-carousel :deep(.el-carousel__container) {
  height: 40px;
}

.notice-item {
  display: flex;
  align-items: center;
  height: 40px;
  cursor: pointer;
}

.notice-content {
  font-size: 14px;
  color: #333;
  line-height: 40px;
}

.notice-close {
  position: absolute;
  right: 0;
  background: transparent;
  border: none;
  font-size: 16px;
  color: #999;
  cursor: pointer;
  padding: 8px;
  transition: color 0.2s;
}

.notice-close:hover {
  color: #333;
}

.container {
  max-width: 80%;
  margin: 0 auto;
  padding: 0 24px;
}

.container-full {
  max-width: 80%;
  margin: 0 auto;
  padding: 0 24px;
}

.top-categories-section {
  padding: 10px 0;
  background: #fff;
}

.categories-wrapper {
  display: flex;
  gap: 16px;
  height: 400px;
}

.all-categories-sidebar {
  width: 225px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  flex-shrink: 0;
  overflow-y: auto;
  padding: 8px;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 15px;
  cursor: pointer;
  transition: all 0.2s;
  line-height: 1.3;
}

.category-item:hover {
  background: #f5f5f5;
}

.category-icon {
  font-size: 20px;
  width: 24px;
  text-align: center;
}

.category-text {
  font-size: 14px;
  font-weight: 500;
}

.hot-categories-grid {
  flex: 3;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  flex-shrink: 0;
}

.hot-category-card {
  border-radius: 12px;
  padding: 16px;
  color: #fff;
  cursor: pointer;
  transition: transform 0.2s;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  height: 190px;
}

.hot-category-card:hover {
  transform: translateY(-2px);
}

.card-content {
  display: flex;
  gap: 10px;
  height: 150px;
}

.category-info {
  flex: 0 0 auto;
  width: 140px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.category-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  line-height: 1.2;
}

.category-desc {
  font-size: 12px;
  opacity: 0.9;
  margin-bottom: 0;
  line-height: 1.3;
}

.category-image {
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  margin-top: auto;
}

.category-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.15));
}

.category-goods {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 2px;
  flex: 1;
  min-width: 0;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  align-items: center;
}

.goods-mini-card {
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 8px;
  flex: 1;
  min-width: 0;
}

.goods-mini-card:hover {
  transform: scale(1.02);
}

.goods-mini-image {
  width: 100%;
  aspect-ratio: 1;
  background: #f5f5f5;
  flex-shrink: 0;
  border-radius: 4px;
  overflow: hidden;
}

.goods-mini-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.goods-mini-price {
  text-align: center;
  margin-top: 10px;
  font-size: 16px;
  font-weight: 700;
  color: #ff4757;
  width: 100%;
}

.banner-section {
  width: 25%;
}

.banner-carousel {
  border-radius: 12px;
  overflow: hidden;
  height: 100%;
}

.banner-item {
  width: 100%;
  height: 440px;
  position: relative;
}

.banner-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.banner-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to right, rgba(0, 0, 0, 0.5) 0%, transparent 70%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 32px;
  color: #fff;
}

.banner-title {
  font-weight: 600;
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.goods-section {
  padding: 30px 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.section-title {
  font-size: 28px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: -0.5px;
}

.goods-col {
  margin-bottom: 24px;
}

.goods-card {
  cursor: pointer;
  transition: transform 0.2s;
  overflow: hidden;
  height: 100%;
}

.goods-card:hover {
  transform: translateY(-4px);
}

.goods-image {
  position: relative;
  width: 100%;
  height: 240px;
  background: #f5f5f5;
  overflow: hidden;
}

.goods-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.goods-card:hover .goods-image img {
  transform: scale(1.05);
}

.goods-info {
  padding: 16px;
}

.good-shipment {
  font-size: 18px;
  font-weight: bold;
  text-decoration: underline;
  text-decoration-color: #ffe610;
  text-decoration-thickness: 8px;
  text-underline-offset: -4px;
}

.goods-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.goods-price {
  font-size: 20px;
  font-weight: 700;
  color: #f56c6c;
}

.goods-user {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.user-avatar {
  flex-shrink: 0;
}

.user-nickname {
  font-size: 13px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>