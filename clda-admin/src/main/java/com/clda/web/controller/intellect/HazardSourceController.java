package com.clda.web.controller.intellect;

import com.clda.common.annotation.Log;
import com.clda.common.core.controller.BaseController;
import com.clda.common.core.domain.AjaxResult;
import com.clda.common.core.page.TableDataInfo;
import com.clda.common.enums.BusinessType;
import com.clda.intellect.domain.HazardSourceCategory;
import com.clda.intellect.domain.HazardSourceItem;
import com.clda.intellect.service.IHazardSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/intellect/hazard-source")
public class HazardSourceController extends BaseController {

    private final IHazardSourceService hazardSourceService;

    /** 获取分类树（业务端 + 管理端共用） */
    @GetMapping("/tree")
    public AjaxResult tree() {
        return success(hazardSourceService.selectCategoryTree());
    }

    /** 获取指定分类下的危险源条目列表 */
    @GetMapping("/items")
    public AjaxResult items(@RequestParam Long categoryId) {
        return success(hazardSourceService.selectItemsByCategoryId(categoryId));
    }

    /** 获取单条危险源详情 */
    @GetMapping("/item/{id}")
    public AjaxResult item(@PathVariable Long id) {
        return success(hazardSourceService.selectItemById(id));
    }

    /** 批量获取原因文字 */
    @GetMapping("/causes")
    public AjaxResult causes(@RequestParam String codes) {
        List<String> codeList = Arrays.asList(codes.split(","));
        return success(hazardSourceService.selectCausesByCodes(codeList));
    }

    /** 批量获取事件信息 */
    @GetMapping("/events")
    public AjaxResult events(@RequestParam String codes) {
        List<String> codeList = Arrays.asList(codes.split(","));
        return success(hazardSourceService.selectEventsByCodes(codeList));
    }

    // ===== 管理端 CRUD =====

    @PreAuthorize("@ss.hasPermi('crane:hazard-source:list')")
    @GetMapping("/items/list")
    public TableDataInfo itemList(HazardSourceItem query) {
        startPage();
        List<HazardSourceItem> list = hazardSourceService.selectItemsByCategoryId(query.getCategoryId());
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('crane:hazard-source:add')")
    @Log(title = "危险源辨识", businessType = BusinessType.INSERT)
    @PostMapping("/item")
    public AjaxResult addItem(@RequestBody HazardSourceItem item) {
        return toAjax(hazardSourceService.insertItem(item));
    }

    @PreAuthorize("@ss.hasPermi('crane:hazard-source:edit')")
    @Log(title = "危险源辨识", businessType = BusinessType.UPDATE)
    @PutMapping("/item")
    public AjaxResult editItem(@RequestBody HazardSourceItem item) {
        return toAjax(hazardSourceService.updateItem(item));
    }

    @PreAuthorize("@ss.hasPermi('crane:hazard-source:remove')")
    @Log(title = "危险源辨识", businessType = BusinessType.DELETE)
    @DeleteMapping("/item/{ids}")
    public AjaxResult removeItem(@PathVariable Long[] ids) {
        return toAjax(hazardSourceService.deleteItemByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('crane:hazard-source:add')")
    @Log(title = "危险源辨识分类", businessType = BusinessType.INSERT)
    @PostMapping("/category")
    public AjaxResult addCategory(@RequestBody HazardSourceCategory category) {
        return toAjax(hazardSourceService.insertCategory(category));
    }

    @PreAuthorize("@ss.hasPermi('crane:hazard-source:edit')")
    @Log(title = "危险源辨识分类", businessType = BusinessType.UPDATE)
    @PutMapping("/category")
    public AjaxResult editCategory(@RequestBody HazardSourceCategory category) {
        return toAjax(hazardSourceService.updateCategory(category));
    }

    @PreAuthorize("@ss.hasPermi('crane:hazard-source:remove')")
    @Log(title = "危险源辨识分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/category/{id}")
    public AjaxResult removeCategory(@PathVariable Long id) {
        return toAjax(hazardSourceService.deleteCategoryById(id));
    }
}
