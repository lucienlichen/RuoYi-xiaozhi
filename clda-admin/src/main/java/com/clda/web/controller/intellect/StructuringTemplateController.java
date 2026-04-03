package com.clda.web.controller.intellect;

import java.util.Arrays;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.clda.common.annotation.Log;
import com.clda.common.core.controller.BaseController;
import com.clda.common.core.domain.AjaxResult;
import com.clda.common.core.page.TableDataInfo;
import com.clda.common.enums.BusinessType;
import com.clda.intellect.domain.StructuringTemplate;
import com.clda.intellect.mapper.StructuringTemplateMapper;

/**
 * 结构化模板配置Controller
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/intellect/structuring")
public class StructuringTemplateController extends BaseController {

    private final StructuringTemplateMapper templateMapper;

    /** 查询模板列表 */
    @PreAuthorize("@ss.hasPermi('intellect:structuring:list')")
    @GetMapping("/list")
    public TableDataInfo list(StructuringTemplate query) {
        startPage();
        LambdaQueryWrapper<StructuringTemplate> qw = new LambdaQueryWrapper<>();
        if (query.getCategoryCode() != null) {
            qw.eq(StructuringTemplate::getCategoryCode, query.getCategoryCode());
        }
        if (query.getEnabled() != null) {
            qw.eq(StructuringTemplate::getEnabled, query.getEnabled());
        }
        qw.orderByAsc(StructuringTemplate::getId);
        List<StructuringTemplate> list = templateMapper.selectList(qw);
        return getDataTable(list);
    }

    /** 查询模板详情 */
    @PreAuthorize("@ss.hasPermi('intellect:structuring:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(templateMapper.selectById(id));
    }

    /** 新增模板 */
    @PreAuthorize("@ss.hasPermi('intellect:structuring:add')")
    @Log(title = "结构化模板", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody StructuringTemplate template) {
        return toAjax(templateMapper.insert(template));
    }

    /** 修改模板 */
    @PreAuthorize("@ss.hasPermi('intellect:structuring:edit')")
    @Log(title = "结构化模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody StructuringTemplate template) {
        return toAjax(templateMapper.updateById(template));
    }

    /** 删除模板 */
    @PreAuthorize("@ss.hasPermi('intellect:structuring:remove')")
    @Log(title = "结构化模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(templateMapper.deleteByIds(Arrays.asList(ids)));
    }
}
