package com.clda.intellect.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.common.core.mapper.CommonMapper;
import com.clda.intellect.domain.DataCategory;

import java.util.List;

/**
 * 数据分类Mapper接口
 *
 * @author clda-xiaozhi
 */
public interface DataCategoryMapper extends CommonMapper<DataCategory> {

    default List<DataCategory> selectAll() {
        LambdaQueryWrapper<DataCategory> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(DataCategory::getOrderNum);
        return this.selectList(qw);
    }

    default List<DataCategory> selectByParentId(Long parentId) {
        LambdaQueryWrapper<DataCategory> qw = new LambdaQueryWrapper<>();
        qw.eq(DataCategory::getParentId, parentId);
        qw.orderByAsc(DataCategory::getOrderNum);
        return this.selectList(qw);
    }
}
