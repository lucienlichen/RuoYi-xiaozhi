package com.clda.intellect.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.bytedeco.opencv.opencv_core.*;
import org.springframework.stereotype.Service;

import java.io.File;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * Tesseract OCR 服务 — 含 OpenCV 图片预处理流水线
 * <p>
 * 预处理产出两个版本：
 * 1. 灰度去噪版（用于 OCR 识别，保留更多细节）
 * 2. 二值化纠偏版（用于视觉展示，存储到 preprocessedPath）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {

    private final AiConfigService aiConfigService;

    /**
     * 对图片文件执行OCR识别
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
     * 图片预处理：生成视觉展示版（二值化+纠偏）和 OCR 输入版（灰度+去噪）
     *
     * @param inputPath  原始图片路径
     * @param outputPath 预处理结果路径（二值化版本，用于展示）
     * @return OCR 输入文件路径（灰度去噪版，用于识别）
     */
    public String preprocessImage(String inputPath, String outputPath) {
        Mat src = null;
        Mat gray = null;
        Mat denoised = null;
        Mat binary = null;
        Mat sharpened = null;
        Mat deskewed = null;
        try {
            src = imread(inputPath, IMREAD_COLOR);
            if (src.empty()) {
                throw new RuntimeException("无法读取图片: " + inputPath);
            }

            // 1. 灰度化
            gray = new Mat();
            cvtColor(src, gray, COLOR_BGR2GRAY);

            // 2. 高斯去噪
            denoised = new Mat();
            GaussianBlur(gray, denoised, new Size(3, 3), 0);

            // === 保存灰度去噪版（用于OCR，保留更多信息） ===
            String ocrInputPath = outputPath.replace("_preprocessed.", "_ocr_input.");
            imwrite(ocrInputPath, denoised);

            // === 生成二值化+纠偏版（用于视觉展示） ===
            // 3. 自适应二值化
            binary = new Mat();
            adaptiveThreshold(denoised, binary, 255,
                    ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 15, 10);

            // 4. 锐化
            sharpened = unsharpenMask(binary);

            // 5. 自动纠偏
            deskewed = autoDeskew(sharpened);

            // 保存二值化版本
            imwrite(outputPath, deskewed);

            log.info("图片预处理完成: {} → 展示版:{}, OCR版:{}", inputPath, outputPath, ocrInputPath);
            return ocrInputPath;
        } catch (Exception e) {
            log.error("图片预处理失败: {}", inputPath, e);
            throw new RuntimeException("图片预处理失败: " + e.getMessage(), e);
        } finally {
            releaseMat(src, gray, denoised, binary, sharpened, deskewed);
        }
    }

    private Mat unsharpenMask(Mat src) {
        Mat blurred = new Mat();
        GaussianBlur(src, blurred, new Size(0, 0), 3);
        Mat sharpened = new Mat();
        addWeighted(src, 1.5, blurred, -0.5, 0, sharpened);
        blurred.close();
        return sharpened;
    }

    private Mat autoDeskew(Mat src) {
        Mat inverted = new Mat();
        bitwise_not(src, inverted);

        Mat points = new Mat();
        findNonZero(inverted, points);
        inverted.close();

        if (points.empty()) {
            points.close();
            return src.clone();
        }

        RotatedRect rect = minAreaRect(points);
        points.close();

        float angle = rect.angle();
        if (angle < -45) angle += 90;
        if (Math.abs(angle) < 0.5 || Math.abs(angle) > 15) return src.clone();

        log.debug("检测到倾斜角度: {}°", String.format("%.2f", angle));
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
