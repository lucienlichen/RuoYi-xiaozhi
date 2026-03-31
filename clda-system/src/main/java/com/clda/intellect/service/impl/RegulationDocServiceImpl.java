package com.clda.intellect.service.impl;

import com.clda.common.config.CldaConfig;
import com.clda.common.utils.file.FileUploadUtils;
import com.clda.intellect.domain.RegulationDoc;
import com.clda.intellect.mapper.RegulationDocMapper;
import com.clda.intellect.service.IRegulationDocService;
import com.clda.intellect.util.DocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegulationDocServiceImpl implements IRegulationDocService {

    private final RegulationDocMapper docMapper;

    @Override
    public List<RegulationDoc> selectDocList(RegulationDoc query) {
        return docMapper.selectList(query);
    }

    @Override
    public RegulationDoc selectDocById(Long id) {
        return docMapper.selectWithContent(id);
    }

    @Override
    public RegulationDoc uploadAndParse(MultipartFile file, String title, String category,
                                         String docNo, String publishDate, String operName) throws Exception {
        // Save file to disk
        String uploadPath = CldaConfig.getUploadPath() + "/regulations";
        String filePath = FileUploadUtils.upload(uploadPath, file);

        // Parse content
        String contentHtml;
        String parseStatus;
        try {
            contentHtml = DocumentParser.parseToHtml(file);
            parseStatus = "DONE";
        } catch (Exception e) {
            log.error("文档解析失败: {}", file.getOriginalFilename(), e);
            contentHtml = "<p>文档解析失败，请手动编辑内容。</p>";
            parseStatus = "FAILED";
        }

        RegulationDoc doc = new RegulationDoc();
        doc.setTitle(title);
        doc.setCategory(category);
        doc.setDocNo(docNo);
        doc.setFileName(file.getOriginalFilename());
        doc.setFilePath(filePath);
        doc.setContentHtml(contentHtml);
        doc.setParseStatus(parseStatus);
        doc.setCreateBy(operName);
        if (publishDate != null && !publishDate.isEmpty()) {
            try {
                doc.setPublishDate(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(publishDate));
            } catch (Exception ignored) {}
        }
        docMapper.insert(doc);
        return doc;
    }

    @Override
    public int insertDoc(RegulationDoc doc) {
        doc.setParseStatus("NONE");
        return docMapper.insert(doc);
    }

    @Override
    public int updateDoc(RegulationDoc doc) {
        return docMapper.updateById(doc);
    }

    @Override
    public int deleteDocByIds(Long[] ids) {
        return docMapper.deleteByIds(Arrays.asList(ids));
    }
}
