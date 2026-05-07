<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request.js'
import { useRoute, useRouter } from 'vue-router'
import { Location,LocationFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus'
const route = useRoute()
const router = useRouter()

const address = ref([])
const selectedAddressId = ref(null)
const storedAccount = localStorage.getItem('account')
const account = ref(storedAccount ? JSON.parse(storedAccount) : {})


const id = route.query.id
const goods = ref()

const load = () => {
  request.get("/goods/" + id).then(res => {
    goods.value = res.data
  })
}

// 加载收货地址
const loadAddress = () => {

  request.get('/address').then(res => {
    address.value = res.data
    if (res.data.length > 0 && !selectedAddressId.value) {
      selectedAddressId.value = res.data[0].id
    }
  })
}

// 确认订单
const confirmOrder = () => {
  if (account.value.id == null) {
    ElMessage.warning("请登录")
    return;
  }
  if (selectedAddressId.value === '') {
    ElMessage.error('请选择您的地址')
    return;
  }
  if (goods.value.status !== '已上架') {
    ElMessage.error('商品未上架或已卖出，请联系卖家确认')
    return;
  }
  request.post('/orders', {
    itemId: id,
    addressId: selectedAddressId.value
  }).then(res => {
    if (res.code === '200') {
      ElMessage.success('已下单，请及时支付订单！')
      router.push('/front/orders')
    } else {
      ElMessage.error('res.msg')
    }
  })
}

onMounted(() => {
  load()
  loadAddress()
})
</script>


<template>
  <div class="order-confirm-page">
    <div class="content-wrapper">
      <!-- 左侧区域 -->
      <div class="left-section">
        <!-- 地址 -->
        <div class="address-info-card">
        <div class="section-header">
          <h2 class="section-title">地址</h2>
          <button class="manage-btn" @click="router.push('/front/address')">管理地址</button>
        </div>

        <div class="address-list">
          <div
              v-for="addr in address"
              :key="addr.id"
              class="address-card"
              :class="{ selected: selectedAddressId === addr.id }"
              @click="selectedAddressId = addr.id"
          >
            <div class="location-icon">
              <el-icon>
                <LocationFilled v-if="selectedAddressId === addr.id" />
                <Location v-else />
              </el-icon>
            </div>
            <div class="address-info">
              <div class="address-detail">{{ addr.address }} </div>
              <div class="address-extra">{{ addr.info }}</div>
              <div class="contact-info">{{ addr.name }} {{ addr.phone }}</div>
            </div>
          </div>
        </div>

        </div>
        <!-- 订单信息 -->
        <div class="order-info-section">
          <h2 class="section-title">订单信息</h2>
          <div v-if="goods" class="goods-card">
            <img :src="goods.img" :alt="goods.name" class="goods-image" />
            <div class="goods-info">
              <div class="goods-name">
                {{ goods.name }}
                <el-tag v-if="goods.shipment" size="large">{{ goods.shipment }}</el-tag>
              </div>
              <div class="goods-place">{{ goods.place }}发货</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧价格明细 -->
      <div class="right-section">
        <div class="price-detail-card">
          <h3 class="price-title">价格明细</h3>

          <div class="price-row">
            <span class="price-label">商品总价 </span>
            <span class="price-value">¥{{ goods?.price || 0 }}</span>
          </div>


          <div class="divider"></div>

          <div class="total-row">
            <span class="total-label">合计</span>
            <span class="total-price">{{ goods?.price || 0 }}</span>
          </div>

          <button class="confirm-btn" @click="confirmOrder">确认购买</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.order-confirm-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 20px;

}

.content-wrapper {
  max-width: 70%;
  margin: 0 auto;
  display: flex;
  gap: 20px;
}

.left-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.address-info-card{
  background: white;
  padding: 20px;
  border-radius: 8px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-weight: 600;
  color: #333;
  margin: 0;
}

.manage-btn {
  background: none;
  border: none;
  color: #666;
  cursor: pointer;
  font-size: 14px;
}

.manage-btn:hover {
  color: #ff6633;
}

.address-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.address-card {
  background: white;
  border: 2px solid rgba(224, 224, 224, 0.82);
  border-radius: 8px;
  padding: 16px;
  display: flex;
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  overflow: hidden;
}

.address-card:hover {
  border-color: #ff9966;
}

.address-card.selected {
  border-color: #ff6633;
  background: #fff9f5;
}

.location-icon {
  flex-shrink: 0;
  font-size: 25px;
}

.address-info {
  flex: 1;
  overflow: hidden;
}

.address-detail {
  font-size: 15px;
  color: #333;
  font-weight: 500;
  margin-bottom: 4px;
}

.address-extra {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contact-info {
  font-size: 14px;
  color: #666;
}

.show-all-btn {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 10px;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  text-align: center;
  margin-top: 20px;
}

.show-all-btn:hover {
  color: #ff6633;
  border-color: #ff6633;
}

.order-info-section {
  background: white;
  padding: 20px;
  border-radius: 8px;
}

.goods-card {
  display: flex;
  gap: 16px;
  margin-top: 16px;
}

.goods-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
  background: #f0f0f0;
}

.goods-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.goods-name {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  line-height: 1.5;
}

.goods-place {
  font-size: 16px;
  color: #4174b6;
}

.right-section {
  width: 320px;
  flex-shrink: 0;
}

.price-detail-card {
  background: white;
  padding: 24px;
  border-radius: 8px;
  position: sticky;
  top: 20px;
}

.price-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 20px 0;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.price-label {
  font-size: 14px;
  color: #666;
}

.item-count {
  font-size: 12px;
  color: #999;
}

.price-value {
  font-size: 16px;
  color: #333;
  font-weight: 500;
}

.divider {
  height: 1px;
  background: #e0e0e0;
  margin: 20px 0;
}

.total-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 24px;
}

.total-label {
  font-size: 14px;
  color: #333;
}

.total-price {
  font-size: 28px;
  color: #ff3300;
  font-weight: 600;
}

.total-price::before {
  content: '¥';
  font-size: 18px;
}

.confirm-btn {
  width: 100%;
  background: linear-gradient(135deg, #ff6633 0%, #ff3300 100%);
  border: none;
  border-radius: 24px;
  padding: 14px;
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.confirm-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 51, 0, 0.3);
}

.confirm-btn:active {
  transform: translateY(0);
}


</style>