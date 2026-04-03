package com.clda.intellect.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clda.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_hazard_item")
@EqualsAndHashCode(callSuper = true)
public class HazardSourceItem extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private Integer itemNo;

    private String description;

    private String causeCodes;

    private String eventCodes;

    private Long equipmentId;

    private Integer orderNum;
}
