package com.clda.intellect.service.impl;

import com.clda.intellect.domain.KnowledgeBook;
import com.clda.intellect.mapper.KnowledgeBookMapper;
import com.clda.intellect.service.IKnowledgeBookService;
import com.clda.intellect.service.IKnowledgeChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeBookServiceImpl implements IKnowledgeBookService {

    private final KnowledgeBookMapper bookMapper;
    private final IKnowledgeChapterService chapterService;

    @Override
    public List<KnowledgeBook> selectBooksWithChapterTree() {
        List<KnowledgeBook> books = bookMapper.selectAllOrdered();
        for (KnowledgeBook book : books) {
            book.setChapters(chapterService.selectChapterTree(book.getId()));
        }
        return books;
    }

    @Override
    public List<KnowledgeBook> selectBookList() {
        return bookMapper.selectAllOrdered();
    }

    @Override
    public KnowledgeBook selectBookById(Long id) {
        return bookMapper.selectById(id);
    }

    @Override
    public int insertBook(KnowledgeBook book) {
        if (book.getOrderNum() == null) book.setOrderNum(0);
        return bookMapper.insert(book);
    }

    @Override
    public int updateBook(KnowledgeBook book) {
        return bookMapper.updateById(book);
    }

    @Override
    @Transactional
    public int deleteBookById(Long id) {
        chapterService.deleteChaptersByBookId(id);
        return bookMapper.deleteById(id);
    }
}
