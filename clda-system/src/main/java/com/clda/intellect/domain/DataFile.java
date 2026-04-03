package com.clda.intellect.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clda.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据文件附件对象 tb_data_file
 *
 * @author clda-xiaozhi
 */
@Data
@TableName("tb_data_file")
@EqualsAndHashCode(callSuper = true)
public class DataFile extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联数据记录ID */
    private Long dataId;

    /** 原始文件名 */
    private String fileName;

    /** 存储路径 */
    private String filePath;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 文件类型(image/pdf/word/excel) */
    private String fileType;

    /** MIME类型 */
    private String mimeType;

    /** 预处理后文件路径 */
    private String preprocessedPath;

    /** AI增强后图片路径 */
    private String enhancedPath;

    /** 预处理状态(NONE/PROCESSING/DONE/FAILED) */
    private String preprocessStatus;

    /** OCR状态(NONE/PROCESSING/DONE/FAILED) */
    private String ocrStatus;

    /** OCR识别文本 */
    private String ocrText;

    /** 结构化数据(JSON) */
    private String structuredData;

    /** 排序 */
    private Integer sortOrder;
}
