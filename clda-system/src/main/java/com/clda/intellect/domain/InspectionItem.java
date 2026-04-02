package com.clda.intellect.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clda.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_inspection_item")
@EqualsAndHashCode(callSuper = true)
public class InspectionItem extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 大类：重大隐患/其他隐患 */
    private String category;

    /** 子类：检验相关/安全装置/... */
    private String subCategory;

    /** 序号1-66 */
    private Integer itemNo;

    /** 排查内容 */
    private String content;

    /** 显示排序 */
    private Integer orderNum;
}
