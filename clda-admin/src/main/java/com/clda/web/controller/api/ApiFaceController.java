package com.clda.web.controller.api;

import com.clda.intellect.service.IFaceService;
import com.clda.feign.core.R;
import com.clda.intellect.domain.Face;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 人脸识别接口控制层
 * @author clda-xiaozhi
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
