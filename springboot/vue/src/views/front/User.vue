<script setup>
import {onMounted, ref, watch,shallowRef} from 'vue'
import {
  Camera,
  Star,
  Clock,
  Guide,
  Ticket,
  LocationInformation,
  ArrowLeft,
  StarFilled,
  UploadFilled, Delete
} from '@element-plus/icons-vue'
import request from '@/utils/request.js'
import { useRoute, useRouter } from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
const route = useRoute()
const router = useRouter()
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import axios from "axios"
import { regionData } from 'element-china-area-data'
import {serverHost} from "../../../config/config.default.js";
let id = route.query.id || ''


const activeTab = ref('服务')
// 用户信息
const account = ref(
    localStorage.getItem('account') ? JSON.parse(localStorage.getItem('account')) : {}
)
// 获取用户信息
const user = ref({})
const loadUser = () => {
  request.get('/user/'+id).then(res => {
    user.value = res.data;
  });
};

const goods = ref([])
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const reviews = ref([])
//获得订单
const loadGoods = () => {
  request.get("/goods/user/" + id).then(res => {
    goods.value = res.data
  })
}

// 获取用户评价
const loadReviews = () => {
  request.get("/orders/user/" + id).then(res => {
    reviews.value = res.data
  })
}


const users = ref([]);
const loadUsers = () => {
  request.get('/user').then(res => {
    users.value = res.data;
  });
};


// 切换标签
const handleTabChange = (tab) => {
  activeTab.value = tab
  if (tab === '历史评价') {
    loadReviews()
  }
}

// 保存
const save = () => {

  if (imgList.value.length > 0) {
    form.value.imgList = imgList.value.join(',');
  } else {
    form.value.imgList = '';
  }
  form.value.content = htmlContent.value;
  if (form.value.place) {
    form.value.place = form.value.place.join('/');
  } else {
    form.value.place = '';
  }

  request.post("/goods", form.value).then(res => {
    if (res.code === '200') {
      ElMessage.success("保存成功")
      dialogFormVisible.value = false
      loadGoods()
    } else {
      ElMessage.error("保存失败")
    }
  })
}
// 表单数据
const form = ref({})
const dialogFormVisible = ref(false)
// 编辑
const handleEdit = (item) => {
  form.value = JSON.parse(JSON.stringify(item))

  if (form.value.imgList) {
    imgList.value = form.value.imgList.split(',');
  } else {
    imgList.value = [];
  }
  htmlContent.value = form.value.content || '';
  if (form.value.place) {
    form.value.place = form.value.place.split('/');
  } else {
    form.value.place = [];
  }

  dialogFormVisible.value = true
}
// 删除
const del = (id) => {
  request.delete("/goods/" + id).then(res => {
    if (res.code === '200') {
      ElMessage.success("删除成功")
      loadGoods()
    } else {
      ElMessage.error("删除失败")
    }
  })
}
// 确认删除
const confirmDelete = (id) => {
  ElMessageBox.confirm(
      '确定要删除这条数据吗？',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
  )
      .then(() => {
        del(id)
      })
}
// 商品主图上传
const handleImgUploadSuccess = (res) => {
  form.value.img = res;
};

// 更多图片列表
const imgList = ref([])

// 多图片上传成功处理
const handleImgListUploadSuccess = (res) => {
  imgList.value.push(res);
};

// 删除已上传的图片
const removeImgList = (index) => {
  imgList.value.splice(index, 1);
};

// 商品详情描述富文本
const htmlContent = ref('');
const editorRefContent = shallowRef();

// 获取表格中显示的图片列表
const getImageList = (imgString) => {
  if (!imgString) return [];
  return imgString.split(',');
};





// 自定义上传方法
const customUpload = (file, insertFn) => {
  const formData = new FormData()
  formData.append('file', file)
  axios({
    url: `${serverHost}/web/upload`,
    method: 'post',
    data: formData,
    headers: {'Content-Type': 'multipart/form-data'},
  }).then(res => {
    insertFn(res.data)
  }).catch((error) => {
    console.error('上传失败:', error)
    ElMessage.error('上传失败')
  })
}

// wangEditor 配置
const editorConfig = {
  placeholder: '请输入内容...',
  MENU_CONF: {
    uploadImage: {
      customUpload: async (file, insertFn) => {
        customUpload(file, insertFn)
      },
    },
    uploadVideo: {
      customUpload: async (file, insertFn) => {
        customUpload(file, insertFn)
      },
    },
  }
}

const types = ref([]);
const loadType = () => {
  request.get('/type').then(res => {
    types.value = res.data;
  });
};



const initData = () => {
  if (id) {
    loadUser()
    loadGoods()
    loadReviews()
    loadUsers()
    loadType()
  }
}

watch(() => route.query.id, (newId) => {
  if (newId) {
    id = newId
    activeTab.value = '服务'
    initData()
  }
})

  onMounted(() => {
    initData()
  })

</script>

<template>
  <div class="user-page">
    <!-- 用户信息头部 -->
    <div class="user-header">
      <div class="user-info">
        <img :src="user.avatarUrl" alt="avatar" class="avatar" />
        <div class="user-details">
          <div class="username-row">
            <span class="username">{{ user.nickname }}</span>
          </div>
        </div>
      </div>

      <div class="header-actions">
        <el-button size="large" type="primary" @click="router.push('/front/chat?userId='+id)" v-if="user.id !== account.id"  >去私聊</el-button>
        <el-button size="large" type="success" v-if="user.id === account.id" @click="router.push('/front/person')">编辑资料</el-button>
      </div>

    </div>

    <!-- 标签栏 -->
    <div class="tabs">
      <div
          :class="['tab-item', { active: activeTab === '服务' }]"
          @click="handleTabChange('服务')"
      >
        服务 <span class="count">{{ goods.length }}</span>
      </div>
      <div
          :class="['tab-item', { active: activeTab === '历史评价' }]"
          @click="handleTabChange('历史评价')"
      >
        历史评价 <span class="count">{{ reviews.length }}</span>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="content" >
      <!-- 列表 -->
      <div v-if="activeTab === '服务'" class="goods-grid">
        <div
            v-for="item in goods"
            :key="item.id"
            class="goods-card">

          <div class="goods-image-wrapper" @click="router.push('/front/goodsDetail?id=' + item.id)">
            <img :src="item.img" :alt="item.name" class="goods-image" />
            <!-- 已售出标签 -->
            <div v-if="item.status === '已售出'" class="sold-overlay">
              <div class="sold-badge">
                <img src="../../assets/已售出.png" alt="已售出" />
              </div>
            </div>

          </div>
          <div class="goods-info">
            <div class="goods-title">
              <span class="good-shipment" >
              {{ item.shipment }}
            </span>
              {{ item.name }}</div>
            <div class="goods-footer">
              <div class="price-row">
                <span class="current-price">¥{{ item.price }}</span>
                <span v-if="item.rePrice" class="original-price">¥{{ item.rePrice }}</span>
              </div>
              <div class="seller-info">
                <img :src="user.avatarUrl" :alt="user.nickname" class="seller-avatar" />
                <span class="seller-name">{{ user.nickname }}</span>
              </div>
            </div>

          </div>
          <div class="goods-manager" v-if="user.id===account.id">
            <el-button type="warning" @click="handleEdit(item)" v-if="item.status==='已上架'">编辑</el-button>
            <el-button type="danger"  @click="confirmDelete(item.id)" v-if="item.status==='已上架'">删除</el-button>
          </div>

        </div>


      </div>

      <!-- 信用及评价 -->
      <div v-if="activeTab === '历史评价'" class="reviews-section">
        <div v-if="reviews.length === 0" class="empty-state">
          暂无评价
        </div>
        <div v-else class="reviews-list">
          <div v-for="review in reviews" :key="review.id" class="review-item">
            <div class="review-header">
              <img :src="users.find(i => i.id === review.toId)?.avatarUrl" class="reviewer-avatar" />
              <div class="reviewer-info">
                <div class="reviewer-name">{{ users.find(i => i.id === review.toId)?.nickname }}</div>
                <div class="review-time">{{ review.time }}</div>
              </div>
              <el-rate v-model="review.toRate" disabled />
            </div>
            <div class="review-content">{{ review.toReview }}</div>
          </div>
        </div>
      </div>
    </div>


    <el-dialog v-model="dialogFormVisible" title="编辑" width="40%" center>
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="请输入名称"/>
        </el-form-item>
        <el-form-item label="商品主图">
          <div class="upload-container">
            <el-avatar v-if="form.img" :src="form.img" :size="80" />
            <el-upload :action="`${serverHost}/web/upload`" :on-success="handleImgUploadSuccess" :show-file-list="false">
              <el-button type="primary" :icon="UploadFilled">{{ form.img ? '更换图片' : '上传图片' }}</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="更多图片">
          <div class="upload-container">
            <div class="image-list" v-if="imgList.length > 0">
              <div v-for="(img, index) in imgList" :key="index" class="image-item">
                <el-avatar :src="img" :size="80"/>
                <el-button type="danger" circle :icon="Delete" class="delete-btn" @click="removeImgList(index)"></el-button>
              </div>
            </div>
            <el-upload :action="`${serverHost}/web/upload`" :on-success="handleImgListUploadSuccess" :show-file-list="false" multiple>
              <el-button type="primary" :icon="UploadFilled">上传图片</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.typeId" placeholder="请选择分类" style="width: 220px">
            <el-option v-for="item in types" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="售价">
          <el-input v-model="form.price" type="number" placeholder="请输入售价"/>
        </el-form-item>
        <el-form-item label="行业常规参考价">
          <el-input v-model="form.rePrice" type="number" placeholder="请输入原价"/>
        </el-form-item>
        <el-form-item label="服务详情描述">
          <div style="border: 1px solid #ccc; z-index: 100;">
            <Toolbar style="border-bottom: 1px solid #ccc" :editor="editorRefContent" :defaultConfig="editorConfig" mode="default" />
            <Editor style="height: 300px; overflow-y: hidden;" v-model="htmlContent" :defaultConfig="editorConfig" mode="default" @onCreated="editorRefContent = $event" />
          </div>
        </el-form-item>
        <el-form-item label="所在地">
          <el-cascader v-model="form.place" :options="regionData" :props="{ value: 'label' }" clearable style="width: 100%" placeholder="请输入商品所在地"/>
        </el-form-item>
        <el-form-item label="服务设置">
          <el-radio-group v-model="form.shipment">
            <el-radio value="无需路费" border>无需路费</el-radio>
            <el-radio value="需要路费" border>需要路费</el-radio>
            <el-radio value="线上服务" border>线上服务</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="服务技能水平">
          <el-radio-group v-model="form.quality">
            <el-radio :value="'入门经验'">入门经验</el-radio>
            <el-radio :value="'1年以内'">1年以内</el-radio>
            <el-radio :value="'1-3年'">1-3年</el-radio>
            <el-radio :value="'3-5年'">3-5年</el-radio>
            <el-radio :value="'5年以上'">5年以上</el-radio>
          </el-radio-group>
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
</template>

<style scoped>

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
}

.image-item {
  position: relative;
}

.delete-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  transform: scale(0.8);
}

.user-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.user-header {
  background: white;
  border-radius: 12px;
  padding: 30px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 1;
}
.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #f0f0f0;
}

.user-details {
  flex: 1;
}

.username-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}


.tabs {
  background: white;
  border-radius: 12px;
  padding: 0 30px;
  margin-bottom: 20px;
  display: flex;
  gap: 40px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.tab-item {
  padding: 20px 0;
  font-size: 16px;
  color: #666;
  cursor: pointer;
  position: relative;
  transition: color 0.3s;
}

.tab-item:hover {
  color: #333;
}

.tab-item.active {
  color: #333;
  font-size: 18px;
  font-weight: bold;
  text-decoration: underline;
  text-decoration-color: #ffe610;
  text-decoration-thickness: 8px;
  text-underline-offset: -4px;
}

.count {
  margin-left: 4px;
  font-size: 14px;
}

.content {
  min-height: 400px;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.goods-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.goods-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.goods-image-wrapper {
  position: relative;
  width: 100%;
  padding-bottom: 100%;
  overflow: hidden;
  background: #f5f5f5;
}

.goods-image {
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

.goods-info {
  padding: 12px;
}

.goods-title {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  line-height: 1.4;
  min-height: 20px;
}

.good-shipment{
  font-size: 18px;
  font-weight: bold;
  text-decoration: underline;
  text-decoration-color: #ffe610;
  text-decoration-thickness: 8px;
  text-underline-offset: -4px;
}

.goods-footer {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.current-price {
  font-size: 20px;
  font-weight: 600;
  color: #ff4d4f;
}

.original-price {
  font-size: 14px;
  color: #999;
  text-decoration: line-through;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.seller-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
}

.seller-name {
  font-size: 12px;
  color: #666;
}

.goods-manager{
  display: flex;
  align-items: center;
  justify-content: space-around;
  margin-bottom:10px ;
}

/* 评价区域 */
.reviews-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 16px;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.review-item {
  padding: 20px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.review-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.reviewer-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.reviewer-info {
  flex: 1;
}

.reviewer-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.review-time {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.review-content {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}


</style>
