package com.clda.intellect.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.intellect.domain.Face;
import com.clda.intellect.mapper.FaceMapper;
import com.clda.intellect.service.IFaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 人脸识别Service业务层处理
 *
 * @author ruoyi-xiaozhi
 */
@Service
@RequiredArgsConstructor
public class FaceServiceImpl implements IFaceService {

    private final FaceMapper faceMapper;

    /**
     * 查询人脸
     *
     * @param id 人脸主键
     * @return 人脸
     */
    @Override
    public Face selectFaceById(Long id) {
        return faceMapper.selectById(id);
    }

    /**
     * 查询人脸列表
     *
     * @param face 人脸
     * @return 人脸
     */
    @Override
    public List<Face> selectFaceList(Face face) {
        LambdaQueryWrapper<Face> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(face.getId() != null, Face::getId, face.getId());
        queryWrapper.like(StrUtil.isNotBlank(face.getName()), Face::getName, face.getName());
        return faceMapper.selectList(queryWrapper);
    }

    /**
     * 新增人脸
     *
     * @param face 人脸
     * @return 结果
     */
    @Override
    public int insertFace(Face face) {
        return faceMapper.insert(face);
    }

    /**
     * 修改人脸
     *
     * @param face 人脸
     * @return 结果
     */
    @Override
    public int updateFace(Face face) {
        return faceMapper.updateById(face);
    }

    /**
     * 批量删除人脸
     *
     * @param ids 需要删除的人脸主键
     * @return 结果
     */
    @Override
    public int deleteFaceByIds(Long[] ids) {
        return faceMapper.deleteByIds(Arrays.asList(ids));
    }

    /**
     * 获取人脸列表（所有）
     *
     * @return 人脸列表（所有）
     */
    @Override
    public List<Face> listAll() {
        LambdaQueryWrapper<Face> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Face::getId, Face::getName, Face::getDescriptor);
        return faceMapper.selectList(queryWrapper);
    }

}
