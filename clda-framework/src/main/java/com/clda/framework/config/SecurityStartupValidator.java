package com.clda.framework.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 启动安全检查：非 dev 环境下校验关键安全配置，缺失则 fail-fast
 *
 * @author clda
 */
@Component
public class SecurityStartupValidator
{
    private static final Logger log = LoggerFactory.getLogger(SecurityStartupValidator.class);

    private static final String WEAK_JWT_SECRET = "abcdefghijklmnopqrstuvwxyz";
    private static final String UNCONFIGURED_MARKER = "UNCONFIGURED";
    private static final int MIN_JWT_SECRET_LENGTH = 32;

    private final Environment environment;

    @Value("${token.secret:}")
    private String tokenSecret;

    @Value("${clda.minio.secret-key:}")
    private String minioSecretKey;

    public SecurityStartupValidator(Environment environment)
    {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validate()
    {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean isDev = activeProfiles.contains("dev");

        if (isDev)
        {
            log.info("======== 安全检查：dev 环境，跳过强制校验 ========");
            return;
        }

        log.info("======== 安全检查：非 dev 环境，执行强制校验 ========");
        List<String> errors = new ArrayList<>();

        // 1. JWT 密钥检查
        if (tokenSecret == null || tokenSecret.isBlank() || UNCONFIGURED_MARKER.equals(tokenSecret))
        {
            errors.add("TOKEN_SECRET 未设置。必须通过环境变量提供 JWT 签名密钥（openssl rand -base64 48）。");
        }
        else if (WEAK_JWT_SECRET.equals(tokenSecret))
        {
            errors.add("TOKEN_SECRET 使用了开发环境弱密钥，禁止在非 dev 环境使用。");
        }
        else if (tokenSecret.length() < MIN_JWT_SECRET_LENGTH)
        {
            errors.add("TOKEN_SECRET 长度不足 " + MIN_JWT_SECRET_LENGTH + " 字符，请使用更强的密钥。");
        }

        // 2. MinIO 密钥检查
        if (minioSecretKey == null || minioSecretKey.isBlank())
        {
            log.warn("MINIO_SECRET_KEY 未设置。如果启用了 MinIO 存储，请通过环境变量提供。");
        }
        else if ("minioadmin".equals(minioSecretKey))
        {
            errors.add("MINIO_SECRET_KEY 使用了默认值 'minioadmin'，禁止在非 dev 环境使用。");
        }

        // 3. 输出结果
        if (!errors.isEmpty())
        {
            StringBuilder sb = new StringBuilder();
            sb.append("\n\n");
            sb.append("╔══════════════════════════════════════════════════════════════╗\n");
            sb.append("║              CLDA 安全配置检查失败                          ║\n");
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            for (String error : errors)
            {
                sb.append("║  ✘ ").append(error).append("\n");
            }
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append("║  当前 profile: ").append(String.join(", ", activeProfiles)).append("\n");
            sb.append("║  如需本地开发，请设置 SPRING_PROFILES_ACTIVE=dev            ║\n");
            sb.append("╚══════════════════════════════════════════════════════════════╝\n");

            log.error(sb.toString());
            throw new SecurityException("安全配置检查未通过，应用拒绝启动。详情见上方日志。");
        }

        log.info("======== 安全检查通过 ========");
    }
}
