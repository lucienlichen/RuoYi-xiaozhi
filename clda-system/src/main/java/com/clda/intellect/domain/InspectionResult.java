package com.clda.intellect.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tb_inspection_result")
public class InspectionResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;

    private Long itemId;

    private Integer itemNo;

    /** 有/无 */
    private String result;

    private String remark;

    /** 排查内容（关联查询时填充） */
    @TableField(exist = false)
    private String content;

    /** 子类别（关联查询时填充） */
    @TableField(exist = false)
    private String subCategory;

    /** 大类（关联查询时填充） */
    @TableField(exist = false)
    private String category;
}
