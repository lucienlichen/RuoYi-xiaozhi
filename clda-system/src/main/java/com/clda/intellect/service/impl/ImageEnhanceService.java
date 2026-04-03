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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * 图片AI超分辨率增强服务 — 调用Python微服务
 * <p>
 * Python端返回文件字节流（FileResponse），本服务写入临时文件后返回路径。
 * 调用方负责在使用完毕后删除该临时文件。
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

    /**
     * 调用增强服务，返回增强后图片的本地临时路径（调用方负责删除）。
     *
     * @param imagePath 本地图片绝对路径
     * @return 增强后图片的临时文件路径，服务不可用或增强失败时返回 null
     */
    public String enhance(String imagePath) {
        if (!aiConfigService.isEnhanceEnabled()) {
            log.info("图片增强服务未启用，跳过");
            return null;
        }

        try {
            String baseUrl = aiConfigService.getEnhanceUrl();
            ResponseEntity<String> healthResp = getRestTemplate().getForEntity(baseUrl + "/health", String.class);
            String healthBody = healthResp.getBody();
            if (!healthResp.getStatusCode().is2xxSuccessful()
                    || healthBody == null || !healthBody.contains("\"status\"")) {
                log.warn("图片增强服务不可用或返回非预期响应: {}", healthBody);
                return null;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(new File(imagePath)));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<byte[]> response = getRestTemplate().exchange(
                    baseUrl + "/enhance", HttpMethod.POST, requestEntity, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Write response bytes to a temp file
                String ext = imagePath.contains(".") ? imagePath.substring(imagePath.lastIndexOf('.')) : ".png";
                Path tmpFile = Files.createTempFile("clda_enhanced_", ext);
                Files.write(tmpFile, response.getBody());
                log.info("图片增强完成: {} → {}", imagePath, tmpFile);
                return tmpFile.toAbsolutePath().toString();
            }
            log.warn("图片增强返回空响应");
            return null;
        } catch (Exception e) {
            log.warn("图片增强服务调用失败(降级跳过): {}", e.getMessage());
            return null;
        }
    }
}
