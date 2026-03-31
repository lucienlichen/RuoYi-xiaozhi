package com.clda.chat.tool;

import cn.hutool.core.util.StrUtil;
import com.clda.chat.connect.ChatServerHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 工具调用 Function Calling
 * @author ruoyi-xiaozhi
 */
@Slf4j
@Component
public class CallTool {

    /**
     * 退出意图处理
     * @param bye 告别语
     */
    @Tool(returnDirect = true, description = "当且仅当用户明确表示要结束聊天时调用，例如说“我没有问题了”、“我们聊到这吧”、“再见”、“结束对话”等。不要在用户表达不满、沉默、犹豫或切换话题时调用此函数，必须确认用户确实想要结束聊天。")
    public String exitChat(@ToolParam(description = "和用户友好结束对话的告别语") String bye, ToolContext toolContext) {
        if (StrUtil.isBlank(bye)) {
            bye = "再见，祝您生活愉快";
        }
        ChatServerHandler conn = (ChatServerHandler) toolContext.getContext().get("conn");
        conn.closeAfterChat = true;
        log.info("exitChat: {}", bye);
        return bye;
    }

    /**
     * 获取当前日期和时间
     * @return  用户时区中的当前日期和时间
     */
    @Tool(description = "获取用户时区中的当前日期和时间")
    public String getCurrentDateTime() {
        String string = LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
        log.info("获取用户时区中的当前日期和时间: {}", string);
        return string;
    }

    /**
     * 导航到指定的AI助手页面
     * @param service 目标服务标识
     */
    @Tool(description = "当用户要求打开或导航到某个页面时调用。支持的目标：主菜单(menu)、区域管理(partition)、设备管理(equipment)、数据服务(data_service_ai)、问题处理(typical_issue_ai)、隐患排查(hazard_check)、风险服务(risk_service_ai)、前沿知识(safety_maintenance_ai)、法规标准(regulations_ai)。当用户说返回、回到菜单、回到主页等意图时，使用menu。")
    public String navigateTo(
            @ToolParam(description = "目标标识：menu, partition, equipment, data_service_ai, hazard_check, risk_service_ai, safety_maintenance_ai, regulations_ai, typical_issue_ai") String service,
            ToolContext toolContext) {
        ChatServerHandler conn = (ChatServerHandler) toolContext.getContext().get("conn");
        conn.sendNavigationCommand(service);

        String serviceName = switch (service) {
            case "menu" -> "主菜单";
            case "partition" -> "区域管理";
            case "equipment" -> "设备管理";
            case "data_service_ai" -> "数据服务AI助手";
            case "typical_issue_ai" -> "问题处理AI助手";
            case "hazard_check" -> "隐患排查AI助手";
            case "risk_service_ai" -> "风险服务AI助手";
            case "safety_maintenance_ai" -> "前沿知识AI助手";
            case "regulations_ai" -> "法规标准AI助手";
            default -> service;
        };
        log.info("navigateTo: {}", serviceName);
        return "已为您打开" + serviceName;
    }

}
