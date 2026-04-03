package com.clda.intellect.service.impl;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_imgproc.CLAHE;
import org.springframework.stereotype.Service;

import java.io.File;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * Tesseract OCR 服务 — 含 OpenCV 图片预处理流水线
 * <p>
 * 预处理流程：自动旋转（EXIF → OSD 两级检测）→ 限尺寸 → CLAHE → 去噪
 * 产出两个版本：
 * 1. 增强去噪版（_ocr_input，Tesseract 自行二值化，效果更好）
 * 2. 二值化纠偏版（_preprocessed，前端展示用）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {

    private final AiConfigService aiConfigService;

    /** OCR 输入图片的最长边上限（超过则等比缩放） */
    private static final int MAX_SIDE_PX = 3000;

    /**
     * 对图片文件执行 OCR 识别
     */
    public String recognize(String imagePath) {
        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(aiConfigService.getOcrTessdataPath());
            tesseract.setLanguage(aiConfigService.getOcrLanguage());
            tesseract.setPageSegMode(aiConfigService.getOcrPageSegMode());
            tesseract.setOcrEngineMode(aiConfigService.getOcrEngineMode());

            File imageFile = new File(imagePath);
            String result = tesseract.doOCR(imageFile);
            log.info("OCR完成, 文件: {}, 文本长度: {}", imagePath, result.length());
            return result;
        } catch (Exception e) {
            log.error("OCR识别失败: {}", imagePath, e);
            throw new RuntimeException("OCR识别失败: " + e.getMessage(), e);
        }
    }

    /**
     * 图片预处理：生成 OCR 输入版（增强去噪）和视觉展示版（二值化+纠偏）
     *
     * @param inputPath  原始图片路径
     * @param outputPath 预处理结果路径（二值化版本，用于展示）
     * @return OCR 输入文件路径（增强去噪版，用于识别）
     */
    public String preprocessImage(String inputPath, String outputPath) {
        Mat src = null;
        Mat rotated = null;
        Mat resized = null;
        Mat gray = null;
        Mat clahe = null;
        Mat denoised = null;
        Mat binary = null;
        Mat deskewed = null;
        try {
            src = imread(inputPath, IMREAD_COLOR);
            if (src.empty()) {
                throw new RuntimeException("无法读取图片: " + inputPath);
            }

            // 1. 自动旋转：EXIF 优先，WeChat/JFIF 图片 fallback 到 Tesseract OSD
            rotated = autoRotate(src, inputPath);

            // 2. 等比缩放（最长边超过 MAX_SIDE_PX 时缩小）
            resized = limitSize(rotated);

            // 3. 灰度化
            gray = new Mat();
            cvtColor(resized, gray, COLOR_BGR2GRAY);

            // 4. CLAHE 局部对比增强（修正光照不均、阴影）
            CLAHE claheObj = createCLAHE(2.0, new Size(8, 8));
            clahe = new Mat();
            claheObj.apply(gray, clahe);
            claheObj.close();

            // 5. 高斯去噪（3×3，轻量快速）
            denoised = new Mat();
            GaussianBlur(clahe, denoised, new Size(3, 3), 0);

            // === 保存 OCR 输入版 ===
            String ocrInputPath = outputPath.replace("_preprocessed.", "_ocr_input.");
            imwrite(ocrInputPath, denoised);

            // === 生成二值化+纠偏版（仅视觉展示用）===
            // blockSize=31 覆盖更大局部区域，C=18 把背景噪点推向白色，减少黑色小点
            binary = new Mat();
            adaptiveThreshold(denoised, binary, 255,
                    ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 31, 18);
            // 形态学开运算：用 2×2 核去除孤立黑色噪点，保留真实笔画
            Mat kernel = getStructuringElement(MORPH_RECT, new Size(2, 2));
            Mat cleaned = new Mat();
            morphologyEx(binary, cleaned, MORPH_OPEN, kernel);
            kernel.close();
            binary.close();
            binary = cleaned;
            deskewed = autoDeskew(binary);
            imwrite(outputPath, deskewed);

            log.info("图片预处理完成: {} → 展示版:{}, OCR版:{}", inputPath, outputPath, ocrInputPath);
            return ocrInputPath;
        } catch (Exception e) {
            log.error("图片预处理失败: {}", inputPath, e);
            throw new RuntimeException("图片预处理失败: " + e.getMessage(), e);
        } finally {
            releaseMat(src, rotated, resized, gray, clahe, denoised, binary, deskewed);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 旋转检测
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 两级自动旋转策略：
     * <ol>
     *   <li>读取 EXIF Orientation 标签（适用于原生相机 JPEG）</li>
     *   <li>EXIF 不可用（WeChat 等剥离 EXIF 的场景）→ Tesseract OSD 内容检测</li>
     * </ol>
     */
    private Mat autoRotate(Mat src, String imagePath) {
        // 优先使用 EXIF
        int exifOrientation = readExifOrientation(imagePath);
        if (exifOrientation > 1) {
            Mat result = applyExifRotation(src, exifOrientation);
            if (result != null) return result;
        }

        // 降级：Tesseract OSD 内容方向检测
        int osdDegrees = detectOrientationByOsd(imagePath);
        if (osdDegrees == 0) return src.clone();

        Mat dst = new Mat();
        switch (osdDegrees) {
            // OSD "Orientation in degrees" = 当前图片相对正确朝向偏转了多少度
            // 需要反向旋转来纠正
            case 90  -> rotate(src, dst, ROTATE_90_COUNTERCLOCKWISE);
            case 180 -> rotate(src, dst, ROTATE_180);
            case 270 -> rotate(src, dst, ROTATE_90_CLOCKWISE);
            default  -> { return src.clone(); }
        }
        log.info("OSD自动旋转: {}° ({})", osdDegrees, imagePath);
        return dst;
    }

    /**
     * 读取 JPEG EXIF Orientation 标签（JFIF/WeChat 图片无此标签）。
     */
    private int readExifOrientation(String imagePath) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new File(imagePath));
            ExifIFD0Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (dir != null && dir.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return dir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (Exception ignored) { }
        return 1;
    }

    /**
     * 按 EXIF Orientation 值旋转图片（标准 EXIF 旋转语义）。
     */
    private Mat applyExifRotation(Mat src, int orientation) {
        Mat dst = new Mat();
        switch (orientation) {
            case 3 -> rotate(src, dst, ROTATE_180);
            case 6 -> rotate(src, dst, ROTATE_90_CLOCKWISE);
            case 8 -> rotate(src, dst, ROTATE_90_COUNTERCLOCKWISE);
            default -> { return null; } // 无需旋转
        }
        log.debug("EXIF旋转: orientation={}", orientation);
        return dst;
    }

    /**
     * 使用 Tesseract OSD（osd.traineddata）检测图片文字方向。
     * 适用于 WeChat 等剥离了 EXIF 的图片。
     *
     * @return 图片当前相对正确朝向的偏转角度（0 / 90 / 180 / 270），无法检测时返回 0
     */
    private int detectOrientationByOsd(String imagePath) {
        try {
            Tesseract osd = new Tesseract();
            osd.setDatapath(aiConfigService.getOcrTessdataPath());
            osd.setLanguage("osd");
            osd.setPageSegMode(0); // PSM_OSD_ONLY

            String result = osd.doOCR(new File(imagePath));
            if (result == null) return 0;

            // 解析 "Orientation in degrees: 90"
            for (String line : result.split("\n")) {
                if (line.startsWith("Orientation in degrees:")) {
                    int degrees = Integer.parseInt(line.split(":")[1].trim());
                    if (degrees != 0) {
                        log.debug("OSD检测旋转角度: {}° ({})", degrees, imagePath);
                    }
                    return degrees;
                }
            }
        } catch (Exception e) {
            log.debug("OSD方向检测跳过: {}", e.getMessage());
        }
        return 0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 工具方法
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 等比缩放：最长边超过 MAX_SIDE_PX 时缩小。
     */
    private Mat limitSize(Mat src) {
        int w = src.cols(), h = src.rows();
        int maxSide = Math.max(w, h);
        if (maxSide <= MAX_SIDE_PX) return src.clone();
        double scale = (double) MAX_SIDE_PX / maxSide;
        Mat dst = new Mat();
        resize(src, dst, new Size((int) (w * scale), (int) (h * scale)), 0, 0, INTER_AREA);
        log.debug("图片缩放: {}×{} → {}×{}", w, h, dst.cols(), dst.rows());
        return dst;
    }

    /**
     * 自动纠偏：修正文档小角度倾斜（±15° 内），大角度已由 autoRotate 处理。
     */
    private Mat autoDeskew(Mat src) {
        Mat inverted = new Mat();
        bitwise_not(src, inverted);
        Mat points = new Mat();
        findNonZero(inverted, points);
        inverted.close();

        if (points.empty()) { points.close(); return src.clone(); }

        RotatedRect rect = minAreaRect(points);
        points.close();

        float angle = rect.angle();
        if (angle < -45) angle += 90;
        if (Math.abs(angle) < 0.5 || Math.abs(angle) > 15) return src.clone();

        log.debug("小角度纠偏: {}°", String.format("%.2f", angle));
        Point2f center = new Point2f(src.cols() / 2.0f, src.rows() / 2.0f);
        Mat rotMatrix = getRotationMatrix2D(center, angle, 1.0);
        Mat deskewed = new Mat();
        warpAffine(src, deskewed, rotMatrix, src.size(),
                INTER_LINEAR, BORDER_REPLICATE, new Scalar(255, 255, 255, 0));
        rotMatrix.close();
        center.close();
        return deskewed;
    }

    private void releaseMat(Mat... mats) {
        for (Mat m : mats) {
            if (m != null && !m.isNull()) {
                try { m.close(); } catch (Exception ignored) {}
            }
        }
    }
}
