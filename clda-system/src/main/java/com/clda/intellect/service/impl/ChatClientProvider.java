package com.clda.intellect.service.impl;

import org.springframework.ai.chat.client.ChatClient;

/**
 * ChatClient提供者接口 — 允许运行时刷新LLM配置
 * <p>
 * clda-system 定义接口，clda-admin 的 RefreshableChatClientHolder 实现
 */
public interface ChatClientProvider {
    ChatClient getChatClient();
}
