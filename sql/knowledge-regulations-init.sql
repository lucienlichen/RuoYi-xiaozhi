-- =============================================
-- 知识管理 & 法规管理 数据库初始化
-- =============================================

-- 知识电子书
CREATE TABLE IF NOT EXISTS `tb_knowledge_book` (
  `id` bigint NOT NULL COMMENT '书籍ID',
  `title` varchar(200) NOT NULL COMMENT '书名',
  `author` varchar(100) DEFAULT NULL COMMENT '作者',
  `cover_image` varchar(500) DEFAULT NULL COMMENT '封面图片URL',
  `order_num` int DEFAULT 0 COMMENT '显示顺序',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识电子书';

-- 知识章节（自引用树形结构）
CREATE TABLE IF NOT EXISTS `tb_knowledge_chapter` (
  `id` bigint NOT NULL COMMENT '章节ID',
  `book_id` bigint NOT NULL COMMENT '所属书籍ID',
  `parent_id` bigint DEFAULT 0 COMMENT '父章节ID（0表示顶级）',
  `title` varchar(200) NOT NULL COMMENT '章节标题',
  `level` int DEFAULT 1 COMMENT '层级（1=章 2=节 3=小节）',
  `order_num` int DEFAULT 0 COMMENT '同级排序',
  `content_html` mediumtext DEFAULT NULL COMMENT '章节正文（HTML格式）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_book_id` (`book_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识电子书章节';

-- 法规文档
CREATE TABLE IF NOT EXISTS `tb_regulation_doc` (
  `id` bigint NOT NULL COMMENT '文档ID',
  `title` varchar(300) NOT NULL COMMENT '文档标题',
  `category` varchar(50) NOT NULL COMMENT '分类(laws/market_rules/tsg/standards)',
  `doc_no` varchar(100) DEFAULT NULL COMMENT '文号/编号',
  `publish_date` date DEFAULT NULL COMMENT '发布日期',
  `file_name` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `file_path` varchar(500) DEFAULT NULL COMMENT '存储路径',
  `content_html` mediumtext DEFAULT NULL COMMENT '解析后的HTML内容',
  `parse_status` varchar(20) DEFAULT 'NONE' COMMENT '解析状态(NONE/DONE/FAILED)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='法规文档';

-- =============================================
-- 菜单配置（挂在 2100 起重设备管理目录下）
-- =============================================

-- 知识管理菜单 (menu_id=2140)
INSERT INTO `sys_menu` VALUES (2140, '知识管理', 2100, 4, 'knowledge-mgmt', 'intellect/knowledge-mgmt/index', NULL, '', 1, 0, 'C', '0', '0', 'crane:knowledge:list', 'reading', 'admin', NOW(), '', NULL, '前沿知识电子书管理');
INSERT INTO `sys_menu` VALUES (2141, '知识查询', 2140, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:knowledge:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2142, '知识新增', 2140, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:knowledge:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2143, '知识修改', 2140, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:knowledge:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2144, '知识删除', 2140, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:knowledge:remove', '#', 'admin', NOW(), '', NULL, '');

-- 法规管理菜单 (menu_id=2150)
INSERT INTO `sys_menu` VALUES (2150, '法规管理', 2100, 5, 'regulations-mgmt', 'intellect/regulations-mgmt/index', NULL, '', 1, 0, 'C', '0', '0', 'crane:regulation:list', 'document', 'admin', NOW(), '', NULL, '法规标准文档管理');
INSERT INTO `sys_menu` VALUES (2151, '法规查询', 2150, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:regulation:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2152, '法规新增', 2150, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:regulation:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2153, '法规修改', 2150, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:regulation:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2154, '法规删除', 2150, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:regulation:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2155, '法规上传', 2150, 5, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'crane:regulation:upload', '#', 'admin', NOW(), '', NULL, '');

-- 为管理员角色(role_id=2)分配权限
INSERT INTO `sys_role_menu` VALUES (2, 2140);
INSERT INTO `sys_role_menu` VALUES (2, 2141);
INSERT INTO `sys_role_menu` VALUES (2, 2142);
INSERT INTO `sys_role_menu` VALUES (2, 2143);
INSERT INTO `sys_role_menu` VALUES (2, 2144);
INSERT INTO `sys_role_menu` VALUES (2, 2150);
INSERT INTO `sys_role_menu` VALUES (2, 2151);
INSERT INTO `sys_role_menu` VALUES (2, 2152);
INSERT INTO `sys_role_menu` VALUES (2, 2153);
INSERT INTO `sys_role_menu` VALUES (2, 2154);
INSERT INTO `sys_role_menu` VALUES (2, 2155);
