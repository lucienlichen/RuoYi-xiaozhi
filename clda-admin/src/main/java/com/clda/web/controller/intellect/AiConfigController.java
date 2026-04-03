package com.clda.web.controller.intellect;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.clda.common.annotation.Log;
import com.clda.common.core.controller.BaseController;
import com.clda.common.core.domain.AjaxResult;
import com.clda.common.enums.BusinessType;
import com.clda.system.domain.SysConfig;
import com.clda.system.service.ISysConfigService;
import com.clda.web.config.RefreshableChatClientHolder;

/**
 * AI配置管理Controller — 管理 ai.* 前缀的系统配置
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/intellect/ai-config")
public class AiConfigController extends BaseController {

    private final ISysConfigService configService;
    private final RefreshableChatClientHolder chatClientHolder;

    /** 查询所有AI配置 */
    @PreAuthorize("@ss.hasPermi('intellect:aiconfig:list')")
    @GetMapping("/list")
    public AjaxResult list() {
        SysConfig query = new SysConfig();
        query.setConfigKey("ai.");
        List<SysConfig> all = configService.selectConfigList(query);
        // 掩码显示API密钥
        all.forEach(c -> {
            if (c.getConfigKey() != null && c.getConfigKey().contains("apiKey")) {
                c.setConfigValue(maskApiKey(c.getConfigValue()));
            }
        });
        return success(all);
    }

    /** 批量更新AI配置 */
    @PreAuthorize("@ss.hasPermi('intellect:aiconfig:edit')")
    @Log(title = "AI配置", businessType = BusinessType.UPDATE)
    @PutMapping("/batch")
    public AjaxResult batchUpdate(@RequestBody List<SysConfig> configs) {
        for (SysConfig config : configs) {
            if (config.getConfigKey() == null || !config.getConfigKey().startsWith("ai.")) {
                continue;
            }
            // 跳过未修改的掩码密钥
            if (config.getConfigKey().contains("apiKey") && isMasked(config.getConfigValue())) {
                continue;
            }
            config.setUpdateBy(getUsername());
            configService.updateConfig(config);
        }
        // 刷新ChatClient（LLM配置可能变更）
        try {
            chatClientHolder.refresh();
        } catch (Exception e) {
            log.warn("ChatClient刷新失败(配置可能不完整): {}", e.getMessage());
        }
        return success();
    }

    /** 测试LLM连通性 */
    @PreAuthorize("@ss.hasPermi('intellect:aiconfig:query')")
    @PostMapping("/test-llm")
    public AjaxResult testLlmConnection() {
        try {
            chatClientHolder.refresh();
            String response = chatClientHolder.getChatClient()
                    .prompt()
                    .user("回复'OK'两个字母，不要说其他任何内容。")
                    .call()
                    .content();
            if (response != null && response.contains("OK")) {
                return success("LLM连接测试成功");
            }
            return success("LLM已响应: " + (response != null ? response.trim() : "empty"));
        } catch (Exception e) {
            log.error("LLM连接测试失败", e);
            return error("LLM连接失败: " + e.getMessage());
        }
    }

    private String maskApiKey(String key) {
        if (key == null || key.length() <= 8) return "****";
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    private boolean isMasked(String value) {
        return value != null && value.contains("****");
    }
}
