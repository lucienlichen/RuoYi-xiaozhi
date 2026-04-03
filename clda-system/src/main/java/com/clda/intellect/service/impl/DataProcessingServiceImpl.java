package com.clda.intellect.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.common.config.CldaConfig;
import com.clda.common.utils.minio.MinioService;
import com.clda.intellect.domain.DataCategory;
import com.clda.intellect.domain.DataFile;
import com.clda.intellect.domain.EquipmentData;
import com.clda.intellect.mapper.DataCategoryMapper;
import com.clda.intellect.mapper.DataFileMapper;
import com.clda.intellect.mapper.EquipmentDataMapper;
import com.clda.intellect.service.IDataProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * 数据处理服务 — 异步流水线：预处理 → 增强 → OCR/提取 → LLM结构化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataProcessingServiceImpl implements IDataProcessingService {

    private final DataFileMapper dataFileMapper;
    private final EquipmentDataMapper equipmentDataMapper;
    private final DataCategoryMapper dataCategoryMapper;
    private final OcrService ocrService;
    private final ImageEnhanceService imageEnhanceService;
    private final LlmStructuringService llmStructuringService;
    private final MinioService minioService;

    @Async
    @Override
    public void processFileAsync(Long fileId) {
        DataFile file = dataFileMapper.selectById(fileId);
        if (file == null) {
            log.warn("文件不存在: {}", fileId);
            return;
        }

        try {
            String text;
            String fileType = file.getFileType();

            if ("image".equals(fileType)) {
                text = processImage(file);
            } else if ("pdf".equals(fileType)) {
                text = processPdf(file);
            } else if ("word".equals(fileType)) {
                text = processWord(file);
            } else if ("excel".equals(fileType)) {
                text = processExcel(file);
            } else {
                log.info("不支持的文件类型, 跳过处理: {} ({})", file.getFileName(), fileType);
                return;
            }

            file.setOcrText(text);
            updateStatus(file, "ocr", "DONE");

            // LLM 结构化（失败不影响OCR结果）
            String categoryCode = resolveCategoryCode(file);
            if (categoryCode != null && text != null && !text.isBlank()) {
                try {
                    String structured = llmStructuringService.structure(text, categoryCode);
                    file.setStructuredData(structured != null ? structured : "{}");
                } catch (Exception e) {
                    log.warn("LLM结构化失败(不影响OCR结果): {}", e.getMessage());
                    file.setStructuredData("{}");
                }
            } else {
                file.setStructuredData("{}");
            }

            dataFileMapper.updateById(file);
            updateEquipmentDataStatus(file.getDataId());
            log.info("文件处理完成: {} ({})", file.getFileName(), fileId);
        } catch (Throwable e) {
            log.error("文件处理失败: {} ({})", file.getFileName(), fileId, e);
            markFailed(file, e.getMessage());
        }
    }

    @Async
    @Override
    public void reprocessFile(Long fileId) {
        DataFile file = dataFileMapper.selectById(fileId);
        if (file == null) return;
        // 重置状态
        file.setPreprocessStatus("NONE");
        file.setOcrStatus("NONE");
        file.setPreprocessedPath(null);
        file.setEnhancedPath(null);
        file.setOcrText(null);
        file.setStructuredData(null);
        dataFileMapper.updateById(file);
        processFileAsync(fileId);
    }

    /**
     * 图片处理：预处理 → 超分辨率增强 → OCR
     */
    private String processImage(DataFile file) {
        String localPath = null;
        String preprocessedLocal = null;
        String ocrInputLocal = null;
        String enhancedLocal = null;
        boolean localIsTemp = false;

        try {
            try {
                localPath = downloadToTemp(file.getFilePath());
            } catch (Exception e) {
                throw new RuntimeException("图片文件获取失败: " + e.getMessage(), e);
            }
            localIsTemp = isTempPath(localPath);

            // 1. 图片预处理
            updateStatus(file, "preprocess", "PROCESSING");
            String ext = FileUtil.extName(file.getFileName());
            preprocessedLocal = localPath.replace("." + ext, "_preprocessed." + ext);
            ocrInputLocal = ocrService.preprocessImage(localPath, preprocessedLocal);
            file.setPreprocessedPath(uploadIntermediate(preprocessedLocal, "process/preprocessed"));
            updateStatus(file, "preprocess", "DONE");

            // 2. AI超分辨率增强(可选，失败降级)
            String ocrSourcePath = ocrInputLocal;
            try {
                String enhanced = imageEnhanceService.enhance(ocrInputLocal);
                if (enhanced != null) {
                    enhancedLocal = enhanced;
                    file.setEnhancedPath(uploadIntermediate(enhancedLocal, "process/enhanced"));
                    ocrSourcePath = enhancedLocal;
                }
            } catch (Exception e) {
                log.warn("超分辨率增强失败，使用去噪图片: {}", e.getMessage());
            }

            // 3. OCR
            updateStatus(file, "ocr", "PROCESSING");
            return ocrService.recognize(ocrSourcePath);
        } finally {
            deleteQuietly(preprocessedLocal);
            deleteQuietly(ocrInputLocal);
            deleteQuietly(enhancedLocal);
            if (localIsTemp) deleteQuietly(localPath);
        }
    }

    /**
     * PDF处理：先尝试原生文字提取，文本过少则视为扫描件走OCR
     */
    private String processPdf(DataFile file) {
        updateStatus(file, "ocr", "PROCESSING");
        String localPath = null;
        boolean localIsTemp = false;
        try {
            localPath = downloadToTemp(file.getFilePath());
            localIsTemp = isTempPath(localPath);
        } catch (Exception e) {
            throw new RuntimeException("PDF文件获取失败: " + e.getMessage(), e);
        }

        try (PDDocument doc = PDDocument.load(new File(localPath))) {
            // 1. 尝试原生文字提取
            PDFTextStripper stripper = new PDFTextStripper();
            String nativeText = stripper.getText(doc);
            int pageCount = doc.getNumberOfPages();

            // 2. 判断是否扫描件：平均每页有效字符 < 50
            String stripped = nativeText != null ? nativeText.replaceAll("\\s+", "") : "";
            double charsPerPage = pageCount > 0 ? (double) stripped.length() / pageCount : 0;

            if (charsPerPage >= 50) {
                log.info("PDF含原生文本，直接提取: {} ({}页, {}字符)", file.getFileName(), pageCount, stripped.length());
                return nativeText;
            }

            // 3. 扫描件PDF — 逐页渲染为图片后OCR
            log.info("PDF检测为扫描件，启动OCR处理: {} ({}页)", file.getFileName(), pageCount);
            updateStatus(file, "preprocess", "PROCESSING");
            return ocrScannedPdf(doc, file, localPath);
        } catch (Exception e) {
            throw new RuntimeException("PDF处理失败: " + e.getMessage(), e);
        } finally {
            if (localIsTemp) deleteQuietly(localPath);
        }
    }

    /**
     * 逐页渲染扫描件PDF为图片并OCR
     */
    private String ocrScannedPdf(PDDocument doc, DataFile file, String absolutePath) {
        PDFRenderer renderer = new PDFRenderer(doc);
        int pageCount = doc.getNumberOfPages();
        StringBuilder fullText = new StringBuilder();
        String basePath = absolutePath.substring(0, absolutePath.lastIndexOf('.'));

        for (int i = 0; i < pageCount; i++) {
            String pageImagePath = null;
            String preprocessedPath = null;
            String ocrInputPath = null;
            String enhancedPath = null;
            try {
                // 渲染为300 DPI图片
                BufferedImage pageImage = renderer.renderImageWithDPI(i, 300, ImageType.RGB);
                pageImagePath = basePath + "_page" + (i + 1) + ".png";
                ImageIO.write(pageImage, "png", new File(pageImagePath));

                // 预处理（返回值是灰度去噪版，用于OCR；preprocessedPath 是二值化版，仅供展示）
                preprocessedPath = basePath + "_page" + (i + 1) + "_preprocessed.png";
                ocrInputPath = ocrService.preprocessImage(pageImagePath, preprocessedPath);

                // 可选增强（失败跳过）
                String ocrSourcePath = ocrInputPath;
                try {
                    enhancedPath = imageEnhanceService.enhance(preprocessedPath);
                    if (enhancedPath != null) ocrSourcePath = enhancedPath;
                } catch (Exception e) {
                    log.warn("PDF第{}页增强失败，使用预处理图: {}", i + 1, e.getMessage());
                }

                // OCR
                String pageText = ocrService.recognize(ocrSourcePath);
                fullText.append("=== 第 ").append(i + 1).append(" 页 ===\n");
                fullText.append(pageText).append("\n\n");
                log.debug("PDF第{}/{}页OCR完成", i + 1, pageCount);
            } catch (Exception e) {
                log.error("PDF第{}页OCR失败: {}", i + 1, e.getMessage());
                fullText.append("=== 第 ").append(i + 1).append(" 页 ===\n");
                fullText.append("[OCR失败: ").append(e.getMessage()).append("]\n\n");
            } finally {
                deleteQuietly(pageImagePath);
                deleteQuietly(preprocessedPath);
                deleteQuietly(ocrInputPath);
                deleteQuietly(enhancedPath);
            }
        }

        updateStatus(file, "preprocess", "DONE");
        return fullText.toString();
    }

    private void deleteQuietly(String path) {
        if (path == null) return;
        try { Files.deleteIfExists(Path.of(path)); } catch (Exception ignored) {}
    }

    /**
     * Word处理：POI文字提取
     */
    private String processWord(DataFile file) {
        updateStatus(file, "ocr", "PROCESSING");
        String localPath;
        boolean localIsTemp;
        try {
            localPath = downloadToTemp(file.getFilePath());
            localIsTemp = isTempPath(localPath);
        } catch (Exception e) {
            throw new RuntimeException("Word文件获取失败: " + e.getMessage(), e);
        }
        try (FileInputStream fis = new FileInputStream(localPath)) {
            var doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(fis);
            StringBuilder sb = new StringBuilder();
            doc.getParagraphs().forEach(p -> {
                String text = p.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text).append("\n");
                }
            });
            doc.close();
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Word文字提取失败: " + e.getMessage(), e);
        } finally {
            if (localIsTemp) deleteQuietly(localPath);
        }
    }

    /**
     * Excel处理：POI表格解析为文本
     */
    private String processExcel(DataFile file) {
        updateStatus(file, "ocr", "PROCESSING");
        String localPath;
        boolean localIsTemp;
        try {
            localPath = downloadToTemp(file.getFilePath());
            localIsTemp = isTempPath(localPath);
        } catch (Exception e) {
            throw new RuntimeException("Excel文件获取失败: " + e.getMessage(), e);
        }
        try (FileInputStream fis = new FileInputStream(localPath);
             Workbook workbook = WorkbookFactory.create(fis)) {
            StringBuilder sb = new StringBuilder();
            DataFormatter formatter = new DataFormatter();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                sb.append("=== ").append(sheet.getSheetName()).append(" ===\n");
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        sb.append(formatter.formatCellValue(cell)).append("\t");
                    }
                    sb.append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Excel解析失败: " + e.getMessage(), e);
        } finally {
            if (localIsTemp) deleteQuietly(localPath);
        }
    }

    /**
     * 根据文件关联的数据记录查找分类编码
     */
    private String resolveCategoryCode(DataFile file) {
        EquipmentData data = equipmentDataMapper.selectById(file.getDataId());
        if (data == null) return null;
        DataCategory category = dataCategoryMapper.selectById(data.getCategoryId());
        if (category == null) return null;
        // 如果是子分类，找父分类的code
        if (category.getParentId() != null && category.getParentId() != 0) {
            DataCategory parent = dataCategoryMapper.selectById(category.getParentId());
            return parent != null ? parent.getCategoryCode() : category.getCategoryCode();
        }
        return category.getCategoryCode();
    }

    private void updateStatus(DataFile file, String stage, String status) {
        if ("preprocess".equals(stage)) {
            file.setPreprocessStatus(status);
        } else if ("ocr".equals(stage)) {
            file.setOcrStatus(status);
        }
        dataFileMapper.updateById(file);
    }

    private void markFailed(DataFile file, String message) {
        if ("NONE".equals(file.getPreprocessStatus()) || "PROCESSING".equals(file.getPreprocessStatus())) {
            file.setPreprocessStatus("FAILED");
        }
        if ("NONE".equals(file.getOcrStatus()) || "PROCESSING".equals(file.getOcrStatus())) {
            file.setOcrStatus("FAILED");
        }
        dataFileMapper.updateById(file);
        // 更新父记录状态
        EquipmentData data = equipmentDataMapper.selectById(file.getDataId());
        if (data != null) {
            data.setStatus("FAILED");
            equipmentDataMapper.updateById(data);
        }
    }

    /**
     * 检查同一数据记录下的所有文件是否处理完成，更新父记录状态
     */
    private void updateEquipmentDataStatus(Long dataId) {
        List<DataFile> files = dataFileMapper.selectByDataId(dataId);
        boolean allDone = files.stream().allMatch(f ->
                "DONE".equals(f.getOcrStatus()) || "other".equals(f.getFileType()));
        boolean anyFailed = files.stream().anyMatch(f -> "FAILED".equals(f.getOcrStatus()));

        EquipmentData data = equipmentDataMapper.selectById(dataId);
        if (data == null) return;

        if (anyFailed) {
            data.setStatus("FAILED");
        } else if (allDone) {
            data.setStatus("COMPLETED");
        } else {
            data.setStatus("PROCESSING");
        }
        equipmentDataMapper.updateById(data);
    }

    /**
     * 获取本地可读路径：
     * - 旧记录 (/profile/...) → 直接映射到本地文件系统
     * - 新记录 (/minio/...) → 下载到系统临时目录
     */
    private String downloadToTemp(String pathOrKey) throws Exception {
        if (pathOrKey.startsWith("/profile")) {
            return CldaConfig.getProfile() + pathOrKey.substring("/profile".length());
        }
        if (pathOrKey.startsWith("/minio/")) {
            String objectKey = pathOrKey.substring("/minio/".length());
            int dotIdx = pathOrKey.lastIndexOf('.');
            String ext = dotIdx > 0 ? pathOrKey.substring(dotIdx) : "";
            Path tmp = Files.createTempFile("clda_ocr_", ext);
            try (InputStream in = minioService.download(objectKey)) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            return tmp.toAbsolutePath().toString();
        }
        return pathOrKey;
    }

    private boolean isTempPath(String path) {
        if (path == null) return false;
        String tmpDir = System.getProperty("java.io.tmpdir");
        return path.startsWith(tmpDir) || path.startsWith("/tmp/");
    }

    /**
     * 上传中间产出文件到 MinIO，返回 /minio/{objectKey} 格式路径
     */
    private String uploadIntermediate(String localPath, String subDir) {
        if (localPath == null || !new File(localPath).exists()) return null;
        String filename = Path.of(localPath).getFileName().toString();
        String objectKey = minioService.generateObjectKey(subDir, filename);
        String contentType = detectContentType(localPath);
        minioService.uploadFile(new File(localPath), objectKey, contentType);
        return "/minio/" + objectKey;
    }

    private String detectContentType(String path) {
        if (path == null) return "application/octet-stream";
        String lower = path.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }
}
