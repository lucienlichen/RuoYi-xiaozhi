package com.clda.intellect.service;

/**
 * 数据处理服务接口 — 文件上传后的异步处理流水线
 */
public interface IDataProcessingService {

    /**
     * 异步处理单个文件：预处理 → 增强 → OCR → 结构化
     */
    void processFileAsync(Long fileId);

    /**
     * 重新处理文件（管理员手动触发）
     */
    void reprocessFile(Long fileId);
}
