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
import java.util.List;

@Data
@TableName("tb_inspection_record")
@EqualsAndHashCode(callSuper = true)
public class InspectionRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long equipmentId;

    private String equipmentName;

    private String inspector;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date inspectDate;

    private String filePath;

    private Integer majorCount;

    private Integer otherCount;

    private Integer totalItems;

    private String status;

    /** 结果明细（查询详情时填充） */
    @TableField(exist = false)
    private List<InspectionResult> results;
}
