-- =============================================
-- EMDS-V2 起重装备全生命周期数据AI智能体平台
-- 阶段一：分区管理 + 设备管理 + 数据采集
-- =============================================

-- 1. 设备分区（厂区）
CREATE TABLE IF NOT EXISTS `tb_partition` (
  `id` bigint NOT NULL COMMENT '分区ID',
  `partition_name` varchar(100) NOT NULL COMMENT '分区名称',
  `partition_code` varchar(50) DEFAULT NULL COMMENT '分区编码',
  `parent_id` bigint DEFAULT 0 COMMENT '父分区ID(0为顶级)',
  `order_num` int DEFAULT 0 COMMENT '显示顺序',
  `status` char(1) DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备分区(厂区)';

-- 默认厂区数据
INSERT INTO `tb_partition` VALUES (1, '厂区一', 'area_1', 0, 1, '0', 'admin', NOW(), '', NULL, '默认厂区');
INSERT INTO `tb_partition` VALUES (2, '厂区二', 'area_2', 0, 2, '0', 'admin', NOW(), '', NULL, '默认厂区');
INSERT INTO `tb_partition` VALUES (3, '厂区三', 'area_3', 0, 3, '0', 'admin', NOW(), '', NULL, '默认厂区');

-- 2. 起重设备
CREATE TABLE IF NOT EXISTS `tb_equipment` (
  `id` bigint NOT NULL COMMENT '设备ID',
  `equipment_name` varchar(200) NOT NULL COMMENT '设备名称',
  `equipment_code` varchar(100) DEFAULT NULL COMMENT '设备编号',
  `equipment_type` varchar(50) DEFAULT NULL COMMENT '设备类型',
  `model` varchar(100) DEFAULT NULL COMMENT '设备型号',
  `manufacturer` varchar(200) DEFAULT NULL COMMENT '制造单位',
  `manufacture_date` date DEFAULT NULL COMMENT '出厂日期',
  `install_date` date DEFAULT NULL COMMENT '安装日期',
  `partition_id` bigint DEFAULT NULL COMMENT '所属分区ID',
  `status` varchar(20) DEFAULT 'NORMAL' COMMENT '设备状态(NORMAL/WARNING/FAULT/STOPPED)',
  `rated_capacity` decimal(10,2) DEFAULT NULL COMMENT '额定起重量(吨)',
  `span` decimal(10,2) DEFAULT NULL COMMENT '跨度(米)',
  `lifting_height` decimal(10,2) DEFAULT NULL COMMENT '起升高度(米)',
  `registration_code` varchar(100) DEFAULT NULL COMMENT '注册登记号',
  `use_cert_no` varchar(100) DEFAULT NULL COMMENT '使用登记证编号',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_partition_id` (`partition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='起重设备';

-- 3. 数据分类（14类+子分类）
CREATE TABLE IF NOT EXISTS `tb_data_category` (
  `id` bigint NOT NULL COMMENT '分类ID',
  `category_code` varchar(50) NOT NULL COMMENT '分类编码',
  `category_name` varchar(100) NOT NULL COMMENT '分类名称',
  `parent_id` bigint DEFAULT 0 COMMENT '父分类ID(0为顶级)',
  `order_num` int DEFAULT 0 COMMENT '排序',
  `date_mode` varchar(10) DEFAULT 'day' COMMENT '日期筛选模式(day/year/none)',
  `enabled` char(1) DEFAULT '1' COMMENT '是否启用(1启用 0禁用)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据分类';

-- 主分类（14类）
INSERT INTO `tb_data_category` VALUES (100, 'inspection', '点巡检数据', 0, 1, 'day', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (200, 'fault', '故障数据', 0, 2, 'day', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (300, 'repair', '维修数据', 0, 3, 'day', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (400, 'examination', '检验数据', 0, 4, 'year', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (500, 'testing', '检测数据', 0, 5, 'year', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (600, 'risk', '风险数据', 0, 6, 'day', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (700, 'operation', '运行数据', 0, 7, 'day', '0', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (800, 'monitoring', '监控数据', 0, 8, 'day', '0', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (900, 'measurement', '监测数据', 0, 9, 'day', '0', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (1000, 'basic_info', '基础信息', 0, 10, 'none', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (1100, 'factory_data', '出厂数据', 0, 11, 'none', '1', 'admin', NOW(), '', NULL);

-- 子分类
-- 点巡检
INSERT INTO `tb_data_category` VALUES (101, 'inspection_plan', '点巡检计划', 100, 1, 'day', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (102, 'inspection_task', '点巡检任务', 100, 2, 'day', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (103, 'inspection_record', '点巡检记录', 100, 3, 'day', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (104, 'inspection_defect', '问题与缺陷', 100, 4, 'day', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (105, 'inspection_fix', '整改闭环', 100, 5, 'day', '1', 'admin', NOW(), '', NULL);
-- 故障
INSERT INTO `tb_data_category` VALUES (201, 'fault_mech', '机械故障', 200, 1, 'day', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (202, 'fault_elec', '电气故障', 200, 2, 'day', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (203, 'fault_hydra', '液压传动故障', 200, 3, 'day', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (204, 'fault_ctrl', '控制系统故障', 200, 4, 'day', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (205, 'fault_safety', '安全装置故障', 200, 5, 'day', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (206, 'fault_other', '其他', 200, 6, 'day', '1', 'admin', NOW(), '', NULL);
-- 维修
INSERT INTO `tb_data_category` VALUES (301, 'repair_preventive', '预防性维修', 300, 1, 'day', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (302, 'repair_fault', '故障维修', 300, 2, 'day', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (303, 'repair_overhaul', '大修与改造', 300, 3, 'day', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (304, 'repair_maintain', '保养与例检', 300, 4, 'day', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (305, 'repair_spare', '备件更换', 300, 5, 'day', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (306, 'repair_other', '其他', 300, 6, 'day', '1', 'admin', NOW(), '', NULL);
-- 检验
INSERT INTO `tb_data_category` VALUES (401, 'exam_periodic', '定期检验报告', 400, 1, 'year', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (402, 'exam_supervision', '监督检验报告', 400, 2, 'year', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (403, 'exam_rectify', '整改通知与复检', 400, 3, 'year', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (404, 'exam_conclusion', '检验结论与证明', 400, 4, 'year', '1', 'admin', NOW(), '', NULL);
-- 检测
INSERT INTO `tb_data_category` VALUES (501, 'test_annual', '年度自检报告', 500, 1, 'year', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (502, 'test_special', '专项检测记录', 500, 2, 'year', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (503, 'test_safety', '安全部件检测', 500, 3, 'year', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (504, 'test_wire', '钢丝绳与金属结构检测', 500, 4, 'year', '1', 'admin', NOW(), '', NULL);
-- 出厂数据
INSERT INTO `tb_data_category` VALUES (1101, 'factory_design', '设计文件', 1100, 1, 'none', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (1102, 'factory_cert', '产品质量合格证明', 1100, 2, 'none', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (1103, 'factory_manual', '使用维护说明书', 1100, 3, 'none', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (1104, 'factory_inspect', '监督检验证明', 1100, 4, 'none', '1', 'admin', NOW(), '', NULL);

-- 4. 设备数据记录
CREATE TABLE IF NOT EXISTS `tb_equipment_data` (
  `id` bigint NOT NULL COMMENT '记录ID',
  `equipment_id` bigint NOT NULL COMMENT '设备ID',
  `category_id` bigint NOT NULL COMMENT '数据分类ID',
  `sub_category_id` bigint DEFAULT NULL COMMENT '子分类ID',
  `data_date` date NOT NULL COMMENT '数据日期',
  `title` varchar(200) DEFAULT NULL COMMENT '标题',
  `content` text DEFAULT NULL COMMENT '备注内容',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '状态(PENDING/PROCESSING/COMPLETED/FAILED)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_equip_category_date` (`equipment_id`, `category_id`, `data_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备生命周期数据记录';

-- 5. 数据文件附件
CREATE TABLE IF NOT EXISTS `tb_data_file` (
  `id` bigint NOT NULL COMMENT '文件ID',
  `data_id` bigint NOT NULL COMMENT '关联数据记录ID',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_path` varchar(500) NOT NULL COMMENT '存储路径',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型(image/pdf/word/excel)',
  `mime_type` varchar(100) DEFAULT NULL COMMENT 'MIME类型',
  `preprocessed_path` varchar(500) DEFAULT NULL COMMENT '预处理后文件路径',
  `preprocess_status` varchar(20) DEFAULT 'NONE' COMMENT '预处理状态(NONE/PROCESSING/DONE/FAILED)',
  `ocr_status` varchar(20) DEFAULT 'NONE' COMMENT 'OCR状态(NONE/PROCESSING/DONE/FAILED)',
  `ocr_text` mediumtext DEFAULT NULL COMMENT 'OCR识别文本',
  `structured_data` json DEFAULT NULL COMMENT '结构化数据',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_data_id` (`data_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据文件附件';

-- =============================================
-- 菜单配置 (sys_menu)
-- =============================================

-- 起重设备管理（一级目录, menu_id=2100）
INSERT INTO `sys_menu` VALUES (2100, '起重设备管理', 0, 1, 'crane', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'server', 'admin', NOW(), '', NULL, '起重装备管理目录');

-- 分区管理 (menu_id=2110)
INSERT INTO `sys_menu` VALUES (2110, '分区管理', 2100, 1, 'partition', 'intellect/partition/index', NULL, '', 1, 0, 'C', '0', '0', 'crane:partition:list', 'tree', 'admin', NOW(), '', NULL, '分区管理菜单');
INSERT INTO `sys_menu` VALUES (2111, '分区查询', 2110, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:partition:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2112, '分区新增', 2110, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:partition:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2113, '分区修改', 2110, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:partition:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2114, '分区删除', 2110, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:partition:remove', '#', 'admin', NOW(), '', NULL, '');

-- 设备管理 (menu_id=2120)
INSERT INTO `sys_menu` VALUES (2120, '设备管理', 2100, 2, 'equipment', 'intellect/equipment/index', NULL, '', 1, 0, 'C', '0', '0', 'crane:equipment:list', 'component', 'admin', NOW(), '', NULL, '设备管理菜单');
INSERT INTO `sys_menu` VALUES (2121, '设备查询', 2120, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:equipment:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2122, '设备新增', 2120, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:equipment:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2123, '设备修改', 2120, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:equipment:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2124, '设备删除', 2120, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:equipment:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2125, '设备导入', 2120, 5, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:equipment:import', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2126, '设备导出', 2120, 6, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:equipment:export', '#', 'admin', NOW(), '', NULL, '');

-- 数据采集 (menu_id=2130)
INSERT INTO `sys_menu` VALUES (2130, '数据采集', 2100, 3, 'equipdata', 'intellect/equipdata/index', NULL, '', 1, 0, 'C', '0', '0', 'crane:equipdata:list', 'upload', 'admin', NOW(), '', NULL, '设备数据采集菜单');
INSERT INTO `sys_menu` VALUES (2131, '数据查询', 2130, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:equipdata:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2132, '数据上传', 2130, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:equipdata:upload', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2133, '数据删除', 2130, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:equipdata:remove', '#', 'admin', NOW(), '', NULL, '');

-- 为管理员角色(role_id=2)分配权限
INSERT INTO `sys_role_menu` VALUES (2, 2100);
INSERT INTO `sys_role_menu` VALUES (2, 2110);
INSERT INTO `sys_role_menu` VALUES (2, 2111);
INSERT INTO `sys_role_menu` VALUES (2, 2112);
INSERT INTO `sys_role_menu` VALUES (2, 2113);
INSERT INTO `sys_role_menu` VALUES (2, 2114);
INSERT INTO `sys_role_menu` VALUES (2, 2120);
INSERT INTO `sys_role_menu` VALUES (2, 2121);
INSERT INTO `sys_role_menu` VALUES (2, 2122);
INSERT INTO `sys_role_menu` VALUES (2, 2123);
INSERT INTO `sys_role_menu` VALUES (2, 2124);
INSERT INTO `sys_role_menu` VALUES (2, 2125);
INSERT INTO `sys_role_menu` VALUES (2, 2126);
INSERT INTO `sys_role_menu` VALUES (2, 2130);
INSERT INTO `sys_role_menu` VALUES (2, 2131);
INSERT INTO `sys_role_menu` VALUES (2, 2132);
INSERT INTO `sys_role_menu` VALUES (2, 2133);
