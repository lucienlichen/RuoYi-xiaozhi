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
