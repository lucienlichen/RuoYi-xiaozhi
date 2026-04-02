package com.clda.intellect.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.common.core.mapper.CommonMapper;
import com.clda.intellect.domain.StructuringTemplate;

/**
 * 结构化模板Mapper接口
 */
public interface StructuringTemplateMapper extends CommonMapper<StructuringTemplate> {

    default StructuringTemplate selectByCategoryCode(String categoryCode) {
        LambdaQueryWrapper<StructuringTemplate> qw = new LambdaQueryWrapper<>();
        qw.eq(StructuringTemplate::getCategoryCode, categoryCode);
        qw.eq(StructuringTemplate::getEnabled, "1");
        qw.last("LIMIT 1");
        return this.selectOne(qw);
    }
}
