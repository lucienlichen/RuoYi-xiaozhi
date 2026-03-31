import {
  DataAnalysis, ChatLineSquare, Warning, Odometer, Reading, Document
} from '@element-plus/icons-vue'

/**
 * 6个AI助手配置（全局共享，消除重复定义）
 */
export const aiAssistants = [
  {
    name: '数据服务AI服务助手',
    shortName: '数据',
    service: 'data_service_ai',
    description: '查看各类数据结构化处理结果',
    icon: DataAnalysis,
    color: '#10B981',
    colorLight: 'rgba(16, 185, 129, 0.08)',
  },
  {
    name: '问题处理AI服务助手',
    shortName: '问题',
    service: 'typical_issue_ai',
    description: '设备问题处理与记录跟踪',
    icon: ChatLineSquare,
    color: '#6366F1',
    colorLight: 'rgba(99, 102, 241, 0.08)',
  },
  {
    name: '隐患排查AI服务助手',
    shortName: '隐患',
    service: 'hazard_check',
    description: '64项隐患排查检查清单管理',
    icon: Warning,
    color: '#F59E0B',
    colorLight: 'rgba(245, 158, 11, 0.08)',
  },
  {
    name: '风险服务AI服务助手',
    shortName: '风险',
    service: 'risk_service_ai',
    description: '设备风险动态评估与监控',
    icon: Odometer,
    color: '#F97316',
    colorLight: 'rgba(249, 115, 22, 0.08)',
  },
  {
    name: '前沿知识AI服务助手',
    shortName: '知识',
    service: 'safety_maintenance_ai',
    description: '安全维保知识检索与问答',
    icon: Reading,
    color: '#EC4899',
    colorLight: 'rgba(236, 72, 153, 0.08)',
  },
  {
    name: '法规标准AI服务助手',
    shortName: '法规',
    service: 'regulations_ai',
    description: '法律法规与技术规范知识库',
    icon: Document,
    color: '#64748B',
    colorLight: 'rgba(100, 116, 139, 0.08)',
  },
]

/**
 * 根据service标识查找助手名称
 */
export function getServiceName(service) {
  const found = aiAssistants.find(a => a.service === service)
  return found ? found.name : service
}
