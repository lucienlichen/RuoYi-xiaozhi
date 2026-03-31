package com.clda.web.controller.intellect;

import com.clda.common.annotation.Log;
import com.clda.common.core.controller.BaseController;
import com.clda.common.core.domain.AjaxResult;
import com.clda.common.core.page.TableDataInfo;
import com.clda.common.enums.BusinessType;
import com.clda.intellect.domain.KnowledgeBook;
import com.clda.intellect.service.IKnowledgeBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/intellect/knowledge")
public class KnowledgeBookController extends BaseController {

    private final IKnowledgeBookService bookService;

    /** 查询所有书籍（含章节树，供业务端前端使用） */
    @GetMapping("/books/tree")
    public AjaxResult booksWithTree() {
        return success(bookService.selectBooksWithChapterTree());
    }

    /** 查询书籍列表（管理端分页） */
    @PreAuthorize("@ss.hasPermi('crane:knowledge:list')")
    @GetMapping("/books")
    public TableDataInfo list() {
        startPage();
        List<KnowledgeBook> list = bookService.selectBookList();
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('crane:knowledge:query')")
    @GetMapping("/books/{id}")
    public AjaxResult getBook(@PathVariable Long id) {
        return success(bookService.selectBookById(id));
    }

    @PreAuthorize("@ss.hasPermi('crane:knowledge:add')")
    @Log(title = "知识管理", businessType = BusinessType.INSERT)
    @PostMapping("/books")
    public AjaxResult addBook(@RequestBody KnowledgeBook book) {
        return toAjax(bookService.insertBook(book));
    }

    @PreAuthorize("@ss.hasPermi('crane:knowledge:edit')")
    @Log(title = "知识管理", businessType = BusinessType.UPDATE)
    @PutMapping("/books")
    public AjaxResult editBook(@RequestBody KnowledgeBook book) {
        return toAjax(bookService.updateBook(book));
    }

    @PreAuthorize("@ss.hasPermi('crane:knowledge:remove')")
    @Log(title = "知识管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/books/{id}")
    public AjaxResult removeBook(@PathVariable Long id) {
        return toAjax(bookService.deleteBookById(id));
    }
}
