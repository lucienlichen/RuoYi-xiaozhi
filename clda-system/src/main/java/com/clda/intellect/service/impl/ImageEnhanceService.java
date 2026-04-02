package com.clda.intellect.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.time.Duration;
import java.util.Map;

/**
 * 图片AI超分辨率增强服务 — 调用Python微服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageEnhanceService {

    private final AiConfigService aiConfigService;

    private volatile RestTemplate restTemplate;
    private volatile int lastTimeout;

    private RestTemplate getRestTemplate() {
        int timeout = aiConfigService.getEnhanceTimeout();
        if (restTemplate == null || timeout != lastTimeout) {
            var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofMillis(5000));
            factory.setReadTimeout(Duration.ofMillis(timeout));
            restTemplate = new RestTemplate(factory);
            lastTimeout = timeout;
        }
        return restTemplate;
    }

    public String enhance(String imagePath) {
        if (!aiConfigService.isEnhanceEnabled()) {
            log.info("图片增强服务未启用，跳过");
            return null;
        }

        try {
            String baseUrl = aiConfigService.getEnhanceUrl();
            ResponseEntity<Map> healthResp = getRestTemplate().getForEntity(baseUrl + "/health", Map.class);
            if (!healthResp.getStatusCode().is2xxSuccessful()) {
                log.warn("图片增强服务不可用");
                return null;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(new File(imagePath)));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = getRestTemplate().exchange(
                    baseUrl + "/enhance", HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object enhancedPath = response.getBody().get("enhanced_path");
                if (enhancedPath != null) {
                    log.info("图片增强完成: {} → {}", imagePath, enhancedPath);
                    return enhancedPath.toString();
                }
            }
            log.warn("图片增强返回异常: {}", response.getBody());
            return null;
        } catch (Exception e) {
            log.warn("图片增强服务调用失败(降级跳过): {}", e.getMessage());
            return null;
        }
    }
}
