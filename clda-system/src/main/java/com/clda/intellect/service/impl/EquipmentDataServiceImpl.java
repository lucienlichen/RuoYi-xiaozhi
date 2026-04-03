package com.clda.intellect.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.common.utils.minio.MinioService;
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
 * @author clda-xiaozhi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentDataServiceImpl implements IEquipmentDataService {

    private final EquipmentDataMapper equipmentDataMapper;
    private final DataFileMapper dataFileMapper;
    private final IDataProcessingService dataProcessingService;
    private final MinioService minioService;

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
        // 创建数据记录
        EquipmentData data = new EquipmentData();
        data.setEquipmentId(equipmentId);
        data.setCategoryId(categoryId);
        data.setSubCategoryId(subCategoryId);
        data.setDataDate(dataDate != null ? dataDate : new Date());
        data.setStatus("PENDING");
        data.setCreateBy(operName);
        equipmentDataMapper.insert(data);

        // 上传并保存文件，追踪成功/失败
        int successCount = 0;
        List<String> failedNames = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                String objectKey = minioService.upload(file, "upload/crane/" + equipmentId);
                String filePath = "/minio/" + objectKey;
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
                successCount++;
            } catch (Exception e) {
                log.error("文件上传失败: {}", file.getOriginalFilename(), e);
                failedNames.add(file.getOriginalFilename());
            }
        }

        // 根据实际结果设置状态
        if (successCount == 0) {
            data.setStatus("FAILED");
            data.setRemark("全部文件上传失败: " + String.join(", ", failedNames));
            equipmentDataMapper.updateById(data);
            throw new com.clda.common.exception.ServiceException(
                    "文件上传失败: " + String.join(", ", failedNames));
        }

        data.setStatus("PROCESSING");
        if (!failedNames.isEmpty()) {
            data.setRemark(successCount + " 个文件上传成功，" + failedNames.size() + " 个失败: " + String.join(", ", failedNames));
        }
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
            List<DataFile> files = dataFileMapper.selectByDataId(id);
            for (DataFile f : files) {
                deleteMinioObjects(f);
                dataFileMapper.deleteById(f.getId());
            }
        }
        return equipmentDataMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public int deleteFile(Long fileId) {
        DataFile file = dataFileMapper.selectById(fileId);
        if (file != null) {
            deleteMinioObjects(file);
        }
        return dataFileMapper.deleteById(fileId);
    }

    /**
     * 删除 MinIO 中的源文件和中间产物（预处理图、增强图）
     */
    private void deleteMinioObjects(DataFile file) {
        deleteMinioKey(file.getFilePath());
        deleteMinioKey(file.getPreprocessedPath());
        deleteMinioKey(file.getEnhancedPath());
    }

    private void deleteMinioKey(String path) {
        if (path != null && path.startsWith("/minio/")) {
            try {
                minioService.delete(path.substring("/minio/".length()));
            } catch (Exception e) {
                log.warn("MinIO 对象删除失败: {}", path, e);
            }
        }
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
