# Knowledge & Regulations Full-Stack Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** End-to-end implementation of upload/management backend + admin UI for 前沿知识AI助手 (safety_maintenance_ai) and 法规标准AI助手 (regulations_ai), replacing static JSON with live API data.

**Architecture:**
- Java backend (clda-system + clda-admin) exposes REST APIs for book/chapter CRUD and regulation document upload with PDF/Word parsing.
- Admin frontend (RuoYi layout, `/index` route) provides management pages for each feature.
- Business-facing views (knowledge/index.vue, regulations/index.vue) call the same APIs instead of static data.

**Tech Stack:** Java 21 / Spring Boot 3.3 / MyBatis-Plus / Apache POI 4.1.2 (already present, for Word) / Apache PDFBox 2.0.31 (add for PDF) / Vue 3 / Element Plus / @vueup/vue-quill (already present, for rich text)

---

## File Map

### New Backend Files (clda-system)
| File | Responsibility |
|------|---------------|
| `domain/KnowledgeBook.java` | Entity: tb_knowledge_book |
| `domain/KnowledgeChapter.java` | Entity: tb_knowledge_chapter (self-referencing tree) |
| `domain/RegulationDoc.java` | Entity: tb_regulation_doc |
| `mapper/KnowledgeBookMapper.java` | MyBatis-Plus mapper for books |
| `mapper/KnowledgeChapterMapper.java` | Mapper with tree query by bookId |
| `mapper/RegulationDocMapper.java` | Mapper with category filter |
| `service/IKnowledgeBookService.java` | Service interface: book CRUD |
| `service/IKnowledgeChapterService.java` | Service interface: chapter CRUD + tree |
| `service/IRegulationDocService.java` | Service interface: doc CRUD + file upload parse |
| `service/impl/KnowledgeBookServiceImpl.java` | Implementation |
| `service/impl/KnowledgeChapterServiceImpl.java` | Implementation with recursive tree builder |
| `service/impl/RegulationDocServiceImpl.java` | Implementation with DocumentParser call |
| `util/DocumentParser.java` | Utility: parse Word/PDF MultipartFile → HTML string |

### New Backend Files (clda-admin)
| File | Responsibility |
|------|---------------|
| `controller/intellect/KnowledgeBookController.java` | Books CRUD + chapter tree endpoints |
| `controller/intellect/KnowledgeChapterController.java` | Chapter CRUD |
| `controller/intellect/RegulationDocController.java` | Regulation docs CRUD + file upload |

### New SQL
| File | Responsibility |
|------|---------------|
| `sql/knowledge-regulations-init.sql` | CREATE TABLE + sys_menu inserts |

### New Frontend Files
| File | Responsibility |
|------|---------------|
| `src/api/intellect/knowledge.js` | API calls for books and chapters |
| `src/api/intellect/regulation.js` | API calls for regulation docs |
| `src/views/intellect/knowledge-mgmt/index.vue` | Admin management page for books/chapters |
| `src/views/intellect/regulations-mgmt/index.vue` | Admin management page for regulation docs |

### Modified Frontend Files
| File | Change |
|------|--------|
| `src/views/intellect/knowledge/index.vue` | Replace static JSON imports with API calls |
| `src/views/intellect/regulations/index.vue` | Replace static JSON imports with API calls |

---

## Task 1: Database Schema

**Files:**
- Create: `sql/knowledge-regulations-init.sql`

- [ ] **Step 1: Write SQL file**

```sql
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
```

- [ ] **Step 2: Execute SQL against the clda database**

```bash
mysql -u root -p clda < sql/knowledge-regulations-init.sql
```
Expected: No errors. Three new tables created, 11 menu entries inserted.

- [ ] **Step 3: Commit**

```bash
git add sql/knowledge-regulations-init.sql
git commit -m "feat: add DB schema for knowledge books and regulation docs"
```

---

## Task 2: Add PDFBox Dependency

**Files:**
- Modify: `clda-system/pom.xml`
- Modify: `pom.xml` (root, add version property)

- [ ] **Step 1: Add version property to root pom.xml**

In `pom.xml`, inside `<properties>`, after `<poi.version>4.1.2</poi.version>`, add:
```xml
<pdfbox.version>2.0.31</pdfbox.version>
```

In `pom.xml`, inside `<dependencyManagement><dependencies>`, after the poi-ooxml block, add:
```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>${pdfbox.version}</version>
</dependency>
```

- [ ] **Step 2: Add PDFBox to clda-system pom.xml**

In `clda-system/pom.xml`, inside `<dependencies>`, add:
```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
</dependency>
```

- [ ] **Step 3: Verify compilation**

```bash
mvn compile -pl clda-system -DskipTests 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add pom.xml clda-system/pom.xml
git commit -m "chore: add PDFBox 2.0.31 dependency for PDF text extraction"
```

---

## Task 3: Backend Entities

**Files:**
- Create: `clda-system/src/main/java/com/clda/intellect/domain/KnowledgeBook.java`
- Create: `clda-system/src/main/java/com/clda/intellect/domain/KnowledgeChapter.java`
- Create: `clda-system/src/main/java/com/clda/intellect/domain/RegulationDoc.java`

- [ ] **Step 1: Create KnowledgeBook.java**

```java
// clda-system/src/main/java/com/clda/intellect/domain/KnowledgeBook.java
package com.clda.intellect.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clda.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@TableName("tb_knowledge_book")
@EqualsAndHashCode(callSuper = true)
public class KnowledgeBook extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 书名 */
    private String title;

    /** 作者 */
    private String author;

    /** 封面图片URL */
    private String coverImage;

    /** 显示顺序 */
    private Integer orderNum;

    /** 章节列表（非数据库字段，用于树形响应） */
    @TableField(exist = false)
    private List<KnowledgeChapter> chapters;
}
```

- [ ] **Step 2: Create KnowledgeChapter.java**

```java
// clda-system/src/main/java/com/clda/intellect/domain/KnowledgeChapter.java
package com.clda.intellect.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clda.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@TableName("tb_knowledge_chapter")
@EqualsAndHashCode(callSuper = true)
public class KnowledgeChapter extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属书籍ID */
    private Long bookId;

    /** 父章节ID（0=顶级） */
    private Long parentId;

    /** 章节标题 */
    private String title;

    /** 层级（1=章 2=节 3=小节） */
    private Integer level;

    /** 同级排序 */
    private Integer orderNum;

    /** 章节正文HTML（列表查询时不返回，详情查询时返回） */
    @TableField(select = false)
    private String contentHtml;

    /** 子章节列表（非数据库字段，树形结构用） */
    @TableField(exist = false)
    private List<KnowledgeChapter> children;
}
```

- [ ] **Step 3: Create RegulationDoc.java**

```java
// clda-system/src/main/java/com/clda/intellect/domain/RegulationDoc.java
package com.clda.intellect.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clda.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Date;

@Data
@TableName("tb_regulation_doc")
@EqualsAndHashCode(callSuper = true)
public class RegulationDoc extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 文档标题 */
    private String title;

    /** 分类(laws/market_rules/tsg/standards) */
    private String category;

    /** 文号/编号 */
    private String docNo;

    /** 发布日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date publishDate;

    /** 原始文件名 */
    private String fileName;

    /** 存储路径 */
    private String filePath;

    /** 解析后HTML内容（列表不返回，详情返回） */
    @TableField(select = false)
    private String contentHtml;

    /** 解析状态(NONE/DONE/FAILED) */
    private String parseStatus;
}
```

- [ ] **Step 4: Verify compilation**

```bash
mvn compile -pl clda-system -DskipTests 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add clda-system/src/main/java/com/clda/intellect/domain/
git commit -m "feat: add KnowledgeBook, KnowledgeChapter, RegulationDoc entities"
```

---

## Task 4: Document Parsing Utility

**Files:**
- Create: `clda-system/src/main/java/com/clda/intellect/util/DocumentParser.java`

- [ ] **Step 1: Create DocumentParser.java**

```java
// clda-system/src/main/java/com/clda/intellect/util/DocumentParser.java
package com.clda.intellect.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 文档解析工具：将 PDF / Word 文件内容解析为简单 HTML
 */
public class DocumentParser {

    /**
     * 解析上传文件为 HTML 字符串
     * @param file 上传的 MultipartFile（.pdf / .docx / .doc）
     * @return 简单 HTML 文本
     */
    public static String parseToHtml(MultipartFile file) throws Exception {
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (originalName.endsWith(".pdf")) {
            return parsePdf(file.getInputStream());
        } else if (originalName.endsWith(".docx") || originalName.endsWith(".doc")) {
            return parseDocx(file.getInputStream());
        } else if (originalName.endsWith(".txt")) {
            return parseTxt(file.getInputStream());
        }
        throw new IllegalArgumentException("不支持的文件格式，仅支持 PDF、Word（.docx/.doc）、TXT");
    }

    private static String parsePdf(InputStream is) throws Exception {
        try (PDDocument doc = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            return textToHtml(text);
        }
    }

    private static String parseDocx(InputStream is) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(is)) {
            StringBuilder sb = new StringBuilder();
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            for (XWPFParagraph p : paragraphs) {
                String text = p.getText();
                if (text == null || text.trim().isEmpty()) continue;
                String styleName = p.getStyle() == null ? "" : p.getStyle().toLowerCase();
                if (styleName.contains("heading1") || styleName.contains("1")) {
                    sb.append("<h2>").append(escapeHtml(text)).append("</h2>\n");
                } else if (styleName.contains("heading2") || styleName.contains("2")) {
                    sb.append("<h3>").append(escapeHtml(text)).append("</h3>\n");
                } else if (styleName.contains("heading3") || styleName.contains("3")) {
                    sb.append("<h4>").append(escapeHtml(text)).append("</h4>\n");
                } else {
                    sb.append("<p>").append(escapeHtml(text)).append("</p>\n");
                }
            }
            return sb.toString();
        }
    }

    private static String parseTxt(InputStream is) throws Exception {
        String text = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        return textToHtml(text);
    }

    /** Plain text → HTML: blank lines become paragraph breaks */
    private static String textToHtml(String text) {
        StringBuilder sb = new StringBuilder();
        String[] lines = text.split("\n");
        StringBuilder para = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (para.length() > 0) {
                    sb.append("<p>").append(escapeHtml(para.toString().trim())).append("</p>\n");
                    para.setLength(0);
                }
            } else {
                if (para.length() > 0) para.append(" ");
                para.append(trimmed);
            }
        }
        if (para.length() > 0) {
            sb.append("<p>").append(escapeHtml(para.toString().trim())).append("</p>\n");
        }
        return sb.toString();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
```

- [ ] **Step 2: Compile to verify no import errors**

```bash
mvn compile -pl clda-system -DskipTests 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add clda-system/src/main/java/com/clda/intellect/util/DocumentParser.java
git commit -m "feat: add DocumentParser utility for PDF/Word → HTML conversion"
```

---

## Task 5: Mappers

**Files:**
- Create: `clda-system/src/main/java/com/clda/intellect/mapper/KnowledgeBookMapper.java`
- Create: `clda-system/src/main/java/com/clda/intellect/mapper/KnowledgeChapterMapper.java`
- Create: `clda-system/src/main/java/com/clda/intellect/mapper/RegulationDocMapper.java`

- [ ] **Step 1: Create KnowledgeBookMapper.java**

```java
package com.clda.intellect.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.common.core.mapper.CommonMapper;
import com.clda.intellect.domain.KnowledgeBook;
import java.util.List;

public interface KnowledgeBookMapper extends CommonMapper<KnowledgeBook> {

    default List<KnowledgeBook> selectAllOrdered() {
        return this.selectList(new LambdaQueryWrapper<KnowledgeBook>()
                .orderByAsc(KnowledgeBook::getOrderNum)
                .orderByAsc(KnowledgeBook::getCreateTime));
    }
}
```

- [ ] **Step 2: Create KnowledgeChapterMapper.java**

```java
package com.clda.intellect.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.common.core.mapper.CommonMapper;
import com.clda.intellect.domain.KnowledgeChapter;
import java.util.List;

public interface KnowledgeChapterMapper extends CommonMapper<KnowledgeChapter> {

    /** 按书籍ID查询所有章节（不含 content_html，按 level+order_num 排序）*/
    default List<KnowledgeChapter> selectByBookId(Long bookId) {
        return this.selectList(new LambdaQueryWrapper<KnowledgeChapter>()
                .eq(KnowledgeChapter::getBookId, bookId)
                .orderByAsc(KnowledgeChapter::getLevel)
                .orderByAsc(KnowledgeChapter::getOrderNum));
    }

    /** 查询单章节（含 content_html），需要 selectById 覆盖默认 select=false */
    default KnowledgeChapter selectWithContent(Long id) {
        return this.selectOne(new LambdaQueryWrapper<KnowledgeChapter>()
                .select(KnowledgeChapter.class, f -> true)   // select all fields including content_html
                .eq(KnowledgeChapter::getId, id));
    }
}
```

- [ ] **Step 3: Create RegulationDocMapper.java**

```java
package com.clda.intellect.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.common.core.mapper.CommonMapper;
import com.clda.intellect.domain.RegulationDoc;
import java.util.List;

public interface RegulationDocMapper extends CommonMapper<RegulationDoc> {

    default List<RegulationDoc> selectList(RegulationDoc query) {
        LambdaQueryWrapper<RegulationDoc> qw = new LambdaQueryWrapper<>();
        qw.eq(StrUtil.isNotBlank(query.getCategory()), RegulationDoc::getCategory, query.getCategory());
        qw.like(StrUtil.isNotBlank(query.getTitle()), RegulationDoc::getTitle, query.getTitle());
        qw.orderByDesc(RegulationDoc::getCreateTime);
        return this.selectList(qw);
    }

    /** 查询单文档（含 content_html）*/
    default RegulationDoc selectWithContent(Long id) {
        return this.selectOne(new LambdaQueryWrapper<RegulationDoc>()
                .select(RegulationDoc.class, f -> true)
                .eq(RegulationDoc::getId, id));
    }
}
```

- [ ] **Step 4: Compile**

```bash
mvn compile -pl clda-system -DskipTests 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add clda-system/src/main/java/com/clda/intellect/mapper/
git commit -m "feat: add mappers for KnowledgeBook, KnowledgeChapter, RegulationDoc"
```

---

## Task 6: Service Layer — Knowledge

**Files:**
- Create: `clda-system/src/main/java/com/clda/intellect/service/IKnowledgeBookService.java`
- Create: `clda-system/src/main/java/com/clda/intellect/service/IKnowledgeChapterService.java`
- Create: `clda-system/src/main/java/com/clda/intellect/service/impl/KnowledgeBookServiceImpl.java`
- Create: `clda-system/src/main/java/com/clda/intellect/service/impl/KnowledgeChapterServiceImpl.java`

- [ ] **Step 1: Create IKnowledgeBookService.java**

```java
package com.clda.intellect.service;

import com.clda.intellect.domain.KnowledgeBook;
import java.util.List;

public interface IKnowledgeBookService {
    /** 查询所有书籍（含章节树，不含章节 content_html） */
    List<KnowledgeBook> selectBooksWithChapterTree();

    /** 查询书籍列表（不含章节，用于管理列表） */
    List<KnowledgeBook> selectBookList();

    KnowledgeBook selectBookById(Long id);

    int insertBook(KnowledgeBook book);

    int updateBook(KnowledgeBook book);

    int deleteBookById(Long id);
}
```

- [ ] **Step 2: Create IKnowledgeChapterService.java**

```java
package com.clda.intellect.service;

import com.clda.intellect.domain.KnowledgeChapter;
import java.util.List;

public interface IKnowledgeChapterService {
    /** 查询书籍的章节树（不含 content_html） */
    List<KnowledgeChapter> selectChapterTree(Long bookId);

    /** 查询单章节（含 content_html） */
    KnowledgeChapter selectChapterWithContent(Long id);

    int insertChapter(KnowledgeChapter chapter);

    int updateChapter(KnowledgeChapter chapter);

    int deleteChapterById(Long id);

    /** 删除书籍的所有章节（书籍删除时调用） */
    int deleteChaptersByBookId(Long bookId);
}
```

- [ ] **Step 3: Create KnowledgeBookServiceImpl.java**

```java
package com.clda.intellect.service.impl;

import com.clda.intellect.domain.KnowledgeBook;
import com.clda.intellect.mapper.KnowledgeBookMapper;
import com.clda.intellect.service.IKnowledgeBookService;
import com.clda.intellect.service.IKnowledgeChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeBookServiceImpl implements IKnowledgeBookService {

    private final KnowledgeBookMapper bookMapper;
    private final IKnowledgeChapterService chapterService;

    @Override
    public List<KnowledgeBook> selectBooksWithChapterTree() {
        List<KnowledgeBook> books = bookMapper.selectAllOrdered();
        for (KnowledgeBook book : books) {
            book.setChapters(chapterService.selectChapterTree(book.getId()));
        }
        return books;
    }

    @Override
    public List<KnowledgeBook> selectBookList() {
        return bookMapper.selectAllOrdered();
    }

    @Override
    public KnowledgeBook selectBookById(Long id) {
        return bookMapper.selectById(id);
    }

    @Override
    public int insertBook(KnowledgeBook book) {
        if (book.getOrderNum() == null) book.setOrderNum(0);
        return bookMapper.insert(book);
    }

    @Override
    public int updateBook(KnowledgeBook book) {
        return bookMapper.updateById(book);
    }

    @Override
    @Transactional
    public int deleteBookById(Long id) {
        chapterService.deleteChaptersByBookId(id);
        return bookMapper.deleteById(id);
    }
}
```

- [ ] **Step 4: Create KnowledgeChapterServiceImpl.java**

```java
package com.clda.intellect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.intellect.domain.KnowledgeChapter;
import com.clda.intellect.mapper.KnowledgeChapterMapper;
import com.clda.intellect.service.IKnowledgeChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeChapterServiceImpl implements IKnowledgeChapterService {

    private final KnowledgeChapterMapper chapterMapper;

    @Override
    public List<KnowledgeChapter> selectChapterTree(Long bookId) {
        List<KnowledgeChapter> all = chapterMapper.selectByBookId(bookId);
        return buildTree(all);
    }

    @Override
    public KnowledgeChapter selectChapterWithContent(Long id) {
        return chapterMapper.selectWithContent(id);
    }

    @Override
    public int insertChapter(KnowledgeChapter chapter) {
        if (chapter.getParentId() == null) chapter.setParentId(0L);
        if (chapter.getOrderNum() == null) chapter.setOrderNum(0);
        if (chapter.getLevel() == null) chapter.setLevel(1);
        return chapterMapper.insert(chapter);
    }

    @Override
    public int updateChapter(KnowledgeChapter chapter) {
        return chapterMapper.updateById(chapter);
    }

    @Override
    public int deleteChapterById(Long id) {
        // Also delete all descendants
        deleteDescendants(id);
        return chapterMapper.deleteById(id);
    }

    @Override
    public int deleteChaptersByBookId(Long bookId) {
        return chapterMapper.delete(new LambdaQueryWrapper<KnowledgeChapter>()
                .eq(KnowledgeChapter::getBookId, bookId));
    }

    private void deleteDescendants(Long parentId) {
        List<KnowledgeChapter> children = chapterMapper.selectList(
                new LambdaQueryWrapper<KnowledgeChapter>().eq(KnowledgeChapter::getParentId, parentId));
        for (KnowledgeChapter child : children) {
            deleteDescendants(child.getId());
            chapterMapper.deleteById(child.getId());
        }
    }

    /** Build nested tree from flat list */
    private List<KnowledgeChapter> buildTree(List<KnowledgeChapter> all) {
        Map<Long, List<KnowledgeChapter>> byParent = all.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() == null ? 0L : c.getParentId()));
        for (KnowledgeChapter chapter : all) {
            List<KnowledgeChapter> children = byParent.getOrDefault(chapter.getId(), new ArrayList<>());
            chapter.setChildren(children.isEmpty() ? new ArrayList<>() : children);
        }
        return byParent.getOrDefault(0L, new ArrayList<>());
    }
}
```

- [ ] **Step 5: Compile**

```bash
mvn compile -pl clda-system -DskipTests 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add clda-system/src/main/java/com/clda/intellect/service/
git commit -m "feat: add knowledge book and chapter service layer"
```

---

## Task 7: Service Layer — Regulation

**Files:**
- Create: `clda-system/src/main/java/com/clda/intellect/service/IRegulationDocService.java`
- Create: `clda-system/src/main/java/com/clda/intellect/service/impl/RegulationDocServiceImpl.java`

- [ ] **Step 1: Create IRegulationDocService.java**

```java
package com.clda.intellect.service;

import com.clda.intellect.domain.RegulationDoc;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface IRegulationDocService {
    List<RegulationDoc> selectDocList(RegulationDoc query);

    RegulationDoc selectDocById(Long id);

    /** Upload file, parse content, save record */
    RegulationDoc uploadAndParse(MultipartFile file, String title, String category,
                                  String docNo, String publishDate, String operName) throws Exception;

    int insertDoc(RegulationDoc doc);

    int updateDoc(RegulationDoc doc);

    int deleteDocByIds(Long[] ids);
}
```

- [ ] **Step 2: Create RegulationDocServiceImpl.java**

```java
package com.clda.intellect.service.impl;

import com.clda.common.config.CldaConfig;
import com.clda.common.utils.file.FileUploadUtils;
import com.clda.intellect.domain.RegulationDoc;
import com.clda.intellect.mapper.RegulationDocMapper;
import com.clda.intellect.service.IRegulationDocService;
import com.clda.intellect.util.DocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegulationDocServiceImpl implements IRegulationDocService {

    private final RegulationDocMapper docMapper;

    @Override
    public List<RegulationDoc> selectDocList(RegulationDoc query) {
        return docMapper.selectList(query);
    }

    @Override
    public RegulationDoc selectDocById(Long id) {
        return docMapper.selectWithContent(id);
    }

    @Override
    public RegulationDoc uploadAndParse(MultipartFile file, String title, String category,
                                         String docNo, String publishDate, String operName) throws Exception {
        // Save file to disk
        String uploadPath = CldaConfig.getUploadPath() + "/regulations";
        String filePath = FileUploadUtils.upload(uploadPath, file);

        // Parse content
        String contentHtml;
        String parseStatus;
        try {
            contentHtml = DocumentParser.parseToHtml(file);
            parseStatus = "DONE";
        } catch (Exception e) {
            log.error("文档解析失败: {}", file.getOriginalFilename(), e);
            contentHtml = "<p>文档解析失败，请手动编辑内容。</p>";
            parseStatus = "FAILED";
        }

        RegulationDoc doc = new RegulationDoc();
        doc.setTitle(title);
        doc.setCategory(category);
        doc.setDocNo(docNo);
        doc.setFileName(file.getOriginalFilename());
        doc.setFilePath(filePath);
        doc.setContentHtml(contentHtml);
        doc.setParseStatus(parseStatus);
        doc.setCreateBy(operName);
        if (publishDate != null && !publishDate.isEmpty()) {
            try {
                doc.setPublishDate(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(publishDate));
            } catch (Exception ignored) {}
        }
        docMapper.insert(doc);
        return doc;
    }

    @Override
    public int insertDoc(RegulationDoc doc) {
        doc.setParseStatus("NONE");
        return docMapper.insert(doc);
    }

    @Override
    public int updateDoc(RegulationDoc doc) {
        return docMapper.updateById(doc);
    }

    @Override
    public int deleteDocByIds(Long[] ids) {
        return docMapper.deleteByIds(Arrays.asList(ids));
    }
}
```

- [ ] **Step 3: Compile**

```bash
mvn compile -pl clda-system -DskipTests 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add clda-system/src/main/java/com/clda/intellect/service/
git commit -m "feat: add regulation doc service with PDF/Word upload and parsing"
```

---

## Task 8: REST Controllers

**Files:**
- Create: `clda-admin/src/main/java/com/clda/web/controller/intellect/KnowledgeBookController.java`
- Create: `clda-admin/src/main/java/com/clda/web/controller/intellect/KnowledgeChapterController.java`
- Create: `clda-admin/src/main/java/com/clda/web/controller/intellect/RegulationDocController.java`

- [ ] **Step 1: Create KnowledgeBookController.java**

```java
package com.clda.web.controller.intellect;

import com.clda.common.annotation.Log;
import com.clda.common.core.controller.BaseController;
import com.clda.common.core.domain.AjaxResult;
import com.clda.common.core.page.TableDataInfo;
import com.clda.common.enums.BusinessType;
import com.clda.intellect.domain.KnowledgeBook;
import com.clda.intellect.service.IKnowledgeBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/intellect/knowledge")
public class KnowledgeBookController extends BaseController {

    private final IKnowledgeBookService bookService;

    /** 查询所有书籍（含章节树，供业务端前端使用） */
    @GetMapping("/books/tree")
    public AjaxResult booksWithTree() {
        return success(bookService.selectBooksWithChapterTree());
    }

    /** 查询书籍列表（管理端分页） */
    @PreAuthorize("@ss.hasPermi('crane:knowledge:list')")
    @GetMapping("/books")
    public TableDataInfo list() {
        startPage();
        List<KnowledgeBook> list = bookService.selectBookList();
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('crane:knowledge:query')")
    @GetMapping("/books/{id}")
    public AjaxResult getBook(@PathVariable Long id) {
        return success(bookService.selectBookById(id));
    }

    @PreAuthorize("@ss.hasPermi('crane:knowledge:add')")
    @Log(title = "知识管理", businessType = BusinessType.INSERT)
    @PostMapping("/books")
    public AjaxResult addBook(@RequestBody KnowledgeBook book) {
        return toAjax(bookService.insertBook(book));
    }

    @PreAuthorize("@ss.hasPermi('crane:knowledge:edit')")
    @Log(title = "知识管理", businessType = BusinessType.UPDATE)
    @PutMapping("/books")
    public AjaxResult editBook(@RequestBody KnowledgeBook book) {
        return toAjax(bookService.updateBook(book));
    }

    @PreAuthorize("@ss.hasPermi('crane:knowledge:remove')")
    @Log(title = "知识管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/books/{id}")
    public AjaxResult removeBook(@PathVariable Long id) {
        return toAjax(bookService.deleteBookById(id));
    }
}
```

- [ ] **Step 2: Create KnowledgeChapterController.java**

```java
package com.clda.web.controller.intellect;

import com.clda.common.annotation.Log;
import com.clda.common.core.controller.BaseController;
import com.clda.common.core.domain.AjaxResult;
import com.clda.common.enums.BusinessType;
import com.clda.intellect.domain.KnowledgeChapter;
import com.clda.intellect.service.IKnowledgeChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/intellect/knowledge/chapters")
public class KnowledgeChapterController extends BaseController {

    private final IKnowledgeChapterService chapterService;

    /** 查询书籍章节树（不含正文，供管理端和业务端列表使用） */
    @GetMapping("/tree")
    public AjaxResult chapterTree(@RequestParam Long bookId) {
        return success(chapterService.selectChapterTree(bookId));
    }

    /** 查询章节详情（含正文 content_html） */
    @GetMapping("/{id}")
    public AjaxResult getChapter(@PathVariable Long id) {
        return success(chapterService.selectChapterWithContent(id));
    }

    @PreAuthorize("@ss.hasPermi('crane:knowledge:add')")
    @Log(title = "章节管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult addChapter(@RequestBody KnowledgeChapter chapter) {
        return toAjax(chapterService.insertChapter(chapter));
    }

    @PreAuthorize("@ss.hasPermi('crane:knowledge:edit')")
    @Log(title = "章节管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult editChapter(@RequestBody KnowledgeChapter chapter) {
        return toAjax(chapterService.updateChapter(chapter));
    }

    @PreAuthorize("@ss.hasPermi('crane:knowledge:remove')")
    @Log(title = "章节管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult removeChapter(@PathVariable Long id) {
        return toAjax(chapterService.deleteChapterById(id));
    }
}
```

- [ ] **Step 3: Create RegulationDocController.java**

```java
package com.clda.web.controller.intellect;

import com.clda.common.annotation.Log;
import com.clda.common.core.controller.BaseController;
import com.clda.common.core.domain.AjaxResult;
import com.clda.common.core.page.TableDataInfo;
import com.clda.common.enums.BusinessType;
import com.clda.common.utils.SecurityUtils;
import com.clda.intellect.domain.RegulationDoc;
import com.clda.intellect.service.IRegulationDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/intellect/regulation")
public class RegulationDocController extends BaseController {

    private final IRegulationDocService docService;

    /** 查询法规列表（分页，管理端及业务端共用） */
    @GetMapping("/list")
    public TableDataInfo list(RegulationDoc query) {
        startPage();
        List<RegulationDoc> list = docService.selectDocList(query);
        return getDataTable(list);
    }

    /** 查询法规详情（含 content_html） */
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(docService.selectDocById(id));
    }

    /** 上传并解析法规文件 */
    @PreAuthorize("@ss.hasPermi('crane:regulation:upload')")
    @Log(title = "法规管理", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestParam(value = "docNo", required = false) String docNo,
            @RequestParam(value = "publishDate", required = false) String publishDate) throws Exception {
        String operName = SecurityUtils.getUsername();
        RegulationDoc doc = docService.uploadAndParse(file, title, category, docNo, publishDate, operName);
        return success(doc);
    }

    /** 手动新增法规（无文件，直接录入内容） */
    @PreAuthorize("@ss.hasPermi('crane:regulation:add')")
    @Log(title = "法规管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody RegulationDoc doc) {
        return toAjax(docService.insertDoc(doc));
    }

    @PreAuthorize("@ss.hasPermi('crane:regulation:edit')")
    @Log(title = "法规管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody RegulationDoc doc) {
        return toAjax(docService.updateDoc(doc));
    }

    @PreAuthorize("@ss.hasPermi('crane:regulation:remove')")
    @Log(title = "法规管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(docService.deleteDocByIds(ids));
    }
}
```

- [ ] **Step 4: Build the full admin module**

```bash
mvn clean install -pl clda-system,clda-admin -am -DskipTests 2>&1 | tail -10
```
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Quick smoke test (server start)**

```bash
mvn spring-boot:run -pl clda-admin &
sleep 15
curl -s -X POST http://localhost:8080/system/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123","code":"1","uuid":"test"}' | python3 -m json.tool | head -5
```
Expected: JSON with `"code": 200` and a token. (Captcha may reject — that's fine, just verify server started.)

Kill the background server: `kill %1`

- [ ] **Step 6: Commit**

```bash
git add clda-admin/src/main/java/com/clda/web/controller/intellect/
git commit -m "feat: add REST controllers for knowledge books/chapters and regulation docs"
```

---

## Task 9: Frontend API Files

**Files:**
- Create: `clda-ui/src/api/intellect/knowledge.js`
- Create: `clda-ui/src/api/intellect/regulation.js`

- [ ] **Step 1: Create knowledge.js**

```javascript
// clda-ui/src/api/intellect/knowledge.js
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
```

- [ ] **Step 2: Create regulation.js**

```javascript
// clda-ui/src/api/intellect/regulation.js
import request from '@/utils/request'

/** 查询法规列表（支持 category / title 过滤，分页） */
export function listRegulations(query) {
  return request({ url: '/intellect/regulation/list', method: 'get', params: query })
}

/** 查询法规详情（含 content_html） */
export function getRegulation(id) {
  return request({ url: '/intellect/regulation/' + id, method: 'get' })
}

export function addRegulation(data) {
  return request({ url: '/intellect/regulation', method: 'post', data })
}

export function updateRegulation(data) {
  return request({ url: '/intellect/regulation', method: 'put', data })
}

export function deleteRegulation(ids) {
  return request({ url: '/intellect/regulation/' + ids, method: 'delete' })
}
```

- [ ] **Step 3: Commit**

```bash
git add clda-ui/src/api/intellect/knowledge.js clda-ui/src/api/intellect/regulation.js
git commit -m "feat: add frontend API helpers for knowledge and regulation endpoints"
```

---

## Task 10: Admin Management Page — Knowledge Books

**Files:**
- Create: `clda-ui/src/views/intellect/knowledge-mgmt/index.vue`

This page has two panels:
- Left: Book list (table with add/edit/delete)
- Right: Chapter tree for the selected book, with add/edit/delete chapter buttons and a Quill editor dialog

- [ ] **Step 1: Create the component**

```vue
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
import { ref, reactive, defineComponent, h } from 'vue'
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
```

- [ ] **Step 2: Commit**

```bash
git add clda-ui/src/views/intellect/knowledge-mgmt/
git commit -m "feat: add knowledge book and chapter admin management page"
```

---

## Task 11: Admin Management Page — Regulations

**Files:**
- Create: `clda-ui/src/views/intellect/regulations-mgmt/index.vue`

- [ ] **Step 1: Create the component**

```vue
<template>
  <div class="app-container">
    <!-- 分类 tabs -->
    <div class="reg-tabs">
      <button
        v-for="cat in categories"
        :key="cat.id"
        class="reg-tab"
        :class="{ active: queryParams.category === cat.id }"
        @click="switchCategory(cat.id)"
      >{{ cat.label }}</button>
    </div>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8" style="margin-top:12px">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Upload" @click="uploadDialog.open = true"
          v-hasPermi="['crane:regulation:upload']">上传法规文件</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="ids.length === 0"
          @click="handleDelete" v-hasPermi="['crane:regulation:remove']">删除</el-button>
      </el-col>
      <el-form :inline="true" style="margin-left:auto">
        <el-form-item>
          <el-input v-model="queryParams.title" placeholder="搜索标题" clearable
            @keyup.enter="handleQuery" style="width:200px" />
          <el-button type="primary" icon="Search" @click="handleQuery" style="margin-left:8px">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-row>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="docList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="docNo" label="文号/编号" width="160" show-overflow-tooltip />
      <el-table-column prop="publishDate" label="发布日期" width="110" />
      <el-table-column prop="parseStatus" label="状态" width="80">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.parseStatus)" size="small">
            {{ statusLabel(scope.row.parseStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handlePreview(scope.row)">预览</el-button>
          <el-button link type="primary" icon="Edit" @click="handleEdit(scope.row)"
            v-hasPermi="['crane:regulation:edit']">编辑</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDeleteOne(scope.row)"
            v-hasPermi="['crane:regulation:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total"
      v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 上传对话框 -->
    <el-dialog title="上传法规文件" v-model="uploadDialog.open" width="520px" append-to-body>
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="90px">
        <el-form-item label="文档标题" prop="title">
          <el-input v-model="uploadForm.title" placeholder="请输入文档标题" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="uploadForm.category" placeholder="请选择分类">
            <el-option v-for="c in categories" :key="c.id" :label="c.label" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="文号/编号">
          <el-input v-model="uploadForm.docNo" placeholder="如: 主席令第88号" />
        </el-form-item>
        <el-form-item label="发布日期">
          <el-date-picker v-model="uploadForm.publishDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="文件" prop="file">
          <el-upload
            ref="fileUploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".pdf,.docx,.doc,.txt"
            :on-change="onFileChange"
            :on-remove="() => uploadForm.file = null"
          >
            <el-button icon="Upload">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 PDF、Word（.docx/.doc）、TXT，文件将自动解析为可读文本</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitUpload" :loading="uploadDialog.loading">上传并解析</el-button>
        <el-button @click="uploadDialog.open = false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 编辑对话框 -->
    <el-dialog title="编辑法规" v-model="editDialog.open" width="860px" append-to-body :close-on-click-modal="false">
      <el-form ref="editFormRef" :model="editForm" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="文档标题" :rules="[{required:true,message:'标题不能为空'}]" prop="title">
              <el-input v-model="editForm.title" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="分类">
              <el-select v-model="editForm.category">
                <el-option v-for="c in categories" :key="c.id" :label="c.label" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="发布日期">
              <el-date-picker v-model="editForm.publishDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="文号/编号">
              <el-input v-model="editForm.docNo" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="正文内容">
          <div style="border:1px solid #e2e8f0;border-radius:6px;overflow:hidden;width:100%">
            <QuillEditor v-model:content="editForm.contentHtml" content-type="html"
              theme="snow" style="min-height:300px" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitEdit" :loading="editDialog.saving">保 存</el-button>
        <el-button @click="editDialog.open = false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog :title="previewTitle" v-model="previewOpen" width="760px" append-to-body>
      <div class="doc-preview" v-html="previewContent" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import { listRegulations, getRegulation, updateRegulation, deleteRegulation } from '@/api/intellect/regulation'
import { getToken } from '@/utils/auth'

const categories = [
  { id: 'laws', label: '法律法规' },
  { id: 'market_rules', label: '市场监管规章' },
  { id: 'tsg', label: 'TSG技术规范' },
  { id: 'standards', label: '标准' }
]

const docList = ref([])
const loading = ref(false)
const total = ref(0)
const ids = ref([])
const queryParams = ref({ pageNum: 1, pageSize: 10, category: 'laws', title: '' })

// Upload
const uploadDialog = reactive({ open: false, loading: false })
const uploadForm = ref({ title: '', category: 'laws', docNo: '', publishDate: '', file: null })
const uploadRules = {
  title: [{ required: true, message: '文档标题不能为空', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  file: [{ required: true, message: '请选择文件', trigger: 'change' }]
}
const uploadFormRef = ref(null)
const fileUploadRef = ref(null)

// Edit
const editDialog = reactive({ open: false, saving: false })
const editForm = ref({})
const editFormRef = ref(null)

// Preview
const previewOpen = ref(false)
const previewTitle = ref('')
const previewContent = ref('')

function statusType(s) {
  return { DONE: 'success', FAILED: 'danger', NONE: 'info' }[s] || 'info'
}
function statusLabel(s) {
  return { DONE: '已解析', FAILED: '解析失败', NONE: '未解析' }[s] || s
}

function getList() {
  loading.value = true
  listRegulations(queryParams.value).then(res => {
    docList.value = res.rows || []
    total.value = res.total
    loading.value = false
  })
}

function switchCategory(cat) {
  queryParams.value.category = cat
  queryParams.value.pageNum = 1
  getList()
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function handleSelectionChange(sel) {
  ids.value = sel.map(r => r.id)
}

function onFileChange(file) {
  uploadForm.value.file = file.raw
}

async function submitUpload() {
  await uploadFormRef.value.validate()
  if (!uploadForm.value.file) { ElMessage.warning('请选择文件'); return }
  uploadDialog.loading = true
  const fd = new FormData()
  fd.append('file', uploadForm.value.file)
  fd.append('title', uploadForm.value.title)
  fd.append('category', uploadForm.value.category)
  if (uploadForm.value.docNo) fd.append('docNo', uploadForm.value.docNo)
  if (uploadForm.value.publishDate) fd.append('publishDate', uploadForm.value.publishDate)
  try {
    const res = await fetch(import.meta.env.VITE_APP_BASE_API + '/intellect/regulation/upload', {
      method: 'POST',
      headers: { Authorization: 'Bearer ' + getToken() },
      body: fd
    })
    const json = await res.json()
    if (json.code === 200) {
      ElMessage.success('上传成功，文档已解析')
      uploadDialog.open = false
      uploadForm.value = { title: '', category: queryParams.value.category, docNo: '', publishDate: '', file: null }
      fileUploadRef.value?.clearFiles()
      getList()
    } else {
      ElMessage.error(json.msg || '上传失败')
    }
  } catch (e) {
    ElMessage.error('上传请求失败')
  } finally {
    uploadDialog.loading = false
  }
}

async function handleEdit(row) {
  const res = await getRegulation(row.id)
  editForm.value = { ...res.data, contentHtml: res.data.contentHtml || '' }
  editDialog.open = true
}

async function submitEdit() {
  editDialog.saving = true
  try {
    await updateRegulation(editForm.value)
    ElMessage.success('保存成功')
    editDialog.open = false
    getList()
  } finally {
    editDialog.saving = false
  }
}

async function handlePreview(row) {
  const res = await getRegulation(row.id)
  previewTitle.value = res.data.title
  previewContent.value = res.data.contentHtml || '<p>暂无内容</p>'
  previewOpen.value = true
}

async function handleDelete() {
  await ElMessageBox.confirm(`确认删除所选 ${ids.value.length} 条法规？`, '提示', { type: 'warning' })
  await deleteRegulation(ids.value.join(','))
  ElMessage.success('删除成功')
  getList()
}

async function handleDeleteOne(row) {
  await ElMessageBox.confirm(`确认删除「${row.title}」？`, '提示', { type: 'warning' })
  await deleteRegulation(row.id)
  ElMessage.success('删除成功')
  getList()
}

onMounted(() => getList())
</script>

<style lang="scss" scoped>
.reg-tabs {
  display: flex; gap: 8px; border-bottom: 1px solid #e2e8f0; padding-bottom: 0;
}
.reg-tab {
  padding: 8px 16px; border: none; border-bottom: 3px solid transparent;
  background: none; cursor: pointer; font-size: 14px; color: #475569;
  transition: all 0.15s;
  &:hover { color: #1e293b; }
  &.active { color: #64748b; font-weight: 600; border-bottom-color: #64748b; }
}
.doc-preview {
  max-height: 60vh; overflow-y: auto;
  :deep(h2) { font-size: 18px; font-weight: 700; margin: 0 0 12px; }
  :deep(h3) { font-size: 15px; font-weight: 600; margin: 16px 0 8px; }
  :deep(p) { font-size: 14px; line-height: 1.8; margin-bottom: 10px; color: #374151; }
  :deep(table) { width: 100%; border-collapse: collapse; margin: 12px 0;
    th, td { border: 1px solid #e2e8f0; padding: 7px 10px; font-size: 13px; }
    th { background: #f1f5f9; font-weight: 600; }
  }
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add clda-ui/src/views/intellect/regulations-mgmt/
git commit -m "feat: add regulation document admin management page with file upload"
```

---

## Task 12: Update Business-Facing Views to Use API

**Files:**
- Modify: `clda-ui/src/views/intellect/knowledge/index.vue`
- Modify: `clda-ui/src/views/intellect/regulations/index.vue`

- [ ] **Step 1: Update knowledge/index.vue**

Remove the static import at the top:
```javascript
// REMOVE this line:
import { knowledgeBooks } from '@/data/knowledgeBooks'
```

Add the API import at the top of `<script setup>`:
```javascript
import { getBooksWithTree, getChapter } from '@/api/intellect/knowledge'
```

Change the data initialization from:
```javascript
const books = knowledgeBooks
```
To:
```javascript
const books = ref([])
const pageLoading = ref(false)

onMounted(() => {
  pageLoading.value = true
  getBooksWithTree().then(res => {
    books.value = res.data || []
    pageLoading.value = false
  })
})
```

Update the template to use `books.value` (since it's now a ref, change `v-for="book in books"` to `v-for="book in books"` — Vue auto-unwraps refs in templates, no change needed in template).

Update `selectedChapter` computed to be async-aware: when a leaf chapter is clicked, **fetch full content** via `getChapter(id)` instead of walking the local tree for content. Change `handleNodeClick` for leaf nodes:

```javascript
async function handleNodeClick(node) {
  if (node.children && node.children.length > 0) {
    const ids = new Set(expandedIds.value)
    ids.has(node.id) ? ids.delete(node.id) : ids.add(node.id)
    expandedIds.value = ids
    selectedChapterId.value = node.id
    selectedChapterContent.value = node  // use cached summary
    if (isTablet.value) isDetailMode.value = true
  } else {
    selectedChapterId.value = node.id
    contentLoading.value = true
    if (isTablet.value) isDetailMode.value = true
    const res = await getChapter(node.id)
    selectedChapterContent.value = res.data
    contentLoading.value = false
  }
}
```

Add `selectedChapterContent` ref and `contentLoading` ref, replace `selectedChapter` computed usage in template with `selectedChapterContent`:
```javascript
const selectedChapterContent = ref(null)
const contentLoading = ref(false)
```

In template, change `v-html="selectedChapter.content_html"` to `v-html="selectedChapterContent?.content_html"` and add `v-loading="contentLoading"` to the content area div.

Also update `flatChapters` to walk `books.value` instead of `books`.

- [ ] **Step 2: Update regulations/index.vue**

Remove the static imports:
```javascript
// REMOVE these:
import { regulationsData, regulationCategories } from '@/data/regulations'
```

Add API imports:
```javascript
import { listRegulations, getRegulation } from '@/api/intellect/regulation'
```

Replace data initialization:
```javascript
// REMOVE:
const categories = regulationCategories
const docs = regulationsData

// REPLACE WITH:
const categories = [
  { id: 'laws', label: '法律法规' },
  { id: 'market_rules', label: '市场监管规章' },
  { id: 'tsg', label: 'TSG技术规范' },
  { id: 'standards', label: '标准' }
]

const allDocs = ref([])         // current category docs (no content_html)
const listLoading = ref(false)

function loadDocs() {
  listLoading.value = true
  listRegulations({ category: activeCategory.value, pageNum: 1, pageSize: 100 }).then(res => {
    allDocs.value = res.rows || []
    listLoading.value = false
  })
}
```

Replace `filteredDocs` computed:
```javascript
// REMOVE: const filteredDocs = computed(() => docs.filter(d => d.category === activeCategory.value))
// REPLACE WITH:
const filteredDocs = computed(() => allDocs.value)
```

Replace `selectedDoc` computed — when a doc is clicked, fetch full content:
```javascript
// REMOVE: const selectedDoc = computed(() => docs.find(d => d.id === selectedDocId.value) || null)
// ADD:
const selectedDoc = ref(null)
const docLoading = ref(false)
```

Update `selectDoc` function:
```javascript
async function selectDoc(docId) {
  selectedDocId.value = docId
  if (isTablet.value) isDetailMode.value = true
  docLoading.value = true
  const res = await getRegulation(docId)
  selectedDoc.value = res.data
  docLoading.value = false
}
```

Update `switchCategory`:
```javascript
function switchCategory(catId) {
  activeCategory.value = catId
  selectedDocId.value = null
  selectedDoc.value = null
  loadDocs()
}
```

Add `v-loading="listLoading"` to the doc list div and `v-loading="docLoading"` to the content panel.

Call `loadDocs()` in `onMounted`.

- [ ] **Step 3: Verify frontend compiles**

```bash
cd clda-ui && npm run build:prod 2>&1 | tail -20
```
Expected: Build completes without errors (warnings OK).

- [ ] **Step 4: Commit**

```bash
git add clda-ui/src/views/intellect/knowledge/index.vue \
        clda-ui/src/views/intellect/regulations/index.vue
git commit -m "feat: wire knowledge and regulations business views to live backend APIs"
```

---

## Task 13: End-to-End Verification

- [ ] **Step 1: Start backend**

```bash
mvn spring-boot:run -pl clda-admin &
```
Wait for "Started CldaApplication" in output.

- [ ] **Step 2: Start frontend dev server**

```bash
cd clda-ui && npm run dev
```
Open: `http://localhost/login` — login with admin / admin123

- [ ] **Step 3: Verify admin menu appears**

After login, the left sidebar should show 知识管理 and 法规管理 under the 起重设备管理 directory. If they don't appear, confirm the SQL was executed and check that the user has role_id=2.

- [ ] **Step 4: Test knowledge management**

1. Navigate to 知识管理 → should see the two-panel page
2. Click 新增书籍 → add "测试书籍" with author "测试作者" → save
3. Click the book card → right panel shows "暂无章节"
4. Click 添加章节 → add "第一章 总则" (level=1, content "这是第一章正文") → save
5. The chapter appears in tree
6. Click 编辑 on the chapter → Quill editor opens with the content → edit and save
7. Click 添加子节 → add "1.1 适用范围" (auto level=2) → save
8. Click 删除书籍 → confirm → book and chapters removed

- [ ] **Step 5: Test regulation management**

1. Navigate to 法规管理 → should see the tabs + table
2. Switch to different category tabs → table updates
3. Click 上传法规文件 → fill in title "测试法规"，分类="法律法规" → select a PDF or Word file → click 上传并解析
4. Record appears in table with parseStatus=已解析
5. Click 预览 → content dialog opens with parsed text
6. Click 编辑 → title/category/content editable → save
7. Click 删除 → record removed

- [ ] **Step 6: Test business-facing views**

1. Navigate to `/app` → click "前沿知识" AI bar button → navigates to `/app?service=safety_maintenance_ai`
2. Left panel shows books loaded from API (or empty if none added)
3. Add books/chapters in admin → refresh business view → new content appears
4. Navigate to `/app` → click "法规标准" → shows docs from DB (or empty if none added)
5. On robot screen (`/robot/app?service=safety_maintenance_ai`) — same views work

---

## Self-Review Checklist

### Spec coverage
- [x] K-01: Chapter tree display (left panel, expandable) — Tasks 3-10
- [x] K-02: Chapter content display with images — Tasks 3, 10, 12
- [x] K-03: Image rendering — content_html stores inline images from Quill
- [x] K-04: Prev/Next chapter nav — preserved in knowledge/index.vue (flat leaves)
- [x] R-01: Category tab switching — regulations/index.vue + regulations-mgmt
- [x] R-02: Document list by category — RegulationDocController list endpoint
- [x] R-03: Full document content viewing — getRegulation(id) loads content_html
- [x] R-04: Admin file upload — Task 11 upload dialog + RegulationDocController
- [x] R-05: PDF/Word parsing to HTML — Task 4 DocumentParser + Task 7 service
- [x] A-01: Regulation upload with title/category/metadata — Task 11
- [x] A-02: Regulation doc management (list/edit/delete) — Task 11
- [x] A-03: Knowledge book/chapter entry via admin — Task 10
- [x] A-04: Chapter content editing via rich text — Task 10 (Quill editor)

### No placeholders
- All code blocks are complete
- All file paths are exact
- All method names are consistent across tasks
