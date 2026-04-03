package com.clda.intellect.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 文档解析工具：将 PDF / Word 文件内容解析为简单 HTML
 */
public class DocumentParser {

    /**
     * 解析上传文件为 HTML 字符串
     * @param file 上传的 MultipartFile（.pdf / .docx / .doc）
     * @return 简单 HTML 文本
     */
    public static String parseToHtml(MultipartFile file) throws Exception {
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (originalName.endsWith(".pdf")) {
            return parsePdf(file.getInputStream());
        } else if (originalName.endsWith(".docx") || originalName.endsWith(".doc")) {
            return parseDocx(file.getInputStream());
        } else if (originalName.endsWith(".txt")) {
            return parseTxt(file.getInputStream());
        }
        throw new IllegalArgumentException("不支持的文件格式，仅支持 PDF、Word（.docx/.doc）、TXT");
    }

    private static String parsePdf(InputStream is) throws Exception {
        try (PDDocument doc = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            return textToHtml(text);
        }
    }

    private static String parseDocx(InputStream is) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(is)) {
            StringBuilder sb = new StringBuilder();
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            for (XWPFParagraph p : paragraphs) {
                String text = p.getText();
                if (text == null || text.trim().isEmpty()) continue;
                String styleName = p.getStyle() == null ? "" : p.getStyle().toLowerCase();
                if (styleName.contains("heading1") || styleName.contains("1")) {
                    sb.append("<h2>").append(escapeHtml(text)).append("</h2>\n");
                } else if (styleName.contains("heading2") || styleName.contains("2")) {
                    sb.append("<h3>").append(escapeHtml(text)).append("</h3>\n");
                } else if (styleName.contains("heading3") || styleName.contains("3")) {
                    sb.append("<h4>").append(escapeHtml(text)).append("</h4>\n");
                } else {
                    sb.append("<p>").append(escapeHtml(text)).append("</p>\n");
                }
            }
            return sb.toString();
        }
    }

    private static String parseTxt(InputStream is) throws Exception {
        String text = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        return textToHtml(text);
    }

    /** Plain text → HTML: blank lines become paragraph breaks */
    private static String textToHtml(String text) {
        StringBuilder sb = new StringBuilder();
        String[] lines = text.split("\n");
        StringBuilder para = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (para.length() > 0) {
                    sb.append("<p>").append(escapeHtml(para.toString().trim())).append("</p>\n");
                    para.setLength(0);
                }
            } else {
                if (para.length() > 0) para.append(" ");
                para.append(trimmed);
            }
        }
        if (para.length() > 0) {
            sb.append("<p>").append(escapeHtml(para.toString().trim())).append("</p>\n");
        }
        return sb.toString();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
