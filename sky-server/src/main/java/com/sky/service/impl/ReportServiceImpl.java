package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = buildDateRange(begin, end);
        List<Double> turnoverList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map<String, Object> map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);

            Double turnover = orderMapper.sumByMap(map);
            turnoverList.add(turnover == null ? 0.0 : turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(dateList.stream().map(LocalDate::toString).collect(Collectors.joining(",")))
                .turnoverList(turnoverList.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .build();
    }

    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = buildDateRange(begin, end);
        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map<String, Object> map = new HashMap<>();
            map.put("end", endTime);
            Integer totalUser = userMapper.countByMap(map);

            map.put("begin", beginTime);
            Integer newUser = userMapper.countByMap(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }

        return UserReportVO.builder()
                .dateList(join(dateList))
                .totalUserList(join(totalUserList))
                .newUserList(join(newUserList))
                .build();
    }

    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = buildDateRange(begin, end);
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Integer orderCount = getOrderCount(beginTime, endTime, null);
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        Integer totalOrderCount = orderCountList.stream().reduce(0, Integer::sum);
        Integer validOrderCount = validOrderCountList.stream().reduce(0, Integer::sum);
        Double orderCompletionRate = totalOrderCount == 0 ? 0.0 : validOrderCount.doubleValue() / totalOrderCount;

        return OrderReportVO.builder()
                .dateList(join(dateList))
                .orderCountList(join(orderCountList))
                .validOrderCountList(join(validOrderCountList))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
        List<String> nameList = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO.builder()
                .nameList(join(nameList))
                .numberList(join(numberList))
                .build();
    }

    public void exportBusinessData(HttpServletResponse response) {
        LocalDate dateEnd = LocalDate.now().minusDays(1);
        LocalDate dateBegin = dateEnd.minusDays(29);
        BusinessDataVO summary = workspaceService.getBusinessData(
                LocalDateTime.of(dateBegin, LocalTime.MIN),
                LocalDateTime.of(dateEnd, LocalTime.MAX));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("运营数据报表");
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle normalStyle = createNormalStyle(workbook);

            writeTitle(sheet, titleStyle, dateBegin, dateEnd);
            writeSummary(sheet, headerStyle, normalStyle, summary);
            writeDetail(sheet, headerStyle, normalStyle, dateBegin);
            writeResponse(response, workbook, dateBegin, dateEnd);
        } catch (IOException e) {
            throw new RuntimeException("Export business data failed", e);
        }
    }

    private void writeTitle(XSSFSheet sheet, CellStyle titleStyle, LocalDate dateBegin, LocalDate dateEnd) {
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        XSSFRow titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28);
        XSSFCell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("运营数据报表");
        titleCell.setCellStyle(titleStyle);

        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));
        XSSFRow rangeRow = sheet.createRow(1);
        XSSFCell rangeCell = rangeRow.createCell(0);
        rangeCell.setCellValue("统计时间：" + dateBegin + " 至 " + dateEnd);
        rangeCell.setCellStyle(createNormalStyle(sheet.getWorkbook()));
    }

    private void writeSummary(XSSFSheet sheet, CellStyle headerStyle, CellStyle normalStyle, BusinessDataVO summary) {
        XSSFRow headerRow = sheet.createRow(3);
        String[] headers = {"营业额", "有效订单数", "订单完成率", "平均客单价", "新增用户数"};
        for (int i = 0; i < headers.length; i++) {
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        XSSFRow dataRow = sheet.createRow(4);
        setCell(dataRow, 0, summary.getTurnover(), normalStyle);
        setCell(dataRow, 1, summary.getValidOrderCount(), normalStyle);
        setCell(dataRow, 2, summary.getOrderCompletionRate(), normalStyle);
        setCell(dataRow, 3, summary.getUnitPrice(), normalStyle);
        setCell(dataRow, 4, summary.getNewUsers(), normalStyle);
    }

    private void writeDetail(XSSFSheet sheet, CellStyle headerStyle, CellStyle normalStyle, LocalDate dateBegin) {
        XSSFRow headerRow = sheet.createRow(6);
        String[] headers = {"日期", "营业额", "有效订单数", "订单完成率", "平均客单价", "新增用户数"};
        for (int i = 0; i < headers.length; i++) {
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        for (int i = 0; i < 30; i++) {
            LocalDate date = dateBegin.plusDays(i);
            BusinessDataVO businessData = workspaceService.getBusinessData(
                    LocalDateTime.of(date, LocalTime.MIN),
                    LocalDateTime.of(date, LocalTime.MAX));

            XSSFRow row = sheet.createRow(7 + i);
            setCell(row, 0, date.toString(), normalStyle);
            setCell(row, 1, businessData.getTurnover(), normalStyle);
            setCell(row, 2, businessData.getValidOrderCount(), normalStyle);
            setCell(row, 3, businessData.getOrderCompletionRate(), normalStyle);
            setCell(row, 4, businessData.getUnitPrice(), normalStyle);
            setCell(row, 5, businessData.getNewUsers(), normalStyle);
        }

        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i), 14 * 256));
        }
    }

    private void writeResponse(HttpServletResponse response, XSSFWorkbook workbook, LocalDate dateBegin, LocalDate dateEnd) throws IOException {
        String filename = "business-data-" + dateBegin + "-" + dateEnd + ".xlsx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.name()).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);

        ServletOutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        outputStream.flush();
    }

    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 18);
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = createNormalStyle(workbook);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createNormalStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void setCell(XSSFRow row, int index, String value, CellStyle style) {
        XSSFCell cell = row.createCell(index);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void setCell(XSSFRow row, int index, Number value, CellStyle style) {
        XSSFCell cell = row.createCell(index);
        cell.setCellValue(value == null ? 0.0 : value.doubleValue());
        cell.setCellStyle(style);
    }

    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status) {
        Map<String, Object> map = new HashMap<>();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }

    private List<LocalDate> buildDateRange(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate current = begin;
        while (!current.isAfter(end)) {
            dateList.add(current);
            current = current.plusDays(1);
        }
        return dateList;
    }

    private String join(List<?> list) {
        return list.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
