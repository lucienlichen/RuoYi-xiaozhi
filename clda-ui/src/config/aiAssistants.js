import {
  DataAnalysis, ChatLineSquare, Warning, Odometer, Reading, Document
} from '@element-plus/icons-vue'

/**
 * 6个AI助手配置（全局共享）
 * 颜色遵循 EMDS-V2 Design System
 */
export const aiAssistants = [
  {
    name: '数据服务AI助手',
    service: 'data_service_ai',
    description: '查看各类数据结构化处理结果',
    icon: DataAnalysis,
    color: '#006c49',
    colorSurface: 'rgba(0, 108, 73, 0.06)',
    colorActive: '#005a3c',
  },
  {
    name: '问题处理AI助手',
    service: 'typical_issue_ai',
    description: '设备问题处理与记录跟踪',
    icon: ChatLineSquare,
    color: '#170dae',
    colorSurface: 'rgba(23, 13, 174, 0.06)',
    colorActive: '#120a8f',
  },
  {
    name: '隐患排查AI助手',
    service: 'hazard_check',
    description: '66项隐患排查表生成与管理',
    icon: Warning,
    color: '#b45309',
    colorSurface: 'rgba(180, 83, 9, 0.06)',
    colorActive: '#92400e',
  },
  {
    name: '风险服务AI助手',
    service: 'risk_service_ai',
    description: '设备风险动态评估与监控',
    icon: Odometer,
    color: '#c2410c',
    colorSurface: 'rgba(194, 65, 12, 0.06)',
    colorActive: '#9a3412',
  },
  {
    name: '前沿知识AI助手',
    service: 'safety_maintenance_ai',
    description: '安全维保知识检索与问答',
    icon: Reading,
    color: '#be185d',
    colorSurface: 'rgba(190, 24, 93, 0.06)',
    colorActive: '#9d174d',
  },
  {
    name: '法规标准AI助手',
    service: 'regulations_ai',
    description: '法律法规与技术规范知识库',
    icon: Document,
    color: '#475569',
    colorSurface: 'rgba(71, 85, 105, 0.06)',
    colorActive: '#334155',
  },
]

/**
 * 根据service标识查找助手名称
 */
export function getServiceName(service) {
  const found = aiAssistants.find(a => a.service === service)
  return found ? found.name : service
}
