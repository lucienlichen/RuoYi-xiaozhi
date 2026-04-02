package com.clda.intellect.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.common.config.CldaConfig;
import com.clda.common.utils.file.FileUploadUtils;
import com.clda.intellect.domain.DataFile;
import com.clda.intellect.domain.EquipmentData;
import com.clda.intellect.mapper.DataFileMapper;
import com.clda.intellect.mapper.EquipmentDataMapper;
import com.clda.intellect.service.IDataProcessingService;
import com.clda.intellect.service.IEquipmentDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备数据采集Service业务层处理
 *
 * @author ruoyi-xiaozhi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentDataServiceImpl implements IEquipmentDataService {

    private final EquipmentDataMapper equipmentDataMapper;
    private final DataFileMapper dataFileMapper;
    private final IDataProcessingService dataProcessingService;

    @Override
    public List<EquipmentData> selectList(Long equipmentId, Long categoryId, Date dataDate) {
        return equipmentDataMapper.selectList(equipmentId, categoryId, dataDate);
    }

    @Override
    public EquipmentData selectById(Long id) {
        return equipmentDataMapper.selectById(id);
    }

    @Override
    public EquipmentData uploadFiles(Long equipmentId, Long categoryId, Long subCategoryId, Date dataDate, MultipartFile[] files, String operName) {
        // 创建或获取当天的数据记录
        EquipmentData data = new EquipmentData();
        data.setEquipmentId(equipmentId);
        data.setCategoryId(categoryId);
        data.setSubCategoryId(subCategoryId);
        data.setDataDate(dataDate != null ? dataDate : new Date());
        data.setStatus("PENDING");
        data.setCreateBy(operName);
        equipmentDataMapper.insert(data);

        // 上传并保存文件
        String uploadPath = CldaConfig.getUploadPath() + "/crane/" + equipmentId;
        for (MultipartFile file : files) {
            try {
                String filePath = FileUploadUtils.upload(uploadPath, file);
                DataFile dataFile = new DataFile();
                dataFile.setDataId(data.getId());
                dataFile.setFileName(file.getOriginalFilename());
                dataFile.setFilePath(filePath);
                dataFile.setFileSize(file.getSize());
                dataFile.setMimeType(file.getContentType());
                dataFile.setFileType(resolveFileType(file.getContentType(), file.getOriginalFilename()));
                dataFile.setPreprocessStatus("NONE");
                dataFile.setOcrStatus("NONE");
                dataFile.setCreateBy(operName);
                dataFileMapper.insert(dataFile);

                // 触发异步处理流水线
                dataProcessingService.processFileAsync(dataFile.getId());
            } catch (Exception e) {
                log.error("文件上传失败: {}", file.getOriginalFilename(), e);
            }
        }

        // 更新状态为处理中
        data.setStatus("PROCESSING");
        equipmentDataMapper.updateById(data);
        return data;
    }

    @Override
    public List<DataFile> selectFilesByDataId(Long dataId) {
        return dataFileMapper.selectByDataId(dataId);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        for (Long id : ids) {
            // 先删关联文件
            List<DataFile> files = dataFileMapper.selectByDataId(id);
            for (DataFile f : files) {
                dataFileMapper.deleteById(f.getId());
            }
        }
        return equipmentDataMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public int deleteFile(Long fileId) {
        return dataFileMapper.deleteById(fileId);
    }

    @Override
    public List<String> selectDataDates(Long equipmentId, Long categoryId, String yearMonth) {
        LambdaQueryWrapper<EquipmentData> qw = new LambdaQueryWrapper<>();
        qw.eq(EquipmentData::getEquipmentId, equipmentId);
        qw.eq(EquipmentData::getCategoryId, categoryId);
        if (StrUtil.isNotBlank(yearMonth)) {
            // yearMonth format: "2026-03"
            qw.apply("DATE_FORMAT(data_date, '%Y-%m') = {0}", yearMonth);
        }
        qw.select(EquipmentData::getDataDate);
        qw.groupBy(EquipmentData::getDataDate);
        List<EquipmentData> list = equipmentDataMapper.selectList(qw);
        return list.stream()
                .map(d -> DateUtil.format(d.getDataDate(), "yyyy-MM-dd"))
                .collect(Collectors.toList());
    }

    @Override
    public DataFile selectFileById(Long fileId) {
        return dataFileMapper.selectById(fileId);
    }

    private String resolveFileType(String contentType, String fileName) {
        if (contentType == null) contentType = "";
        if (contentType.startsWith("image/")) return "image";
        if (contentType.equals("application/pdf")) return "pdf";
        if (contentType.contains("word") || contentType.contains("document")) return "word";
        if (contentType.contains("excel") || contentType.contains("spreadsheet")) return "excel";
        // Fallback by extension
        String ext = FileUtil.extName(fileName);
        if (ext == null) return "other";
        return switch (ext.toLowerCase()) {
            case "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "image";
            case "pdf" -> "pdf";
            case "doc", "docx" -> "word";
            case "xls", "xlsx", "csv" -> "excel";
            default -> "other";
        };
    }
}
