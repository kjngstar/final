<script setup>
import { ref, reactive } from 'vue'
// import { Search, Plus, Delete, Edit } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import { regionData } from 'element-china-area-data'
import { Search, Plus, Delete, Edit, Check, Close } from '@element-plus/icons-vue' // 引入图标


// 表格数据
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

// 搜索条件
const searchForm = reactive({
  keyword: '',
})

// 表单数据
const form = ref({})
const dialogFormVisible = ref(false)
const multipleSelection = ref([])

// 用户信息
const account = ref(localStorage.getItem('account') ? JSON.parse(localStorage.getItem('account')) : {})

// 加载数据
const load = () => {
  request.get("/orders/page", {
    params: {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchForm.keyword,
    }
  }).then(res => {
    if (res.data) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  })
}
load()

// 保存
const save = () => {

  if (form.value.address) {
      form.value.address = form.value.address.join('/');
  } else {
      form.value.address = '';
  }

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

// 添加
const handleAdd = () => {


  form.value = {}
  dialogFormVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  form.value = JSON.parse(JSON.stringify(row))

  if (form.value.address) {
      form.value.address = form.value.address.split('/');
  } else {
      form.value.address = [];
  }

  dialogFormVisible.value = true
}

// 删除
const del = (id) => {
  request.delete("/orders/" + id).then(res => {
    if (res.code === '200') {
      ElMessage.success("删除成功")
      load()
    } else {
      ElMessage.error("删除失败")
    }
  })
}

// 批量删除
const delBatch = () => {
  if (multipleSelection.value.length === 0) {
    ElMessage.warning("请至少选择一条记录")
    return
  }

  const ids = multipleSelection.value.map(v => v.id)
  request.post("/orders/del/batch", ids).then(res => {
    if (res.code === '200') {
      ElMessage.success("批量删除成功")
      load()
    } else {
      ElMessage.error("批量删除失败")
    }
  })
}

// 重置搜索
const reset = () => {
  searchForm.keyword = ""
  load()
}



// 管理员审核售后逻辑
const handleAudit = (row, targetStatus) => {
  // 根据目标状态决定调用哪个接口
  const url = targetStatus === '交易关闭' ? '/orders/refund/approve/' : '/orders/refund/reject/';
  const actionText = targetStatus === '交易关闭' ? '通过并退款' : '驳回';

  ElMessageBox.confirm(
      `确定要${actionText}该订单的售后申请吗？`,
      '审核确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
  ).then(() => {
    // 使用 GET 请求调用专用的审核接口
    request.get(url + row.id).then(res => {
      if (res.code === '200') {
        ElMessage.success(`已成功${actionText}`);
        load(); // 刷新表格，数据将保留且不再是空行
      } else {
        ElMessage.error(res.msg || "操作失败");
      }
    });
  }).catch(() => {});
}


// 表格选择变化
const handleSelectionChange = (val) => {
  multipleSelection.value = val
}

// 分页大小变化
const handleSizeChange = (size) => {
  pageSize.value = size
  load()
}

// 页码变化
const handleCurrentChange = (current) => {
  pageNum.value = current
  load()
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

// 确认批量删除
const confirmBatchDelete = () => {
  if (multipleSelection.value.length === 0) {
    ElMessage.warning("请至少选择一条记录")
    return
  }

  ElMessageBox.confirm(
      '确定要批量删除这些数据吗？',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
  )
      .then(() => {
        delBatch()
      })
}



const goodss = ref([]);
const loadGoods = () => {
  request.get('/goods').then(res => {
    goodss.value = res.data;
  });
};
loadGoods();

const users = ref([]);
const loadUser = () => {
  request.get('/user').then(res => {
    users.value = res.data;
  });
};
loadUser();



</script>

<template>
  <div class="content-container">

    <!-- 搜索区域 -->
    <div class="header-section">
      <el-input v-model="searchForm.keyword" placeholder="请输入订单号" class="filter-input" :prefix-icon="Search" clearable/>
      <el-button class="ml-10" plain type="primary" @click="load">搜索</el-button>
      <el-button plain type="info" @click="reset">重置</el-button>
    </div>

    <!-- 操作按钮区域 -->
    <div class="toolbar-section">
      <el-button plain type="primary" @click="handleAdd" :icon="Plus">新增</el-button>
      <el-button plain type="danger" @click="confirmBatchDelete" :icon="Delete">批量删除</el-button>
    </div>

    <!-- 表格区域 -->
    <el-card>
      <el-table :data="tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="60" align="center"/>
        <el-table-column prop="id" label="ID" width="80" align="center"/>
        <el-table-column prop="no" label="订单号"/>
        <el-table-column label="商品" width="100" align="center">
          <template #default="scope">
            <span v-if="scope.row.itemId">{{ goodss.find(item => item.id === scope.row.itemId)?.name }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="itemName" label="商品名称(快照)" width="100" align="center"></el-table-column>

        <el-table-column label="商品图片(快照)" width="100" align="center">
          <template #default="scope">
            <el-image style="width: 80px; height: 80px" :src="scope.row.itemImg" :preview-src-list="[scope.row.itemImg]" :preview-teleported=true></el-image>
          </template>
        </el-table-column>

        <el-table-column label="卖家" width="100" align="center">
          <template #default="scope">
            <span v-if="scope.row.fromId">{{ users.find(item => item.id === scope.row.fromId)?.nickname }}</span>
          </template>
        </el-table-column>
        <el-table-column label="买家" width="100" align="center">
          <template #default="scope">
            <span v-if="scope.row.toId">{{ users.find(item => item.id === scope.row.toId)?.nickname }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格"/>
        <el-table-column prop="time" label="支付时间" width="140" align="center"/>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === '售后中' ? 'danger' : 'success'">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
            prop="refundReason"
            label="售后原因"
            show-overflow-tooltip
            v-if="tableData.some(item => item.refundReason)"
        />
        <el-table-column label="买家评分" width="100" align="center">
          <template #default="scope">
            <el-rate v-model="scope.row.toRate" disabled />
          </template>
        </el-table-column>
        <el-table-column prop="toReview" label="买家评价"/>
        <el-table-column prop="address" label="省市区"/>
        <el-table-column prop="info" label="详细地址"/>
        <el-table-column prop="name" label="被服务人姓名"/>
        <el-table-column prop="phone" label="被服务人人联系方式"/>

        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="scope">
            <template v-if="scope.row.status === '售后中'">
              <el-tooltip content="通过售后(退款)" placement="top">
                <el-button circle type="success" :icon="Check" @click="handleAudit(scope.row, '交易关闭')"/>
              </el-tooltip>
              <el-tooltip content="驳回售后" placement="top">
                <el-button circle type="warning" :icon="Close" @click="handleAudit(scope.row, '交易完成')"/>
              </el-tooltip>
            </template>

            <el-tooltip content="编辑" placement="top" :effect="'light'">
              <el-button circle type="primary" :icon="Edit" @click="handleEdit(scope.row)"/>
            </el-tooltip>

            <el-tooltip content="删除" placement="top" :effect="'light'">
              <el-button circle type="danger" :icon="Delete" @click="confirmDelete(scope.row.id)"/>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页区域 -->
      <div class="pagination-section">
        <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 表单对话框 -->
    <el-dialog v-model="dialogFormVisible" :title="form.id ? '编辑' : '新增'" width="30%" center>
      <el-form :model="form" label-width="100px">
        <el-form-item label="订单号">
          <el-input v-model="form.no" placeholder="请输入订单号"/>
        </el-form-item>
        <el-form-item label="商品">
          <el-select v-model="form.itemId" placeholder="请选择商品" style="width: 220px">
            <el-option v-for="item in goodss" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="卖家">
          <el-select v-model="form.fromId" placeholder="请选择卖家" style="width: 220px">
            <el-option v-for="item in users" :key="item.id" :label="item.nickname" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="买家">
          <el-select v-model="form.toId" placeholder="请选择买家" style="width: 220px">
            <el-option v-for="item in users" :key="item.id" :label="item.nickname" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格">
          <el-input v-model="form.price" type="number" placeholder="请输入价格"/>
        </el-form-item>
        <el-form-item label="支付时间">
          <el-date-picker v-model="form.time" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择日期时间"></el-date-picker>
        </el-form-item>
        <el-form-item label="状态">
          <el-input v-model="form.status" placeholder="请输入状态"/>
        </el-form-item>
        <el-form-item label="买家评分">
          <el-rate v-model="form.toRate"/>
        </el-form-item>
        <el-form-item label="买家评价">
          <el-input v-model="form.toReview" type="textarea" placeholder="请输入买家评价"/>
        </el-form-item>
        <el-form-item label="省市区">
          <el-cascader v-model="form.address" :options="regionData" :props="{ value: 'label' }" clearable style="width: 100%" placeholder="请输入省市区"/>
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="form.info" type="textarea" placeholder="请输入详细地址"/>
        </el-form-item>
        <el-form-item label="被服务人姓名">
          <el-input v-model="form.name" placeholder="请输入被服务人姓名"/>
        </el-form-item>
        <el-form-item label="被服务人联系方式">
          <el-input v-model="form.phone" placeholder="请输入被服务人联系方式"/>
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


</style>