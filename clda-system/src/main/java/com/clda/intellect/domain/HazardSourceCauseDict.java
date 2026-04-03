package com.clda.intellect.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tb_hazard_cause_dict")
public class HazardSourceCauseDict {

    @TableId
    private String code;

    private String stage;

    private String description;
}
