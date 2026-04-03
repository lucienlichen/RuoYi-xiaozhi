package com.clda.intellect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.intellect.domain.*;
import com.clda.intellect.mapper.*;
import com.clda.intellect.service.IHazardSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HazardSourceServiceImpl implements IHazardSourceService {

    private final HazardSourceCategoryMapper categoryMapper;
    private final HazardSourceItemMapper itemMapper;
    private final HazardSourceCauseDictMapper causeMapper;
    private final HazardSourceEventDictMapper eventMapper;

    @Override
    public List<HazardSourceCategory> selectCategoryTree() {
        List<HazardSourceCategory> all = categoryMapper.selectList(
                new LambdaQueryWrapper<HazardSourceCategory>().orderByAsc(HazardSourceCategory::getOrderNum));
        return buildTree(all);
    }

    @Override
    public List<HazardSourceItem> selectItemsByCategoryId(Long categoryId) {
        return itemMapper.selectList(
                new LambdaQueryWrapper<HazardSourceItem>()
                        .eq(HazardSourceItem::getCategoryId, categoryId)
                        .orderByAsc(HazardSourceItem::getOrderNum));
    }

    @Override
    public HazardSourceItem selectItemById(Long id) {
        return itemMapper.selectById(id);
    }

    @Override
    public List<HazardSourceCauseDict> selectCausesByCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) return Collections.emptyList();
        return causeMapper.selectList(
                new LambdaQueryWrapper<HazardSourceCauseDict>().in(HazardSourceCauseDict::getCode, codes));
    }

    @Override
    public List<HazardSourceEventDict> selectEventsByCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) return Collections.emptyList();
        return eventMapper.selectList(
                new LambdaQueryWrapper<HazardSourceEventDict>().in(HazardSourceEventDict::getCode, codes));
    }

    @Override
    public int insertItem(HazardSourceItem item) {
        return itemMapper.insert(item);
    }

    @Override
    public int updateItem(HazardSourceItem item) {
        return itemMapper.updateById(item);
    }

    @Override
    public int deleteItemByIds(Long[] ids) {
        return itemMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public int insertCategory(HazardSourceCategory category) {
        return categoryMapper.insert(category);
    }

    @Override
    public int updateCategory(HazardSourceCategory category) {
        return categoryMapper.updateById(category);
    }

    @Override
    public int deleteCategoryById(Long id) {
        return categoryMapper.deleteById(id);
    }

    private List<HazardSourceCategory> buildTree(List<HazardSourceCategory> all) {
        Map<Long, List<HazardSourceCategory>> byParent = all.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() == null ? 0L : c.getParentId()));
        for (HazardSourceCategory cat : all) {
            cat.setChildren(byParent.getOrDefault(cat.getId(), new ArrayList<>()));
        }
        return byParent.getOrDefault(0L, new ArrayList<>());
    }
}
