import request from '@/utils/request'

export function listAiConfig() {
  return request({ url: '/intellect/ai-config/list', method: 'get' })
}

export function batchUpdateAiConfig(data) {
  return request({ url: '/intellect/ai-config/batch', method: 'put', data })
}

export function testLlmConnection() {
  return request({ url: '/intellect/ai-config/test-llm', method: 'post' })
}
