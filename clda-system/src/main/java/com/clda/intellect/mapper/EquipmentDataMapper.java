package com.clda.intellect.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.common.core.mapper.CommonMapper;
import com.clda.intellect.domain.EquipmentData;

import java.util.Date;
import java.util.List;

/**
 * 设备数据记录Mapper接口
 *
 * @author ruoyi-xiaozhi
 */
public interface EquipmentDataMapper extends CommonMapper<EquipmentData> {

    default List<EquipmentData> selectList(Long equipmentId, Long categoryId, Date dataDate) {
        LambdaQueryWrapper<EquipmentData> qw = new LambdaQueryWrapper<>();
        qw.eq(equipmentId != null, EquipmentData::getEquipmentId, equipmentId);
        qw.eq(categoryId != null, EquipmentData::getCategoryId, categoryId);
        qw.eq(dataDate != null, EquipmentData::getDataDate, dataDate);
        qw.orderByDesc(EquipmentData::getCreateTime);
        return this.selectList(qw);
    }
}
