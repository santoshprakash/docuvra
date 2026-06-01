package com.docuvra.util;

import com.docuvra.exception.InvalidFileException;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExcelCellRefUtil {

    private static final Pattern CELL_REF_PATTERN = Pattern.compile("^([A-Z]+)([1-9][0-9]*)$");

    private ExcelCellRefUtil() {
    }

    public static String toCellRef(int rowIndex, int columnIndex) {
        return columnIndexToName(columnIndex) + (rowIndex + 1);
    }

    public static String columnIndexToName(int columnIndex) {
        if (columnIndex < 0) {
            throw new InvalidFileException("Invalid column index.");
        }

        StringBuilder builder = new StringBuilder();
        int value = columnIndex;
        do {
            int remainder = value % 26;
            builder.insert(0, (char) ('A' + remainder));
            value = value / 26 - 1;
        } while (value >= 0);
        return builder.toString();
    }

    public static CellPosition parseCellRef(String cellRef) {
        if (cellRef == null || cellRef.isBlank()) {
            throw new InvalidFileException("Cell reference is required.");
        }

        Matcher matcher = CELL_REF_PATTERN.matcher(cellRef.trim().toUpperCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw new InvalidFileException("Invalid cell reference.");
        }

        int column = 0;
        String letters = matcher.group(1);
        for (int index = 0; index < letters.length(); index += 1) {
            column = column * 26 + (letters.charAt(index) - 'A' + 1);
        }

        return new CellPosition(Integer.parseInt(matcher.group(2)) - 1, column - 1);
    }

    public static NormalizedCellRange normalizeRange(String startCell, String endCell) {
        CellPosition start = parseCellRef(startCell);
        CellPosition end = endCell == null || endCell.isBlank() ? start : parseCellRef(endCell);
        int startRow = Math.min(start.rowIndex(), end.rowIndex());
        int startColumn = Math.min(start.columnIndex(), end.columnIndex());
        int endRow = Math.max(start.rowIndex(), end.rowIndex());
        int endColumn = Math.max(start.columnIndex(), end.columnIndex());
        return new NormalizedCellRange(
                toCellRef(startRow, startColumn),
                toCellRef(endRow, endColumn),
                startRow,
                startColumn,
                endRow,
                endColumn
        );
    }

    public record NormalizedCellRange(
            String startCell,
            String endCell,
            int startRow,
            int startColumn,
            int endRow,
            int endColumn
    ) {
    }
}
