package com.clda.intellect.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.common.core.mapper.CommonMapper;
import com.clda.intellect.domain.KnowledgeChapter;
import java.util.List;

public interface KnowledgeChapterMapper extends CommonMapper<KnowledgeChapter> {

    /** 按书籍ID查询所有章节（不含 content_html，按 level+order_num 排序）*/
    default List<KnowledgeChapter> selectByBookId(Long bookId) {
        return this.selectList(new LambdaQueryWrapper<KnowledgeChapter>()
                .eq(KnowledgeChapter::getBookId, bookId)
                .orderByAsc(KnowledgeChapter::getLevel)
                .orderByAsc(KnowledgeChapter::getOrderNum));
    }

    /** 查询单章节（含 content_html），需要覆盖默认 select=false */
    default KnowledgeChapter selectWithContent(Long id) {
        return this.selectOne(new LambdaQueryWrapper<KnowledgeChapter>()
                .select(KnowledgeChapter.class, f -> true)   // select all fields including content_html
                .eq(KnowledgeChapter::getId, id));
    }
}
