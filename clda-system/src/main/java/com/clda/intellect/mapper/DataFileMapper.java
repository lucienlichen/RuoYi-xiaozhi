package com.clda.intellect.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.common.core.mapper.CommonMapper;
import com.clda.intellect.domain.DataFile;

import java.util.List;

/**
 * 数据文件Mapper接口
 *
 * @author ruoyi-xiaozhi
 */
public interface DataFileMapper extends CommonMapper<DataFile> {

    default List<DataFile> selectByDataId(Long dataId) {
        LambdaQueryWrapper<DataFile> qw = new LambdaQueryWrapper<>();
        qw.eq(DataFile::getDataId, dataId);
        qw.orderByAsc(DataFile::getSortOrder);
        return this.selectList(qw);
    }
}
