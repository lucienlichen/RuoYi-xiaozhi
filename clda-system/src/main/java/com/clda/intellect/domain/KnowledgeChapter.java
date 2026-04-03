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

    /** 原始文件名 */
    private String fileName;

    /** 文件存储路径 */
    private String filePath;

    /** 子章节列表（非数据库字段，树形结构用） */
    @TableField(exist = false)
    private List<KnowledgeChapter> children;
}
