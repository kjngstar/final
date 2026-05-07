<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { StarFilled,ChatDotSquare } from '@element-plus/icons-vue';
const router = useRouter()
const goods = ref([])
// 响应式数据
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const load = () => {
  request.get("/goods/collect/page", {
    params: {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value,
    }
  }).then(res => {
    if (res.code === '200') {
      goods.value = res.data.records || []
      total.value = res.data?.total || 0
    }
  })
}


const reset = () => {
  keyword.value = ""
  load()
}

const handleSizeChange = (newPageSize) => {
  pageSize.value = newPageSize
  load()
}

const handleCurrentChange = (newPageNum) => {
  pageNum.value = newPageNum
  load()
}

const cancel = (id) => {
  request.delete('/collect/' + id).then(res => {
    if (res.code === '200') {
      ElMessage.success('取消收藏成功')
      load()
    }
  })
}


onMounted(() => {
  load()
})
</script>

<template>
  <div class="collect-page">
    <div class="header">
      <h2>我的收藏</h2>
    </div>
    <div style="margin: 10px 0">
      <el-input style="width: 400px" placeholder="输入商品名称进行搜索" v-model="keyword"></el-input>
      <el-button style="margin-left: 5px" type="primary" plain @click="load">搜索</el-button>
      <el-button type="info" plain @click="reset">重置</el-button>
    </div>
    <el-divider></el-divider>
    <div v-if="goods.length === 0" class="empty">
      <p>暂无收藏商品</p>
    </div>

    <div v-else class="goods-grid">
      <div
          v-for="item in goods"
          :key="item.id"
          class="goods-card"
      >
        <div class="image-container">
          <img :src="item.img || '/placeholder.svg?height=200&width=200'" :alt="item.name"/>

          <!-- 已售出标签 -->
          <div v-if="item.status === '已售出'" class="sold-overlay">
            <div class="sold-badge">
              <img src="../../assets/已售出.png" alt="已售出" />
            </div>
          </div>

          <div class="hover-actions">
            <button class="action-btn uncollect" @click="cancel(item.id)">
              <el-icon><StarFilled /></el-icon>
              取消收藏
            </button>
            <!-- 如果商品未下架，显示"去聊聊"按钮 -->
            <button
                v-if="item.status !== '已售出'"
                class="action-btn chat"
                @click.stop="router.push('/front/chat?userId='+item.userId)"
            >
              <el-icon><ChatDotSquare /></el-icon>
              我想要
            </button>
          </div>
        </div>

        <div class="goods-info"   @click="router.push('/front/goodsDetail?id=' + item.id)">
          <div class="goods-name" >{{ item.name || '商品名称' }}</div>
          <div class="price-section">
            <span class="current-price">¥{{ item.price || 0 }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="pagination-wrapper">
      <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="pageNum"
          :page-sizes="[4, 8, 12, 16]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total">
      </el-pagination>
    </div>
  </div>
</template>


<style scoped>
.collect-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  margin-bottom: 24px;
}

.header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

 .empty {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 16px;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.goods-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.goods-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.image-container {
  position: relative;
  width: 100%;
  padding-top: 100%;
  overflow: hidden;
  background: #f5f5f5;
}

.image-container img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sold-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(150, 147, 147, 0.6);
  z-index: 1;
  pointer-events: none;
}

.sold-badge {
  position: absolute;
  top: 30%;
  left: 30%;
  width: 130px;
  height: 130px;
  z-index: 1;
  pointer-events: none;
}

.sold-badge img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.hover-actions {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 0;
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.3s ease;
  z-index: 2;
}

.goods-card:hover .hover-actions {
  opacity: 1;
  transform: translateY(0);
}
.action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px;
  border: none;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  background: rgba(0, 0, 0, 0.35);
  color: white;
  backdrop-filter: blur(8px);
}

.action-btn:hover {
  background: rgba(0, 0, 0, 0.55);
}

.action-btn.uncollect svg {
  fill: currentColor;
}

.action-btn.chat {
  border-left: 1px solid rgba(255, 255, 255, 0.2);
}

.goods-info {
  padding: 12px;
  cursor: pointer;
}

.goods-name {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
  min-height: 40px;
}

.price-section {
  display: flex;
  align-items: center;
  gap: 8px;
}

.current-price {
  font-size: 18px;
  font-weight: 600;
  color: #ff4d4f;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 32px 0;
}

</style>
