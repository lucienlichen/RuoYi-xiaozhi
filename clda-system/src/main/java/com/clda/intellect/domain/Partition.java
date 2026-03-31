package com.clda.intellect.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clda.common.annotation.Excel;
import com.clda.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备分区（厂区）对象 tb_partition
 *
 * @author ruoyi-xiaozhi
 */
@Data
@TableName("tb_partition")
@EqualsAndHashCode(callSuper = true)
public class Partition extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 分区名称 */
    @Excel(name = "分区名称")
    private String partitionName;

    /** 分区编码 */
    @Excel(name = "分区编码")
    private String partitionCode;

    /** 父分区ID */
    private Long parentId;

    /** 显示顺序 */
    private Integer orderNum;

    /** 状态(0正常 1停用) */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;
}
