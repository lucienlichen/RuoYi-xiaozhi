package com.rouyi.xiaozhi.web.controller.intellect;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.rouyi.xiaozhi.common.annotation.Log;
import com.rouyi.xiaozhi.common.core.controller.BaseController;
import com.rouyi.xiaozhi.common.core.domain.AjaxResult;
import com.rouyi.xiaozhi.common.enums.BusinessType;
import com.rouyi.xiaozhi.intellect.domain.Face;
import com.rouyi.xiaozhi.intellect.service.IFaceService;
import com.rouyi.xiaozhi.common.core.page.TableDataInfo;

/**
 * 人脸识别Controller
 *
 * @author ruoyi-xiaozhi
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/intellect/face")
public class FaceController extends BaseController {

    private final IFaceService faceService;

    /**
     * 查询人脸列表
     */
    @PreAuthorize("@ss.hasPermi('intellect:face:list')")
    @GetMapping("/list")
    public TableDataInfo list(Face face) {
        startPage();
        List<Face> list = faceService.selectFaceList(face);
        return getDataTable(list);
    }

    /**
     * 获取人脸详细信息
     */
    @PreAuthorize("@ss.hasPermi('intellect:face:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(faceService.selectFaceById(id));
    }

    /**
     * 新增人脸
     */
    @PreAuthorize("@ss.hasPermi('intellect:face:add')")
    @Log(title = "人脸识别", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Face face) {
        return toAjax(faceService.insertFace(face));
    }

    /**
     * 修改人脸
     */
    @PreAuthorize("@ss.hasPermi('intellect:face:edit')")
    @Log(title = "人脸识别", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Face face) {
        return toAjax(faceService.updateFace(face));
    }

    /**
     * 删除人脸
     */
    @PreAuthorize("@ss.hasPermi('intellect:face:remove')")
    @Log(title = "人脸识别", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(faceService.deleteFaceByIds(ids));
    }

    /**
     * 获取人脸下拉列表
     * @return  人脸下拉列表
     */
    @GetMapping("/listAll")
    public AjaxResult listAll() {
        return success(faceService.listAll());
    }
}
