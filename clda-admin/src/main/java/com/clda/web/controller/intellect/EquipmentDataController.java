package com.clda.web.controller.intellect;

import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.clda.common.annotation.Log;
import com.clda.common.core.controller.BaseController;
import com.clda.common.core.domain.AjaxResult;
import com.clda.common.core.page.TableDataInfo;
import com.clda.common.enums.BusinessType;
import com.clda.common.utils.SecurityUtils;
import com.clda.intellect.domain.DataCategory;
import com.clda.intellect.domain.DataFile;
import com.clda.intellect.domain.EquipmentData;
import com.clda.intellect.mapper.DataCategoryMapper;
import com.clda.intellect.service.IDataProcessingService;
import com.clda.intellect.service.IEquipmentDataService;

/**
 * 设备数据采集Controller
 *
 * @author clda-xiaozhi
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/intellect/equipdata")
public class EquipmentDataController extends BaseController {

    private final IEquipmentDataService equipmentDataService;
    private final IDataProcessingService dataProcessingService;
    private final DataCategoryMapper dataCategoryMapper;

    /** 查询数据分类(含子分类) */
    @GetMapping("/categories")
    public AjaxResult categories() {
        List<DataCategory> list = dataCategoryMapper.selectAll();
        return success(list);
    }

    /** 查询子分类 */
    @GetMapping("/categories/{parentId}")
    public AjaxResult subCategories(@PathVariable Long parentId) {
        List<DataCategory> list = dataCategoryMapper.selectByParentId(parentId);
        return success(list);
    }

    /** 查询数据记录列表 */
    @PreAuthorize("@ss.hasPermi('crane:equipdata:list')")
    @GetMapping("/list")
    public TableDataInfo list(
            @RequestParam Long equipmentId,
            @RequestParam Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dataDate) {
        startPage();
        List<EquipmentData> list = equipmentDataService.selectList(equipmentId, categoryId, dataDate);
        return getDataTable(list);
    }

    /** 获取数据记录关联文件 */
    @PreAuthorize("@ss.hasPermi('crane:equipdata:query')")
    @GetMapping("/files/{dataId}")
    public AjaxResult files(@PathVariable Long dataId) {
        return success(equipmentDataService.selectFilesByDataId(dataId));
    }

    /** 上传文件 */
    @PreAuthorize("@ss.hasPermi('crane:equipdata:upload')")
    @Log(title = "数据采集", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(
            @RequestParam Long equipmentId,
            @RequestParam Long categoryId,
            @RequestParam(required = false) Long subCategoryId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dataDate,
            @RequestParam("files") MultipartFile[] files) {
        String operName = SecurityUtils.getUsername();
        EquipmentData data = equipmentDataService.uploadFiles(equipmentId, categoryId, subCategoryId, dataDate, files, operName);
        return success(data);
    }

    /** 查询有数据的日期(日历标记) */
    @GetMapping("/dataDates")
    public AjaxResult dataDates(
            @RequestParam Long equipmentId,
            @RequestParam Long categoryId,
            @RequestParam(required = false) String yearMonth) {
        List<String> dates = equipmentDataService.selectDataDates(equipmentId, categoryId, yearMonth);
        return success(dates);
    }

    /** 删除数据记录 */
    @PreAuthorize("@ss.hasPermi('crane:equipdata:remove')")
    @Log(title = "数据采集", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(equipmentDataService.deleteByIds(ids));
    }

    /** 删除单个文件 */
    @PreAuthorize("@ss.hasPermi('crane:equipdata:remove')")
    @Log(title = "数据采集-删除文件", businessType = BusinessType.DELETE)
    @DeleteMapping("/file/{fileId}")
    public AjaxResult removeFile(@PathVariable Long fileId) {
        return toAjax(equipmentDataService.deleteFile(fileId));
    }

    /** 批量查询文件处理状态(前端轮询用) */
    @GetMapping("/files/status")
    public AjaxResult filesStatus(@RequestParam Long dataId) {
        List<DataFile> files = equipmentDataService.selectFilesByDataId(dataId);
        return success(files);
    }

    /** 获取单个文件的结构化数据 */
    @GetMapping("/file/structured/{fileId}")
    public AjaxResult structuredData(@PathVariable Long fileId) {
        DataFile file = equipmentDataService.selectFileById(fileId);
        if (file == null) {
            return error("文件不存在");
        }
        return success(file);
    }

    /** 手动重新处理文件 */
    @PreAuthorize("@ss.hasPermi('crane:equipdata:upload')")
    @PostMapping("/file/reprocess/{fileId}")
    public AjaxResult reprocessFile(@PathVariable Long fileId) {
        dataProcessingService.reprocessFile(fileId);
        return success("已触发重新处理");
    }
}
