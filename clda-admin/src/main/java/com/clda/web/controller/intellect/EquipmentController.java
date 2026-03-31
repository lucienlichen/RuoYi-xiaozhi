package com.clda.web.controller.intellect;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.clda.common.annotation.Log;
import com.clda.common.core.controller.BaseController;
import com.clda.common.core.domain.AjaxResult;
import com.clda.common.core.page.TableDataInfo;
import com.clda.common.enums.BusinessType;
import com.clda.common.utils.SecurityUtils;
import com.clda.common.utils.poi.ExcelUtil;
import com.clda.intellect.domain.Equipment;
import com.clda.intellect.service.IEquipmentService;

/**
 * 起重设备Controller
 *
 * @author ruoyi-xiaozhi
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/intellect/equipment")
public class EquipmentController extends BaseController {

    private final IEquipmentService equipmentService;

    @PreAuthorize("@ss.hasPermi('crane:equipment:list')")
    @GetMapping("/list")
    public TableDataInfo list(Equipment equipment) {
        startPage();
        List<Equipment> list = equipmentService.selectEquipmentList(equipment);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('crane:equipment:export')")
    @Log(title = "设备管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Equipment equipment) {
        List<Equipment> list = equipmentService.selectEquipmentList(equipment);
        ExcelUtil<Equipment> util = new ExcelUtil<>(Equipment.class);
        util.exportExcel(response, list, "起重设备数据");
    }

    @PreAuthorize("@ss.hasPermi('crane:equipment:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(equipmentService.selectEquipmentById(id));
    }

    @PreAuthorize("@ss.hasPermi('crane:equipment:add')")
    @Log(title = "设备管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Equipment equipment) {
        return toAjax(equipmentService.insertEquipment(equipment));
    }

    @PreAuthorize("@ss.hasPermi('crane:equipment:edit')")
    @Log(title = "设备管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Equipment equipment) {
        return toAjax(equipmentService.updateEquipment(equipment));
    }

    @PreAuthorize("@ss.hasPermi('crane:equipment:remove')")
    @Log(title = "设备管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(equipmentService.deleteEquipmentByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('crane:equipment:import')")
    @Log(title = "设备管理", businessType = BusinessType.IMPORT)
    @PostMapping("/import")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<Equipment> util = new ExcelUtil<>(Equipment.class);
        List<Equipment> list = util.importExcel(file.getInputStream());
        String operName = SecurityUtils.getUsername();
        String message = equipmentService.importEquipment(list, updateSupport, operName);
        return success(message);
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil<Equipment> util = new ExcelUtil<>(Equipment.class);
        util.importTemplateExcel(response, "设备数据");
    }
}
