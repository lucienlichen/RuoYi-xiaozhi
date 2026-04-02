package com.clda.intellect.service.impl;

import com.clda.system.service.ISysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI配置服务 — 从 sys_config 表读取 ai.* 前缀的配置项，支持运行时修改
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConfigService {

    private final ISysConfigService sysConfigService;

    // ========== OCR ==========
    public String getOcrTessdataPath() {
        return getOrDefault("ai.ocr.tessdataPath", "/usr/local/share/tessdata");
    }

    public String getOcrLanguage() {
        return getOrDefault("ai.ocr.language", "chi_sim+eng");
    }

    public int getOcrPageSegMode() {
        return Integer.parseInt(getOrDefault("ai.ocr.pageSegMode", "3"));
    }

    public int getOcrEngineMode() {
        return Integer.parseInt(getOrDefault("ai.ocr.engineMode", "1"));
    }

    // ========== 图片增强 ==========
    public String getEnhanceUrl() {
        return getOrDefault("ai.enhance.url", "http://localhost:8090");
    }

    public int getEnhanceTimeout() {
        return Integer.parseInt(getOrDefault("ai.enhance.timeout", "30000"));
    }

    public boolean isEnhanceEnabled() {
        return Boolean.parseBoolean(getOrDefault("ai.enhance.enabled", "true"));
    }

    // ========== LLM ==========
    public String getLlmBaseUrl() {
        return getOrDefault("ai.llm.baseUrl", "https://api.openai.com");
    }

    public String getLlmApiKey() {
        return getOrDefault("ai.llm.apiKey", "sk-placeholder");
    }

    public String getLlmModel() {
        return getOrDefault("ai.llm.model", "gpt-4o-mini");
    }

    public boolean isLlmEnabled() {
        return Boolean.parseBoolean(getOrDefault("ai.llm.enabled", "true"));
    }

    // ========== 结构化 ==========
    public boolean isStructuringEnabled() {
        return Boolean.parseBoolean(getOrDefault("ai.structuring.enabled", "true"));
    }

    private String getOrDefault(String key, String defaultValue) {
        String val = sysConfigService.selectConfigByKey(key);
        return (val == null || val.isBlank()) ? defaultValue : val;
    }
}
