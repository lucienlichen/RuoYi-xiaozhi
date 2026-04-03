package com.clda.web.config;

import com.clda.intellect.service.impl.AiConfigService;
import com.clda.intellect.service.impl.ChatClientProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

/**
 * 可刷新的 ChatClient 持有者 — 管理页面修改LLM配置后，调用 refresh() 重建 ChatClient
 */
@Slf4j
@Component
public class RefreshableChatClientHolder implements ChatClientProvider {

    private final AiConfigService aiConfigService;
    private volatile ChatClient chatClient;

    public RefreshableChatClientHolder(AiConfigService aiConfigService) {
        this.aiConfigService = aiConfigService;
        this.chatClient = buildChatClient();
    }

    public ChatClient getChatClient() {
        return chatClient;
    }

    public synchronized void refresh() {
        log.info("正在刷新ChatClient配置... baseUrl={}, model={}",
                aiConfigService.getLlmBaseUrl(), aiConfigService.getLlmModel());
        this.chatClient = buildChatClient();
        log.info("ChatClient刷新完成");
    }

    private ChatClient buildChatClient() {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(aiConfigService.getLlmBaseUrl())
                .apiKey(aiConfigService.getLlmApiKey())
                .completionsPath(aiConfigService.getLlmChatPath())
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(aiConfigService.getLlmModel())
                        .build())
                .build();
        return ChatClient.builder(chatModel).build();
    }
}
