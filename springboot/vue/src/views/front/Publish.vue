<script setup>
import { ref, computed, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete, UploadFilled } from '@element-plus/icons-vue'
import request from '../../utils/request'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import axios from 'axios'
import { regionData } from 'element-china-area-data'
import {serverHost} from "../../../config/config.default.js";

const form = ref({
  quality: '全新',
  shipment: '包邮'
})

const types = ref([])
const loadType = () => {
  request.get('/type').then(res => {
    types.value = res.data
  })
}
loadType()


const imgList = ref([])

const handleImgUploadSuccess = (res) => {
  form.value.img = res
}

const handleImgListUploadSuccess = (res) => {
  imgList.value.push(res)
}

const removeImgList = (index) => {
  imgList.value.splice(index, 1)
}

const htmlContent = ref('')
const editorRefContent = shallowRef()

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

const editorConfig = {
  placeholder: '请输入服务详情...',
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

const handleSubmit = () => {
  // 表单验证
  if (!form.value.img) {
    ElMessage.warning('请上传主图')
    return
  }
  if (!form.value.name) {
    ElMessage.warning('请输入服务名称')
    return
  }
  if (!form.value.typeId) {
    ElMessage.warning('请选择服务分类')
    return
  }
  if (!form.value.price) {
    ElMessage.warning('请输入售价')
    return
  }

  const submitData = {
    ...form.value,
    imgList: imgList.value.length > 0 ? imgList.value.join(',') : '',
    content: htmlContent.value,
    place: form.value.place.length > 0 ? form.value.place.join('/') : ''
  }

  request.post('/goods', submitData).then(res => {
    if (res.code === '200') {
      ElMessage.success('发布成功！')
      // 重置表单
      form.value = {
        shipment: '包邮',
        quality: '全新'
      }
      imgList.value = []
      htmlContent.value = ''
    } else {
      ElMessage.error('发布失败')
    }
  })
}
</script>

<template>
  <div class="publish-container">
    <div class="publish-card">
      <div class="page-title">发布</div>

      <div class="section">
        <div class="section-title">基础信息</div>

        <div class="form-item">
          <div class="label required">主图</div>
          <div class="upload-container">
            <el-avatar v-if="form.img" :src="form.img" :size="100" />
            <el-upload
                :action="`${serverHost}/web/upload`"
                :on-success="handleImgUploadSuccess"
                :show-file-list="false"
            >
              <el-button type="primary" :icon="UploadFilled">
                {{ form.img ? '更换图片' : '上传图片' }}
              </el-button>
            </el-upload>
          </div>
        </div>

        <div class="form-item">
          <div class="label">更多图片</div>
          <div class="upload-container">
            <div class="image-list" v-if="imgList.length > 0">
              <div v-for="(img, index) in imgList" :key="index" class="image-item">
                <el-avatar :src="img" :size="100"/>
                <el-button
                    type="danger"
                    circle
                    :icon="Delete"
                    class="delete-btn"
                    @click="removeImgList(index)"
                />
              </div>
            </div>
            <el-upload
                :action="`${serverHost}/web/upload`"
                :on-success="handleImgListUploadSuccess"
                :show-file-list="false"
                multiple
            >
              <el-button type="primary" :icon="UploadFilled">上传图片</el-button>
            </el-upload>
          </div>
          <div class="tip">最多上传4张图片</div>
        </div>

        <div class="form-item">
          <div class="label required">服务名称</div>
          <el-input
              v-model="form.name"
              placeholder="简洁清晰的标题会吸引更多人"
              maxlength="100"
              show-word-limit
          />
        </div>

        <div class="form-item">
          <div class="label required">服务分类</div>
          <el-select
              v-model="form.typeId"
              placeholder="请选择服务分类"
              style="width: 100%"
          >
            <el-option
                v-for="item in types"
                :key="item.id"
                :label="item.name"
                :value="item.id"
            />
          </el-select>
        </div>


        <div class="form-item">
          <div class="label">服务技能水平</div>
          <el-radio-group v-model="form.quality">
            <el-radio :value="'入门经验'">入门经验</el-radio>
            <el-radio :value="'1年以内'">1年以内</el-radio>
            <el-radio :value="'1-3年'">1-3年</el-radio>
            <el-radio :value="'3-5年'">3-5年</el-radio>
            <el-radio :value="'5年以上'">5年以上</el-radio>
          </el-radio-group>
        </div>

        <div class="form-item">
          <div class="label">服务详情描述</div>
          <div class="editor-wrapper">
            <Toolbar
                class="editor-toolbar"
                :editor="editorRefContent"
                :defaultConfig="editorConfig"
                mode="default"
            />
            <Editor
                class="editor-content"
                v-model="htmlContent"
                :defaultConfig="editorConfig"
                mode="default"
                @onCreated="editorRefContent = $event"
            />
          </div>
        </div>

        <div class="form-item">
          <div class="label">所在地</div>
          <el-cascader
              v-model="form.place"
              :options="regionData"
              :props="{ value: 'label' }"
              clearable
              style="width: 100%"
              placeholder="请选择所在地"
          />
        </div>

        <!-- 发货设置 -->
        <div class="form-item">
          <div class="label">服务设置</div>
          <el-radio-group v-model="form.shipment">
            <el-radio value="无需路费" border>无需路费</el-radio>
            <el-radio value="需要路费" border>需要路费</el-radio>
            <el-radio value="线上服务" border>线上服务</el-radio>
          </el-radio-group>
        </div>
      </div>

      <!-- 价格信息 -->
      <div class="section">
        <div class="section-title">价格</div>

        <div class="price-row">
          <div class="form-item flex-1">
            <div class="label required">售价</div>
            <el-input
                v-model="form.price"
                placeholder="0.00"
                type="number"
            >
              <template #prefix>
                <span class="price-symbol">¥</span>
              </template>
            </el-input>
          </div>


          <div class="form-item flex-1">
            <div class="label">行业常规参考价</div>
            <el-input
                v-model="form.rePrice"
                placeholder="0.00"
                type="number"
            >
              <template #prefix>
                <span class="price-symbol">¥</span>
              </template>
            </el-input>
          </div>
        </div>
      </div>

      <!-- 发布按钮 -->
      <div class="submit-section">
        <el-button
            type="warning"
            size="large"
            class="submit-btn"
            @click="handleSubmit"
        >
          发布
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.publish-container {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 40px 20px;
}

.publish-card {
  max-width: 900px;
  margin: 0 auto;
  background: white;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 40px;
}

.section {
  margin-bottom: 40px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
}

.form-item {
  margin-bottom: 24px;
}

.label {
  font-size: 14px;
  color: #606060;
  margin-bottom: 12px;
  font-weight: 500;
}

.label.required::after {
  content: '*';
  color: #ff4d4f;
  margin-left: 4px;
}

/* 上传容器样式 */
.upload-container {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 图片列表样式 */
.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
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

/* 富文本编辑器样式 */
.editor-wrapper {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
}

.editor-toolbar {
  border-bottom: 1px solid #dcdfe6;
  background: #fafafa;
}

.editor-content {
  height: 300px;
  overflow-y: auto;
}

.tip {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}

.price-row {
  display: flex;
  gap: 20px;
}

.flex-1 {
  flex: 1;
}

.price-symbol {
  color: #606060;
  font-weight: 500;
}

:deep(.el-input__inner) {
  border-radius: 8px;
}

:deep(.el-textarea__inner) {
  border-radius: 8px;
}

:deep(.el-select) {
  border-radius: 8px;
}

:deep(.el-radio.is-bordered) {
  border-radius: 8px;
  margin-right: 12px;
  margin-bottom: 12px;
}

.submit-section {
  margin-top: 48px;
  display: flex;
  justify-content: center;
}

.submit-btn {
  width: 400px;
  height: 48px;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #ffd966 0%, #ffb84d 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(255, 184, 77, 0.3);
  transition: all 0.3s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 184, 77, 0.4);
}

.submit-btn:active {
  transform: translateY(0);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .publish-card {
    padding: 24px;
  }

  .page-title {
    font-size: 24px;
  }

  .price-row {
    flex-direction: column;
    gap: 0;
  }

  .submit-btn {
    width: 100%;
  }

  .image-list {
    gap: 8px;
  }
}
</style>
