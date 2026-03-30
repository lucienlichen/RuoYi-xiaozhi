package com.rouyi.xiaozhi.intellect.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rouyi.xiaozhi.common.core.mapper.CommonMapper;
import com.rouyi.xiaozhi.intellect.domain.Face;

import java.util.List;

/**
 * 人脸识别Mapper接口
 *
 * @author ruoyi-xiaozhi
 */
public interface FaceMapper extends CommonMapper<Face> {

    default List<Face> selectList(Face face) {
        LambdaQueryWrapper<Face> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(face.getName()), Face::getName, face.getName());
        return selectList(queryWrapper);
    }

}
