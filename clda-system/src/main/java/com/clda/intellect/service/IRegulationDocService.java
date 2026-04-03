package com.clda.intellect.service;

import com.clda.intellect.domain.RegulationDoc;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface IRegulationDocService {
    List<RegulationDoc> selectDocList(RegulationDoc query);

    RegulationDoc selectDocById(Long id);

    /** Upload file, parse content, save record */
    RegulationDoc uploadAndParse(MultipartFile file, String title, String category,
                                  String docNo, String publishDate, String operName) throws Exception;

    int insertDoc(RegulationDoc doc);

    int updateDoc(RegulationDoc doc);

    int deleteDocByIds(Long[] ids);
}
