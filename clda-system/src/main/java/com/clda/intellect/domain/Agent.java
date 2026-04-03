package com.clda.intellect.domain;

import com.clda.feign.enums.TTSProviderEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clda.common.annotation.Excel;
import com.clda.common.core.domain.BaseEntity;

/**
 * 智能体对象 tb_agent
 *
 * @author clda-xiaozhi
 */
@Data
@TableName("tb_agent")
@EqualsAndHashCode(callSuper = true)
public class Agent extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 智能体名称 */
    @Excel(name = "智能体名称")
    private String agentName;

    /** 系统提示词 */
    @Excel(name = "系统提示词")
    private String prompt;

    /** TTS供应商 */
    @Excel(name = "TTS供应商")
    private TTSProviderEnum ttsProvider;

}
