import { Component, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { DocumentDetailsResponse, DocumentListResponse, DocumentVersionResponse } from '../../core/models/document.model';
import { DocumentService } from '../../core/services/document.service';
import { ComparePdfPanelComponent } from './compare-pdf-panel/compare-pdf-panel.component';

type CompareViewMode = 'side-by-side' | 'stacked' | 'tabs';
type CompareSide = 'left' | 'right';

interface CompareSelection {
  documentId: string;
  versionId: string;
  details: DocumentDetailsResponse | null;
  loadingVersions: boolean;
}

@Component({
  selector: 'app-compare',
  standalone: true,
  imports: [ComparePdfPanelComponent, FormsModule, RouterLink],
  templateUrl: './compare.component.html',
  styleUrl: './compare.component.scss'
})
export class CompareComponent implements OnInit {
  @ViewChild('leftPanel') private readonly leftPanel?: ComparePdfPanelComponent;
  @ViewChild('rightPanel') private readonly rightPanel?: ComparePdfPanelComponent;

  protected documents: DocumentListResponse[] = [];
  protected isLoadingDocuments = false;
  protected errorMessage = '';
  protected viewMode: CompareViewMode = 'side-by-side';
  protected activeTab: CompareSide = 'left';

  protected left: CompareSelection = this.emptySelection();
  protected right: CompareSelection = this.emptySelection();

  private readonly detailsCache = new Map<string, DocumentDetailsResponse>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly documentService: DocumentService
  ) {
  }

  ngOnInit(): void {
    this.loadDocuments();
  }

  protected get leftVersion(): DocumentVersionResponse | null {
    return this.selectedVersion(this.left);
  }

  protected get rightVersion(): DocumentVersionResponse | null {
    return this.selectedVersion(this.right);
  }

  protected get leftTitle(): string {
    return this.left.details?.title ?? this.documents.find(document => document.documentId === this.left.documentId)?.title ?? '';
  }

  protected get rightTitle(): string {
    return this.right.details?.title ?? this.documents.find(document => document.documentId === this.right.documentId)?.title ?? '';
  }

  protected onDocumentChange(side: CompareSide): void {
    const selection = this.selectionFor(side);
    selection.versionId = '';
    selection.details = null;

    if (!selection.documentId) {
      return;
    }

    this.loadDetails(side, selection.documentId, null);
  }

  protected swapSelections(): void {
    const nextLeft = { ...this.right };
    const nextRight = { ...this.left };
    this.left = nextLeft;
    this.right = nextRight;
  }

  protected resetZoom(): void {
    this.leftPanel?.resetZoom();
    this.rightPanel?.resetZoom();
  }

  protected setViewMode(mode: CompareViewMode): void {
    this.viewMode = mode;
    if (mode !== 'tabs') {
      this.activeTab = 'left';
    }
  }

  protected versionLabel(version: DocumentVersionResponse): string {
    const date = new Date(version.uploadedAt);
    const uploaded = Number.isNaN(date.getTime()) ? version.uploadedAt : date.toLocaleDateString();
    return `V${version.versionNumber} · ${version.fileName} · ${uploaded}`;
  }

  private loadDocuments(): void {
    this.isLoadingDocuments = true;
    this.errorMessage = '';
    this.documentService.listDocuments().subscribe({
      next: documents => {
        this.documents = documents;
        this.isLoadingDocuments = false;
        this.applyQueryParams();
      },
      error: () => {
        this.errorMessage = 'Unable to load documents for comparison.';
        this.isLoadingDocuments = false;
      }
    });
  }

  private applyQueryParams(): void {
    const query = this.route.snapshot.queryParamMap;
    const leftDocumentId = query.get('leftDocumentId') ?? '';
    const leftVersionId = query.get('leftVersionId');
    const rightDocumentId = query.get('rightDocumentId') ?? '';
    const rightVersionId = query.get('rightVersionId');

    if (leftDocumentId) {
      this.left.documentId = leftDocumentId;
      this.loadDetails('left', leftDocumentId, leftVersionId);
    }

    if (rightDocumentId) {
      this.right.documentId = rightDocumentId;
      this.loadDetails('right', rightDocumentId, rightVersionId);
    }
  }

  private loadDetails(side: CompareSide, documentId: string, preferredVersionId: string | null): void {
    const selection = this.selectionFor(side);
    selection.loadingVersions = true;

    const cached = this.detailsCache.get(documentId);
    if (cached) {
      this.applyDetails(side, cached, preferredVersionId);
      return;
    }

    this.documentService.getDocumentDetails(documentId).subscribe({
      next: details => {
        const sorted = {
          ...details,
          versions: [...details.versions].sort((first, second) => second.versionNumber - first.versionNumber)
        };
        this.detailsCache.set(documentId, sorted);
        this.applyDetails(side, sorted, preferredVersionId);
      },
      error: () => {
        selection.loadingVersions = false;
        this.errorMessage = 'Unable to load document versions.';
      }
    });
  }

  private applyDetails(side: CompareSide, details: DocumentDetailsResponse, preferredVersionId: string | null): void {
    const selection = this.selectionFor(side);
    selection.details = details;
    selection.versionId = preferredVersionId && details.versions.some(version => version.versionId === preferredVersionId)
      ? preferredVersionId
      : details.versions[0]?.versionId ?? '';
    selection.loadingVersions = false;
  }

  private selectedVersion(selection: CompareSelection): DocumentVersionResponse | null {
    return selection.details?.versions.find(version => version.versionId === selection.versionId) ?? null;
  }

  private selectionFor(side: CompareSide): CompareSelection {
    return side === 'left' ? this.left : this.right;
  }

  private emptySelection(): CompareSelection {
    return {
      documentId: '',
      versionId: '',
      details: null,
      loadingVersions: false
    };
  }
}
