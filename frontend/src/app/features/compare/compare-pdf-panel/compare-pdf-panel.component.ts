import { DatePipe } from '@angular/common';
import { Component, ElementRef, Input, OnChanges, OnDestroy, QueryList, SimpleChanges, ViewChild, ViewChildren } from '@angular/core';
import * as pdfjsLib from 'pdfjs-dist';
import type { PDFDocumentProxy, RenderTask } from 'pdfjs-dist';

import { AnnotationResponse } from '../../../core/models/annotation.model';
import { DocumentVersionResponse } from '../../../core/models/document.model';
import { ExcelComment, ExcelSheetData, ExcelSheetSummary, ExcelWorkbook } from '../../../core/models/excel.model';
import { AnnotationService } from '../../../core/services/annotation.service';
import { DocumentService } from '../../../core/services/document.service';
import { ExcelPreviewService } from '../../../core/services/excel-preview.service';

pdfjsLib.GlobalWorkerOptions.workerSrc = '/assets/pdf.worker.min.mjs';

interface Point {
  x: number;
  y: number;
}

interface PercentPoint {
  xPercent: number;
  yPercent: number;
}

interface FreehandDrawingData {
  type: 'FREEHAND_DRAW';
  shape?: 'OVAL' | 'FREEHAND';
  path?: string;
  pointsPercent?: PercentPoint[];
  strokeColor?: string;
  strokeWidth?: number;
}

interface PdfPageView {
  pageNumber: number;
  width: number;
  height: number;
  rendered: boolean;
}

@Component({
  selector: 'app-compare-pdf-panel',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './compare-pdf-panel.component.html',
  styleUrl: './compare-pdf-panel.component.scss'
})
export class ComparePdfPanelComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) side: 'LEFT' | 'RIGHT' = 'LEFT';
  @Input() documentId: string | null = null;
  @Input() documentTitle = '';
  @Input() version: DocumentVersionResponse | null = null;

  @ViewChild('panelShell') private readonly panelShell?: ElementRef<HTMLElement>;
  @ViewChildren('pdfCanvas') private readonly pdfCanvases?: QueryList<ElementRef<HTMLCanvasElement>>;

  protected readonly minZoom = 0.5;
  protected readonly maxZoom = 2.5;
  protected currentPage = 1;
  protected totalPages = 0;
  protected zoom = 1;
  protected loading = false;
  protected errorMessage = '';
  protected annotations: AnnotationResponse[] = [];
  protected selectedAnnotationId: string | null = null;
  protected pages: PdfPageView[] = [];
  protected excelWorkbook: ExcelWorkbook | null = null;
  protected excelSheetData: ExcelSheetData | null = null;
  protected excelComments: ExcelComment[] = [];
  protected selectedExcelSheetIndex = 0;
  protected readonly excelPageSize = 200;

  private pdfDocument: PDFDocumentProxy | null = null;
  private renderTasks = new Map<number, RenderTask>();

  constructor(
    private readonly documentService: DocumentService,
    private readonly annotationService: AnnotationService,
    private readonly excelPreviewService: ExcelPreviewService
  ) {
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['documentId'] || changes['version']) {
      void this.reload();
    }
  }

  ngOnDestroy(): void {
    this.cleanupPdf();
  }

  protected get sideLabel(): string {
    return this.side === 'LEFT' ? 'Left' : 'Right';
  }

  protected get selectedAnnotation(): AnnotationResponse | null {
    return this.annotations.find(annotation => annotation.annotationId === this.selectedAnnotationId) ?? null;
  }

  protected get isExcel(): boolean {
    if (!this.version) {
      return false;
    }
    const fileName = this.version.fileName.toLowerCase();
    return fileName.endsWith('.xls') || fileName.endsWith('.xlsx') || fileName.endsWith('.csv');
  }

  protected pageAnnotations(pageNumber: number): AnnotationResponse[] {
    return this.annotations.filter(annotation => annotation.pageNumber === pageNumber);
  }

  protected get excelHasMoreRows(): boolean {
    if (!this.excelSheetData) {
      return false;
    }
    return this.excelSheetData.rows.length < this.excelSheetData.totalRows;
  }

  protected onPanelScroll(): void {
    if (!this.panelShell || this.pages.length === 0) {
      return;
    }

    const shellRect = this.panelShell.nativeElement.getBoundingClientRect();
    const shellCenter = shellRect.top + shellRect.height / 2;
    let closestPage = this.currentPage;
    let closestDistance = Number.MAX_VALUE;

    for (const page of this.pages) {
      const element = this.pageElement(page.pageNumber);
      if (!element) {
        continue;
      }

      const rect = element.getBoundingClientRect();
      const pageCenter = rect.top + rect.height / 2;
      const distance = Math.abs(pageCenter - shellCenter);
      if (distance < closestDistance) {
        closestDistance = distance;
        closestPage = page.pageNumber;
      }
    }

    this.currentPage = closestPage;
  }

  protected previousPage(): void {
    if (this.isExcel) {
      return;
    }
    this.goToPage(Math.max(1, this.currentPage - 1));
  }

  protected nextPage(): void {
    if (this.isExcel) {
      return;
    }
    this.goToPage(Math.min(this.totalPages, this.currentPage + 1));
  }

  protected zoomIn(): void {
    if (this.isExcel) {
      return;
    }
    this.zoom = Math.min(this.maxZoom, Number((this.zoom + 0.1).toFixed(2)));
    void this.renderAllPages(false);
  }

  protected zoomOut(): void {
    if (this.isExcel) {
      return;
    }
    this.zoom = Math.max(this.minZoom, Number((this.zoom - 0.1).toFixed(2)));
    void this.renderAllPages(false);
  }

  protected fitWidth(): void {
    if (this.isExcel) {
      return;
    }
    void this.renderAllPages(true);
  }

  resetZoom(): void {
    if (this.isExcel) {
      return;
    }
    this.zoom = 1;
    void this.renderAllPages(false);
  }

  protected download(): void {
    if (!this.documentId || !this.version) {
      return;
    }

    window.open(this.documentService.getDownloadUrl(this.documentId, this.version.versionId), '_blank');
  }

  protected selectAnnotation(annotation: AnnotationResponse): void {
    this.selectedAnnotationId = annotation.annotationId;
  }

  protected switchExcelSheet(sheet: ExcelSheetSummary): void {
    if (!this.documentId || !this.version || sheet.sheetIndex === this.selectedExcelSheetIndex) {
      return;
    }

    this.selectedExcelSheetIndex = sheet.sheetIndex;
    this.loadExcelSheet(0);
    this.loadExcelComments();
  }

  protected excelCellHasComment(rowIndex: number, columnIndex: number): boolean {
    return this.excelComments.some(comment => {
      const endRow = comment.endRow ?? comment.startRow;
      const endColumn = comment.endColumn ?? comment.startColumn;
      return rowIndex >= comment.startRow
        && rowIndex <= endRow
        && columnIndex >= comment.startColumn
        && columnIndex <= endColumn;
    });
  }

  protected loadMoreExcelRows(): void {
    if (!this.excelSheetData || !this.documentId || !this.version || this.loading) {
      return;
    }

    this.loading = true;
    const startRow = this.excelSheetData.rows.length;
    this.excelPreviewService.getSheet(this.documentId, this.version.versionId, this.selectedExcelSheetIndex, startRow, this.excelPageSize).subscribe({
      next: sheet => {
        this.excelSheetData = {
          ...this.excelSheetData!,
          rows: [...this.excelSheetData!.rows, ...sheet.rows],
          rowCount: this.excelSheetData!.rows.length + sheet.rows.length
        };
        this.loading = false;
      },
      error: error => {
        this.errorMessage = error?.error?.message ?? 'Unable to load Excel rows.';
        this.loading = false;
      }
    });
  }

  protected annotationStyle(annotation: AnnotationResponse): Record<string, string> {
    return {
      left: `${annotation.xPercent}%`,
      top: `${annotation.yPercent}%`,
      width: `${annotation.widthPercent}%`,
      height: `${annotation.heightPercent}%`,
      borderColor: annotation.color,
      color: annotation.color,
      '--annotation-color': annotation.color,
      '--annotation-stroke-width': `${annotation.strokeWidth}px`
    };
  }

  protected freehandPathFor(annotation: AnnotationResponse, page: PdfPageView): string {
    if (!annotation.drawingData) {
      return '';
    }

    try {
      const data = JSON.parse(annotation.drawingData) as FreehandDrawingData | Point[];
      if (!Array.isArray(data) && data.shape === 'OVAL') {
        return this.ovalPathForAnnotation(annotation, page);
      }

      const points = Array.isArray(data)
        ? data
        : this.percentPointsToBoundedPixels(data.pointsPercent ?? [], annotation, page);
      return this.pointsToSmoothPath(points);
    } catch {
      return '';
    }
  }

  protected freehandViewBox(annotation: AnnotationResponse, page: PdfPageView): string {
    const width = Math.max(1, (annotation.widthPercent / 100) * page.width);
    const height = Math.max(1, (annotation.heightPercent / 100) * page.height);
    return `0 0 ${width} ${height}`;
  }

  private async reload(): Promise<void> {
    this.cleanupPdf();
    this.annotations = [];
    this.pages = [];
    this.totalPages = 0;
    this.currentPage = 1;
    this.selectedAnnotationId = null;
    this.excelWorkbook = null;
    this.excelSheetData = null;
    this.excelComments = [];
    this.selectedExcelSheetIndex = 0;
    this.errorMessage = '';

    if (!this.documentId || !this.version) {
      this.loading = false;
      return;
    }

    if (this.isExcel) {
      this.loadExcelWorkbook();
      return;
    }

    this.loading = true;
    this.loadAnnotations();
    await this.loadPdf();
  }

  private loadExcelWorkbook(): void {
    if (!this.documentId || !this.version) {
      return;
    }

    this.loading = true;
    this.excelPreviewService.getWorkbook(this.documentId, this.version.versionId).subscribe({
      next: workbook => {
        this.excelWorkbook = workbook;
        this.selectedExcelSheetIndex = workbook.sheets[0]?.sheetIndex ?? 0;
        this.loadExcelSheet(0);
        this.loadExcelComments();
      },
      error: error => {
        this.errorMessage = error?.error?.message ?? 'Unable to load Excel workbook.';
        this.loading = false;
      }
    });
  }

  private loadExcelSheet(startRow: number): void {
    if (!this.documentId || !this.version) {
      return;
    }

    this.loading = true;
    this.excelPreviewService.getSheet(this.documentId, this.version.versionId, this.selectedExcelSheetIndex, startRow, this.excelPageSize).subscribe({
      next: sheet => {
        this.excelSheetData = sheet;
        this.loading = false;
      },
      error: error => {
        this.errorMessage = error?.error?.message ?? 'Unable to load Excel sheet.';
        this.loading = false;
      }
    });
  }

  private loadExcelComments(): void {
    if (!this.documentId || !this.version) {
      return;
    }

    this.excelPreviewService.getComments(this.documentId, this.version.versionId, this.selectedExcelSheetIndex).subscribe({
      next: comments => {
        this.excelComments = comments;
      },
      error: error => {
        this.errorMessage = error?.error?.message ?? 'Unable to load Excel comments.';
      }
    });
  }

  private loadAnnotations(): void {
    if (!this.documentId || !this.version) {
      return;
    }

    this.annotationService.listAnnotations(this.documentId, this.version.versionId).subscribe({
      next: annotations => {
        this.annotations = annotations;
      },
      error: () => {
        this.errorMessage = 'Unable to load read-only annotations.';
      }
    });
  }

  private async loadPdf(): Promise<void> {
    if (!this.documentId || !this.version) {
      return;
    }

    try {
      const pdfData = await this.fetchViewPdf();
      const loadingTask = pdfjsLib.getDocument({ data: pdfData });
      this.pdfDocument = await loadingTask.promise;
      this.totalPages = this.pdfDocument.numPages;
      this.pages = Array.from({ length: this.totalPages }, (_, index) => ({
        pageNumber: index + 1,
        width: 1,
        height: 1,
        rendered: false
      }));
      this.loading = false;
      await new Promise(resolve => window.setTimeout(resolve, 0));
      await this.renderAllPages(true);
    } catch (error) {
      this.errorMessage = error instanceof Error
        ? error.message
        : 'Unable to load PDF preview. Please download the file and try again.';
      this.loading = false;
    }
  }

  private async renderAllPages(fitToWidth: boolean): Promise<void> {
    if (!this.pdfDocument) {
      return;
    }

    this.cancelRenderTasks();
    await new Promise(resolve => window.setTimeout(resolve, 0));
    const canvases = this.pdfCanvases?.toArray() ?? [];

    if (fitToWidth && canvases[0] && this.panelShell) {
      const firstPage = await this.pdfDocument.getPage(1);
      const baseViewport = firstPage.getViewport({ scale: 1 });
      const shellWidth = Math.max(this.panelShell.nativeElement.clientWidth - 56, 280);
      this.zoom = Number(Math.min(this.maxZoom, Math.max(this.minZoom, shellWidth / baseViewport.width)).toFixed(2));
    }

    for (let pageNumber = 1; pageNumber <= this.totalPages; pageNumber += 1) {
      await this.renderPageToCanvas(pageNumber, canvases[pageNumber - 1]?.nativeElement);
    }
  }

  private async fetchViewPdf(): Promise<Uint8Array> {
    if (!this.documentId || !this.version) {
      throw new Error('Select a document and version.');
    }

    const response = await fetch(this.documentService.getViewUrl(this.documentId, this.version.versionId));
    if (response.ok) {
      return new Uint8Array(await response.arrayBuffer());
    }

    try {
      const error = await response.json() as { message?: string };
      throw new Error(error.message || 'Unable to load PDF preview. Please download the file and try again.');
    } catch (error) {
      if (error instanceof Error) {
        throw error;
      }
      throw new Error('Unable to load PDF preview. Please download the file and try again.');
    }
  }

  private async renderPageToCanvas(pageNumber: number, canvas?: HTMLCanvasElement): Promise<void> {
    if (!this.pdfDocument || !canvas) {
      return;
    }

    const page = await this.pdfDocument.getPage(pageNumber);
    const viewport = page.getViewport({ scale: this.zoom });
    const context = canvas.getContext('2d');

    if (!context) {
      this.errorMessage = 'Unable to initialize PDF canvas.';
      return;
    }

    const outputScale = window.devicePixelRatio || 1;
    const displayWidth = Math.floor(viewport.width);
    const displayHeight = Math.floor(viewport.height);
    canvas.width = Math.floor(viewport.width * outputScale);
    canvas.height = Math.floor(viewport.height * outputScale);
    canvas.style.width = `${displayWidth}px`;
    canvas.style.height = `${displayHeight}px`;

    this.pages = this.pages.map(item => item.pageNumber === pageNumber
      ? { ...item, width: displayWidth, height: displayHeight, rendered: true }
      : item);

    context.setTransform(outputScale, 0, 0, outputScale, 0, 0);
    context.clearRect(0, 0, viewport.width, viewport.height);

    const task = page.render({
      canvas,
      canvasContext: context,
      viewport
    });
    this.renderTasks.set(pageNumber, task);

    try {
      await task.promise;
    } catch (error) {
      if (!(error instanceof Error) || error.name !== 'RenderingCancelledException') {
        this.errorMessage = 'Unable to render this PDF page.';
      }
    } finally {
      this.renderTasks.delete(pageNumber);
    }
  }

  private goToPage(pageNumber: number): void {
    this.currentPage = pageNumber;
    this.pageElement(pageNumber)?.scrollIntoView({ block: 'start', behavior: 'smooth' });
  }

  private pageElement(pageNumber: number): HTMLElement | null {
    return document.getElementById(`${this.side.toLowerCase()}-compare-page-${pageNumber}`);
  }

  private ovalPathForAnnotation(annotation: AnnotationResponse, page: PdfPageView): string {
    const width = Math.max(1, (annotation.widthPercent / 100) * page.width);
    const height = Math.max(1, (annotation.heightPercent / 100) * page.height);
    const rx = Math.max(1, width / 2);
    const ry = Math.max(1, height / 2);
    const cx = rx;
    const cy = ry;
    return [
      `M ${cx + rx * 0.72} ${cy - ry * 0.58}`,
      `C ${cx + rx * 0.24} ${cy - ry} ${cx - rx * 0.88} ${cy - ry * 0.92} ${cx - rx * 0.98} ${cy - ry * 0.06}`,
      `C ${cx - rx * 1.06} ${cy + ry * 0.76} ${cx - rx * 0.12} ${cy + ry * 1.06} ${cx + rx * 0.7} ${cy + ry * 0.64}`,
      `C ${cx + rx * 1.08} ${cy + ry * 0.44} ${cx + rx * 0.98} ${cy - ry * 0.12} ${cx + rx * 0.72} ${cy - ry * 0.42}`
    ].join(' ');
  }

  private percentPointsToBoundedPixels(pointsPercent: PercentPoint[], annotation: AnnotationResponse, page: PdfPageView): Point[] {
    const offsetX = (annotation.xPercent / 100) * page.width;
    const offsetY = (annotation.yPercent / 100) * page.height;
    return pointsPercent.map(point => ({
      x: (point.xPercent / 100) * page.width - offsetX,
      y: (point.yPercent / 100) * page.height - offsetY
    }));
  }

  private pointsToSmoothPath(points: Point[]): string {
    if (points.length < 2) {
      return '';
    }

    const [firstPoint] = points;
    const commands = [`M ${firstPoint.x.toFixed(2)} ${firstPoint.y.toFixed(2)}`];
    for (let index = 1; index < points.length - 1; index += 1) {
      const currentPoint = points[index];
      const nextPoint = points[index + 1];
      commands.push(`Q ${currentPoint.x.toFixed(2)} ${currentPoint.y.toFixed(2)} ${((currentPoint.x + nextPoint.x) / 2).toFixed(2)} ${((currentPoint.y + nextPoint.y) / 2).toFixed(2)}`);
    }
    const lastPoint = points[points.length - 1];
    commands.push(`L ${lastPoint.x.toFixed(2)} ${lastPoint.y.toFixed(2)}`);
    return commands.join(' ');
  }

  private cleanupPdf(): void {
    this.cancelRenderTasks();
    if (this.pdfDocument) {
      void this.pdfDocument.destroy();
      this.pdfDocument = null;
    }
  }

  private cancelRenderTasks(): void {
    for (const task of this.renderTasks.values()) {
      task.cancel();
    }
    this.renderTasks.clear();
  }
}
