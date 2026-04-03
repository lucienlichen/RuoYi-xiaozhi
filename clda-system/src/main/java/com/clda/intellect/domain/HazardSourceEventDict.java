package com.clda.intellect.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tb_hazard_event_dict")
public class HazardSourceEventDict {

    @TableId
    private String code;

    private String name;

    private String icon;
}
