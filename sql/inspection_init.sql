-- =============================================
-- 隐患排查功能初始化脚本
-- =============================================

-- 1. 检查项模板表
CREATE TABLE IF NOT EXISTS tb_inspection_item (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  category    VARCHAR(20)  NOT NULL COMMENT '大类：重大隐患/其他隐患',
  sub_category VARCHAR(50) NOT NULL COMMENT '子类：检验相关/安全装置/...',
  item_no     INT          NOT NULL COMMENT '序号1-66',
  content     VARCHAR(500) NOT NULL COMMENT '排查内容',
  order_num   INT          DEFAULT 0 COMMENT '显示排序',
  create_by   VARCHAR(64)  DEFAULT '',
  create_time DATETIME     DEFAULT NULL,
  update_by   VARCHAR(64)  DEFAULT '',
  update_time DATETIME     DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='隐患排查检查项';

-- 2. 排查记录主表
CREATE TABLE IF NOT EXISTS tb_inspection_record (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  equipment_id   BIGINT       NOT NULL COMMENT '设备ID',
  equipment_name VARCHAR(100) DEFAULT '' COMMENT '设备名称冗余',
  inspector      VARCHAR(64)  NOT NULL COMMENT '排查人',
  inspect_date   DATE         NOT NULL COMMENT '排查日期',
  file_path      VARCHAR(255) DEFAULT '' COMMENT '上传的Excel文件路径',
  major_count    INT          DEFAULT 0 COMMENT '重大隐患数量',
  other_count    INT          DEFAULT 0 COMMENT '其他隐患数量',
  total_items    INT          DEFAULT 0 COMMENT '总检查项数',
  status         CHAR(1)      DEFAULT '0' COMMENT '0已上传 1已审核',
  create_by      VARCHAR(64)  DEFAULT '',
  create_time    DATETIME     DEFAULT NULL,
  update_by      VARCHAR(64)  DEFAULT '',
  update_time    DATETIME     DEFAULT NULL,
  remark         VARCHAR(500) DEFAULT '',
  PRIMARY KEY (id),
  KEY idx_equipment (equipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='隐患排查记录';

-- 3. 排查结果明细表
CREATE TABLE IF NOT EXISTS tb_inspection_result (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  record_id  BIGINT       NOT NULL COMMENT '排查记录ID',
  item_id    BIGINT       NOT NULL COMMENT '检查项ID',
  item_no    INT          NOT NULL COMMENT '序号',
  result     CHAR(2)      DEFAULT '' COMMENT '有/无',
  remark     VARCHAR(255) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_record (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='隐患排查结果明细';

-- 4. 初始化66条检查项数据
INSERT INTO tb_inspection_item (category, sub_category, item_no, content, order_num, create_by, create_time) VALUES
-- ===== 重大隐患 (1-8) =====
('重大隐患', '检验相关', 1, '特种设备未取得许可生产、因安全问题国家令淘汰、已经报废或达到报废条件。', 1, 'admin', NOW()),
('重大隐患', '检验相关', 2, '特种设备发生过事故,未对其进行全面检查、消除事故隐患。', 2, 'admin', NOW()),
('重大隐患', '检验相关', 3, '未按规定进行监督检验或者监督检验不合格。', 3, 'admin', NOW()),
('重大隐患', '检验相关', 4, '未经首次检验。', 4, 'admin', NOW()),
('重大隐患', '检验相关', 5, '定期检验（含首次检验）的检验结论为"不合格"。', 5, 'admin', NOW()),
('重大隐患', '安全装置', 6, '接触式开关缺失或失效。', 6, 'admin', NOW()),
('重大隐患', '安全装置', 7, '起重量限制器、起升高度限制器、防坠安全器缺失或失效。', 7, 'admin', NOW()),
('重大隐患', '安全装置', 8, '室外工作的轨道式起重机械防风防滑装置缺失或失效。', 8, 'admin', NOW()),
-- ===== 其他隐患 (9-66) =====
-- 使用登记状况
('其他隐患', '使用登记状况', 9, '设施能否办理使用登记', 9, 'admin', NOW()),
('其他隐患', '使用登记状况', 10, '能否购置合法起重机械', 10, 'admin', NOW()),
('其他隐患', '使用登记状况', 11, '起重机械品种（型式）能否满足使用条件要求', 11, 'admin', NOW()),
('其他隐患', '使用登记状况', 12, '检验合格标记能否按规定标放', 12, 'admin', NOW()),
('其他隐患', '使用登记状况', 13, '能否按规定进行年度检验', 13, 'admin', NOW()),
-- 持证上岗状况
('其他隐患', '持证上岗状况', 14, '起重机司机、司索和信号指挥人员能否按规定要求进行培训和考核', 14, 'admin', NOW()),
-- 安全管理制度建立情况
('其他隐患', '安全管理制度建立情况', 15, '能否编订起重机械操作规程', 15, 'admin', NOW()),
('其他隐患', '安全管理制度建立情况', 16, '能否建立起重机械年度检验和隐患整顿制度', 16, 'admin', NOW()),
('其他隐患', '安全管理制度建立情况', 17, '能否建立起重机械事故防范举措和事故应急营救方案', 17, 'admin', NOW()),
('其他隐患', '安全管理制度建立情况', 18, '制度能否落实到有关人员', 18, 'admin', NOW()),
-- 安全责任落实状况
('其他隐患', '安全责任落实状况', 19, '能否设置专人负责起重机械安全管理工作', 19, 'admin', NOW()),
('其他隐患', '安全责任落实状况', 20, '起重机械保护维护规程和操作规程掌握状况', 20, 'admin', NOW()),
-- 安全使用状况
('其他隐患', '安全使用状况', 21, '起重机械检验合格标记、牌等能否醒目', 21, 'admin', NOW()),
('其他隐患', '安全使用状况', 22, '危险性较大的吊裝或施工区域能否有明显标表记', 22, 'admin', NOW()),
('其他隐患', '安全使用状况', 23, '安全区域内能否有无关人员活动', 23, 'admin', NOW()),
('其他隐患', '安全使用状况', 24, '制动器和各样安全保护装置能否齐备、有效', 24, 'admin', NOW()),
-- 安全装置
('其他隐患', '安全装置', 25, '高度、行程、速度等保护装置能否完好、灵敏可靠', 25, 'admin', NOW()),
('其他隐患', '安全装置', 26, '各样缓冲装置能否完好、反应', 26, 'admin', NOW()),
('其他隐患', '安全装置', 27, '紧迫报警装置能否可靠', 27, 'admin', NOW()),
('其他隐患', '安全装置', 28, '各样连锁保护装置能否可靠，保证进入起重机的门或司机室门开启时滑触电源不接通', 28, 'admin', NOW()),
-- 设施本体状况
('其他隐患', '设施本体状况', 29, '基础或轨道能否存在破坏、变形', 29, 'admin', NOW()),
('其他隐患', '设施本体状况', 30, '预制构造能否存在破坏、变形、腐蚀、开裂现象', 30, 'admin', NOW()),
('其他隐患', '设施本体状况', 31, '吊钩有无损害', 31, 'admin', NOW()),
('其他隐患', '设施本体状况', 32, '钢丝绳润滑优秀、无变形，断丝数未超出规定', 32, 'admin', NOW()),
('其他隐患', '设施本体状况', 33, '各样制动器零部件完好优秀，制动装置运转可靠', 33, 'admin', NOW()),
('其他隐患', '设施本体状况', 34, '电气设施各部件完好固定牢固，接地良好、布线和缆无破坏，适应使用环境', 34, 'admin', NOW()),
('其他隐患', '设施本体状况', 35, '驾驶室和操作室无变形破坏，视线良好、通风优秀', 35, 'admin', NOW()),
-- 起重机的钢丝绳
('其他隐患', '起重机的钢丝绳', 36, '有安全检验合格证并固定在醒目位置，在有效期内使用', 36, 'admin', NOW()),
('其他隐患', '起重机的钢丝绳', 37, '钢丝绳在一个捻距内，断丝数不超过钢丝总数的____%', 37, 'admin', NOW()),
('其他隐患', '起重机的钢丝绳', 38, '钢丝绳磨损使直径较公称直径减少____% 及以上，即使未断丝也应报废更新', 38, 'admin', NOW()),
-- 钢丝绳与卷筒 / 滑轮槽磨损
('其他隐患', '钢丝绳与卷筒/滑轮槽磨损', 39, '钢丝绳尾端装卡固定，夹板数量不少于____个', 39, 'admin', NOW()),
('其他隐患', '钢丝绳与卷筒/滑轮槽磨损', 40, '吊钩处于最低点时，卷筒上起码有____圈', 40, 'admin', NOW()),
-- 滑轮
('其他隐患', '滑轮', 41, '无裂纹缺损，转动灵巧', 41, 'admin', NOW()),
-- 吊钩
('其他隐患', '吊钩', 42, '钩体无裂纹，不得补焊', 42, 'admin', NOW()),
-- 制动器
('其他隐患', '制动器', 43, '制动器灵敏可靠，摩擦片磨损不超过原厚度的____%', 43, 'admin', NOW()),
('其他隐患', '制动器', 44, '制动时间不大于____', 44, 'admin', NOW()),
-- 安全防护系统
('其他隐患', '安全防护系统', 45, '运行极限位置限位器完满可靠', 45, 'admin', NOW()),
('其他隐患', '安全防护系统', 46, '大小车行程限位器完满可靠', 46, 'admin', NOW()),
('其他隐患', '安全防护系统', 47, '门窗电气联锁保护装置完满可靠', 47, 'admin', NOW()),
('其他隐患', '安全防护系统', 48, '终端缓冲器完满可靠', 48, 'admin', NOW()),
('其他隐患', '安全防护系统', 49, '信号装置齐备完满可靠', 49, 'admin', NOW()),
('其他隐患', '安全防护系统', 50, '轨道端部挡块器完满可靠', 50, 'admin', NOW()),
('其他隐患', '安全防护系统', 51, '轨道接地完满可靠', 51, 'admin', NOW()),
('其他隐患', '安全防护系统', 52, '转动部位保护罩完满可靠', 52, 'admin', NOW()),
('其他隐患', '安全防护系统', 53, '滑线保护挡板完满可靠', 53, 'admin', NOW()),
('其他隐患', '安全防护系统', 54, '护栏完满可靠', 54, 'admin', NOW()),
-- 司索具
('其他隐患', '司索具', 55, '司索具与钩完好有效、无明显缺失，注意钢丝板绳索和钢丝绳有无断裂，性能完好', 55, 'admin', NOW()),
-- 轻微隐患 → 归入其他隐患
('其他隐患', '使用登记状况', 56, '安全技术档案能否齐备', 56, 'admin', NOW()),
('其他隐患', '持证上岗状况', 57, '能否建立作业人员培训、考核档案', 57, 'admin', NOW()),
('其他隐患', '安全管理制度建立情况', 58, '能否建立保护维护和平时检查制度', 58, 'admin', NOW()),
('其他隐患', '安全管理制度建立情况', 59, '能否建立安全技术档案管理制度', 59, 'admin', NOW()),
('其他隐患', '安全管理制度建立情况', 60, '能否建立作业人员安全培训制度', 60, 'admin', NOW()),
('其他隐患', '安全管理制度建立情况', 61, '制度能否符合本单位状况', 61, 'admin', NOW()),
('其他隐患', '安全责任落实状况', 62, '单位负责人能否认识掌握国家有关起重机械安全管理法例、规章的有关规定', 62, 'admin', NOW()),
('其他隐患', '设施本体状况', 63, '各传动部分运转正常，润滑优秀', 63, 'admin', NOW()),
('其他隐患', '设施本体状况', 64, '秤重能否符合规定', 64, 'admin', NOW()),
('其他隐患', '钢丝绳与卷筒/滑轮槽磨损', 65, '卷筒终端部位和常经滑轮部位，绳面应有润滑油', 65, 'admin', NOW()),
('其他隐患', '安全防护系统', 66, '驾驶室地面绝缘垫完满可靠', 66, 'admin', NOW());
