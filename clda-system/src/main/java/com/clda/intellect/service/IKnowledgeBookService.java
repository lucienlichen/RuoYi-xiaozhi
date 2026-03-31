package com.clda.intellect.service;

import com.clda.intellect.domain.KnowledgeBook;
import java.util.List;

public interface IKnowledgeBookService {
    /** 查询所有书籍（含章节树，不含章节 content_html） */
    List<KnowledgeBook> selectBooksWithChapterTree();

    /** 查询书籍列表（不含章节，用于管理列表） */
    List<KnowledgeBook> selectBookList();

    KnowledgeBook selectBookById(Long id);

    int insertBook(KnowledgeBook book);

    int updateBook(KnowledgeBook book);

    int deleteBookById(Long id);
}
