package com.clda.web.controller.intellect;

import com.clda.common.annotation.Log;
import com.clda.common.core.controller.BaseController;
import com.clda.common.core.domain.AjaxResult;
import com.clda.common.core.page.TableDataInfo;
import com.clda.common.enums.BusinessType;
import com.clda.common.utils.SecurityUtils;
import com.clda.intellect.domain.InspectionItem;
import com.clda.intellect.domain.InspectionRecord;
import com.clda.intellect.service.IInspectionService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/intellect/inspection")
public class InspectionController extends BaseController {

    private final IInspectionService inspectionService;

    // ===== 业务端 API =====

    /** 获取全部检查项（业务端预览用） */
    @GetMapping("/items")
    public AjaxResult items() {
        return success(inspectionService.selectAllItems());
    }

    /** 生成并下载Excel排查表 */
    @GetMapping("/generate")
    public void generate(HttpServletResponse response,
                         @RequestParam Long equipmentId,
                         @RequestParam(required = false) String equipmentName) throws IOException {
        String inspector = SecurityUtils.getUsername();
        if (equipmentName == null || equipmentName.isEmpty()) {
            equipmentName = "设备" + equipmentId;
        }
        byte[] data = inspectionService.generateExcel(equipmentId, equipmentName, inspector);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = URLEncoder.encode("隐患排查表_" + equipmentName + ".xlsx", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    /** 上传Excel排查表并解析 */
    @Log(title = "隐患排查", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam Long equipmentId,
                             @RequestParam(required = false) String equipmentName) {
        String inspector = SecurityUtils.getUsername();
        if (equipmentName == null || equipmentName.isEmpty()) {
            equipmentName = "设备" + equipmentId;
        }
        InspectionRecord record = inspectionService.uploadAndParse(file, equipmentId, equipmentName, inspector);
        return success(record);
    }

    /** 设备历史排查记录 */
    @GetMapping("/records")
    public AjaxResult records(@RequestParam Long equipmentId) {
        return success(inspectionService.selectRecordsByEquipmentId(equipmentId));
    }

    /** 单条记录详情（含结果明细） */
    @GetMapping("/record/{id}")
    public AjaxResult recordDetail(@PathVariable Long id) {
        return success(inspectionService.selectRecordDetail(id));
    }

    // ===== 管理端 CRUD =====

    @PreAuthorize("@ss.hasPermi('crane:inspection:list')")
    @GetMapping("/items/list")
    public TableDataInfo itemList(InspectionItem query) {
        startPage();
        List<InspectionItem> list = inspectionService.selectItemList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('crane:inspection:add')")
    @Log(title = "隐患排查检查项", businessType = BusinessType.INSERT)
    @PostMapping("/item")
    public AjaxResult addItem(@RequestBody InspectionItem item) {
        return toAjax(inspectionService.insertItem(item));
    }

    @PreAuthorize("@ss.hasPermi('crane:inspection:edit')")
    @Log(title = "隐患排查检查项", businessType = BusinessType.UPDATE)
    @PutMapping("/item")
    public AjaxResult editItem(@RequestBody InspectionItem item) {
        return toAjax(inspectionService.updateItem(item));
    }

    @PreAuthorize("@ss.hasPermi('crane:inspection:remove')")
    @Log(title = "隐患排查检查项", businessType = BusinessType.DELETE)
    @DeleteMapping("/item/{ids}")
    public AjaxResult removeItem(@PathVariable Long[] ids) {
        return toAjax(inspectionService.deleteItemByIds(ids));
    }
}
