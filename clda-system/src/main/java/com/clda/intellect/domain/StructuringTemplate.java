package com.clda.intellect.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clda.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据结构化模板配置对象 tb_structuring_template
 */
@Data
@TableName("tb_structuring_template")
@EqualsAndHashCode(callSuper = true)
public class StructuringTemplate extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联数据分类编码 */
    private String categoryCode;

    /** 模板名称 */
    private String templateName;

    /** LLM提取prompt模板 */
    private String llmPrompt;

    /** 字段定义JSON [{key,label,type,required}] */
    private String fieldSchema;

    /** 规则提取配置(正则/关键词) */
    private String ruleConfig;

    /** 是否启用(1启用 0禁用) */
    private String enabled;
}
