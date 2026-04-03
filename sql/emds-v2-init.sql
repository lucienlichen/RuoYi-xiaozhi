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
INSERT INTO `tb_data_category` VALUES (600, 'risk', '风险数据', 0, 6, 'year', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (700, 'operation', '运行数据', 0, 7, 'day', '0', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (800, 'monitoring', '监控数据', 0, 8, 'day', '0', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (900, 'measurement', '监测数据', 0, 9, 'day', '0', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (1000, 'basic_info', '基础信息', 0, 10, 'none', '1', 'admin', NOW(), '', NULL);
INSERT INTO `tb_data_category` VALUES (1100, 'factory_data', '出厂数据', 0, 11, 'none', '1', 'admin', NOW(), '', NULL);

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
  `enhanced_path` varchar(500) DEFAULT NULL COMMENT 'AI增强后图片路径',
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

-- =============================================
-- 6. 结构化模板配置
-- =============================================
CREATE TABLE IF NOT EXISTS `tb_structuring_template` (
  `id` bigint NOT NULL COMMENT '模板ID',
  `category_code` varchar(50) NOT NULL COMMENT '关联数据分类编码',
  `template_name` varchar(100) DEFAULT NULL COMMENT '模板名称',
  `llm_prompt` text DEFAULT NULL COMMENT 'LLM提取prompt模板',
  `field_schema` json DEFAULT NULL COMMENT '字段定义[{key,label,type,required}]',
  `rule_config` json DEFAULT NULL COMMENT '规则提取配置(正则/关键词)',
  `enabled` char(1) DEFAULT '1' COMMENT '是否启用(1启用 0禁用)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_category_code` (`category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据结构化模板配置';

-- 预置默认模板
INSERT INTO `tb_structuring_template` VALUES (1, 'inspection', '点巡检数据模板', '请从以下文本中提取点巡检相关信息，以JSON格式返回。字段包括：巡检日期、巡检人、设备部位、检查项目、检查结果、备注。如果某字段无法提取，值设为null。', '[{"key":"inspectionDate","label":"巡检日期","type":"date","required":true},{"key":"inspector","label":"巡检人","type":"string","required":true},{"key":"equipmentPart","label":"设备部位","type":"string","required":true},{"key":"checkItem","label":"检查项目","type":"string","required":true},{"key":"checkResult","label":"检查结果","type":"string","required":true},{"key":"remark","label":"备注","type":"string","required":false}]', NULL, '1', 'admin', NOW(), '', NULL, '点巡检数据默认提取模板');

INSERT INTO `tb_structuring_template` VALUES (2, 'examination', '检验报告模板', '请从以下文本中提取检验报告相关信息，以JSON格式返回。字段包括：检验日期、检验机构、检验类型、检验结论、有效期至、检验员。如果某字段无法提取，值设为null。', '[{"key":"examDate","label":"检验日期","type":"date","required":true},{"key":"examOrg","label":"检验机构","type":"string","required":true},{"key":"examType","label":"检验类型","type":"string","required":true},{"key":"conclusion","label":"检验结论","type":"string","required":true},{"key":"validUntil","label":"有效期至","type":"date","required":false},{"key":"examiner","label":"检验员","type":"string","required":false}]', NULL, '1', 'admin', NOW(), '', NULL, '检验报告默认提取模板');

INSERT INTO `tb_structuring_template` VALUES (3, 'fault', '故障记录模板', '请从以下文本中提取故障记录相关信息，以JSON格式返回。字段包括：故障日期、故障部位、故障现象、故障原因、处理措施、处理人。如果某字段无法提取，值设为null。', '[{"key":"faultDate","label":"故障日期","type":"date","required":true},{"key":"faultPart","label":"故障部位","type":"string","required":true},{"key":"faultSymptom","label":"故障现象","type":"string","required":true},{"key":"faultCause","label":"故障原因","type":"string","required":false},{"key":"treatment","label":"处理措施","type":"string","required":true},{"key":"handler","label":"处理人","type":"string","required":false}]', NULL, '1', 'admin', NOW(), '', NULL, '故障记录默认提取模板');

INSERT INTO `tb_structuring_template` VALUES (4, 'repair', '维修记录模板', '请从以下文本中提取维修记录相关信息，以JSON格式返回。字段包括：维修日期、维修类型、维修部位、维修内容、更换部件、维修人、验收结果。如果某字段无法提取，值设为null。', '[{"key":"repairDate","label":"维修日期","type":"date","required":true},{"key":"repairType","label":"维修类型","type":"string","required":true},{"key":"repairPart","label":"维修部位","type":"string","required":true},{"key":"repairContent","label":"维修内容","type":"string","required":true},{"key":"replacedParts","label":"更换部件","type":"string","required":false},{"key":"repairer","label":"维修人","type":"string","required":false},{"key":"acceptResult","label":"验收结果","type":"string","required":false}]', NULL, '1', 'admin', NOW(), '', NULL, '维修记录默认提取模板');

INSERT INTO `tb_structuring_template` VALUES (5, 'testing', '检测报告模板', '请从以下文本中提取检测报告相关信息，以JSON格式返回。字段包括：检测日期、检测机构、检测项目、检测方法、检测结果、检测员。如果某字段无法提取，值设为null。', '[{"key":"testDate","label":"检测日期","type":"date","required":true},{"key":"testOrg","label":"检测机构","type":"string","required":true},{"key":"testItem","label":"检测项目","type":"string","required":true},{"key":"testMethod","label":"检测方法","type":"string","required":false},{"key":"testResult","label":"检测结果","type":"string","required":true},{"key":"tester","label":"检测员","type":"string","required":false}]', NULL, '1', 'admin', NOW(), '', NULL, '检测报告默认提取模板');

INSERT INTO `tb_structuring_template` VALUES (6, 'factory_data', '出厂数据模板', '请从以下文本中提取出厂数据相关信息，以JSON格式返回。字段包括：制造单位、出厂编号、出厂日期、产品型号、额定起重量、主要技术参数。如果某字段无法提取，值设为null。', '[{"key":"manufacturer","label":"制造单位","type":"string","required":true},{"key":"factoryNo","label":"出厂编号","type":"string","required":true},{"key":"factoryDate","label":"出厂日期","type":"date","required":true},{"key":"productModel","label":"产品型号","type":"string","required":true},{"key":"ratedCapacity","label":"额定起重量","type":"string","required":false},{"key":"techParams","label":"主要技术参数","type":"string","required":false}]', NULL, '1', 'admin', NOW(), '', NULL, '出厂数据默认提取模板');

INSERT INTO `tb_structuring_template` VALUES (7, 'risk', '风险评估模板', '请从以下文本中提取风险评估相关信息，以JSON格式返回。字段包括：评估日期、评估人、风险等级、风险描述、控制措施、整改期限。如果某字段无法提取，值设为null。', '[{"key":"assessDate","label":"评估日期","type":"date","required":true},{"key":"assessor","label":"评估人","type":"string","required":true},{"key":"riskLevel","label":"风险等级","type":"string","required":true},{"key":"riskDesc","label":"风险描述","type":"string","required":true},{"key":"controlMeasure","label":"控制措施","type":"string","required":false},{"key":"deadline","label":"整改期限","type":"date","required":false}]', NULL, '1', 'admin', NOW(), '', NULL, '风险评估默认提取模板');

-- 结构化模板管理菜单 (menu_id=2180)
INSERT INTO `sys_menu` VALUES (2180, '结构化模板', 2100, 6, 'structuring', 'intellect/structuring/index', NULL, '', 1, 0, 'C', '0', '0', 'intellect:structuring:list', 'edit', 'admin', NOW(), '', NULL, '结构化模板配置菜单');
INSERT INTO `sys_menu` VALUES (2181, '模板查询', 2180, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'intellect:structuring:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2182, '模板新增', 2180, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'intellect:structuring:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2183, '模板修改', 2180, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'intellect:structuring:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2184, '模板删除', 2180, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'intellect:structuring:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_role_menu` VALUES (2, 2180);
INSERT INTO `sys_role_menu` VALUES (2, 2181);
INSERT INTO `sys_role_menu` VALUES (2, 2182);
INSERT INTO `sys_role_menu` VALUES (2, 2183);
INSERT INTO `sys_role_menu` VALUES (2, 2184);
