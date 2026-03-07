package com.fabbitinc.server.application.mapping.support;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class SpreadsheetParserSupport {

    private static final Set<String> NULL_MARKERS = Set.of("~", "-", "N/A", "n/a", "NA", "없음", "해당없음");

    private final DataFormatter dataFormatter = new DataFormatter(Locale.KOREAN);

    public List<String> getSheetNames(byte[] content, String filename) {
        if (!isExcel(content, filename)) {
            return List.of();
        }

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            List<String> names = new ArrayList<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                names.add(workbook.getSheetName(i));
            }
            return names;
        } catch (IOException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST, "엑셀 파일을 파싱할 수 없습니다");
        }
    }

    public ParsedSheet parse(
            byte[] content,
            String filename,
            String requestedSheetName,
            int maxRows
    ) {
        if (isExcel(content, filename)) {
            return parseExcel(content, requestedSheetName, maxRows);
        }
        if (isCsv(filename)) {
            return parseCsv(content, maxRows);
        }
        throw new AppException(ErrorCode.BAD_REQUEST, "지원하지 않는 파일 형식입니다");
    }

    private ParsedSheet parseExcel(byte[] content, String requestedSheetName, int maxRows) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet;
            if (requestedSheetName != null && !requestedSheetName.isBlank()) {
                sheet = workbook.getSheet(requestedSheetName);
                if (sheet == null) {
                    throw new AppException(ErrorCode.BAD_REQUEST, "시트를 찾을 수 없습니다: " + requestedSheetName);
                }
            } else {
                sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            }

            if (sheet == null) {
                return new ParsedSheet(List.of(), List.of());
            }

            int headerRowNum = findFirstNonEmptyRow(sheet);
            if (headerRowNum < 0) {
                return new ParsedSheet(List.of(), List.of());
            }

            Row headerRow = sheet.getRow(headerRowNum);
            List<String> headers = extractHeaders(headerRow);
            if (headers.isEmpty()) {
                return new ParsedSheet(List.of(), List.of());
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            int count = 0;
            for (int rowNum = headerRowNum + 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                if (maxRows >= 0 && count >= maxRows) {
                    break;
                }

                Row row = sheet.getRow(rowNum);
                Map<String, Object> rowMap = new LinkedHashMap<>();
                boolean hasValue = false;

                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row == null ? null : row.getCell(i);
                    String raw = cell == null ? null : dataFormatter.formatCellValue(cell);
                    Object value = cleanValue(raw);
                    rowMap.put(headers.get(i), value);
                    if (value != null) {
                        hasValue = true;
                    }
                }

                if (hasValue) {
                    rows.add(rowMap);
                    count++;
                }
            }

            return new ParsedSheet(headers, rows);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST, "엑셀 파일을 파싱할 수 없습니다");
        }
    }

    private ParsedSheet parseCsv(byte[] content, int maxRows) {
        String text = decode(content);
        char delimiter = detectDelimiter(text);

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setTrim(false)
                .setIgnoreEmptyLines(true)
                .build();

        try (Reader reader = new StringReader(text); CSVParser parser = new CSVParser(reader, format)) {
            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                return new ParsedSheet(List.of(), List.of());
            }

            List<String> headers = extractHeaders(records.getFirst());
            if (headers.isEmpty()) {
                return new ParsedSheet(List.of(), List.of());
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            int count = 0;
            for (int i = 1; i < records.size(); i++) {
                if (maxRows >= 0 && count >= maxRows) {
                    break;
                }

                CSVRecord record = records.get(i);
                Map<String, Object> rowMap = new LinkedHashMap<>();
                boolean hasValue = false;

                for (int col = 0; col < headers.size(); col++) {
                    String raw = col < record.size() ? record.get(col) : null;
                    Object value = cleanValue(raw);
                    rowMap.put(headers.get(col), value);
                    if (value != null) {
                        hasValue = true;
                    }
                }

                if (hasValue) {
                    rows.add(rowMap);
                    count++;
                }
            }

            return new ParsedSheet(headers, rows);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST, "CSV 파일을 파싱할 수 없습니다");
        }
    }

    private List<String> extractHeaders(Row headerRow) {
        if (headerRow == null) {
            return List.of();
        }

        List<String> headers = new ArrayList<>();
        short lastCell = headerRow.getLastCellNum();
        if (lastCell < 0) {
            return List.of();
        }

        for (int i = 0; i < lastCell; i++) {
            Cell cell = headerRow.getCell(i);
            String raw = cell == null ? null : dataFormatter.formatCellValue(cell);
            String header = raw == null ? "" : raw.trim();
            if (!header.isBlank()) {
                headers.add(header);
            }
        }
        return headers;
    }

    private List<String> extractHeaders(CSVRecord headerRecord) {
        List<String> headers = new ArrayList<>();
        for (String value : headerRecord) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isBlank()) {
                headers.add(trimmed);
            }
        }
        return headers;
    }

    private int findFirstNonEmptyRow(Sheet sheet) {
        for (int rowNum = sheet.getFirstRowNum(); rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) {
                continue;
            }
            short lastCell = row.getLastCellNum();
            for (int cellNum = 0; cellNum < lastCell; cellNum++) {
                Cell cell = row.getCell(cellNum);
                String raw = cell == null ? null : dataFormatter.formatCellValue(cell);
                if (raw != null && !raw.trim().isBlank()) {
                    return rowNum;
                }
            }
        }
        return -1;
    }

    private Object cleanValue(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().replace("\u0000", "");
        if (normalized.isBlank() || NULL_MARKERS.contains(normalized)) {
            return null;
        }
        return normalized;
    }

    private boolean isExcel(byte[] content, String filename) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            return true;
        }

        if (content.length >= 4
                && content[0] == 'P'
                && content[1] == 'K'
                && content[2] == 3
                && content[3] == 4) {
            return true;
        }

        return content.length >= 8
                && (content[0] & 0xFF) == 0xD0
                && (content[1] & 0xFF) == 0xCF
                && (content[2] & 0xFF) == 0x11
                && (content[3] & 0xFF) == 0xE0
                && (content[4] & 0xFF) == 0xA1
                && (content[5] & 0xFF) == 0xB1
                && (content[6] & 0xFF) == 0x1A
                && (content[7] & 0xFF) == 0xE1;
    }

    private boolean isCsv(String filename) {
        if (filename == null) {
            return false;
        }
        return filename.toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    private String decode(byte[] content) {
        Charset[] candidates = new Charset[]{
                StandardCharsets.UTF_8,
                Charset.forName("UTF-8"),
                Charset.forName("MS949"),
                Charset.forName("EUC-KR")
        };

        for (Charset charset : candidates) {
            try {
                return new String(content, charset);
            } catch (RuntimeException ignored) {
                // 다음 후보 인코딩을 시도한다.
            }
        }

        return new String(content, StandardCharsets.UTF_8);
    }

    private char detectDelimiter(String text) {
        String[] lines = text.split("\\R", 3);
        String sample = "";
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                sample = line;
                break;
            }
        }

        int comma = count(sample, ',');
        int semicolon = count(sample, ';');
        int tab = count(sample, '\t');

        if (tab >= comma && tab >= semicolon && tab > 0) {
            return '\t';
        }
        if (semicolon >= comma && semicolon > 0) {
            return ';';
        }
        return ',';
    }

    private int count(String text, char target) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }

    public record ParsedSheet(
            List<String> headers,
            List<Map<String, Object>> rows
    ) {
    }
}
