package com.clda.web.controller.intellect;

import com.clda.common.annotation.Log;
import com.clda.common.config.CldaConfig;
import com.clda.common.core.controller.BaseController;
import com.clda.common.core.domain.AjaxResult;
import com.clda.common.enums.BusinessType;
import com.clda.common.utils.SecurityUtils;
import com.clda.common.utils.file.FileUploadUtils;
import com.clda.intellect.domain.KnowledgeChapter;
import com.clda.intellect.service.IKnowledgeChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/intellect/knowledge/chapters")
public class KnowledgeChapterController extends BaseController {

    private final IKnowledgeChapterService chapterService;

    /** 查询书籍章节树（不含正文，供管理端和业务端列表使用） */
    @GetMapping("/tree")
    public AjaxResult chapterTree(@RequestParam Long bookId) {
        return success(chapterService.selectChapterTree(bookId));
    }

    /** 查询章节详情（含正文 content_html） */
    @GetMapping("/{id}")
    public AjaxResult getChapter(@PathVariable Long id) {
        return success(chapterService.selectChapterWithContent(id));
    }

    @PreAuthorize("@ss.hasPermi('crane:knowledge:add')")
    @Log(title = "章节管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult addChapter(@RequestBody KnowledgeChapter chapter) {
        return toAjax(chapterService.insertChapter(chapter));
    }

    /** 上传章节文件 */
    @PreAuthorize("@ss.hasPermi('crane:knowledge:add')")
    @Log(title = "章节管理", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult uploadChapter(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bookId") Long bookId,
            @RequestParam("title") String title,
            @RequestParam(value = "orderNum", required = false, defaultValue = "0") Integer orderNum) throws Exception {
        String uploadPath = CldaConfig.getUploadPath() + "/knowledge";
        String filePath = FileUploadUtils.upload(uploadPath, file);

        KnowledgeChapter chapter = new KnowledgeChapter();
        chapter.setBookId(bookId);
        chapter.setParentId(0L);
        chapter.setTitle(title);
        chapter.setLevel(1);
        chapter.setOrderNum(orderNum);
        chapter.setFileName(file.getOriginalFilename());
        chapter.setFilePath(filePath);
        chapter.setCreateBy(SecurityUtils.getUsername());
        chapterService.insertChapter(chapter);
        return success(chapter);
    }

    @PreAuthorize("@ss.hasPermi('crane:knowledge:edit')")
    @Log(title = "章节管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult editChapter(@RequestBody KnowledgeChapter chapter) {
        return toAjax(chapterService.updateChapter(chapter));
    }

    @PreAuthorize("@ss.hasPermi('crane:knowledge:remove')")
    @Log(title = "章节管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult removeChapter(@PathVariable Long id) {
        return toAjax(chapterService.deleteChapterById(id));
    }
}
