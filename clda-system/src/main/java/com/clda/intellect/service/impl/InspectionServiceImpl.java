package com.clda.intellect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clda.common.config.CldaConfig;
import com.clda.common.utils.file.FileUploadUtils;
import com.clda.intellect.domain.InspectionItem;
import com.clda.intellect.domain.InspectionRecord;
import com.clda.intellect.domain.InspectionResult;
import com.clda.intellect.mapper.InspectionItemMapper;
import com.clda.intellect.mapper.InspectionRecordMapper;
import com.clda.intellect.mapper.InspectionResultMapper;
import com.clda.intellect.service.IInspectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionServiceImpl implements IInspectionService {

    private final InspectionItemMapper itemMapper;
    private final InspectionRecordMapper recordMapper;
    private final InspectionResultMapper resultMapper;

    /** 序号列 */
    private static final int COL_NO = 0;
    /** 隐患类别列 */
    private static final int COL_CATEGORY = 1;
    /** 排查项目列 */
    private static final int COL_SUB = 2;
    /** 排查内容列 */
    private static final int COL_CONTENT = 3;
    /** 结果列 */
    private static final int COL_RESULT = 4;

    @Override
    public List<InspectionItem> selectAllItems() {
        return itemMapper.selectList(
                new LambdaQueryWrapper<InspectionItem>().orderByAsc(InspectionItem::getOrderNum));
    }

    @Override
    public List<InspectionItem> selectItemList(InspectionItem query) {
        LambdaQueryWrapper<InspectionItem> wrapper = new LambdaQueryWrapper<>();
        if (query.getCategory() != null && !query.getCategory().isEmpty()) {
            wrapper.eq(InspectionItem::getCategory, query.getCategory());
        }
        if (query.getSubCategory() != null && !query.getSubCategory().isEmpty()) {
            wrapper.eq(InspectionItem::getSubCategory, query.getSubCategory());
        }
        wrapper.orderByAsc(InspectionItem::getOrderNum);
        return itemMapper.selectList(wrapper);
    }

    @Override
    public byte[] generateExcel(Long equipmentId, String equipmentName, String inspector) {
        List<InspectionItem> items = selectAllItems();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("隐患排查表");
            sheet.setDefaultColumnWidth(18);
            sheet.setColumnWidth(COL_NO, 2000);
            sheet.setColumnWidth(COL_CATEGORY, 4000);
            sheet.setColumnWidth(COL_SUB, 5000);
            sheet.setColumnWidth(COL_CONTENT, 16000);
            sheet.setColumnWidth(COL_RESULT, 3000);

            // 样式
            CellStyle titleStyle = createTitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle metaStyle = createMetaStyle(wb);
            CellStyle catStyle = createCategoryStyle(wb);
            CellStyle cellStyle = createCellStyle(wb);
            CellStyle resultStyle = createCellStyle(wb);
            resultStyle.setAlignment(HorizontalAlignment.CENTER);

            int rowIdx = 0;

            // 标题行
            Row titleRow = sheet.createRow(rowIdx++);
            titleRow.setHeightInPoints(30);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("起重装备隐患排查表");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, COL_RESULT));

            // 信息行
            Row metaRow = sheet.createRow(rowIdx++);
            metaRow.setHeightInPoints(22);
            Cell metaCell = metaRow.createCell(0);
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            metaCell.setCellValue("设备：" + equipmentName + "    排查人：" + inspector + "    日期：" + dateStr);
            metaCell.setCellStyle(metaStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, COL_RESULT));

            // 按大类分组
            Map<String, List<InspectionItem>> grouped = items.stream()
                    .collect(Collectors.groupingBy(InspectionItem::getCategory, LinkedHashMap::new, Collectors.toList()));

            for (Map.Entry<String, List<InspectionItem>> entry : grouped.entrySet()) {
                String category = entry.getKey();
                List<InspectionItem> catItems = entry.getValue();

                // 大类标题行
                Row catRow = sheet.createRow(rowIdx++);
                catRow.setHeightInPoints(24);
                Cell catCell = catRow.createCell(0);
                catCell.setCellValue(category);
                catCell.setCellStyle(catStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, COL_RESULT));

                // 表头行
                Row hRow = sheet.createRow(rowIdx++);
                hRow.setHeightInPoints(20);
                String[] headers = {"序号", "隐患类别", "排查项目", "排查内容", "结果(有/无)"};
                for (int c = 0; c < headers.length; c++) {
                    Cell hc = hRow.createCell(c);
                    hc.setCellValue(headers[c]);
                    hc.setCellStyle(headerStyle);
                }

                // 数据行 — 合并相同子类的"排查项目"列
                int dataStartRow = rowIdx;
                String prevSub = null;
                int mergeStart = -1;

                for (int i = 0; i < catItems.size(); i++) {
                    InspectionItem item = catItems.get(i);
                    Row dRow = sheet.createRow(rowIdx);
                    dRow.setHeightInPoints(28);

                    // 序号
                    Cell noCell = dRow.createCell(COL_NO);
                    noCell.setCellValue(item.getItemNo());
                    noCell.setCellStyle(cellStyle);

                    // 隐患类别
                    Cell catColCell = dRow.createCell(COL_CATEGORY);
                    catColCell.setCellValue(item.getCategory());
                    catColCell.setCellStyle(cellStyle);

                    // 排查项目
                    Cell subCell = dRow.createCell(COL_SUB);
                    subCell.setCellValue(item.getSubCategory());
                    subCell.setCellStyle(cellStyle);

                    // 排查内容
                    Cell contentCell = dRow.createCell(COL_CONTENT);
                    contentCell.setCellValue(item.getContent());
                    CellStyle wrapStyle = createCellStyle(wb);
                    wrapStyle.setWrapText(true);
                    contentCell.setCellStyle(wrapStyle);

                    // 结果列（空白待填）
                    Cell resCell = dRow.createCell(COL_RESULT);
                    resCell.setCellStyle(resultStyle);

                    // 合并排查项目列
                    if (prevSub != null && !prevSub.equals(item.getSubCategory()) && mergeStart < rowIdx - 1) {
                        sheet.addMergedRegion(new CellRangeAddress(mergeStart, rowIdx - 1, COL_SUB, COL_SUB));
                    }
                    if (prevSub == null || !prevSub.equals(item.getSubCategory())) {
                        mergeStart = rowIdx;
                    }
                    prevSub = item.getSubCategory();
                    rowIdx++;
                }
                // 最后一组合并
                if (mergeStart < rowIdx - 1) {
                    sheet.addMergedRegion(new CellRangeAddress(mergeStart, rowIdx - 1, COL_SUB, COL_SUB));
                }

                // 为结果列添加下拉验证（有/无）
                DataValidationHelper dvHelper = sheet.getDataValidationHelper();
                DataValidationConstraint dvConstraint = dvHelper.createExplicitListConstraint(new String[]{"有", "无"});
                CellRangeAddressList addressList = new CellRangeAddressList(dataStartRow, rowIdx - 1, COL_RESULT, COL_RESULT);
                DataValidation validation = dvHelper.createValidation(dvConstraint, addressList);
                validation.setShowErrorBox(true);
                sheet.addValidationData(validation);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("生成Excel失败", e);
        }
    }

    @Override
    @Transactional
    public InspectionRecord uploadAndParse(MultipartFile file, Long equipmentId, String equipmentName, String inspector) {
        // 1. 保存文件
        String filePath;
        try {
            filePath = FileUploadUtils.upload(CldaConfig.getUploadPath(), file);
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败", e);
        }

        // 2. 获取全部检查项（按序号映射）
        List<InspectionItem> allItems = selectAllItems();
        Map<Integer, InspectionItem> itemByNo = allItems.stream()
                .collect(Collectors.toMap(InspectionItem::getItemNo, i -> i));

        // 3. 解析 Excel
        List<InspectionResult> results = new ArrayList<>();
        int majorCount = 0;
        int otherCount = 0;

        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // 读序号列
                Cell noCell = row.getCell(COL_NO);
                if (noCell == null) continue;
                Integer itemNo = null;
                try {
                    if (noCell.getCellType() == CellType.NUMERIC) {
                        itemNo = (int) noCell.getNumericCellValue();
                    } else if (noCell.getCellType() == CellType.STRING) {
                        String s = noCell.getStringCellValue().trim();
                        if (s.matches("\\d+")) itemNo = Integer.parseInt(s);
                    }
                } catch (Exception ignored) {}

                if (itemNo == null || !itemByNo.containsKey(itemNo)) continue;

                // 读结果列
                Cell resCell = row.getCell(COL_RESULT);
                String resultVal = "";
                if (resCell != null) {
                    if (resCell.getCellType() == CellType.STRING) {
                        resultVal = resCell.getStringCellValue().trim();
                    }
                }

                InspectionItem item = itemByNo.get(itemNo);
                InspectionResult ir = new InspectionResult();
                ir.setItemId(item.getId());
                ir.setItemNo(itemNo);
                ir.setResult(resultVal);
                results.add(ir);

                if ("有".equals(resultVal)) {
                    if ("重大隐患".equals(item.getCategory())) {
                        majorCount++;
                    } else {
                        otherCount++;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("解析Excel失败", e);
        }

        // 4. 创建记录
        InspectionRecord record = new InspectionRecord();
        record.setEquipmentId(equipmentId);
        record.setEquipmentName(equipmentName);
        record.setInspector(inspector);
        record.setInspectDate(new Date());
        record.setFilePath(filePath);
        record.setMajorCount(majorCount);
        record.setOtherCount(otherCount);
        record.setTotalItems(results.size());
        record.setStatus("0");
        recordMapper.insert(record);

        // 5. 批量插入结果明细
        for (InspectionResult r : results) {
            r.setRecordId(record.getId());
            resultMapper.insert(r);
        }

        return record;
    }

    @Override
    public List<InspectionRecord> selectRecordsByEquipmentId(Long equipmentId) {
        return recordMapper.selectList(
                new LambdaQueryWrapper<InspectionRecord>()
                        .eq(InspectionRecord::getEquipmentId, equipmentId)
                        .orderByDesc(InspectionRecord::getCreateTime));
    }

    @Override
    public InspectionRecord selectRecordDetail(Long recordId) {
        InspectionRecord record = recordMapper.selectById(recordId);
        if (record == null) return null;

        List<InspectionResult> results = resultMapper.selectList(
                new LambdaQueryWrapper<InspectionResult>()
                        .eq(InspectionResult::getRecordId, recordId)
                        .orderByAsc(InspectionResult::getItemNo));

        // 关联检查项内容
        Map<Long, InspectionItem> itemMap = selectAllItems().stream()
                .collect(Collectors.toMap(InspectionItem::getId, i -> i));
        for (InspectionResult r : results) {
            InspectionItem item = itemMap.get(r.getItemId());
            if (item != null) {
                r.setContent(item.getContent());
                r.setSubCategory(item.getSubCategory());
                r.setCategory(item.getCategory());
            }
        }

        record.setResults(results);
        return record;
    }

    @Override
    public int insertItem(InspectionItem item) {
        return itemMapper.insert(item);
    }

    @Override
    public int updateItem(InspectionItem item) {
        return itemMapper.updateById(item);
    }

    @Override
    public int deleteItemByIds(Long[] ids) {
        return itemMapper.deleteByIds(Arrays.asList(ids));
    }

    // ===== Excel 样式工厂 =====

    private CellStyle createTitleStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 16);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createMetaStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorder(style);
        return style;
    }

    private CellStyle createCategoryStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createCellStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(style);
        return style;
    }

    private void setBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
