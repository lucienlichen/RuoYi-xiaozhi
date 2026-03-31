import request from '@/utils/request'

/** 查询所有书籍及章节树（业务端使用，不含 content_html） */
export function getBooksWithTree() {
  return request({ url: '/intellect/knowledge/books/tree', method: 'get' })
}

/** 查询书籍列表（管理端分页） */
export function listBooks(query) {
  return request({ url: '/intellect/knowledge/books', method: 'get', params: query })
}

export function getBook(id) {
  return request({ url: '/intellect/knowledge/books/' + id, method: 'get' })
}

export function addBook(data) {
  return request({ url: '/intellect/knowledge/books', method: 'post', data })
}

export function updateBook(data) {
  return request({ url: '/intellect/knowledge/books', method: 'put', data })
}

export function deleteBook(id) {
  return request({ url: '/intellect/knowledge/books/' + id, method: 'delete' })
}

/** 查询书籍章节树（管理端，不含 content_html） */
export function getChapterTree(bookId) {
  return request({ url: '/intellect/knowledge/chapters/tree', method: 'get', params: { bookId } })
}

/** 查询章节详情（含 content_html） */
export function getChapter(id) {
  return request({ url: '/intellect/knowledge/chapters/' + id, method: 'get' })
}

export function addChapter(data) {
  return request({ url: '/intellect/knowledge/chapters', method: 'post', data })
}

export function updateChapter(data) {
  return request({ url: '/intellect/knowledge/chapters', method: 'put', data })
}

export function deleteChapter(id) {
  return request({ url: '/intellect/knowledge/chapters/' + id, method: 'delete' })
}
