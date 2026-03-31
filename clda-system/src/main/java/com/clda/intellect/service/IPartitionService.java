package com.clda.intellect.service;

import com.clda.intellect.domain.Partition;
import java.util.List;

/**
 * 设备分区Service接口
 *
 * @author ruoyi-xiaozhi
 */
public interface IPartitionService {

    Partition selectPartitionById(Long id);

    List<Partition> selectPartitionList(Partition partition);

    int insertPartition(Partition partition);

    int updatePartition(Partition partition);

    int deletePartitionByIds(Long[] ids);
}
