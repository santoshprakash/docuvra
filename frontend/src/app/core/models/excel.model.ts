export interface ExcelSheetSummary {
  sheetIndex: number;
  sheetName: string;
  rowCount: number;
  columnCount: number;
}

export interface ExcelWorkbook {
  documentId: string;
  versionId: string;
  fileName: string;
  sheets: ExcelSheetSummary[];
}

export interface ExcelColumn {
  index: number;
  name: string;
}

export interface ExcelCell {
  rowIndex: number;
  columnIndex: number;
  cellRef: string;
  value: string;
  displayValue: string;
  cellType: string;
}

export interface ExcelRow {
  rowIndex: number;
  cells: ExcelCell[];
}

export interface ExcelSheetData {
  sheetIndex: number;
  sheetName: string;
  startRow: number;
  rowCount: number;
  totalRows: number;
  columns: ExcelColumn[];
  rows: ExcelRow[];
}

export interface CreateExcelCommentRequest {
  sheetIndex: number;
  sheetName: string;
  startCell: string;
  endCell: string | null;
  commentText: string;
}

export interface ExcelComment {
  id: string;
  documentId: string;
  versionId: string;
  sheetIndex: number;
  sheetName: string;
  startCell: string;
  endCell: string | null;
  startRow: number;
  startColumn: number;
  endRow: number | null;
  endColumn: number | null;
  commentText: string;
  createdByName: string;
  createdAt: string;
}
