-- 人脸注册表
CREATE TABLE IF NOT EXISTS `tb_face` (
  `id` bigint NOT NULL COMMENT '主键',
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '人员姓名',
  `descriptor` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '人脸特征向量（JSON数组）',
  `photo_url` mediumtext COLLATE utf8mb4_general_ci COMMENT '照片（base64）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='人脸注册';

-- 菜单：人脸管理（父菜单）
INSERT INTO `sys_menu` VALUES (2020, '人脸管理', 2000, 3, 'face', 'intellect/face/index', NULL, '', 1, 0, 'C', '0', '0', 'intellect:face:list', 'peoples', 'admin', NOW(), '', NULL, '人脸管理菜单');
-- 按钮权限
INSERT INTO `sys_menu` VALUES (2021, '人脸查询', 2020, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'intellect:face:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2022, '人脸新增', 2020, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'intellect:face:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2023, '人脸修改', 2020, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'intellect:face:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` VALUES (2024, '人脸删除', 2020, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'intellect:face:remove', '#', 'admin', NOW(), '', NULL, '');

-- 为管理员角色分配人脸管理权限
INSERT INTO `sys_role_menu` VALUES (2, 2020);
INSERT INTO `sys_role_menu` VALUES (2, 2021);
INSERT INTO `sys_role_menu` VALUES (2, 2022);
INSERT INTO `sys_role_menu` VALUES (2, 2023);
INSERT INTO `sys_role_menu` VALUES (2, 2024);
