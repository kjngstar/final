<script setup>
import {ref, onMounted} from 'vue'
import request from '@/utils/request.js'
import {ElMessage, ElMessageBox} from 'element-plus'
import { regionData } from 'element-china-area-data'

const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(-1)

const load = () => {
  request.get("/address/page", {
    params: {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: '',
    }
  }).then(res => {
    if (res.data) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  })
}
const form = ref({})
const dialogFormVisible = ref(false)

const handleAdd = () => {

  form.value = {}
  dialogFormVisible.value = true
}

const handleEdit = (row) => {
  form.value = JSON.parse(JSON.stringify(row))

  if (form.value.address) {
    form.value.address = form.value.address.split('/');
  } else {
    form.value.address = [];
  }

  dialogFormVisible.value = true
}

const del = (id) => {
  request.delete("/address/" + id).then(res => {
    if (res.code === '200') {
      ElMessage.success("删除成功")
      load()
    } else {
      ElMessage.error("删除失败")
    }
  })
}


const save = () => {

  if (form.value.address) {
    form.value.address = form.value.address.join('/');
  } else {
    form.value.address = '';
  }

  request.post("/address", form.value).then(res => {
    if (res.code === '200') {
      ElMessage.success("保存成功")
      dialogFormVisible.value = false
      load()
    } else {
      ElMessage.error("保存失败")
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


onMounted(() => {
  load()
})

</script>

<template>
  <div class="address-page">
    <div class="header">
      <h1>地址管理</h1>
      <button @click="handleAdd" class="add-btn">+ 新增地址</button>
    </div>

    <div class="address-list">
      <div
          v-for="address in tableData"
          :key="address.id"
          class="address-card"
      >
        <div class="card-header">
          <div class="user-info">
            <span class="name">{{ address.name }}</span>
            <span class="phone">{{ address.phone }}</span>

          </div>
          <div class="actions">
            <button @click="handleEdit(address)" class="action-btn edit">编辑</button>
            <button @click="confirmDelete(address.id)" class="action-btn delete">删除</button>
          </div>
        </div>

        <div class="address-detail">
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
            <circle cx="12" cy="10" r="3"></circle>
          </svg>
          <span>{{ address.address }} {{ address.info }}</span>
        </div>

      </div>

      <div v-if="tableData.length === 0" class="empty-state">
        <p>暂无地址</p>
        <button @click="handleAdd" class="add-btn-secondary">添加地址</button>
      </div>
    </div>

    <!-- 对话框 -->
    <el-dialog v-model="dialogFormVisible" :title="form.id ? '编辑地址' : '新增地址'" width="40%" center>
      <el-form :model="form" label-width="100px">
        <el-form-item label="省市区">
          <el-cascader v-model="form.address" :options="regionData" :props="{ value: 'label' }" clearable style="width: 100%" placeholder="请选择省市区"/>
        </el-form-item>
        <el-form-item label="具体地址">
          <el-input v-model="form.info" type="textarea" placeholder="请输入具体地址"/>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="请输入名称"/>
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="请输入联系电话"/>
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
.address-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  background: #f5f5f5;
  min-height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: white;
  padding: 20px 24px;
  border-radius: 8px;
}

.header h1 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.add-btn {
  background: #ff6b00;
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.3s;
}

.add-btn:hover {
  background: #e55f00;
}

.address-list {
  display: grid;
  gap: 16px;
}

.address-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  border: 2px solid transparent;
  transition: all 0.3s;
}

.address-card.is-default {
  border-color: #ff6b00;
  background: #fff8f0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.phone {
  font-size: 14px;
  color: #666;
}

.actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  border: none;
  background: none;
  color: #666;
  cursor: pointer;
  font-size: 14px;
  padding: 4px 8px;
  transition: color 0.3s;
}

.action-btn.edit:hover {
  color: #ff6b00;
}

.action-btn.delete:hover {
  color: #ff4444;
}

.address-detail {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 12px;
}

.icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  margin-top: 2px;
  color: #ff6b00;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 8px;
}

.empty-state p {
  color: #999;
  font-size: 16px;
  margin-bottom: 24px;
}

.add-btn-secondary {
  background: #ff6b00;
  color: white;
  border: none;
  padding: 10px 32px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}


.dialog-header h2 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.form-group input[type="text"],
.form-group input[type="tel"],
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
  transition: border-color 0.3s;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #ff6b00;
}


.checkbox-label input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

</style>
