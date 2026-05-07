<script setup>
import { ref, shallowRef, onMounted } from 'vue'
import request from '../../utils/request'
import * as echarts from 'echarts'

// 基础数据
const cardList = ref([
  { title: '用户总数', value: 0, color: '#303133' },
  { title: '在线商品', value: 0, color: '#303133' },
  { title: '已成交订单', value: 0, color: '#303133' },
  { title: '平台成交额', value: '￥0', color: '#67C23A' }
])
const currentTime = ref(new Date().toLocaleString())

// 图表实例
const typePieChart = shallowRef(null)
const statusBarChart = shallowRef(null)
const lineChart = shallowRef(null)

const loadData = function() {
  // 1. 用户和商品基础统计
  request.get('/user').then(function(res) {
    if (res.data) cardList.value[0].value = res.data.length
  })
  request.get('/goods').then(function(res) {
    if (res.data) cardList.value[1].value = res.data.length
  })

  // 2. 订单数据分析
  request.get('/orders').then(function(res) {
    var orders = res.data || []
    var finishOrders = orders.filter(function(o) { return o.status === '交易完成' })

    cardList.value[2].value = finishOrders.length
    var total = finishOrders.reduce(function(sum, o) { return sum + (o.price || 0) }, 0)
    cardList.value[3].value = '￥' + total.toFixed(2)

    // 柱状图数据处理
    var statusMap = {}
    orders.forEach(function(o) { statusMap[o.status] = (statusMap[o.status] || 0) + 1 })

    if (statusBarChart.value) {
      statusBarChart.value.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: Object.keys(statusMap) },
        yAxis: { type: 'value' },
        series: [{
          name: '数量',
          type: 'bar',
          itemStyle: { color: '#409EFF', borderRadius: [5, 5, 0, 0] },
          data: Object.values(statusMap)
        }]
      })
    }

    // 折线图趋势处理
    var trendMap = {}
    finishOrders.forEach(function(o) {
      var date = o.time ? o.time.split(' ')[0] : '未知'
      trendMap[date] = (trendMap[date] || 0) + o.price
    })
    var sortedDates = Object.keys(trendMap).sort()

    if (lineChart.value) {
      lineChart.value.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', boundaryGap: false, data: sortedDates },
        yAxis: { type: 'value' },
        series: [{
          name: '成交额',
          type: 'line',
          smooth: true,
          areaStyle: { color: 'rgba(103, 194, 58, 0.3)' },
          lineStyle: { color: '#67C23A', width: 3 },
          data: sortedDates.map(function(d) { return trendMap[d] })
        }]
      })
    }
  })

  // 3. 商品分类饼图
  request.get("/echarts/count").then(function(res) {
    if (typePieChart.value && res.data) {
      typePieChart.value.setOption({
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          type: 'scroll',
          orient: 'horizontal',
          bottom: '0',
          left: 'center'
        },
        series: [{
          name: '分类占比',
          type: 'pie',
          radius: ['35%', '60%'], // 稍微缩小一点圆环，给外部指示线留出更多空间
          center: ['50%', '45%'], // 中心向上微调，给底部图例留位
          avoidLabelOverlap: true,   // 关键：开启自动防重叠
          itemStyle: {
            borderRadius: 8,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: true,
            position: 'outside',
            // 优化文字格式：分两行显示，第一行加粗
            formatter: '{b|{b}}\n{d|{d}%}',
            rich: {
              b: {
                fontSize: 12,
                fontWeight: 'bold',
                lineHeight: 18,
                color: '#333'
              },
              d: {
                fontSize: 11,
                color: '#666'
              }
            }
          },
          labelLine: {
            show: true,
            length: 15,       // 第一段线长度
            length2: 20,      // 第二段线长度
            smooth: true,     // 线条平滑化，更美观
            lineStyle: {
              width: 1,
              type: 'solid'
            }
          },
          data: res.data
        }]
      })
    }
  })
}

onMounted(function() {
  typePieChart.value = echarts.init(document.getElementById('typePie'))
  statusBarChart.value = echarts.init(document.getElementById('statusBar'))
  lineChart.value = echarts.init(document.getElementById('lineChart'))

  loadData()

  window.addEventListener('resize', function() {
    if (typePieChart.value) typePieChart.value.resize()
    if (statusBarChart.value) statusBarChart.value.resize()
    if (lineChart.value) lineChart.value.resize()
  })

  setInterval(function() {
    currentTime.value = new Date().toLocaleString()
  }, 1000)
})
</script>


<template>
  <div style="padding: 20px; background-color: #f5f7fa; min-height: 100vh">

    <el-row :gutter="20">
      <el-col :span="6" v-for="item in cardList" :key="item.title">
        <el-card shadow="hover" class="data-card">
          <template #header><div class="card-title">{{ item.title }}</div></template>
          <div class="card-value" :style="{ color: item.color }">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="10">
        <el-card header="商品分类占比">
          <div id="typePie" style="width: 100%; height: 350px"></div>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card header="订单状态分布统计 (柱状图)">
          <div id="statusBar" style="width: 100%; height: 350px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card header="近期成交额趋势分析">
          <div id="lineChart" style="width: 100%; height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card header="系统运行概览">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="当前时间">{{ currentTime }}</el-descriptions-item>
            <el-descriptions-item label="后端状态">服务已连接</el-descriptions-item>
            <el-descriptions-item label="技术栈">SpringBoot / Vue3 / ECharts</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>



<style scoped>
.data-card { transition: transform 0.3s; }
.data-card:hover { transform: translateY(-5px); }
.card-title { font-weight: bold; color: #606266; }
.card-value { font-size: 28px; font-weight: bold; text-align: center; padding: 10px 0; }
</style>