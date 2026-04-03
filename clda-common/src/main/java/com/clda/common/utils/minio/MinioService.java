package com.clda.common.utils.minio;

import com.clda.common.config.MinioProperties;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

/**
 * MinIO 对象存储服务
 * <p>
 * 新上传文件存为裸 object key（如 {@code upload/crane/1/2025/03/02/file_abc.jpg}）。
 * 旧文件路径以 {@code /profile} 开头，由调用方负责兼容处理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "clda.minio", name = "endpoint")
public class MinioService {

    private final MinioProperties props;
    private MinioClient client;

    @PostConstruct
    public void init() {
        String accessKey = props.getAccessKey();
        String secretKey = props.getSecretKey();
        if (accessKey == null || accessKey.isBlank() || "UNCONFIGURED".equals(accessKey)
                || secretKey == null || secretKey.isBlank() || "UNCONFIGURED".equals(secretKey)) {
            log.warn("MinIO 凭据未配置，对象存储功能不可用。请设置 MINIO_ACCESS_KEY 和 MINIO_SECRET_KEY 环境变量。");
            return;
        }
        client = MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(accessKey, secretKey)
                .build();
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(props.getBucket()).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(props.getBucket()).build());
                log.info("MinIO: 已创建 bucket '{}'", props.getBucket());
            }
        } catch (Exception e) {
            log.warn("MinIO bucket 检查/创建失败（服务可能尚未就绪）: {}", e.getMessage());
        }
    }

    /**
     * 上传 MultipartFile，返回 object key。
     *
     * @param file   上传文件
     * @param subDir 子目录，如 {@code "upload/crane/1"} 或 {@code "avatar"}
     */
    private void requireClient() {
        if (client == null) {
            throw new IllegalStateException("MinIO 未初始化：请设置 MINIO_ACCESS_KEY 和 MINIO_SECRET_KEY 环境变量。");
        }
    }

    public String upload(MultipartFile file, String subDir) {
        requireClient();
        String objectKey = generateObjectKey(subDir, file.getOriginalFilename());
        try (InputStream in = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.debug("MinIO upload: {} → {}", file.getOriginalFilename(), objectKey);
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("MinIO 上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传本地文件到 MinIO。
     *
     * @param localFile   本地文件
     * @param objectKey   目标 object key
     * @param contentType MIME 类型，如 {@code "image/png"}
     */
    public void uploadFile(File localFile, String objectKey, String contentType) {
        requireClient();
        try (InputStream in = new FileInputStream(localFile)) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .stream(in, localFile.length(), -1)
                    .contentType(contentType)
                    .build());
            log.debug("MinIO uploadFile: {} → {}", localFile.getName(), objectKey);
        } catch (Exception e) {
            throw new RuntimeException("MinIO 上传文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传字节数组到 MinIO。
     */
    public void uploadBytes(byte[] data, String objectKey, String contentType) {
        requireClient();
        try (InputStream in = new java.io.ByteArrayInputStream(data)) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .stream(in, data.length, -1)
                    .contentType(contentType)
                    .build());
            log.debug("MinIO uploadBytes: {} bytes → {}", data.length, objectKey);
        } catch (Exception e) {
            throw new RuntimeException("MinIO 上传字节失败: " + e.getMessage(), e);
        }
    }

    /**
     * 下载 object，返回 InputStream（调用方负责关闭）。
     */
    public InputStream download(String objectKey) {
        requireClient();
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO 下载失败: " + objectKey + " — " + e.getMessage(), e);
        }
    }

    /**
     * 删除 object（不存在时静默忽略）。
     */
    public void delete(String objectKey) {
        requireClient();
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .build());
            log.debug("MinIO delete: {}", objectKey);
        } catch (ErrorResponseException e) {
            if (!"NoSuchKey".equals(e.errorResponse().code())) {
                throw new RuntimeException("MinIO 删除失败: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new RuntimeException("MinIO 删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成带日期分组的 object key。
     * <p>
     * 示例：{@code upload/crane/1/2025/03/02/report_a1b2c3d4.pdf}
     */
    public String generateObjectKey(String subDir, String filename) {
        LocalDate now = LocalDate.now();
        String ext = "";
        if (filename != null && filename.contains(".")) {
            ext = filename.substring(filename.lastIndexOf('.'));
        }
        // 只保留 ASCII 字母数字和常见符号，非 ASCII 字符（如中文）替换为下划线，避免 URL 编码问题
        String rawBase = filename != null
                ? filename.substring(0, filename.contains(".") ? filename.lastIndexOf('.') : filename.length())
                : "file";
        String baseName = rawBase.replaceAll("[^\\x00-\\x7F]+", "_").replaceAll("[^a-zA-Z0-9._-]", "_");
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return String.format("%s/%d/%02d/%02d/%s_%s%s",
                subDir, now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                baseName, unique, ext);
    }

    public String getBucket() {
        return props.getBucket();
    }
}
