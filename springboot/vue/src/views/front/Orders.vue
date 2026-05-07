<script setup>
import {ref, onMounted, reactive, watch} from 'vue'
import { Shop, Document, Search } from '@element-plus/icons-vue';
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";


// 1. 定义售后相关的变量
const refundDialogVisible = ref(false)
const refundForm = ref({
  id: null,
  reason: ''
})

const activeTab = ref('全部')
const selectedSidebar = ref('我卖出的')
const tabs = [
  { label: '全部', value: '全部' },
  { label: '待支付', value: '待支付' },
  { label: '待完成', value: '待完成' },
  { label: '待确认完成', value: '待确认完成' },
]

const sidebarItems = [
  { label: '我卖出的', value: '我卖出的' },
  { label: '我买到的', value: '我买到的' }
]

// 表单数据
const form = ref({})
const dialogFormVisible = ref(false)
const orders = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

// 搜索条件
const searchForm = reactive({
  keyword: '',
})

const load = () => {
  request.get("/orders/front/page", {
    params: {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchForm.keyword,
      status: activeTab.value,
      flag: selectedSidebar.value
    }
  }).then(res => {
    if (res.data) {
      orders.value = res.data.records
      total.value = res.data.total
    }
  })
}

const getStatusClass = (status) => {
  const classes = {
    '交易关闭': 'status-closed',
    '交易完成': 'status-success',
    '待支付': 'status-payment',
    '待完成': 'status-shipment',
    '待确认完成': 'status-receipt',
    '售后中': 'status-refund', // 新增样式类
  }
  return classes[status] || ''
}

const handleSearch = () => {
  pageNum.value = 1
  load()
}

const handlePageChange = (val) => {
  pageNum.value = val
  load()
}

const users = ref([]);
const loadUser = () => {
  request.get('/user').then(res => {
    users.value = res.data;
  });
};

watch([activeTab, selectedSidebar], () => {
  pageNum.value = 1
  load()
})

const pay = (id) => {
  request.get('/orders/pay/' + id).then(res => {
    if (res.code === '200') {
      ElMessage.success("已支付")
    } else {
      ElMessage.error(res.msg)
    }
    load()
  })
}

const cancel = (id) => {
  request.get('/orders/cancel/' + id).then(res => {
    if (res.code === '200') {
      ElMessage.success("已取消")
    } else {
      ElMessage.error(res.msg)
    }
    load()
  })
}

const shipment = (id) => {
  request.get('/orders/shipment/' + id).then(res => {
    if (res.code === '200') {
      ElMessage.success("开始服务")
    } else {
      ElMessage.error(res.msg)
    }
    load()
  })
}

const receipt = (id) => {
  request.get('/orders/receipt/' + id).then(res => {
    if (res.code === '200') {
      ElMessage.success("服务完成")
    } else {
      ElMessage.error(res.msg)
    }
    load()
  })
}

const handleEdit = (order) => {
  form.value = JSON.parse(JSON.stringify(order))
  dialogFormVisible.value = true
}
//售后
const save = () => {
  request.post("/orders", form.value).then(res => {
    if (res.code === '200') {
      ElMessage.success("保存成功")
      dialogFormVisible.value = false
      load()
    } else {
      ElMessage.error("保存失败")
    }
  })
}

const handleRefund = (order) => {
  refundForm.value.id = order.id
  refundForm.value.reason = ''
  refundDialogVisible.value = true
}
// 提交售后申请函数
const submitRefund = () => {
  if (!refundForm.value.reason) {
    ElMessage.warning("请填写申请原因")
    return
  }
  // 注意路径：/orders/refund/ID
  request.get('/orders/refund/' + refundForm.value.id, {
    params: { reason: refundForm.value.reason }
  }).then(res => {
    if (res.code === '200') {
      ElMessage.success("申请成功")
      refundDialogVisible.value = false
      load()
    } else {
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
  <div class="orders-container">
    <div class="orders-page">
      <div class="sidebar">
        <div class="sidebar-header">
          <el-icon style="font-size:30px">
            <Shop/>
          </el-icon>
          <span>我的交易</span>
        </div>

        <div class="category-nav">
          <button
              v-for="item in sidebarItems"
              :key="item.value"
              :class="['category-item', { active: selectedSidebar === item.value }]"
              @click="selectedSidebar = item.value"
          >
            {{ item.label }}
          </button>
        </div>
      </div>

      <div class="main-content">
        <div class="search-bar">
          <el-input
              v-model="searchForm.keyword"
              placeholder="请输入订单号搜索"
              class="search-input"
              clearable
              @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon>
                <Search/>
              </el-icon>
            </template>
          </el-input>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </div>

        <div class="tabs">
          <button
              v-for="tab in tabs"
              :key="tab.value"
              :class="['tab-item', { active: activeTab === tab.value }]"
              @click="activeTab = tab.value"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="orders-list">
          <div v-for="order in orders" :key="order.id" class="order-card">
            <div class="order-header">
              <div class="order-no">订单号: {{ order.no }}</div>
              <div class="seller-info">
                <h3>{{ selectedSidebar === '我卖出的' ? '买家：' : '卖家：' }}</h3>
                <img
                    :src="selectedSidebar === '我卖出的'
                      ? users.find(item => item.id === order.toId)?.avatarUrl
                      : users.find(item => item.id === order.fromId)?.avatarUrl"
                    class="seller-avatar"
                    alt="头像"
                />

                <span class="seller-name">
                  {{ selectedSidebar === '我卖出的'
                    ? users.find(item => item.id === order.toId)?.nickname
                    : users.find(item => item.id === order.fromId)?.nickname }}
                </span>
              </div>
            </div>

            <div class="order-body">
              <img
                  :src="order.itemImg"
                  class="product-image"
                  alt="商品图片"
              />
              <div class="product-info">
                <div class="product-title">{{ order.itemName }}</div>

                <div class="product-meta">
                  <span>下单时间: {{ order.time }}</span>
                </div>
                <div class="product-price">¥{{ order.price?.toFixed(2) }}</div>
              </div>
            </div>

            <div class="order-footer">
              <div class="btn-more"><span :class="['order-status', getStatusClass(order.status)]">{{ order.status }}</span></div>
              <div class="action-buttons">
                <el-button type="primary"  v-if="order.status === '待支付' && selectedSidebar === '我买到的'"  @click="pay(order.id)">立即支付</el-button>
                <el-button type="danger"   v-if="order.status ==='待支付'" @click="cancel(order.id)">取消订单</el-button>
                <el-button type="success"  v-if="order.status==='待完成' && selectedSidebar==='我卖出的'" @click="shipment(order.id)">立即服务</el-button>
                <el-button type="success"  v-if="order.status==='待确认完成' && selectedSidebar==='我买到的'" @click="receipt(order.id)">确认完成</el-button>
                <el-button type="warning"  v-if="order.status === '交易完成' && selectedSidebar === '我买到的' && !order.toReview" @click="handleEdit(order)">去评价</el-button>
                <el-button type="info"     v-if="(order.status === '待确认完成' || order.status === '交易完成') && selectedSidebar === '我买到的'" @click="handleRefund(order)">申请售后</el-button>
              </div>
            </div>
          </div>

          <div v-if="orders.length === 0" class="empty-state">
            <el-icon class="empty-icon">
              <Document/>
            </el-icon>
            <p>暂无订单</p>
          </div>
        </div>

        <div v-if="total > 0" class="pagination-container">
          <el-pagination
              v-model:current-page="pageNum"
              :page-size="pageSize"
              :total="total"
              layout="total, prev, pager, next, jumper"
              @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogFormVisible" :title="form.id ? '编辑' : '新增'" width="30%" center>
      <el-form :model="form" label-width="100px">
        <el-form-item label="评分">
          <el-rate v-model="form.toRate"/>
        </el-form-item>
        <el-form-item label="评价">
          <el-input v-model="form.toReview" type="textarea" placeholder="请输入买家评价"/>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">取消</el-button>
          <el-button type="primary" @click="save">确定</el-button>
        </div>
      </template>

    </el-dialog>
  </div>
  <el-dialog v-model="refundDialogVisible" title="申请售后" width="30%" center>
    <el-form :model="refundForm" label-width="80px">
      <el-form-item label="售后原因">
        <el-input
            v-model="refundForm.reason"
            type="textarea"
            rows="4"
            placeholder="请描述您的售后理由（如商品破损、描述不符等）"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitRefund">提交申请</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.orders-container {
  background-color: #f5f5f5;
  min-height: 100vh;
}

.orders-page {
  max-width: 80%;
  margin: 0 auto;
  display: flex;
  gap: 24px;
}

.sidebar {
  width: 250px;
  margin-top: 25px;
  border-radius: 10px;
  background-color: white;
  padding: 24px 0;
  height: fit-content;
  min-height: 240px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 24px;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 500;
}

.category-nav {
  display: flex;
  flex-direction: column;
}

.category-item {
  padding: 12px 24px;
  margin: 10px;
  text-align: left;
  background: none;
  border: none;
  cursor: pointer;
  color: #333;
  border-radius: 10px;
  transition: background-color 0.3s ease;
}

.category-item:hover {
  background-color: #f5f5f5;
}

.category-item.active {
  background-color: #f5f5f5;
  font-weight: 500;
  border-radius: 10px;
}

.main-content {
  flex: 1;
  padding: 24px;
}

.search-bar {
  display: flex;
  gap: 12px;
  background-color: white;
  padding: 16px 24px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.search-input {
  flex: 1;
  max-width: 400px;
}

.tabs {
  display: flex;
  gap: 32px;
  background-color: white;
  padding: 16px 24px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.tab-item {
  background: none;
  border: none;
  padding: 8px 0;
  cursor: pointer;
  color: #666;
  font-size: 15px;
  position: relative;
  transition: color 0.2s;
}

.tab-item:hover {
  color: #333;
}

.tab-item.active {
  color: #333;
  font-weight: 500;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background-color: #ff6b00;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.order-no {
  font-size: 13px;
  color: #666;
  margin-right: auto;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 16px;
}

.seller-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.seller-name {
  font-size: 14px;
  color: #333;
}

.order-status {
  font-size: 14px;
  font-weight: 500;
}

.order-status.status-closed {
  color: #8c8c8c;
}

.order-status.status-success {
  color: #52c41a;
}

.order-status.status-payment {
  color: #f5222d;
}

.order-status.status-shipment {
  color: #1890ff;
}

.order-status.status-receipt {
  color: #faad14;
}

.order-status.status-review {
  color: #ff6b00;
}

.order-status.status-refund {
  color: #722ed1; /* 紫色表示售后 */
  font-weight: bold;
}

.order-body {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.product-image {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}

.product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.product-title {
  font-size: 15px;
  color: #333;
  line-height: 1.5;
  margin-bottom: 8px;
}

.product-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #999;
  margin-bottom: auto;
}

.product-price {
  font-size: 18px;
  font-weight: 600;
  color: #ff6b00;
  margin-top: 8px;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.empty-state {
  background-color: white;
  border-radius: 8px;
  padding: 48px 24px;
  text-align: center;
}

.empty-icon {
  font-size: 60px;
  margin: 0 auto 16px;
  color: #d9d9d9;
}

.empty-state p {
  color: #8c8c8c;
  font-size: 14px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 16px;
  background-color: white;
  border-radius: 8px;
}
</style>
