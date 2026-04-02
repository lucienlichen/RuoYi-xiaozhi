package com.clda.intellect.service.impl;

import com.clda.intellect.domain.StructuringTemplate;
import com.clda.intellect.mapper.StructuringTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * LLM结构化提取服务 — 使用Spring AI调用LLM将OCR文本转为结构化JSON
 * <p>
 * ChatClient 通过 ChatClientProvider 获取，支持运行时刷新配置。
 * clda-admin 模块提供 RefreshableChatClientHolder 实现该接口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmStructuringService {

    private final ChatClientProvider chatClientProvider;
    private final StructuringTemplateMapper templateMapper;
    private final AiConfigService aiConfigService;

    private static final int MAX_RETRIES = 2;

    public String structure(String text, String categoryCode) {
        if (!aiConfigService.isStructuringEnabled() || !aiConfigService.isLlmEnabled()) {
            log.info("LLM结构化或LLM服务未启用，跳过");
            return null;
        }

        // 检查API Key有效性
        String apiKey = aiConfigService.getLlmApiKey();
        if (apiKey == null || apiKey.startsWith("sk-placeholder") || apiKey.length() < 10) {
            log.warn("LLM API密钥未配置，跳过结构化。请在AI配置页面设置有效的API密钥。");
            return null;
        }

        if (text == null || text.isBlank()) {
            log.warn("文本为空，跳过结构化");
            return null;
        }

        StructuringTemplate template = templateMapper.selectByCategoryCode(categoryCode);
        if (template == null) {
            log.warn("未找到分类 {} 的结构化模板，使用通用提取", categoryCode);
            return callLlmWithRetry(
                    "你是一个专业的数据提取助手。从文本中提取所有关键信息，以JSON格式返回键值对。不要包含任何其他文字说明。",
                    "请从以下文本中提取结构化信息：\n---\n" + truncateText(text, 4000) + "\n---",
                    "通用"
            );
        }

        String systemPrompt = """
                你是一个专业的数据提取助手。你的任务是从用户提供的文本中提取结构化信息。
                请严格按照JSON格式返回结果，不要包含任何其他文字说明。
                如果某个字段在文本中找不到对应信息，该字段值设为null。
                字段定义：%s
                """.formatted(template.getFieldSchema());

        String userPrompt = """
                %s

                以下是需要提取信息的原始文本：
                ---
                %s
                ---

                请提取上述文本中的结构化信息，以JSON格式返回。
                """.formatted(template.getLlmPrompt(), truncateText(text, 4000));

        return callLlmWithRetry(systemPrompt, userPrompt, template.getCategoryCode());
    }

    /**
     * 带重试的LLM调用（处理429 rate limit 和 timeout）
     */
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
