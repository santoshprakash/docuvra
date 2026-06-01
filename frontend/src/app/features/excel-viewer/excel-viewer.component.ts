import { DatePipe } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ExcelCell, ExcelComment, ExcelSheetData, ExcelSheetSummary, ExcelWorkbook } from '../../core/models/excel.model';
import { DocumentService } from '../../core/services/document.service';
import { ExcelPreviewService } from '../../core/services/excel-preview.service';

@Component({
  selector: 'app-excel-viewer',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './excel-viewer.component.html',
  styleUrl: './excel-viewer.component.scss'
})
export class ExcelViewerComponent implements OnInit {
  @ViewChild('gridShell') private readonly gridShell?: ElementRef<HTMLElement>;

  protected workbook: ExcelWorkbook | null = null;
  protected sheetData: ExcelSheetData | null = null;
  protected comments: ExcelComment[] = [];
  protected selectedSheetIndex = 0;
  protected selectedCell: ExcelCell | null = null;
  protected commentText = '';
  protected isLoading = true;
  protected isSheetLoading = false;
  protected isSavingComment = false;
  protected errorMessage = '';
  protected readonly pageSize = 200;

  protected documentId = '';
  protected versionId = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly excelPreviewService: ExcelPreviewService,
    private readonly documentService: DocumentService
  ) {
  }

  ngOnInit(): void {
    this.documentId = this.route.snapshot.paramMap.get('documentId') ?? '';
    this.versionId = this.route.snapshot.paramMap.get('versionId') ?? '';
    this.loadWorkbook();
  }

  protected get selectedSheet(): ExcelSheetSummary | null {
    return this.workbook?.sheets.find(sheet => sheet.sheetIndex === this.selectedSheetIndex) ?? null;
  }

  protected get visibleComments(): ExcelComment[] {
    if (!this.selectedCell) {
      return this.comments;
    }
    return this.comments.filter(comment => this.cellInsideComment(this.selectedCell!, comment));
  }

  protected get hasMoreRows(): boolean {
    if (!this.sheetData) {
      return false;
    }
    return this.sheetData.rows.length < this.sheetData.totalRows;
  }

  protected download(): void {
    window.open(this.documentService.getDownloadUrl(this.documentId, this.versionId), '_blank');
  }

  protected switchSheet(sheet: ExcelSheetSummary): void {
    if (sheet.sheetIndex === this.selectedSheetIndex) {
      return;
    }
    this.selectedSheetIndex = sheet.sheetIndex;
    this.selectedCell = null;
    this.commentText = '';
    this.loadSheet(0);
    this.loadComments();
  }

  protected selectCell(cell: ExcelCell): void {
    this.selectedCell = cell;
  }

  protected cellHasComment(cell: ExcelCell): boolean {
    return this.comments.some(comment => this.cellInsideComment(cell, comment));
  }

  protected addComment(): void {
    if (!this.selectedCell || !this.sheetData || !this.commentText.trim()) {
      return;
    }

    this.isSavingComment = true;
    this.excelPreviewService.addComment(this.documentId, this.versionId, {
      sheetIndex: this.sheetData.sheetIndex,
      sheetName: this.sheetData.sheetName,
      startCell: this.selectedCell.cellRef,
      endCell: null,
      commentText: this.commentText.trim()
    }).subscribe({
      next: comment => {
        this.comments = [...this.comments, comment];
        this.commentText = '';
        this.isSavingComment = false;
      },
      error: error => {
        this.errorMessage = error?.error?.message ?? 'Unable to add comment.';
        this.isSavingComment = false;
      }
    });
  }

  protected deleteComment(comment: ExcelComment): void {
    this.excelPreviewService.deleteComment(comment.id).subscribe({
      next: () => {
        this.comments = this.comments.filter(item => item.id !== comment.id);
      },
      error: error => {
        this.errorMessage = error?.error?.message ?? 'Unable to delete comment.';
      }
    });
  }

  protected jumpToComment(comment: ExcelComment): void {
    const row = this.sheetData?.rows.find(item => item.rowIndex === comment.startRow);
    const cell = row?.cells.find(item => item.columnIndex === comment.startColumn);
    if (cell) {
      this.selectedCell = cell;
      window.setTimeout(() => document.getElementById(`excel-cell-${cell.cellRef}`)?.scrollIntoView({
        block: 'center',
        inline: 'center',
        behavior: 'smooth'
      }), 0);
    }
  }

  protected loadMoreRows(): void {
    if (!this.sheetData || this.isSheetLoading) {
      return;
    }

    const startRow = this.sheetData.rows.length;
    this.isSheetLoading = true;
    this.excelPreviewService.getSheet(this.documentId, this.versionId, this.selectedSheetIndex, startRow, this.pageSize).subscribe({
      next: sheet => {
        this.sheetData = {
          ...this.sheetData!,
          rows: [...this.sheetData!.rows, ...sheet.rows],
          rowCount: this.sheetData!.rows.length + sheet.rows.length
        };
        this.isSheetLoading = false;
      },
      error: error => {
        this.errorMessage = error?.error?.message ?? 'Unable to load more rows.';
        this.isSheetLoading = false;
      }
    });
  }

  protected updateCommentText(value: string): void {
    this.commentText = value;
  }

  private loadWorkbook(): void {
    if (!this.documentId || !this.versionId) {
      this.errorMessage = 'Excel viewer route is missing document or version information.';
      this.isLoading = false;
      return;
    }

    this.excelPreviewService.getWorkbook(this.documentId, this.versionId).subscribe({
      next: workbook => {
        this.workbook = workbook;
        this.selectedSheetIndex = workbook.sheets[0]?.sheetIndex ?? 0;
        this.isLoading = false;
        this.loadSheet(0);
        this.loadComments();
      },
      error: error => {
        this.errorMessage = error?.error?.message ?? 'Unable to load Excel workbook.';
        this.isLoading = false;
      }
    });
  }

  private loadSheet(startRow: number): void {
    this.isSheetLoading = true;
    this.excelPreviewService.getSheet(this.documentId, this.versionId, this.selectedSheetIndex, startRow, this.pageSize).subscribe({
      next: sheet => {
        this.sheetData = sheet;
        this.isSheetLoading = false;
        this.gridShell?.nativeElement.scrollTo({ top: 0, left: 0 });
      },
      error: error => {
        this.errorMessage = error?.error?.message ?? 'Unable to load Excel sheet.';
        this.isSheetLoading = false;
      }
    });
  }

  private loadComments(): void {
    this.excelPreviewService.getComments(this.documentId, this.versionId, this.selectedSheetIndex).subscribe({
      next: comments => {
        this.comments = comments;
      },
      error: error => {
        this.errorMessage = error?.error?.message ?? 'Unable to load Excel comments.';
      }
    });
  }

  private cellInsideComment(cell: ExcelCell, comment: ExcelComment): boolean {
    const endRow = comment.endRow ?? comment.startRow;
    const endColumn = comment.endColumn ?? comment.startColumn;
    return cell.rowIndex >= comment.startRow
      && cell.rowIndex <= endRow
      && cell.columnIndex >= comment.startColumn
      && cell.columnIndex <= endColumn;
  }
}
