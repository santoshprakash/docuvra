import { DatePipe } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { DocumentDetailsResponse, DocumentVersionResponse } from '../../core/models/document.model';
import { DocumentService } from '../../core/services/document.service';
import { FileSizePipe } from '../../shared/pipes/file-size.pipe';

@Component({
  selector: 'app-document-details',
  standalone: true,
  imports: [DatePipe, RouterLink, FileSizePipe],
  templateUrl: './document-details.component.html',
  styleUrl: './document-details.component.scss'
})
export class DocumentDetailsComponent implements OnInit {
  private static readonly maxFileSizeBytes = 50 * 1024 * 1024;

  protected document: DocumentDetailsResponse | null = null;
  protected selectedFile: File | null = null;
  protected errorMessage = '';
  protected uploadErrorMessage = '';
  protected successMessage = '';
  protected isLoading = false;
  protected isUploading = false;
  protected isUploadPanelOpen = false;
  protected deletingVersionId: string | null = null;
  protected failedThumbnailIds = new Set<string>();

  private documentId = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly documentService: DocumentService
  ) {
  }

  ngOnInit(): void {
    this.documentId = this.route.snapshot.paramMap.get('documentId') ?? '';
    this.loadDetails();
  }

  protected loadDetails(): void {
    if (!this.documentId) {
      this.errorMessage = 'Document ID is missing.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.documentService.getDocumentDetails(this.documentId).subscribe({
      next: document => {
        this.document = {
          ...document,
          versions: [...document.versions].sort((first, second) => second.versionNumber - first.versionNumber)
        };
        this.failedThumbnailIds.clear();
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Unable to load document details. Please try again.';
        this.isLoading = false;
      }
    });
  }

  protected canUploadNewVersion(): boolean {
    return (this.document?.versions.length ?? 0) < 5;
  }

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.setSelectedFile(file);
  }

  protected uploadNewVersion(): void {
    if (!this.selectedFile || !this.validateFile(this.selectedFile)) {
      return;
    }

    this.isUploading = true;
    this.uploadErrorMessage = '';
    this.successMessage = '';

    this.documentService.uploadNewVersion(this.documentId, this.selectedFile).subscribe({
      next: () => {
        this.isUploading = false;
        this.selectedFile = null;
        this.isUploadPanelOpen = false;
        this.successMessage = 'New version uploaded successfully.';
        this.loadDetails();
      },
      error: error => {
        this.isUploading = false;
        this.uploadErrorMessage = error?.error?.message ?? 'Unable to upload new version. Please try again.';
      }
    });
  }

  protected deleteVersion(version: DocumentVersionResponse): void {
    const confirmed = window.confirm(`Delete version V${version.versionNumber}?`);
    if (!confirmed) {
      return;
    }

    this.deletingVersionId = version.versionId;
    this.errorMessage = '';
    this.successMessage = '';

    this.documentService.deleteVersion(this.documentId, version.versionId).subscribe({
      next: () => {
        this.deletingVersionId = null;
        this.successMessage = `Version V${version.versionNumber} deleted.`;
        this.loadDetails();
      },
      error: error => {
        this.deletingVersionId = null;
        this.errorMessage = error?.error?.message ?? 'Unable to delete version. Please try again.';
      }
    });
  }

  protected getDownloadUrl(version: DocumentVersionResponse): string {
    return this.documentService.getDownloadUrl(this.documentId, version.versionId);
  }

  protected getThumbnailUrl(version: DocumentVersionResponse): string {
    return this.documentService.getApiAssetUrl(version.thumbnailUrl);
  }

  protected markThumbnailFailed(versionId: string): void {
    this.failedThumbnailIds.add(versionId);
  }

  protected shouldShowThumbnail(version: DocumentVersionResponse): boolean {
    return !!version.thumbnailUrl && !this.failedThumbnailIds.has(version.versionId);
  }

  protected clearSelectedFile(): void {
    if (this.isUploading) {
      return;
    }

    this.selectedFile = null;
    this.uploadErrorMessage = '';
  }

  protected isPdf(version: DocumentVersionResponse): boolean {
    return version.mimeType.toLowerCase() === 'application/pdf'
      || version.fileName.toLowerCase().endsWith('.pdf');
  }

  protected isExcel(version: DocumentVersionResponse): boolean {
    const fileName = version.fileName.toLowerCase();
    return fileName.endsWith('.xls') || fileName.endsWith('.xlsx') || fileName.endsWith('.csv');
  }

  protected viewRoute(version: DocumentVersionResponse): string[] {
    return this.isExcel(version)
      ? ['/excel-viewer', this.documentId, version.versionId]
      : ['/viewer', this.documentId, version.versionId];
  }

  private setSelectedFile(file: File | null): void {
    this.uploadErrorMessage = '';
    this.successMessage = '';

    if (!file) {
      this.selectedFile = null;
      return;
    }

    if (this.validateFile(file)) {
      this.selectedFile = file;
    }
  }

  private validateFile(file: File): boolean {
    if (file.size === 0) {
      this.uploadErrorMessage = 'File is required and cannot be empty.';
      return false;
    }

    if (file.size > DocumentDetailsComponent.maxFileSizeBytes) {
      this.uploadErrorMessage = 'File size must not exceed 50 MB.';
      return false;
    }

    return true;
  }
}
