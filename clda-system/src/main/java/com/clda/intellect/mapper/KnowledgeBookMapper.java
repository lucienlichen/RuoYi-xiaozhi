package com.clda.intellect.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.common.core.mapper.CommonMapper;
import com.clda.intellect.domain.KnowledgeBook;
import java.util.List;

public interface KnowledgeBookMapper extends CommonMapper<KnowledgeBook> {

    default List<KnowledgeBook> selectAllOrdered() {
        return this.selectList(new LambdaQueryWrapper<KnowledgeBook>()
                .orderByAsc(KnowledgeBook::getOrderNum)
                .orderByAsc(KnowledgeBook::getCreateTime));
    }
}
