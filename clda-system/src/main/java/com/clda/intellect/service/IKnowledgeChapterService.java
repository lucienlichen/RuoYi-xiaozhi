package com.clda.intellect.service;

import com.clda.intellect.domain.KnowledgeChapter;
import java.util.List;

public interface IKnowledgeChapterService {
    /** 查询书籍的章节树（不含 content_html） */
    List<KnowledgeChapter> selectChapterTree(Long bookId);

    /** 查询单章节（含 content_html） */
    KnowledgeChapter selectChapterWithContent(Long id);

    int insertChapter(KnowledgeChapter chapter);

    int updateChapter(KnowledgeChapter chapter);

    int deleteChapterById(Long id);

    /** 删除书籍的所有章节（书籍删除时调用） */
    int deleteChaptersByBookId(Long bookId);
}
