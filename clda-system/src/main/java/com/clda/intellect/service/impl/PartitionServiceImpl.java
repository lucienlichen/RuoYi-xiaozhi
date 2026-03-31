package com.clda.intellect.service.impl;

import com.clda.intellect.domain.Equipment;
import com.clda.intellect.domain.Partition;
import com.clda.intellect.mapper.EquipmentMapper;
import com.clda.intellect.mapper.PartitionMapper;
import com.clda.intellect.service.IPartitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 设备分区Service业务层处理
 *
 * @author ruoyi-xiaozhi
 */
@Service
@RequiredArgsConstructor
public class PartitionServiceImpl implements IPartitionService {

    private final PartitionMapper partitionMapper;
    private final EquipmentMapper equipmentMapper;

    @Override
    public Partition selectPartitionById(Long id) {
        return partitionMapper.selectById(id);
    }

    @Override
    public List<Partition> selectPartitionList(Partition partition) {
        return partitionMapper.selectList(partition);
    }

    @Override
    public int insertPartition(Partition partition) {
        return partitionMapper.insert(partition);
    }

    @Override
    public int updatePartition(Partition partition) {
        return partitionMapper.updateById(partition);
    }

    @Override
    public int deletePartitionByIds(Long[] ids) {
        // 检查分区下是否有设备
        for (Long id : ids) {
            Equipment query = new Equipment();
            query.setPartitionId(id);
            List<Equipment> equipments = equipmentMapper.selectList(query);
            if (!equipments.isEmpty()) {
                Partition p = partitionMapper.selectById(id);
                throw new RuntimeException("分区【" + (p != null ? p.getPartitionName() : id) + "】下存在设备，不允许删除");
            }
        }
        return partitionMapper.deleteByIds(Arrays.asList(ids));
    }
}
