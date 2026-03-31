package com.clda.intellect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.intellect.domain.KnowledgeChapter;
import com.clda.intellect.mapper.KnowledgeChapterMapper;
import com.clda.intellect.service.IKnowledgeChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeChapterServiceImpl implements IKnowledgeChapterService {

    private final KnowledgeChapterMapper chapterMapper;

    @Override
    public List<KnowledgeChapter> selectChapterTree(Long bookId) {
        List<KnowledgeChapter> all = chapterMapper.selectByBookId(bookId);
        return buildTree(all);
    }

    @Override
    public KnowledgeChapter selectChapterWithContent(Long id) {
        return chapterMapper.selectWithContent(id);
    }

    @Override
    public int insertChapter(KnowledgeChapter chapter) {
        if (chapter.getParentId() == null) chapter.setParentId(0L);
        if (chapter.getOrderNum() == null) chapter.setOrderNum(0);
        if (chapter.getLevel() == null) chapter.setLevel(1);
        return chapterMapper.insert(chapter);
    }

    @Override
    public int updateChapter(KnowledgeChapter chapter) {
        return chapterMapper.updateById(chapter);
    }

    @Override
    public int deleteChapterById(Long id) {
        deleteDescendants(id);
        return chapterMapper.deleteById(id);
    }

    @Override
    public int deleteChaptersByBookId(Long bookId) {
        return chapterMapper.delete(new LambdaQueryWrapper<KnowledgeChapter>()
                .eq(KnowledgeChapter::getBookId, bookId));
    }

    private void deleteDescendants(Long parentId) {
        List<KnowledgeChapter> children = chapterMapper.selectList(
                new LambdaQueryWrapper<KnowledgeChapter>().eq(KnowledgeChapter::getParentId, parentId));
        for (KnowledgeChapter child : children) {
            deleteDescendants(child.getId());
            chapterMapper.deleteById(child.getId());
        }
    }

    /** Build nested tree from flat list */
    private List<KnowledgeChapter> buildTree(List<KnowledgeChapter> all) {
        Map<Long, List<KnowledgeChapter>> byParent = all.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() == null ? 0L : c.getParentId()));
        for (KnowledgeChapter chapter : all) {
            List<KnowledgeChapter> children = byParent.getOrDefault(chapter.getId(), new ArrayList<>());
            chapter.setChildren(children.isEmpty() ? new ArrayList<>() : children);
        }
        return byParent.getOrDefault(0L, new ArrayList<>());
    }
}
