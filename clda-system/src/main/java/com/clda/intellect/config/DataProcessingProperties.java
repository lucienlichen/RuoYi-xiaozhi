package com.clda.intellect.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 数据处理相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "clda")
public class DataProcessingProperties {

    private Ocr ocr = new Ocr();
    private Enhance enhance = new Enhance();
    private Structuring structuring = new Structuring();

    @Data
    public static class Ocr {
        /** Tesseract训练数据路径 */
        private String tessdataPath = "/opt/tessdata";
        /** OCR语言 */
        private String language = "chi_sim+eng";
    }

    @Data
    public static class Enhance {
        /** Python超分辨率服务地址 */
        private String url = "http://localhost:8090";
        /** 超时(ms) */
        private int timeout = 30000;
        /** 是否启用 */
        private boolean enabled = true;
    }

    @Data
    public static class Structuring {
        /** 是否启用LLM结构化 */
        private boolean enabled = true;
    }
}
