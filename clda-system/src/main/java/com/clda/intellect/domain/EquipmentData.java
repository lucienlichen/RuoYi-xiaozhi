package com.clda.intellect.domain;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.clda.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备生命周期数据记录对象 tb_equipment_data
 *
 * @author clda-xiaozhi
 */
@Data
@TableName("tb_equipment_data")
@EqualsAndHashCode(callSuper = true)
public class EquipmentData extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 设备ID */
    private Long equipmentId;

    /** 数据分类ID */
    private Long categoryId;

    /** 子分类ID */
    private Long subCategoryId;

    /** 数据日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date dataDate;

    /** 标题 */
    private String title;

    /** 备注内容 */
    private String content;

    /** 状态(PENDING/PROCESSING/COMPLETED/FAILED) */
    private String status;
}
