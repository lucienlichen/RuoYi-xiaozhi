package com.clda.intellect.service.impl;

import com.clda.intellect.domain.StructuringTemplate;
import com.clda.intellect.mapper.StructuringTemplateMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM结构化提取服务 — 优先使用LLM，失败时回退到正则提取
 */
@Slf4j
@Service
public class LlmStructuringService {

    /** 可选注入：LLM客户端不可用时回退正则 */
    @Autowired(required = false)
    private ChatClientProvider chatClientProvider;

    private final StructuringTemplateMapper templateMapper;
    private final AiConfigService aiConfigService;
    private final ObjectMapper objectMapper;

    public LlmStructuringService(StructuringTemplateMapper templateMapper,
                                 AiConfigService aiConfigService,
                                 ObjectMapper objectMapper) {
        this.templateMapper = templateMapper;
        this.aiConfigService = aiConfigService;
        this.objectMapper = objectMapper;
    }

    private static final int MAX_RETRIES = 2;

    public String structure(String text, String categoryCode) {
        if (text == null || text.isBlank()) {
            return null;
        }

        // 1. 尝试 LLM
        if (chatClientProvider != null && aiConfigService.isStructuringEnabled() && aiConfigService.isLlmEnabled()) {
            String apiKey = aiConfigService.getLlmApiKey();
            if (apiKey != null && !apiKey.startsWith("sk-placeholder") && apiKey.length() >= 10) {
                try {
                    String llmResult = callLlm(text, categoryCode);
                    if (llmResult != null && !llmResult.isBlank()) {
                        log.info("LLM结构化成功, 分类: {}", categoryCode);
                        return llmResult;
                    }
                } catch (Exception e) {
                    log.warn("LLM结构化失败，回退正则提取: {}", e.getMessage());
                }
            } else {
                log.info("LLM API密钥未配置，使用正则提取");
            }
        } else {
            log.info("LLM结构化未启用，使用正则提取");
        }

        // 2. 回退：正则/关键词提取
        return tryRegexExtraction(text, categoryCode);
    }

    // ========== LLM ==========

    private String callLlm(String text, String categoryCode) {
        StructuringTemplate template = templateMapper.selectByCategoryCode(categoryCode);

        String systemPrompt;
        String userPrompt;

        if (template == null) {
            log.warn("未找到分类 {} 的结构化模板，使用通用提取", categoryCode);
            systemPrompt = "你是一个专业的数据提取助手。从文本中提取所有关键信息，以JSON格式返回键值对。不要包含任何其他文字说明。";
            userPrompt = "请从以下文本中提取结构化信息：\n---\n" + truncateText(text, 4000) + "\n---";
        } else {
            systemPrompt = """
                    你是一个专业的数据提取助手。你的任务是从用户提供的文本中提取结构化信息。
                    请严格按照JSON格式返回结果，不要包含任何其他文字说明。
                    如果某个字段在文本中找不到对应信息，该字段值设为null。
                    字段定义：%s
                    """.formatted(template.getFieldSchema());
            userPrompt = """
                    %s

                    以下是需要提取信息的原始文本：
                    ---
                    %s
                    ---

                    请提取上述文本中的结构化信息，以JSON格式返回。
                    """.formatted(template.getLlmPrompt(), truncateText(text, 4000));
        }

        return callLlmWithRetry(systemPrompt, userPrompt, categoryCode);
    }

    private String callLlmWithRetry(String systemPrompt, String userPrompt, String label) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                ChatClient client = chatClientProvider.getChatClient();
                String result = client.prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .call()
                        .content();

                result = cleanJsonResponse(result);
                log.info("LLM结构化完成, 分类: {}, 结果长度: {}", label, result != null ? result.length() : 0);
                return result;
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                boolean isRetryable = msg.contains("429") || msg.contains("rate_limit")
                        || msg.contains("timeout") || msg.contains("timed out");

                if (isRetryable && attempt < MAX_RETRIES) {
                    long waitMs = 5000L * (attempt + 1);
                    log.warn("LLM调用失败(attempt {}/{}), {}ms后重试: {}", attempt + 1, MAX_RETRIES + 1, waitMs, msg);
                    try { Thread.sleep(waitMs); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("LLM结构化被中断", ie);
                    }
                    continue;
                }

                log.error("LLM结构化失败({}), 分类: {}", attempt + 1, label, e);
                throw new RuntimeException("LLM结构化失败: " + msg, e);
            }
        }
        throw new RuntimeException("LLM结构化在重试后仍然失败");
    }

    // ========== 正则回退 ==========

    private String tryRegexExtraction(String text, String categoryCode) {
        StructuringTemplate template = templateMapper.selectByCategoryCode(categoryCode);
        if (template == null || template.getFieldSchema() == null) {
            return null;
        }
        try {
            String result = extractByRegex(text, template.getFieldSchema());
            if (result != null) {
                log.info("正则结构化完成, 分类: {}", categoryCode);
            }
            return result;
        } catch (Exception e) {
            log.warn("正则结构化提取异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 根据 fieldSchema 中每个字段的 label 在文本中做关键词提取。
     * 匹配规则：label + 中/英冒号 + 后续内容（到行尾，最长80字符）
     * 日期类型额外尝试 yyyy年MM月dd日 / yyyy-MM-dd / yyyy/MM/dd 模式。
     */
    private String extractByRegex(String text, String fieldSchemaJson) throws Exception {
        JsonNode fields = objectMapper.readTree(fieldSchemaJson);
        Map<String, Object> result = new LinkedHashMap<>();

        for (JsonNode field : fields) {
            String key = field.path("key").asText(null);
            String label = field.path("label").asText(null);
            String type = field.path("type").asText("string");
            if (key == null || label == null) continue;

            String value = null;

            if ("date".equals(type)) {
                value = extractDateNearLabel(text, label);
            }
            if (value == null) {
                value = extractAfterLabel(text, label);
            }

            result.put(key, value);
        }

        boolean anyFound = result.values().stream().anyMatch(v -> v != null);
        return anyFound ? objectMapper.writeValueAsString(result) : null;
    }

    /** 在 label 附近（前50字符内）查找日期格式 */
    private String extractDateNearLabel(String text, String label) {
        // 先找 label 位置，再在其后100字符内匹配日期
        int idx = text.indexOf(label);
        if (idx < 0) return null;
        String window = text.substring(idx, Math.min(idx + 100, text.length()));
        Pattern datePattern = Pattern.compile(
                "(\\d{4}\\s*[年/-]\\s*\\d{1,2}\\s*[月/-]\\s*\\d{1,2}\\s*[日]?)");
        Matcher m = datePattern.matcher(window);
        if (m.find()) return m.group(1).replaceAll("\\s+", "").trim();
        return null;
    }

    /** 匹配 "label：value" 模式，取到行尾（最长80字符） */
    private String extractAfterLabel(String text, String label) {
        // 支持中文全角冒号、英文冒号，label后可有空格
        Pattern p = Pattern.compile(
                Pattern.quote(label) + "\\s*[：:﹕]\\s*([^\\n\\r]{1,80})");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String val = m.group(1).trim();
            // 去掉末尾的标点
            val = val.replaceAll("[，。；,;]+$", "").trim();
            return val.isEmpty() ? null : val;
        }
        return null;
    }

    // ========== 工具方法 ==========

    private String cleanJsonResponse(String response) {
        if (response == null) return null;
        response = response.trim();
        if (response.startsWith("```json")) {
            response = response.substring(7);
        } else if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        return response.trim();
    }

    private String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "\n...(文本已截断)";
    }
}
