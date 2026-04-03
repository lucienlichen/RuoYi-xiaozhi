package com.clda.intellect.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clda.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据分类对象 tb_data_category
 *
 * @author clda-xiaozhi
 */
@Data
@TableName("tb_data_category")
@EqualsAndHashCode(callSuper = true)
public class DataCategory extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 分类编码 */
    private String categoryCode;

    /** 分类名称 */
    private String categoryName;

    /** 父分类ID */
    private Long parentId;

    /** 排序 */
    private Integer orderNum;

    /** 日期筛选模式(day/year/none) */
    private String dateMode;

    /** 是否启用(1启用 0禁用) */
    private String enabled;
}
