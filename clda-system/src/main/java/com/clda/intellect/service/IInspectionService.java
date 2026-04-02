package com.clda.intellect.service;

import com.clda.intellect.domain.InspectionItem;
import com.clda.intellect.domain.InspectionRecord;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IInspectionService {

    /** 获取全部检查项（按order_num排序） */
    List<InspectionItem> selectAllItems();

    /** 检查项列表（分页用，按category筛选） */
    List<InspectionItem> selectItemList(InspectionItem query);

    /** 生成 Excel 字节流 */
    byte[] generateExcel(Long equipmentId, String equipmentName, String inspector);

    /** 上传 Excel 并解析结果 */
    InspectionRecord uploadAndParse(MultipartFile file, Long equipmentId, String equipmentName, String inspector);

    /** 获取设备历史排查记录 */
    List<InspectionRecord> selectRecordsByEquipmentId(Long equipmentId);

    /** 获取记录详情（含结果明细） */
    InspectionRecord selectRecordDetail(Long recordId);

    /** 检查项 CRUD */
    int insertItem(InspectionItem item);
    int updateItem(InspectionItem item);
    int deleteItemByIds(Long[] ids);
}
