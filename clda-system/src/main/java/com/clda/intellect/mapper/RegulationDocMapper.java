package com.clda.intellect.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.common.core.mapper.CommonMapper;
import com.clda.intellect.domain.RegulationDoc;
import java.util.List;

public interface RegulationDocMapper extends CommonMapper<RegulationDoc> {

    default List<RegulationDoc> selectList(RegulationDoc query) {
        LambdaQueryWrapper<RegulationDoc> qw = new LambdaQueryWrapper<>();
        qw.eq(StrUtil.isNotBlank(query.getCategory()), RegulationDoc::getCategory, query.getCategory());
        qw.like(StrUtil.isNotBlank(query.getTitle()), RegulationDoc::getTitle, query.getTitle());
        qw.orderByDesc(RegulationDoc::getCreateTime);
        return this.selectList(qw);
    }

    /** 查询单文档（含 content_html）*/
    default RegulationDoc selectWithContent(Long id) {
        return this.selectOne(new LambdaQueryWrapper<RegulationDoc>()
                .select(RegulationDoc.class, f -> true)
                .eq(RegulationDoc::getId, id));
    }
}
