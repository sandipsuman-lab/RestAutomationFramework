package com.restautomation.commons.utilities.readerhelpers;

import com.restautomation.commons.utilities.common.DateTimeUtils;
import com.restautomation.commons.utilities.common.FileUtils;
import com.restautomation.commons.utilities.common.JsonUtils;
import com.restautomation.exceptions.FixtureNotFoundException;
import com.restautomation.exceptions.FrameworkException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads .xlsx/.xls sheets (first row = header) into ordered maps with typed values:
 * numbers as {@link BigDecimal}, dates as ISO UTC strings, formulas evaluated.
 * Also writes sheets, e.g. to export mismatch reports or build fixtures.
 */
public final class ExcelFileReader {

    private ExcelFileReader() {
    }

    public static List<String> sheetNames(String path) {
        try (InputStream in = FileUtils.openStream(path); Workbook workbook = WorkbookFactory.create(in)) {
            List<String> names = new ArrayList<>();
            workbook.forEach(sheet -> names.add(sheet.getSheetName()));
            return names;
        } catch (IOException e) {
            throw new FrameworkException("Unable to read Excel file " + path, e);
        }
    }

    /** Reads the named sheet, or the first sheet when {@code sheetName} is null/blank. */
    public static List<Map<String, Object>> readSheet(String path, String sheetName) {
        try (InputStream in = FileUtils.openStream(path); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = sheetName == null || sheetName.isBlank() ? workbook.getSheetAt(0) : workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new FixtureNotFoundException("Sheet '" + sheetName + "' not found in " + path);
            }
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return List.of();
            }
            List<String> headers = new ArrayList<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                Object header = cellValue(headerRow.getCell(c), evaluator);
                headers.add(header == null ? "column" + c : header.toString().trim());
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                Map<String, Object> values = new LinkedHashMap<>();
                boolean blank = true;
                for (int c = 0; c < headers.size(); c++) {
                    Object value = cellValue(row.getCell(c), evaluator);
                    blank &= value == null;
                    values.put(headers.get(c), value);
                }
                if (!blank) {
                    rows.add(values);
                }
            }
            return rows;
        } catch (IOException e) {
            throw new FrameworkException("Unable to read Excel file " + path, e);
        }
    }

    public static <T> List<T> readAs(String path, String sheetName, Class<T> type) {
        return JsonUtils.convertList(readSheet(path, sheetName), type);
    }

    /** Data-driven lookup, e.g. the row whose {@code TestCaseId} column equals {@code TC_01}. */
    public static Optional<Map<String, Object>> getRowByKey(String path, String sheetName, String keyColumn, String keyValue) {
        return readSheet(path, sheetName).stream()
                .filter(row -> Objects.equals(stringValue(row.get(keyColumn)), keyValue))
                .findFirst();
    }

    /** Writes rows to a new workbook (one sheet); column order follows the first row's keys. */
    public static Path writeSheet(String path, String sheetName, List<Map<String, Object>> rows) {
        Path file = Paths.get(path);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            if (!rows.isEmpty()) {
                List<String> headers = new ArrayList<>(rows.get(0).keySet());
                Row header = sheet.createRow(0);
                for (int c = 0; c < headers.size(); c++) {
                    header.createCell(c).setCellValue(headers.get(c));
                }
                for (int r = 0; r < rows.size(); r++) {
                    Row row = sheet.createRow(r + 1);
                    for (int c = 0; c < headers.size(); c++) {
                        Object value = rows.get(r).get(headers.get(c));
                        Cell cell = row.createCell(c);
                        if (value instanceof Number number) {
                            cell.setCellValue(number.doubleValue());
                        } else if (value instanceof Boolean bool) {
                            cell.setCellValue(bool);
                        } else if (value != null) {
                            cell.setCellValue(value.toString());
                        }
                    }
                }
            }
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            try (OutputStream out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
            return file;
        } catch (IOException e) {
            throw new FrameworkException("Unable to write Excel file " + path, e);
        }
    }

    private static Object cellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType() == CellType.FORMULA ? evaluator.evaluateFormulaCell(cell) : cell.getCellType();
        return switch (type) {
            case STRING -> {
                String text = cell.getStringCellValue();
                yield text == null || text.isBlank() ? null : text.trim();
            }
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? DateTimeUtils.toIsoUtc(cell.getLocalDateTimeCellValue().toInstant(ZoneOffset.UTC))
                    : normaliseNumber(BigDecimal.valueOf(cell.getNumericCellValue()));
            case BOOLEAN -> cell.getBooleanCellValue();
            default -> null;
        };
    }

    private static BigDecimal normaliseNumber(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }

    private static String stringValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        return value == null ? null : value.toString();
    }
}
