import { DatePipe } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

import { DocumentListResponse } from '../../core/models/document.model';
import { DocumentService } from '../../core/services/document.service';
import { DocumentUploadComponent } from '../document-upload/document-upload.component';

@Component({
  selector: 'app-document-list',
  standalone: true,
  imports: [DatePipe, RouterLink, DocumentUploadComponent],
  templateUrl: './document-list.component.html',
  styleUrl: './document-list.component.scss'
})
export class DocumentListComponent implements OnInit {
  protected isUploadOpen = false;
  protected documents: DocumentListResponse[] = [];
  protected errorMessage = '';
  protected isLoading = false;
  protected deletingDocumentId: string | null = null;
  protected failedThumbnailIds = new Set<string>();

  constructor(private readonly documentService: DocumentService) {
  }

  ngOnInit(): void {
    this.loadDocuments();
  }

  protected loadDocuments(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.documentService.listDocuments().subscribe({
      next: documents => {
        this.documents = documents;
        this.failedThumbnailIds.clear();
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Unable to load documents. Please try again.';
        this.isLoading = false;
      }
    });
  }

  protected onUploadComplete(): void {
    this.isUploadOpen = false;
    this.loadDocuments();
  }

  protected deleteDocument(document: DocumentListResponse): void {
    const confirmed = window.confirm(`Delete "${document.title}" and all of its versions?`);
    if (!confirmed) {
      return;
    }

    this.deletingDocumentId = document.documentId;
    this.errorMessage = '';

    this.documentService.deleteDocument(document.documentId).subscribe({
      next: () => {
        this.documents = this.documents.filter(item => item.documentId !== document.documentId);
        this.deletingDocumentId = null;
      },
      error: () => {
        this.errorMessage = 'Unable to delete document. Please try again.';
        this.deletingDocumentId = null;
      }
    });
  }

  protected getThumbnailUrl(document: DocumentListResponse): string {
    return this.documentService.getApiAssetUrl(document.thumbnailUrl);
  }

  protected markThumbnailFailed(documentId: string): void {
    this.failedThumbnailIds.add(documentId);
  }

  protected shouldShowThumbnail(document: DocumentListResponse): boolean {
    return !!document.thumbnailUrl && !this.failedThumbnailIds.has(document.documentId);
  }
}
