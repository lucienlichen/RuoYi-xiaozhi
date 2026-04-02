package com.clda.intellect.service;

import com.clda.intellect.domain.*;
import java.util.List;

public interface IHazardSourceService {

    /** 获取分类树（含子类） */
    List<HazardSourceCategory> selectCategoryTree();

    /** 获取指定分类下的危险源条目 */
    List<HazardSourceItem> selectItemsByCategoryId(Long categoryId);

    /** 获取单条危险源详情 */
    HazardSourceItem selectItemById(Long id);

    /** 批量获取原因（按逗号分隔的codes） */
    List<HazardSourceCauseDict> selectCausesByCodes(List<String> codes);

    /** 批量获取事件（按逗号分隔的codes） */
    List<HazardSourceEventDict> selectEventsByCodes(List<String> codes);

    /** 管理端CRUD */
    int insertItem(HazardSourceItem item);
    int updateItem(HazardSourceItem item);
    int deleteItemByIds(Long[] ids);

    int insertCategory(HazardSourceCategory category);
    int updateCategory(HazardSourceCategory category);
    int deleteCategoryById(Long id);
}
