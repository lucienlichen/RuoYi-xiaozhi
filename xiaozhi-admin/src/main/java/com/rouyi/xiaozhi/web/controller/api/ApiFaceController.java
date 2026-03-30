package com.rouyi.xiaozhi.web.controller.api;

import com.rouyi.xiaozhi.intellect.service.IFaceService;
import com.ruoyi.xiaozhi.feign.core.R;
import com.rouyi.xiaozhi.intellect.domain.Face;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 人脸识别接口控制层
 * @author ruoyi-xiaozhi
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/face")
public class ApiFaceController {

    private final IFaceService faceService;

    /**
     * 获取所有人脸数据（名称+描述符）
     * @return 人脸列表
     */
    @GetMapping("/list")
    public R<List<Face>> list() {
        return R.ok(faceService.listAll());
    }

}
