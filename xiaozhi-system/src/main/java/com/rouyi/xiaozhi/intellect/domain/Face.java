package com.rouyi.xiaozhi.intellect.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rouyi.xiaozhi.common.annotation.Excel;
import com.rouyi.xiaozhi.common.core.domain.BaseEntity;

/**
 * 人脸识别对象 tb_face
 *
 * @author ruoyi-xiaozhi
 */
@Data
@TableName("tb_face")
@EqualsAndHashCode(callSuper = true)
public class Face extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 人脸名称 */
    @Excel(name = "人脸名称")
    private String name;

    /** 人脸描述符 */
    @Excel(name = "人脸描述符")
    private String descriptor;

    /** 照片地址 */
    @Excel(name = "照片地址")
    private String photoUrl;

}
