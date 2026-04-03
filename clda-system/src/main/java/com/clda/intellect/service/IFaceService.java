package com.clda.intellect.service;

import java.util.List;
import com.clda.intellect.domain.Face;

/**
 * 人脸识别Service接口
 *
 * @author clda-xiaozhi
 */
public interface IFaceService {
    /**
     * 查询人脸
     *
     * @param id 人脸主键
     * @return 人脸
     */
    Face selectFaceById(Long id);

    /**
     * 查询人脸列表
     *
     * @param face 人脸
     * @return 人脸集合
     */
    List<Face> selectFaceList(Face face);

    /**
     * 新增人脸
     *
     * @param face 人脸
     * @return 结果
     */
    int insertFace(Face face);

    /**
     * 修改人脸
     *
     * @param face 人脸
     * @return 结果
     */
    int updateFace(Face face);

    /**
     * 批量删除人脸
     *
     * @param ids 需要删除的人脸主键集合
     * @return 结果
     */
    int deleteFaceByIds(Long[] ids);

    /**
     * 获取人脸列表（所有）
     * @return 人脸列表（所有）
     */
    List<Face> listAll();
}
