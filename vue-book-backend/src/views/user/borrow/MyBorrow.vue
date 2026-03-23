 <template>
  <div class="borrow-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的借阅</span>
        </div>
      </template>

      <el-table :data="borrowList" style="width: 100%" v-loading="loading">
        <el-table-column prop="bookName" label="书名" min-width="150" />
        <el-table-column prop="bookNumber" label="图书编号" width="120" />
        <el-table-column prop="bookAuthor" label="作者" width="100" />
        <el-table-column prop="bookType" label="分类" width="100" />
        <el-table-column prop="borrowDate" label="借阅日期" width="120" />
        <el-table-column prop="closeDate" label="应还日期" width="120">
          <template #default="{ row }">
      <span :style="{ color: row.isOverdue ? 'red' : 'inherit' }">
        {{ row.closeDate || row.dueDate }}
      </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === '在借' || row.status === '未归还' ? 'warning' : 'success'">
              {{ row.status || '未归还' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>


      <div style="margin-top: 20px; text-align: right">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

 <script setup>
 import { ref, reactive, onMounted } from 'vue'
 import { ElMessage } from 'element-plus'
 import request from '@/utils/request'

 const loading = ref(false)
 const borrowList = ref([])
 const currentPage = ref(1)
 const pageSize = ref(10)
 const total = ref(0)

 // 加载借阅记录
 const loadBorrowList = async () => {
   try {
     loading.value = true
     const userId = localStorage.getItem('userId')
     if (!userId) {
       ElMessage.error('未找到用户 ID')
       return
     }

     const res = await request({
       url: '/user/get_bookborrow',
       method: 'post',
       data: {
         pageNum: currentPage.value,
         pageSize: pageSize.value,
         userId: parseInt(userId),
       },
     })

     if (res.code === 200 || res.status === 200) {
       const records = res.data.records || []
       total.value = Number(res.data.total) || 0

       // 遍历借阅记录，查询每本图书的详细信息
       const enrichedList = await Promise.all(records.map(async (record) => {
         try {
           // 根据图书编号查询图书信息
           const bookRes = await request({
             url: '/user/get_book_information/' + record.bookNumber,
             method: 'get',
           })

           if ((bookRes.code === 200 || bookRes.status === 200) && bookRes.data) {
             // 合并借阅记录和图书信息
             return {
               ...record,
               bookName: bookRes.data.bookName || '未知图书',
               bookAuthor: bookRes.data.bookAuthor || '未知作者',
               bookType: bookRes.data.bookType || '未分类',
             }
           }
         } catch (error) {
           console.error('查询图书信息失败:', error)
         }
         // 如果查询失败，返回原始记录
         return {
           ...record,
           bookName: '未知图书',
           bookAuthor: '未知作者',
           bookType: '未分类',
         }
       }))

       borrowList.value = enrichedList
     } else {
       // 没有数据时不显示错误提示，正常业务场景
       borrowList.value = []
       total.value = 0
     }
   } catch (error) {
     console.error('加载借阅记录失败:', error)
     // 只在真正出错时显示提示
     if (error.message && !error.message.includes('获取不到')) {
       ElMessage.error('加载借阅记录失败')
     }
     borrowList.value = []
     total.value = 0
   } finally {
     loading.value = false
   }
 }

 // 分页处理
 const handleSizeChange = (val) => {
   pageSize.value = val
   loadBorrowList()
 }

 const handleCurrentChange = (val) => {
   currentPage.value = val
   loadBorrowList()
 }

 onMounted(() => {
   loadBorrowList()
 })
 </script>

<style scoped>
.borrow-container {
  height: 100%;
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
