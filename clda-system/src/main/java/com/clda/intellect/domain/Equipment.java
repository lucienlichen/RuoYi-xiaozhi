package com.clda.intellect.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.clda.common.annotation.Excel;
import com.clda.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 起重设备对象 tb_equipment
 *
 * @author clda-xiaozhi
 */
@Data
@TableName("tb_equipment")
@EqualsAndHashCode(callSuper = true)
public class Equipment extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 设备名称 */
    @Excel(name = "设备名称")
    private String equipmentName;

    /** 设备编号 */
    @Excel(name = "设备编号")
    private String equipmentCode;

    /** 设备类型 */
    @Excel(name = "设备类型")
    private String equipmentType;

    /** 设备型号 */
    @Excel(name = "设备型号")
    private String model;

    /** 制造单位 */
    @Excel(name = "制造单位")
    private String manufacturer;

    /** 出厂日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "出厂日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date manufactureDate;

    /** 安装日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "安装日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date installDate;

    /** 所属分区ID */
    @Excel(name = "所属分区ID")
    private Long partitionId;

    /** 分区名称 */
    @TableField(exist = false)
    private String partitionName;

    /** 设备状态 */
    @Excel(name = "设备状态")
    private String status;

    /** 额定起重量(吨) */
    @Excel(name = "额定起重量(吨)")
    private BigDecimal ratedCapacity;

    /** 跨度(米) */
    private BigDecimal span;

    /** 起升高度(米) */
    private BigDecimal liftingHeight;

    /** 注册登记号 */
    @Excel(name = "注册登记号")
    private String registrationCode;

    /** 使用登记证编号 */
    @Excel(name = "使用登记证编号")
    private String useCertNo;
}
