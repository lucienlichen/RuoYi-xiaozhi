package com.clda.intellect.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.common.core.mapper.CommonMapper;
import com.clda.intellect.domain.Equipment;

import java.util.List;

/**
 * 起重设备Mapper接口
 *
 * @author ruoyi-xiaozhi
 */
public interface EquipmentMapper extends CommonMapper<Equipment> {

    default List<Equipment> selectList(Equipment equipment) {
        LambdaQueryWrapper<Equipment> qw = new LambdaQueryWrapper<>();
        qw.like(StrUtil.isNotBlank(equipment.getEquipmentName()), Equipment::getEquipmentName, equipment.getEquipmentName());
        qw.eq(StrUtil.isNotBlank(equipment.getEquipmentCode()), Equipment::getEquipmentCode, equipment.getEquipmentCode());
        qw.eq(equipment.getPartitionId() != null, Equipment::getPartitionId, equipment.getPartitionId());
        qw.eq(StrUtil.isNotBlank(equipment.getStatus()), Equipment::getStatus, equipment.getStatus());
        qw.orderByDesc(Equipment::getCreateTime);
        return this.selectList(qw);
    }
}
