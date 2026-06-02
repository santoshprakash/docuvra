import { Component, ElementRef, HostListener, OnDestroy, OnInit, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideArrowLeft, LucideDownload, LucideMaximize2, LucidePrinter, LucideZoomIn, LucideZoomOut } from '@lucide/angular';
import * as pdfjsLib from 'pdfjs-dist';
import type { PDFDocumentProxy, RenderTask } from 'pdfjs-dist';

import {
  AnnotationRequest,
  AnnotationResponse,
  AnnotationTool,
  AnnotationType
} from '../../core/models/annotation.model';
import { UserResponse } from '../../core/models/auth.model';
import { DocumentDetailsResponse, DocumentVersionResponse } from '../../core/models/document.model';
import { AnnotationService } from '../../core/services/annotation.service';
import { AuthService } from '../../core/services/auth.service';
import { DocumentService } from '../../core/services/document.service';
import { AnnotationToolbarComponent } from './annotation-toolbar/annotation-toolbar.component';
import { CommentPanelComponent } from './comment-panel/comment-panel.component';

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

type ResizeHandle = 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right';

interface AnnotationInteraction {
  mode: 'move' | 'resize';
  annotationId: string;
  page: PdfPageView;
  startPoint: Point;
  original: AnnotationResponse;
  handle?: ResizeHandle;
}

@Component({
  selector: 'app-viewer',
  standalone: true,
  imports: [
    RouterLink,
    AnnotationToolbarComponent,
    CommentPanelComponent,
    LucideArrowLeft,
    LucideDownload,
    LucideMaximize2,
    LucidePrinter,
    LucideZoomIn,
    LucideZoomOut
  ],
  templateUrl: './viewer.component.html',
  styleUrl: './viewer.component.scss'
})
export class ViewerComponent implements OnInit, OnDestroy {
  @ViewChild('canvasShell') private readonly canvasShell?: ElementRef<HTMLElement>;
  @ViewChildren('pdfCanvas') private readonly pdfCanvases?: QueryList<ElementRef<HTMLCanvasElement>>;

  protected document: DocumentDetailsResponse | null = null;
  protected selectedVersion: DocumentVersionResponse | null = null;
  protected isLoading = true;
  protected isPdfLoading = false;
  protected errorMessage = '';
  protected currentPage = 1;
  protected totalPages = 0;
  protected zoom = 1;
  protected readonly minZoom = 0.5;
  protected readonly maxZoom = 2.5;
  protected activeTool: AnnotationTool = 'SELECT';
  protected annotations: AnnotationResponse[] = [];
  protected selectedAnnotationId: string | null = null;
  protected draftAnnotation: AnnotationRequest | null = null;
  protected draftComment = '';
  protected mentionableUsers: UserResponse[] = [];
  protected mentionSuggestions: UserResponse[] = [];
  protected selectedMentionUserIds = new Set<string>();
  protected mentionStartIndex = -1;
  protected isSavingAnnotation = false;
  protected annotationError = '';
  protected drawPreview: AnnotationRequest | null = null;
  protected pages: PdfPageView[] = [];
  protected selectedColor = '#2563EB';

  protected documentId = '';
  protected versionId = '';
  private pdfDocument: PDFDocumentProxy | null = null;
  private renderTasks = new Map<number, RenderTask>();
  private isDrawing = false;
  private drawingStart: Point | null = null;
  private drawingPoints: Point[] = [];
  private activePageNumber = 1;
  private activePageWidth = 1;
  private activePageHeight = 1;
  private annotationInteraction: AnnotationInteraction | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly documentService: DocumentService,
    private readonly annotationService: AnnotationService,
    private readonly authService: AuthService
  ) {
  }

  ngOnInit(): void {
    this.documentId = this.route.snapshot.paramMap.get('documentId') ?? '';
    this.versionId = this.route.snapshot.paramMap.get('versionId') ?? '';
    this.loadMentionableUsers();
    this.loadMetadata();
  }

  ngOnDestroy(): void {
    this.cleanupPdf();
  }

  protected get isPdf(): boolean {
    if (!this.selectedVersion) {
      return false;
    }

    return this.selectedVersion.mimeType.toLowerCase() === 'application/pdf'
      || this.selectedVersion.fileName.toLowerCase().endsWith('.pdf');
  }

  protected get canAnnotate(): boolean {
    const role = this.authService.currentUser()?.role;
    return role === 'STAFF' || role === 'SUPERVISOR';
  }

  protected get canReply(): boolean {
    const role = this.authService.currentUser()?.role;
    return role === 'STAFF' || role === 'SUPERVISOR' || role === 'NORMAL_USER';
  }

  protected pageAnnotations(pageNumber: number): AnnotationResponse[] {
    return this.annotations.filter(annotation => annotation.pageNumber === pageNumber);
  }

  protected zoomIn(): void {
    this.zoom = Math.min(this.maxZoom, Number((this.zoom + 0.1).toFixed(2)));
    void this.renderAllPages(false);
  }

  protected zoomOut(): void {
    this.zoom = Math.max(this.minZoom, Number((this.zoom - 0.1).toFixed(2)));
    void this.renderAllPages(false);
  }

  protected fitWidth(): void {
    void this.renderAllPages(true);
  }

  protected download(): void {
    if (!this.selectedVersion) {
      return;
    }

    window.open(this.documentService.getDownloadUrl(this.documentId, this.selectedVersion.versionId), '_blank');
  }

  protected print(): void {
    if (!this.selectedVersion) {
      return;
    }

    const printWindow = window.open(this.documentService.getViewUrl(this.documentId, this.selectedVersion.versionId), '_blank');
    try {
      window.setTimeout(() => printWindow?.print(), 800);
    } catch {
      // Cross-origin browser PDF viewers may block programmatic print; opening the PDF still supports manual print.
    }
  }

  protected switchVersion(version: DocumentVersionResponse): void {
    void this.router.navigate(['/viewer', this.documentId, version.versionId]);
  }

  protected setTool(tool: AnnotationTool): void {
    this.activeTool = tool;
    if (tool !== 'SELECT') {
      this.selectedAnnotationId = null;
      this.selectedColor = this.defaultColorFor(tool);
    }
  }

  protected setSelectedColor(color: string): void {
    this.selectedColor = color;
    this.applyColorToSelectedAnnotation();
  }

  protected onCanvasShellScroll(): void {
    if (!this.canvasShell || this.pages.length === 0) {
      return;
    }

    const shellRect = this.canvasShell.nativeElement.getBoundingClientRect();
    const shellCenter = shellRect.top + shellRect.height / 2;
    let closestPage = this.currentPage;
    let closestDistance = Number.MAX_VALUE;

    for (const page of this.pages) {
      const element = document.getElementById(`pdf-page-${page.pageNumber}`);
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

  protected onOverlayPointerDown(page: PdfPageView, event: PointerEvent): void {
    if (!this.canAnnotate || this.activeTool === 'SELECT' || this.draftAnnotation) {
      return;
    }

    event.preventDefault();
    this.activePageNumber = page.pageNumber;
    this.activePageWidth = page.width;
    this.activePageHeight = page.height;
    this.currentPage = page.pageNumber;
    const point = this.pointFromEvent(event);
    this.isDrawing = true;
    this.drawingStart = point;
    this.drawingPoints = [point];

    if (this.activeTool === 'COMMENT') {
      this.finishDraft(point, point);
      return;
    }

    event.currentTarget instanceof HTMLElement && event.currentTarget.setPointerCapture(event.pointerId);
    this.drawPreview = this.buildAnnotationRequest(this.activeTool, point, point, 'FREEHAND');
  }

  protected onOverlayPointerMove(event: PointerEvent): void {
    if (this.annotationInteraction) {
      this.moveOrResizeAnnotation(event);
      return;
    }

    if (!this.isDrawing || !this.drawingStart || this.activeTool === 'COMMENT') {
      return;
    }

    event.preventDefault();
    const point = this.pointFromEvent(event);
    if (this.activeTool === 'FREEHAND_DRAW') {
      this.addFreehandPoint(point);
    }
    this.drawPreview = this.buildAnnotationRequest(this.activeTool, this.drawingStart, point, 'FREEHAND');
  }

  protected onOverlayPointerUp(event: PointerEvent): void {
    if (this.annotationInteraction) {
      this.endAnnotationInteraction(event);
      return;
    }

    if (!this.isDrawing || !this.drawingStart || this.activeTool === 'COMMENT') {
      return;
    }

    event.preventDefault();
    const point = this.pointFromEvent(event);
    if (this.activeTool === 'FREEHAND_DRAW') {
      this.addFreehandPoint(point);
    }
    if (event.currentTarget instanceof HTMLElement && event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    this.finishDraft(this.drawingStart, point);
  }

  protected selectAnnotation(annotation: AnnotationResponse, scrollComment = true): void {
    this.activeTool = 'SELECT';
    this.selectedAnnotationId = annotation.annotationId;
    this.selectedColor = annotation.color;
    this.currentPage = annotation.pageNumber;
    document.getElementById(`pdf-page-${annotation.pageNumber}`)?.scrollIntoView({
      block: 'center',
      behavior: 'smooth'
    });
    if (scrollComment) {
      window.setTimeout(() => document.getElementById(`comment-${annotation.annotationId}`)?.scrollIntoView({
        block: 'nearest',
        behavior: 'smooth'
      }), 0);
    }
  }

  protected deleteSelectedAnnotation(): void {
    if (!this.selectedAnnotationId) {
      return;
    }

    const annotation = this.annotations.find(item => item.annotationId === this.selectedAnnotationId);
    if (!annotation || !window.confirm('Deleting this annotation will also delete all linked comments. Continue?')) {
      return;
    }

    const annotationId = this.selectedAnnotationId;
    this.annotationService.deleteAnnotation(annotationId).subscribe({
      next: () => {
        this.annotations = this.annotations.filter(annotation => annotation.annotationId !== annotationId);
        this.selectedAnnotationId = null;
      },
      error: () => {
        this.annotationError = 'Unable to delete annotation.';
      }
    });
  }

  protected deleteCommentAndLinkedAnnotation(commentId: string, annotationId: string): void {
    const annotation = this.annotations.find(item => item.annotationId === annotationId);
    const hasMultipleComments = (annotation?.comments.length ?? 0) > 1;
    const message = hasMultipleComments
      ? 'This annotation has multiple comments. Deleting this comment will delete the annotation and all linked comments. Continue?'
      : 'Deleting this comment will also delete the linked annotation. Continue?';

    if (!window.confirm(message)) {
      return;
    }

    this.annotationService.deleteCommentAndLinkedAnnotation(commentId).subscribe({
      next: () => {
        this.annotations = this.annotations.filter(item => item.annotationId !== annotationId);
        if (this.selectedAnnotationId === annotationId) {
          this.selectedAnnotationId = null;
        }
      },
      error: () => {
        this.annotationError = 'Unable to delete comment and linked annotation.';
      }
    });
  }

  protected addReply(event: { annotationId: string; commentText: string; mentionedUserIds: string[] }): void {
    this.annotationService.createComment(event.annotationId, event.commentText, event.mentionedUserIds).subscribe({
      next: comment => {
        this.annotations = this.annotations.map(annotation => annotation.annotationId === event.annotationId
          ? { ...annotation, comments: [...annotation.comments, comment] }
          : annotation);
      },
      error: error => {
        this.annotationError = error?.error?.message ?? 'Unable to save reply.';
      }
    });
  }

  protected startMoveAnnotation(event: PointerEvent, annotation: AnnotationResponse, page: PdfPageView): void {
    if (this.activeTool !== 'SELECT') {
      return;
    }

    event.preventDefault();
    event.stopPropagation();
    this.selectAnnotation(annotation, false);
    this.annotationInteraction = {
      mode: 'move',
      annotationId: annotation.annotationId,
      page,
      startPoint: this.pointFromClient(event, page),
      original: { ...annotation }
    };
  }

  protected startResizeAnnotation(
    event: PointerEvent,
    annotation: AnnotationResponse,
    page: PdfPageView,
    handle: ResizeHandle
  ): void {
    event.preventDefault();
    event.stopPropagation();
    this.selectAnnotation(annotation, false);
    this.annotationInteraction = {
      mode: 'resize',
      annotationId: annotation.annotationId,
      page,
      startPoint: this.pointFromClient(event, page),
      original: { ...annotation },
      handle
    };
  }

  @HostListener('window:pointermove', ['$event'])
  protected onWindowPointerMove(event: PointerEvent): void {
    if (this.annotationInteraction) {
      this.moveOrResizeAnnotation(event);
    }
  }

  @HostListener('window:pointerup', ['$event'])
  @HostListener('window:pointercancel', ['$event'])
  protected onWindowPointerUp(event: PointerEvent): void {
    if (this.annotationInteraction) {
      this.endAnnotationInteraction(event);
    }
  }

  @HostListener('window:keydown', ['$event'])
  protected onWindowKeyDown(event: KeyboardEvent): void {
    if (this.canAnnotate && (event.key === 'Delete' || event.key === 'Backspace') && this.selectedAnnotationId && !this.draftAnnotation) {
      event.preventDefault();
      this.deleteSelectedAnnotation();
    }
  }

  protected saveDraft(): void {
    if (!this.draftAnnotation || !this.draftComment.trim()) {
      this.annotationError = 'Comment is required.';
      return;
    }

    this.isSavingAnnotation = true;
    this.annotationError = '';
    this.annotationService.createAnnotation(this.documentId, this.versionId, {
      ...this.draftAnnotation,
      commentText: this.draftComment.trim(),
      mentionedUserIds: [...this.selectedMentionUserIds]
    }).subscribe({
      next: annotation => {
        this.annotations = [...this.annotations, annotation];
        this.selectedAnnotationId = annotation.annotationId;
        this.closeDraft();
        this.selectAnnotation(annotation);
      },
      error: error => {
        this.annotationError = error?.error?.message ?? 'Unable to save annotation.';
        this.isSavingAnnotation = false;
      }
    });
  }

  protected closeDraft(): void {
    this.draftAnnotation = null;
    this.draftComment = '';
    this.mentionSuggestions = [];
    this.selectedMentionUserIds.clear();
    this.mentionStartIndex = -1;
    this.drawPreview = null;
    this.annotationError = '';
    this.isSavingAnnotation = false;
    this.resetDrawing();
  }

  protected annotationStyle(annotation: AnnotationRequest | AnnotationResponse): Record<string, string> {
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

  protected freehandPathFor(annotation: AnnotationRequest | AnnotationResponse, page: PdfPageView): string {
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

  protected freehandViewBox(annotation: AnnotationRequest | AnnotationResponse, page: PdfPageView): string {
    const width = Math.max(1, (annotation.widthPercent / 100) * page.width);
    const height = Math.max(1, (annotation.heightPercent / 100) * page.height);
    return `0 0 ${width} ${height}`;
  }

  protected onDraftCommentInput(value: string, caretIndex: number | null): void {
    this.draftComment = value;
    this.updateMentionSuggestions(caretIndex ?? value.length);
  }

  protected insertMention(user: UserResponse): void {
    const startIndex = this.mentionStartIndex >= 0 ? this.mentionStartIndex : this.draftComment.length;
    const before = this.draftComment.slice(0, startIndex);
    const after = this.draftComment.slice(startIndex).replace(/^@[A-Za-z0-9._-]*/, '');
    this.draftComment = `${before}@${user.username} ${after}`.replace(/\s{2,}/g, ' ');
    this.selectedMentionUserIds.add(user.userId);
    this.mentionSuggestions = [];
    this.mentionStartIndex = -1;
  }

  private loadMentionableUsers(): void {
    this.authService.listMentionableUsers().subscribe({
      next: users => this.mentionableUsers = users,
      error: () => this.mentionableUsers = []
    });
  }

  private updateMentionSuggestions(caretIndex: number): void {
    const textBeforeCaret = this.draftComment.slice(0, caretIndex);
    const match = /(^|\s)@([A-Za-z0-9._-]*)$/.exec(textBeforeCaret);
    if (!match) {
      this.mentionSuggestions = [];
      this.mentionStartIndex = -1;
      return;
    }

    const query = match[2].toLowerCase();
    this.mentionStartIndex = caretIndex - query.length - 1;
    this.mentionSuggestions = this.mentionableUsers
      .filter(user => user.username.toLowerCase().includes(query) || user.email.toLowerCase().includes(query))
      .slice(0, 8);
  }

  private loadMetadata(): void {
    if (!this.documentId || !this.versionId) {
      this.errorMessage = 'Viewer route is missing document or version information.';
      this.isLoading = false;
      return;
    }

    this.documentService.getDocumentDetails(this.documentId).subscribe({
      next: document => {
        const versions = [...document.versions].sort((first, second) => first.versionNumber - second.versionNumber);
        this.document = { ...document, versions };
        this.selectedVersion = versions.find(version => version.versionId === this.versionId) ?? null;
        this.isLoading = false;

        if (!this.selectedVersion) {
          this.errorMessage = 'Selected document version was not found.';
          return;
        }

        this.isPdfLoading = true;
        this.loadAnnotations();
        window.setTimeout(() => void this.loadPdf(), 0);
      },
      error: () => {
        this.errorMessage = 'Unable to load document details. Please try again.';
        this.isLoading = false;
      }
    });
  }

  private loadAnnotations(): void {
    this.annotationService.listAnnotations(this.documentId, this.versionId).subscribe({
      next: annotations => {
        this.annotations = annotations;
      },
      error: () => {
        this.annotationError = 'Unable to load annotations.';
      }
    });
  }

  private async loadPdf(): Promise<void> {
    this.cleanupPdf();
    this.errorMessage = '';
    this.isPdfLoading = true;

    try {
      const pdfData = await this.fetchViewPdf();
      const loadingTask = pdfjsLib.getDocument({ data: pdfData });
      this.pdfDocument = await loadingTask.promise;
      this.totalPages = this.pdfDocument.numPages;
      this.currentPage = 1;
      this.pages = Array.from({ length: this.totalPages }, (_, index) => ({
        pageNumber: index + 1,
        width: 1,
        height: 1,
        rendered: false
      }));
      this.isPdfLoading = false;
      await new Promise(resolve => window.setTimeout(resolve, 0));
      await this.renderAllPages(true);
    } catch (error) {
      this.errorMessage = error instanceof Error
        ? error.message
        : 'Unable to load PDF preview. Please download the file and try again.';
      this.isPdfLoading = false;
    } finally {
      this.isPdfLoading = false;
    }
  }

  private async renderAllPages(fitToWidth: boolean): Promise<void> {
    if (!this.pdfDocument) {
      return;
    }

    this.cancelRenderTasks();
    await new Promise(resolve => window.setTimeout(resolve, 0));
    const canvases = this.pdfCanvases?.toArray() ?? [];

    if (fitToWidth && canvases[0] && this.canvasShell) {
      const firstPage = await this.pdfDocument.getPage(1);
      const baseViewport = firstPage.getViewport({ scale: 1 });
      const shellWidth = Math.max(this.canvasShell.nativeElement.clientWidth - 112, 320);
      this.zoom = Number(Math.min(this.maxZoom, Math.max(this.minZoom, shellWidth / baseViewport.width)).toFixed(2));
    }

    await this.renderPageToCanvas(1, canvases[0]?.nativeElement);
    for (let pageNumber = 2; pageNumber <= this.totalPages; pageNumber += 1) {
      await this.renderPageToCanvas(pageNumber, canvases[pageNumber - 1]?.nativeElement);
    }
  }

  private async fetchViewPdf(): Promise<Uint8Array> {
    const response = await fetch(
      this.documentService.getViewUrl(this.documentId, this.versionId),
      this.documentService.getAuthenticatedFetchOptions()
    );
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

  private pointFromEvent(event: PointerEvent): Point {
    const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
    return {
      x: Math.max(0, Math.min(event.clientX - rect.left, rect.width)),
      y: Math.max(0, Math.min(event.clientY - rect.top, rect.height))
    };
  }

  private pointFromClient(event: PointerEvent, page: PdfPageView): Point {
    const rect = document.getElementById(`pdf-page-${page.pageNumber}`)?.getBoundingClientRect();
    if (!rect) {
      return { x: 0, y: 0 };
    }

    return {
      x: Math.max(0, Math.min(event.clientX - rect.left, rect.width)),
      y: Math.max(0, Math.min(event.clientY - rect.top, rect.height))
    };
  }

  private finishDraft(start: Point, end: Point): void {
    const request = this.buildAnnotationRequest(this.activeTool, start, end);
    if (request.annotationType === 'FREEHAND_DRAW' && this.drawingPoints.length < 2) {
      this.closeDraft();
      return;
    }

    if (request.pixelWidth < 4 && request.pixelHeight < 4 && request.annotationType !== 'COMMENT') {
      this.closeDraft();
      return;
    }

    this.draftAnnotation = request;
    this.drawPreview = request;
    this.isDrawing = false;
  }

  private buildAnnotationRequest(
    tool: AnnotationTool,
    start: Point,
    end: Point,
    freehandShape: 'OVAL' | 'FREEHAND' = 'OVAL'
  ): AnnotationRequest {
    const type = tool === 'SELECT' ? 'COMMENT' : tool;
    const bounds = type === 'FREEHAND_DRAW' ? this.freehandBounds() : this.rectBounds(start, end, type);
    const width = Math.max(this.activePageWidth, 1);
    const height = Math.max(this.activePageHeight, 1);

    return {
      pageNumber: this.activePageNumber,
      annotationType: type,
      xPercent: (bounds.x / width) * 100,
      yPercent: (bounds.y / height) * 100,
      widthPercent: (bounds.width / width) * 100,
      heightPercent: (bounds.height / height) * 100,
      pixelX: bounds.x,
      pixelY: bounds.y,
      pixelWidth: bounds.width,
      pixelHeight: bounds.height,
      pageRenderWidth: width,
      pageRenderHeight: height,
      color: this.selectedColor || this.colorFor(type),
      strokeWidth: type === 'FREEHAND_DRAW' ? 2 : 2,
      selectedText: null,
      drawingData: type === 'FREEHAND_DRAW' ? this.freehandDrawingData(width, height, freehandShape) : null
    };
  }

  private rectBounds(start: Point, end: Point, type: AnnotationType): { x: number; y: number; width: number; height: number } {
    if (type === 'COMMENT') {
      return { x: start.x - 12, y: start.y - 12, width: 24, height: 24 };
    }

    const x = Math.min(start.x, end.x);
    const y = Math.min(start.y, end.y);
    const width = Math.abs(end.x - start.x);
    const height = Math.abs(end.y - start.y);

    if (type === 'UNDERLINE' || type === 'STRIKE_THROUGH') {
      return { x, y: type === 'UNDERLINE' ? y + height - 4 : y + height / 2 - 2, width, height: 4 };
    }

    return { x, y, width, height };
  }

  private freehandBounds(): { x: number; y: number; width: number; height: number } {
    const xs = this.drawingPoints.map(point => point.x);
    const ys = this.drawingPoints.map(point => point.y);
    const x = Math.min(...xs);
    const y = Math.min(...ys);
    return {
      x,
      y,
      width: Math.max(1, Math.max(...xs) - x),
      height: Math.max(1, Math.max(...ys) - y)
    };
  }

  private freehandDrawingData(pageWidth: number, pageHeight: number, shape: 'OVAL' | 'FREEHAND'): string | null {
    if (this.drawingPoints.length === 0) {
      return null;
    }

    const pointsPercent = this.drawingPoints.map(point => ({
      xPercent: (point.x / pageWidth) * 100,
      yPercent: (point.y / pageHeight) * 100
    }));
    const bounds = this.freehandBounds();
    const data: FreehandDrawingData = {
      type: 'FREEHAND_DRAW',
      shape,
      path: shape === 'OVAL' ? this.ovalPath(bounds.width, bounds.height) : this.pointsToSmoothPath(this.drawingPoints),
      pointsPercent,
      strokeColor: '#dc2626',
      strokeWidth: 2
    };
    return JSON.stringify(data);
  }

  private colorFor(type: AnnotationType): string {
    return this.defaultColorFor(type);
  }

  private defaultColorFor(type: AnnotationType | AnnotationTool): string {
    switch (type) {
      case 'HIGHLIGHT':
        return '#FDE047';
      case 'COMMENT':
      case 'RECTANGLE':
        return '#2563EB';
      case 'FREEHAND_DRAW':
      case 'STRIKE_THROUGH':
        return '#dc2626';
      case 'UNDERLINE':
        return '#16A34A';
      case 'SELECT':
        return '#2563EB';
    }
  }

  private applyColorToSelectedAnnotation(): void {
    if (!this.selectedAnnotationId) {
      return;
    }

    const annotation = this.annotations.find(item => item.annotationId === this.selectedAnnotationId);
    if (!annotation) {
      return;
    }

    const updated = { ...annotation, color: this.selectedColor };
    this.replaceAnnotation(updated);
    this.persistAnnotation(updated, 'Unable to update annotation color.');
  }

  private moveOrResizeAnnotation(event: PointerEvent): void {
    const interaction = this.annotationInteraction;
    if (!interaction) {
      return;
    }

    event.preventDefault();
    const currentPoint = this.pointFromClient(event, interaction.page);
    const updated = interaction.mode === 'move'
      ? this.moveAnnotation(interaction.original, interaction.page, currentPoint, interaction.startPoint)
      : this.resizeAnnotation(interaction.original, interaction.page, currentPoint, interaction.handle ?? 'bottom-right');
    this.replaceAnnotation(updated);
  }

  private endAnnotationInteraction(event: PointerEvent): void {
    const interaction = this.annotationInteraction;
    if (!interaction) {
      return;
    }

    event.preventDefault();
    const annotation = this.annotations.find(item => item.annotationId === interaction.annotationId);
    this.annotationInteraction = null;
    if (annotation) {
      this.persistAnnotation(annotation, 'Unable to save annotation position.');
    }
  }

  private moveAnnotation(annotation: AnnotationResponse, page: PdfPageView, currentPoint: Point, startPoint: Point): AnnotationResponse {
    const deltaXPercent = ((currentPoint.x - startPoint.x) / page.width) * 100;
    const deltaYPercent = ((currentPoint.y - startPoint.y) / page.height) * 100;
    const xPercent = this.clamp(annotation.xPercent + deltaXPercent, 0, 100 - annotation.widthPercent);
    const yPercent = this.clamp(annotation.yPercent + deltaYPercent, 0, 100 - annotation.heightPercent);
    const movedDeltaX = xPercent - annotation.xPercent;
    const movedDeltaY = yPercent - annotation.yPercent;

    return this.withPixelCoordinates({
      ...annotation,
      xPercent,
      yPercent,
      drawingData: annotation.annotationType === 'FREEHAND_DRAW'
        ? this.moveFreehandAnnotation(annotation, movedDeltaX, movedDeltaY)
        : annotation.drawingData
    }, page);
  }

  private resizeAnnotation(annotation: AnnotationResponse, page: PdfPageView, currentPoint: Point, handle: ResizeHandle): AnnotationResponse {
    const currentXPercent = (currentPoint.x / page.width) * 100;
    const currentYPercent = (currentPoint.y / page.height) * 100;
    let left = annotation.xPercent;
    let top = annotation.yPercent;
    let right = annotation.xPercent + annotation.widthPercent;
    let bottom = annotation.yPercent + annotation.heightPercent;

    if (handle.includes('left')) {
      left = this.clamp(currentXPercent, 0, right - 1);
    } else {
      right = this.clamp(currentXPercent, left + 1, 100);
    }

    if (handle.includes('top')) {
      top = this.clamp(currentYPercent, 0, bottom - 1);
    } else {
      bottom = this.clamp(currentYPercent, top + 1, 100);
    }

    const resized = this.withPixelCoordinates({
      ...annotation,
      xPercent: left,
      yPercent: top,
      widthPercent: right - left,
      heightPercent: bottom - top
    }, page);

    return {
      ...resized,
      drawingData: annotation.annotationType === 'FREEHAND_DRAW'
        ? this.resizeFreehandAnnotation(annotation, resized)
        : annotation.drawingData
    };
  }

  private withPixelCoordinates<T extends AnnotationRequest | AnnotationResponse>(annotation: T, page: PdfPageView): T {
    return {
      ...annotation,
      pixelX: (annotation.xPercent / 100) * page.width,
      pixelY: (annotation.yPercent / 100) * page.height,
      pixelWidth: (annotation.widthPercent / 100) * page.width,
      pixelHeight: (annotation.heightPercent / 100) * page.height,
      pageRenderWidth: page.width,
      pageRenderHeight: page.height
    };
  }

  private replaceAnnotation(updated: AnnotationResponse): void {
    this.annotations = this.annotations.map(annotation => annotation.annotationId === updated.annotationId ? updated : annotation);
  }

  private persistAnnotation(annotation: AnnotationResponse, errorMessage: string): void {
    this.annotationService.updateAnnotation(annotation.annotationId, annotation).subscribe({
      next: updated => {
        this.replaceAnnotation(updated);
      },
      error: () => {
        this.annotationError = errorMessage;
        this.loadAnnotations();
      }
    });
  }

  private moveFreehandAnnotation(annotation: AnnotationResponse, deltaXPercent: number, deltaYPercent: number): string | null {
    const data = this.parseFreehandData(annotation);
    if (!data?.pointsPercent) {
      return annotation.drawingData ?? null;
    }

    return JSON.stringify({
      ...data,
      pointsPercent: data.pointsPercent.map(point => ({
        xPercent: this.clamp(point.xPercent + deltaXPercent, 0, 100),
        yPercent: this.clamp(point.yPercent + deltaYPercent, 0, 100)
      }))
    });
  }

  private resizeFreehandAnnotation(original: AnnotationResponse, resized: AnnotationResponse): string | null {
    const data = this.parseFreehandData(original);
    if (!data?.pointsPercent) {
      return original.drawingData ?? null;
    }

    return JSON.stringify({
      ...data,
      path: data.shape === 'OVAL' ? this.ovalPath(resized.pixelWidth, resized.pixelHeight) : data.path,
      pointsPercent: data.pointsPercent.map(point => {
        const xRatio = original.widthPercent === 0 ? 0 : (point.xPercent - original.xPercent) / original.widthPercent;
        const yRatio = original.heightPercent === 0 ? 0 : (point.yPercent - original.yPercent) / original.heightPercent;
        return {
          xPercent: this.clamp(resized.xPercent + xRatio * resized.widthPercent, 0, 100),
          yPercent: this.clamp(resized.yPercent + yRatio * resized.heightPercent, 0, 100)
        };
      })
    });
  }

  private parseFreehandData(annotation: AnnotationRequest | AnnotationResponse): FreehandDrawingData | null {
    if (!annotation.drawingData) {
      return null;
    }

    try {
      const data = JSON.parse(annotation.drawingData) as FreehandDrawingData | Point[];
      return Array.isArray(data) ? null : data;
    } catch {
      return null;
    }
  }

  private clamp(value: number, min: number, max: number): number {
    return Math.min(Math.max(value, min), max);
  }

  private resetDrawing(): void {
    this.isDrawing = false;
    this.drawingStart = null;
    this.drawingPoints = [];
  }

  private addFreehandPoint(point: Point): void {
    const previous = this.drawingPoints[this.drawingPoints.length - 1];
    if (!previous || Math.hypot(point.x - previous.x, point.y - previous.y) >= 1.5) {
      this.drawingPoints.push(point);
    }
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
      const midPoint = {
        x: (currentPoint.x + nextPoint.x) / 2,
        y: (currentPoint.y + nextPoint.y) / 2
      };
      commands.push(`Q ${currentPoint.x.toFixed(2)} ${currentPoint.y.toFixed(2)} ${midPoint.x.toFixed(2)} ${midPoint.y.toFixed(2)}`);
    }

    const lastPoint = points[points.length - 1];
    commands.push(`L ${lastPoint.x.toFixed(2)} ${lastPoint.y.toFixed(2)}`);
    return commands.join(' ');
  }

  private ovalPathForAnnotation(annotation: AnnotationRequest | AnnotationResponse, page: PdfPageView): string {
    const width = Math.max(1, (annotation.widthPercent / 100) * page.width);
    const height = Math.max(1, (annotation.heightPercent / 100) * page.height);
    return this.ovalPath(width, height);
  }

  private ovalPath(width: number, height: number): string {
    const rx = Math.max(1, width / 2);
    const ry = Math.max(1, height / 2);
    const cx = rx;
    const cy = ry;
    return [
      `M ${cx + rx * 0.72} ${cy - ry * 0.58}`,
      `C ${cx + rx * 0.24} ${cy - ry * 1.0} ${cx - rx * 0.88} ${cy - ry * 0.92} ${cx - rx * 0.98} ${cy - ry * 0.06}`,
      `C ${cx - rx * 1.06} ${cy + ry * 0.76} ${cx - rx * 0.12} ${cy + ry * 1.06} ${cx + rx * 0.7} ${cy + ry * 0.64}`,
      `C ${cx + rx * 1.08} ${cy + ry * 0.44} ${cx + rx * 0.98} ${cy - ry * 0.12} ${cx + rx * 0.72} ${cy - ry * 0.42}`,
      `M ${cx - rx * 0.74} ${cy - ry * 0.33}`,
      `C ${cx - rx * 0.18} ${cy - ry * 0.72} ${cx + rx * 0.44} ${cy - ry * 0.72} ${cx + rx * 0.82} ${cy - ry * 0.34}`
    ].join(' ');
  }

  private percentPointsToBoundedPixels(
    pointsPercent: PercentPoint[],
    annotation: AnnotationRequest | AnnotationResponse,
    page: PdfPageView
  ): Point[] {
    const offsetX = (annotation.xPercent / 100) * page.width;
    const offsetY = (annotation.yPercent / 100) * page.height;
    return pointsPercent.map(point => ({
      x: (point.xPercent / 100) * page.width - offsetX,
      y: (point.yPercent / 100) * page.height - offsetY
    }));
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
