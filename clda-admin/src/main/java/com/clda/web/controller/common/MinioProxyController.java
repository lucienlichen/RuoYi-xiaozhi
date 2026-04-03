package com.clda.web.controller.common;

import com.clda.common.utils.minio.MinioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * MinIO 文件代理控制器
 * <p>
 * 新上传文件存储路径格式：/minio/{objectKey}
 * 前端访问 URL：{VITE_APP_BASE_API}/minio/{objectKey}
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class MinioProxyController {

    private final MinioService minioService;

    @GetMapping("/minio/**")
    public void minioProxy(HttpServletRequest request, HttpServletResponse response) {
        String objectKey = URLDecoder.decode(
                request.getRequestURI().replaceFirst("^/minio/", ""),
                StandardCharsets.UTF_8);
        try (InputStream in = minioService.download(objectKey)) {
            response.setContentType(detectContentType(objectKey));
            FileCopyUtils.copy(in, response.getOutputStream());
        } catch (Exception e) {
            log.warn("MinIO文件不存在或读取失败: {}", objectKey);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private String detectContentType(String key) {
        if (key == null) return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String lower = key.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".bmp")) return "image/bmp";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
