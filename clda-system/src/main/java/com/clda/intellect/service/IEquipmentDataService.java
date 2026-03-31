package com.clda.intellect.service;

import com.clda.intellect.domain.DataFile;
import com.clda.intellect.domain.EquipmentData;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

/**
 * 设备数据采集Service接口
 *
 * @author ruoyi-xiaozhi
 */
public interface IEquipmentDataService {

    List<EquipmentData> selectList(Long equipmentId, Long categoryId, Date dataDate);

    EquipmentData selectById(Long id);

    /** 上传文件并创建数据记录 */
    EquipmentData uploadFiles(Long equipmentId, Long categoryId, Long subCategoryId, Date dataDate, MultipartFile[] files, String operName);

    /** 获取数据记录关联的文件列表 */
    List<DataFile> selectFilesByDataId(Long dataId);

    /** 删除数据记录及关联文件 */
    int deleteByIds(Long[] ids);

    /** 删除单个文件 */
    int deleteFile(Long fileId);

    /** 查询指定设备+分类有数据的日期列表(用于日历标记) */
    List<String> selectDataDates(Long equipmentId, Long categoryId, String yearMonth);
}
