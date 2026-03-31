package com.clda.intellect.service.impl;

import cn.hutool.core.util.StrUtil;
import com.clda.intellect.domain.Equipment;
import com.clda.intellect.domain.Partition;
import com.clda.intellect.mapper.EquipmentMapper;
import com.clda.intellect.mapper.PartitionMapper;
import com.clda.intellect.service.IEquipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 起重设备Service业务层处理
 *
 * @author ruoyi-xiaozhi
 */
@Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements IEquipmentService {

    private final EquipmentMapper equipmentMapper;
    private final PartitionMapper partitionMapper;

    @Override
    public Equipment selectEquipmentById(Long id) {
        Equipment eq = equipmentMapper.selectById(id);
        if (eq != null && eq.getPartitionId() != null) {
            Partition p = partitionMapper.selectById(eq.getPartitionId());
            eq.setPartitionName(p != null ? p.getPartitionName() : null);
        }
        return eq;
    }

    @Override
    public List<Equipment> selectEquipmentList(Equipment equipment) {
        List<Equipment> list = equipmentMapper.selectList(equipment);
        // 回显分区名称
        Map<Long, Partition> partitionMap = partitionMapper.mapRecordsByIds(
                list.stream().map(Equipment::getPartitionId).collect(Collectors.toSet()));
        for (Equipment eq : list) {
            Partition p = partitionMap.get(eq.getPartitionId());
            eq.setPartitionName(p != null ? p.getPartitionName() : null);
        }
        return list;
    }

    @Override
    public int insertEquipment(Equipment equipment) {
        return equipmentMapper.insert(equipment);
    }

    @Override
    public int updateEquipment(Equipment equipment) {
        return equipmentMapper.updateById(equipment);
    }

    @Override
    public int deleteEquipmentByIds(Long[] ids) {
        return equipmentMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public String importEquipment(List<Equipment> list, boolean updateSupport, String operName) {
        int successCount = 0;
        int failCount = 0;
        StringBuilder failMsg = new StringBuilder();
        for (Equipment eq : list) {
            try {
                eq.setCreateBy(operName);
                equipmentMapper.insert(eq);
                successCount++;
            } catch (Exception e) {
                failCount++;
                failMsg.append(StrUtil.format("设备 {} 导入失败: {}<br/>", eq.getEquipmentName(), e.getMessage()));
            }
        }
        if (failCount > 0) {
            return StrUtil.format("导入完成，成功{}条，失败{}条<br/>{}", successCount, failCount, failMsg);
        }
        return StrUtil.format("导入成功，共{}条", successCount);
    }
}
