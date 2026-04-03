<template>
  <div class="app-container">
    <el-row :gutter="16" style="height: calc(100vh - 130px)">

      <!-- 左侧：书籍列表 -->
      <el-col :span="8">
        <el-card shadow="never" style="height: 100%">
          <template #header>
            <div style="display:flex;align-items:center;justify-content:space-between">
              <span style="font-weight:600">书籍列表</span>
              <el-button type="primary" size="small" icon="Plus" @click="handleAddBook"
                v-hasPermi="['crane:knowledge:add']">新增书籍</el-button>
            </div>
          </template>
          <div v-loading="bookLoading">
            <div
              v-for="book in bookList"
              :key="book.id"
              class="book-card"
              :class="{ active: selectedBookId === book.id }"
              @click="selectBook(book)"
            >
              <div class="book-card-title">{{ book.title }}</div>
              <div class="book-card-author" v-if="book.author">{{ book.author }}</div>
              <div class="book-card-actions">
                <el-button link size="small" type="primary" icon="Edit"
                  @click.stop="handleEditBook(book)" v-hasPermi="['crane:knowledge:edit']">编辑</el-button>
                <el-button link size="small" type="danger" icon="Delete"
                  @click.stop="handleDeleteBook(book)" v-hasPermi="['crane:knowledge:remove']">删除</el-button>
              </div>
            </div>
            <el-empty v-if="!bookLoading && bookList.length === 0" description="暂无书籍" />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：章节树 -->
      <el-col :span="16">
        <el-card shadow="never" style="height: 100%; overflow-y: auto">
          <template #header>
            <div style="display:flex;align-items:center;justify-content:space-between">
              <span style="font-weight:600">
                {{ selectedBook ? selectedBook.title + ' — 章节管理' : '请选择左侧书籍' }}
              </span>
              <el-button v-if="selectedBook" type="primary" size="small" icon="Plus"
                @click="handleAddChapter(null)" v-hasPermi="['crane:knowledge:add']">添加章节</el-button>
            </div>
          </template>

          <div v-if="!selectedBook" style="padding:40px;text-align:center;color:#94a3b8">
            请在左侧选择一本书籍
          </div>

          <div v-else v-loading="chapterLoading">
            <template v-for="chapter in chapterTree" :key="chapter.id">
              <ChapterNode :node="chapter" :depth="0"
                @edit="handleEditChapter"
                @add-child="handleAddChapter"
                @delete="handleDeleteChapter" />
            </template>
            <el-empty v-if="!chapterLoading && chapterTree.length === 0" description="暂无章节，点击「添加章节」开始" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 书籍表单对话框 -->
    <el-dialog :title="bookDialog.title" v-model="bookDialog.open" width="480px" append-to-body>
      <el-form ref="bookFormRef" :model="bookForm" :rules="bookRules" label-width="80px">
        <el-form-item label="书名" prop="title">
          <el-input v-model="bookForm.title" placeholder="请输入书名" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="bookForm.author" placeholder="请输入作者" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="bookForm.orderNum" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitBookForm">确 定</el-button>
        <el-button @click="bookDialog.open = false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 章节表单对话框 -->
    <el-dialog :title="chapterDialog.title" v-model="chapterDialog.open" width="860px" append-to-body
      :close-on-click-modal="false">
      <el-form ref="chapterFormRef" :model="chapterForm" :rules="chapterRules" label-width="80px">
        <el-row :gutter="12">
          <el-col :span="16">
            <el-form-item label="章节标题" prop="title">
              <el-input v-model="chapterForm.title" placeholder="请输入章节标题" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="层级" prop="level">
              <el-select v-model="chapterForm.level">
                <el-option label="章(1级)" :value="1" />
                <el-option label="节(2级)" :value="2" />
                <el-option label="小节(3级)" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="排序">
              <el-input-number v-model="chapterForm.orderNum" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="正文内容">
          <div style="border:1px solid #e2e8f0;border-radius:6px;overflow:hidden;width:100%">
            <QuillEditor
              v-model:content="chapterForm.contentHtml"
              content-type="html"
              theme="snow"
              style="min-height:320px"
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitChapterForm" :loading="chapterDialog.saving">保 存</el-button>
        <el-button @click="chapterDialog.open = false">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, defineComponent, h, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import { listBooks, getBook, addBook, updateBook, deleteBook,
         getChapterTree, getChapter, addChapter, updateChapter, deleteChapter } from '@/api/intellect/knowledge'

// ===== 书籍 =====
const bookList = ref([])
const bookLoading = ref(false)
const selectedBookId = ref(null)
const selectedBook = ref(null)
const bookDialog = reactive({ open: false, title: '' })
const bookForm = ref({})
const bookFormRef = ref(null)
const bookRules = { title: [{ required: true, message: '书名不能为空', trigger: 'blur' }] }

function loadBooks() {
  bookLoading.value = true
  listBooks({}).then(res => {
    bookList.value = res.rows || []
    bookLoading.value = false
  })
}

function selectBook(book) {
  selectedBookId.value = book.id
  selectedBook.value = book
  loadChapterTree(book.id)
}

function handleAddBook() {
  bookForm.value = { title: '', author: '', orderNum: 0 }
  bookDialog.title = '添加书籍'
  bookDialog.open = true
}

function handleEditBook(book) {
  bookForm.value = { ...book }
  bookDialog.title = '编辑书籍'
  bookDialog.open = true
}

async function submitBookForm() {
  await bookFormRef.value.validate()
  if (bookForm.value.id) {
    await updateBook(bookForm.value)
    ElMessage.success('修改成功')
  } else {
    await addBook(bookForm.value)
    ElMessage.success('添加成功')
  }
  bookDialog.open = false
  loadBooks()
}

async function handleDeleteBook(book) {
  await ElMessageBox.confirm(`确认删除书籍「${book.title}」及其所有章节？`, '提示', { type: 'warning' })
  await deleteBook(book.id)
  ElMessage.success('删除成功')
  if (selectedBookId.value === book.id) {
    selectedBookId.value = null
    selectedBook.value = null
    chapterTree.value = []
  }
  loadBooks()
}

// ===== 章节 =====
const chapterTree = ref([])
const chapterLoading = ref(false)
const chapterDialog = reactive({ open: false, title: '', saving: false })
const chapterForm = ref({})
const chapterFormRef = ref(null)
const chapterRules = { title: [{ required: true, message: '章节标题不能为空', trigger: 'blur' }] }

function loadChapterTree(bookId) {
  chapterLoading.value = true
  getChapterTree(bookId).then(res => {
    chapterTree.value = res.data || []
    chapterLoading.value = false
  })
}

function handleAddChapter(parentChapter) {
  chapterForm.value = {
    bookId: selectedBookId.value,
    parentId: parentChapter ? parentChapter.id : 0,
    title: '',
    level: parentChapter ? Math.min(parentChapter.level + 1, 3) : 1,
    orderNum: 0,
    contentHtml: ''
  }
  chapterDialog.title = parentChapter ? `在「${parentChapter.title}」下添加子节` : '添加章节'
  chapterDialog.open = true
}

async function handleEditChapter(chapter) {
  const res = await getChapter(chapter.id)
  chapterForm.value = { ...res.data, contentHtml: res.data.contentHtml || '' }
  chapterDialog.title = '编辑章节'
  chapterDialog.open = true
}

async function submitChapterForm() {
  await chapterFormRef.value.validate()
  chapterDialog.saving = true
  try {
    if (chapterForm.value.id) {
      await updateChapter(chapterForm.value)
    } else {
      await addChapter(chapterForm.value)
    }
    ElMessage.success('保存成功')
    chapterDialog.open = false
    loadChapterTree(selectedBookId.value)
  } finally {
    chapterDialog.saving = false
  }
}

async function handleDeleteChapter(chapter) {
  await ElMessageBox.confirm(`确认删除章节「${chapter.title}」及其所有子节？`, '提示', { type: 'warning' })
  await deleteChapter(chapter.id)
  ElMessage.success('删除成功')
  loadChapterTree(selectedBookId.value)
}

// ===== 递归章节节点组件 =====
const ChapterNode = defineComponent({
  name: 'ChapterNode',
  props: { node: Object, depth: Number },
  emits: ['edit', 'add-child', 'delete'],
  setup(props, { emit }) {
    const expanded = ref(true)
    return () => h('div', { class: 'chapter-node', style: `padding-left: ${props.depth * 20}px` }, [
      h('div', { class: 'chapter-row' }, [
        props.node.children?.length
          ? h('el-icon', {
              class: 'expand-btn',
              onClick: () => { expanded.value = !expanded.value }
            }, () => h(expanded.value ? 'ArrowDown' : 'ArrowRight'))
          : h('span', { class: 'node-dot' }, '·'),
        h('span', { class: `chapter-title level-${props.node.level}` }, props.node.title),
        h('div', { class: 'chapter-actions' }, [
          h('el-button', { link: true, size: 'small', type: 'primary', onClick: () => emit('edit', props.node) }, '编辑'),
          h('el-button', { link: true, size: 'small', onClick: () => emit('add-child', props.node) }, '添加子节'),
          h('el-button', { link: true, size: 'small', type: 'danger', onClick: () => emit('delete', props.node) }, '删除'),
        ])
      ]),
      expanded.value && props.node.children?.length
        ? h('div', props.node.children.map(child =>
            h(ChapterNode, {
              node: child, depth: props.depth + 1,
              onEdit: (n) => emit('edit', n),
              onAddChild: (n) => emit('add-child', n),
              onDelete: (n) => emit('delete', n)
            })
          ))
        : null
    ])
  }
})

onMounted(() => loadBooks())
</script>

<style lang="scss" scoped>
.book-card {
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.15s;

  &:hover { border-color: #EC4899; }
  &.active { border-color: #EC4899; background: #fce7f3; }
}

.book-card-title { font-weight: 600; color: #1e293b; }
.book-card-author { font-size: 12px; color: #94a3b8; margin-top: 2px; }
.book-card-actions { margin-top: 6px; }

.chapter-node { border-bottom: 1px solid #f1f5f9; }
.chapter-row {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 6px;
  &:hover { background: #f8fafc; }
}
.expand-btn { cursor: pointer; color: #64748b; flex-shrink: 0; }
.node-dot { width: 16px; text-align: center; color: #94a3b8; flex-shrink: 0; }
.chapter-title {
  flex: 1; font-size: 14px; color: #374151;
  &.level-1 { font-weight: 600; }
  &.level-2 { font-size: 13px; }
  &.level-3 { font-size: 12px; color: #64748b; }
}
.chapter-actions { display: flex; gap: 4px; flex-shrink: 0; }
</style>
