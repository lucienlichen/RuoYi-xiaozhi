package com.clda.intellect.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.common.core.mapper.CommonMapper;
import com.clda.intellect.domain.Partition;

import java.util.List;

/**
 * 设备分区Mapper接口
 *
 * @author ruoyi-xiaozhi
 */
public interface PartitionMapper extends CommonMapper<Partition> {

    default List<Partition> selectList(Partition partition) {
        LambdaQueryWrapper<Partition> qw = new LambdaQueryWrapper<>();
        qw.like(StrUtil.isNotBlank(partition.getPartitionName()), Partition::getPartitionName, partition.getPartitionName());
        qw.eq(partition.getParentId() != null, Partition::getParentId, partition.getParentId());
        qw.eq(StrUtil.isNotBlank(partition.getStatus()), Partition::getStatus, partition.getStatus());
        qw.orderByAsc(Partition::getOrderNum);
        return this.selectList(qw);
    }
}
