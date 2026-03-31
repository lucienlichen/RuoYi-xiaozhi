package com.clda.web.controller.intellect;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.clda.common.annotation.Log;
import com.clda.common.core.controller.BaseController;
import com.clda.common.core.domain.AjaxResult;
import com.clda.common.enums.BusinessType;
import com.clda.intellect.domain.Partition;
import com.clda.intellect.service.IPartitionService;

/**
 * 设备分区Controller
 *
 * @author ruoyi-xiaozhi
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/intellect/partition")
public class PartitionController extends BaseController {

    private final IPartitionService partitionService;

    @PreAuthorize("@ss.hasPermi('crane:partition:list')")
    @GetMapping("/list")
    public AjaxResult list(Partition partition) {
        List<Partition> list = partitionService.selectPartitionList(partition);
        return success(list);
    }

    @PreAuthorize("@ss.hasPermi('crane:partition:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(partitionService.selectPartitionById(id));
    }

    @PreAuthorize("@ss.hasPermi('crane:partition:add')")
    @Log(title = "分区管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Partition partition) {
        return toAjax(partitionService.insertPartition(partition));
    }

    @PreAuthorize("@ss.hasPermi('crane:partition:edit')")
    @Log(title = "分区管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Partition partition) {
        return toAjax(partitionService.updatePartition(partition));
    }

    @PreAuthorize("@ss.hasPermi('crane:partition:remove')")
    @Log(title = "分区管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(partitionService.deletePartitionByIds(ids));
    }
}
