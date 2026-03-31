package com.clda.intellect.service;

import com.clda.intellect.domain.Equipment;
import java.util.List;

/**
 * 起重设备Service接口
 *
 * @author ruoyi-xiaozhi
 */
public interface IEquipmentService {

    Equipment selectEquipmentById(Long id);

    List<Equipment> selectEquipmentList(Equipment equipment);

    int insertEquipment(Equipment equipment);

    int updateEquipment(Equipment equipment);

    int deleteEquipmentByIds(Long[] ids);

    String importEquipment(List<Equipment> list, boolean updateSupport, String operName);
}
