package com.clda.web.controller.intellect;

import com.clda.common.annotation.Log;
import com.clda.common.core.controller.BaseController;
import com.clda.common.core.domain.AjaxResult;
import com.clda.common.core.page.TableDataInfo;
import com.clda.common.enums.BusinessType;
import com.clda.common.utils.SecurityUtils;
import com.clda.intellect.domain.RegulationDoc;
import com.clda.intellect.service.IRegulationDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/intellect/regulation")
public class RegulationDocController extends BaseController {

    private final IRegulationDocService docService;

    /** 查询法规列表（分页，管理端及业务端共用） */
    @GetMapping("/list")
    public TableDataInfo list(RegulationDoc query) {
        startPage();
        List<RegulationDoc> list = docService.selectDocList(query);
        return getDataTable(list);
    }

    /** 查询法规详情（含 content_html） */
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(docService.selectDocById(id));
    }

    /** 上传并解析法规文件 */
    @PreAuthorize("@ss.hasPermi('crane:regulation:upload')")
    @Log(title = "法规管理", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestParam(value = "docNo", required = false) String docNo,
            @RequestParam(value = "publishDate", required = false) String publishDate) throws Exception {
        String operName = SecurityUtils.getUsername();
        RegulationDoc doc = docService.uploadAndParse(file, title, category, docNo, publishDate, operName);
        return success(doc);
    }

    /** 手动新增法规（无文件，直接录入内容） */
    @PreAuthorize("@ss.hasPermi('crane:regulation:add')")
    @Log(title = "法规管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody RegulationDoc doc) {
        return toAjax(docService.insertDoc(doc));
    }

    @PreAuthorize("@ss.hasPermi('crane:regulation:edit')")
    @Log(title = "法规管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody RegulationDoc doc) {
        return toAjax(docService.updateDoc(doc));
    }

    @PreAuthorize("@ss.hasPermi('crane:regulation:remove')")
    @Log(title = "法规管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(docService.deleteDocByIds(ids));
    }
}
