package com.docuvra.service;

import com.docuvra.config.ExcelPreviewProperties;
import com.docuvra.dto.CreateExcelCommentRequest;
import com.docuvra.dto.ExcelCellResponse;
import com.docuvra.dto.ExcelColumnResponse;
import com.docuvra.dto.ExcelCommentResponse;
import com.docuvra.dto.ExcelRowResponse;
import com.docuvra.dto.ExcelSheetDataResponse;
import com.docuvra.dto.ExcelSheetSummary;
import com.docuvra.dto.ExcelWorkbookResponse;
import com.docuvra.entity.DocumentVersionEntity;
import com.docuvra.entity.ExcelCommentEntity;
import com.docuvra.exception.InvalidFileException;
import com.docuvra.exception.UnsupportedConversionException;
import com.docuvra.repository.ExcelCommentRepository;
import com.docuvra.util.ExcelCellRefUtil;
import com.docuvra.util.ExcelCellRefUtil.NormalizedCellRange;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExcelPreviewService {

    private static final int DEFAULT_PAGE_SIZE = 200;

    private final DocumentService documentService;
    private final FileStorageService fileStorageService;
    private final ExcelPreviewProperties excelPreviewProperties;
    private final ExcelCommentRepository excelCommentRepository;

    @Transactional(readOnly = true)
    public ExcelWorkbookResponse getWorkbook(UUID documentId, UUID versionId) {
        DocumentVersionEntity version = requireExcelVersion(documentId, versionId);
        Path file = fileStorageService.restoreOriginalFileIfMissing(version);
        if (isCsv(version)) {
            return csvWorkbook(version, file);
        }
        return poiWorkbook(version, file);
    }

    @Transactional(readOnly = true)
    public ExcelSheetDataResponse getSheet(UUID documentId, UUID versionId, int sheetIndex, int startRow, int limit) {
        DocumentVersionEntity version = requireExcelVersion(documentId, versionId);
        Path file = fileStorageService.restoreOriginalFileIfMissing(version);
        int normalizedStart = Math.max(0, startRow);
        int normalizedLimit = Math.max(1, Math.min(limit <= 0 ? DEFAULT_PAGE_SIZE : limit, maxRows()));
        if (isCsv(version)) {
            if (sheetIndex != 0) {
                throw new InvalidFileException("CSV preview has only one sheet.");
            }
            return csvSheet(file, normalizedStart, normalizedLimit);
        }
        return poiSheet(file, sheetIndex, normalizedStart, normalizedLimit);
    }

    @Transactional(readOnly = true)
    public List<ExcelCommentResponse> listSheetComments(UUID documentId, UUID versionId, int sheetIndex) {
        requireExcelVersion(documentId, versionId);
        return excelCommentRepository
                .findByDocumentIdAndVersionIdAndSheetIndexOrderByCreatedAtAsc(documentId, versionId, sheetIndex)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ExcelCommentResponse createComment(UUID documentId, UUID versionId, CreateExcelCommentRequest request) {
        requireExcelVersion(documentId, versionId);
        NormalizedCellRange range = ExcelCellRefUtil.normalizeRange(request.startCell(), request.endCell());
        LocalDateTime now = LocalDateTime.now();
        ExcelCommentEntity comment = ExcelCommentEntity.builder()
                .id(UUID.randomUUID())
                .documentId(documentId)
                .versionId(versionId)
                .sheetIndex(request.sheetIndex())
                .sheetName(request.sheetName().trim())
                .startCell(range.startCell())
                .endCell(range.endCell().equals(range.startCell()) ? null : range.endCell())
                .startRow(range.startRow())
                .startColumn(range.startColumn())
                .endRow(range.endRow() == range.startRow() ? null : range.endRow())
                .endColumn(range.endColumn() == range.startColumn() ? null : range.endColumn())
                .commentText(request.commentText().trim())
                .createdByName("Staff")
                .createdAt(now)
                .updatedAt(now)
                .build();
        return toResponse(excelCommentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(UUID commentId) {
        ExcelCommentEntity comment = excelCommentRepository.findById(commentId)
                .orElseThrow(() -> new InvalidFileException("Excel comment was not found."));
        excelCommentRepository.delete(comment);
    }

    public boolean isExcel(DocumentVersionEntity version) {
        String extension = extension(version);
        return extension.equals("xls") || extension.equals("xlsx") || extension.equals("csv");
    }

    private DocumentVersionEntity requireExcelVersion(UUID documentId, UUID versionId) {
        DocumentVersionEntity version = documentService.getVersion(documentId, versionId);
        if (!isExcel(version)) {
            throw new UnsupportedConversionException();
        }
        return version;
    }

    private ExcelWorkbookResponse poiWorkbook(DocumentVersionEntity version, Path file) {
        try (Workbook workbook = WorkbookFactory.create(file.toFile(), null, true)) {
            List<ExcelSheetSummary> sheets = new ArrayList<>();
            for (int index = 0; index < workbook.getNumberOfSheets(); index += 1) {
                Sheet sheet = workbook.getSheetAt(index);
                sheets.add(new ExcelSheetSummary(index, sheet.getSheetName(), countRows(sheet), countColumns(sheet)));
            }
            return new ExcelWorkbookResponse(version.getDocument().getId(), version.getId(), version.getOriginalFileName(), sheets);
        } catch (Exception exception) {
            throw new InvalidFileException("Unable to read Excel workbook.");
        }
    }

    private ExcelSheetDataResponse poiSheet(Path file, int sheetIndex, int startRow, int limit) {
        try (Workbook workbook = WorkbookFactory.create(file.toFile(), null, true)) {
            if (sheetIndex < 0 || sheetIndex >= workbook.getNumberOfSheets()) {
                throw new InvalidFileException("Excel sheet was not found.");
            }
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            int totalRows = countRows(sheet);
            int columnCount = countColumns(sheet);
            List<ExcelColumnResponse> columns = columns(columnCount);
            List<ExcelRowResponse> rows = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();
            int endRow = Math.min(totalRows, startRow + limit);
            for (int rowIndex = startRow; rowIndex < endRow; rowIndex += 1) {
                Row row = sheet.getRow(rowIndex);
                rows.add(new ExcelRowResponse(rowIndex, cells(row, rowIndex, columnCount, formatter)));
            }
            return new ExcelSheetDataResponse(sheetIndex, sheet.getSheetName(), startRow, rows.size(), totalRows, columns, rows);
        } catch (InvalidFileException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidFileException("Unable to read Excel sheet.");
        }
    }

    private ExcelWorkbookResponse csvWorkbook(DocumentVersionEntity version, Path file) {
        CsvStats stats = csvStats(file);
        return new ExcelWorkbookResponse(version.getDocument().getId(), version.getId(), version.getOriginalFileName(),
                List.of(new ExcelSheetSummary(0, "CSV", stats.rowCount(), stats.columnCount())));
    }

    private ExcelSheetDataResponse csvSheet(Path file, int startRow, int limit) {
        CsvStats stats = csvStats(file);
        List<ExcelRowResponse> rows = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setIgnoreSurroundingSpaces(false).get().parse(reader)) {
            int rowIndex = 0;
            int endRow = Math.min(stats.rowCount(), startRow + limit);
            for (CSVRecord record : parser) {
                if (rowIndex >= maxRows() || rowIndex >= endRow) {
                    break;
                }
                if (rowIndex >= startRow) {
                    rows.add(new ExcelRowResponse(rowIndex, csvCells(record, rowIndex, stats.columnCount())));
                }
                rowIndex += 1;
            }
        } catch (IOException exception) {
            throw new InvalidFileException("Unable to read CSV file.");
        }
        return new ExcelSheetDataResponse(0, "CSV", startRow, rows.size(), stats.rowCount(), columns(stats.columnCount()), rows);
    }

    private List<ExcelCellResponse> cells(Row row, int rowIndex, int columnCount, DataFormatter formatter) {
        List<ExcelCellResponse> cells = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex += 1) {
            Cell cell = row == null ? null : row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            String value = cell == null ? "" : formatter.formatCellValue(cell);
            String type = cell == null ? "BLANK" : cell.getCellType().name();
            cells.add(new ExcelCellResponse(rowIndex, columnIndex, ExcelCellRefUtil.toCellRef(rowIndex, columnIndex), value, value, type));
        }
        return cells;
    }

    private List<ExcelCellResponse> csvCells(CSVRecord record, int rowIndex, int columnCount) {
        List<ExcelCellResponse> cells = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex += 1) {
            String value = columnIndex < record.size() ? record.get(columnIndex) : "";
            cells.add(new ExcelCellResponse(rowIndex, columnIndex, ExcelCellRefUtil.toCellRef(rowIndex, columnIndex), value, value, "STRING"));
        }
        return cells;
    }

    private int countRows(Sheet sheet) {
        if (sheet.getLastRowNum() < 0) {
            return 0;
        }
        return Math.min(sheet.getLastRowNum() + 1, maxRows());
    }

    private int countColumns(Sheet sheet) {
        int columns = 0;
        int rowLimit = countRows(sheet);
        for (int rowIndex = 0; rowIndex < rowLimit; rowIndex += 1) {
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                columns = Math.max(columns, row.getLastCellNum());
            }
        }
        return Math.max(0, Math.min(columns, maxColumns()));
    }

    private CsvStats csvStats(Path file) {
        int rows = 0;
        int columns = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setIgnoreSurroundingSpaces(false).get().parse(reader)) {
            for (CSVRecord record : parser) {
                if (rows >= maxRows()) {
                    break;
                }
                columns = Math.max(columns, record.size());
                rows += 1;
            }
        } catch (IOException exception) {
            throw new InvalidFileException("Unable to read CSV file.");
        }
        return new CsvStats(rows, Math.min(columns, maxColumns()));
    }

    private List<ExcelColumnResponse> columns(int columnCount) {
        List<ExcelColumnResponse> columns = new ArrayList<>();
        for (int index = 0; index < columnCount; index += 1) {
            columns.add(new ExcelColumnResponse(index, ExcelCellRefUtil.columnIndexToName(index)));
        }
        return columns;
    }

    private ExcelCommentResponse toResponse(ExcelCommentEntity comment) {
        return new ExcelCommentResponse(
                comment.getId(),
                comment.getDocumentId(),
                comment.getVersionId(),
                comment.getSheetIndex(),
                comment.getSheetName(),
                comment.getStartCell(),
                comment.getEndCell(),
                comment.getStartRow(),
                comment.getStartColumn(),
                comment.getEndRow(),
                comment.getEndColumn(),
                comment.getCommentText(),
                comment.getCreatedByName(),
                comment.getCreatedAt()
        );
    }

    private boolean isCsv(DocumentVersionEntity version) {
        return extension(version).equals("csv");
    }

    private String extension(DocumentVersionEntity version) {
        String fileName = version.getOriginalFileName();
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex < 0 ? "" : fileName.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
    }

    private int maxRows() {
        return excelPreviewProperties.maxRows() <= 0 ? 1000 : excelPreviewProperties.maxRows();
    }

    private int maxColumns() {
        return excelPreviewProperties.maxColumns() <= 0 ? 100 : excelPreviewProperties.maxColumns();
    }

    private record CsvStats(int rowCount, int columnCount) {
    }
}
