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
