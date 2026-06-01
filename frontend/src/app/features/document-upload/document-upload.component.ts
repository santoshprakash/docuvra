import { HttpErrorResponse, HttpEvent, HttpEventType } from '@angular/common/http';
import { Component, EventEmitter, Output } from '@angular/core';

import { DocumentUploadResponse } from '../../core/models/document.model';
import { DocumentService } from '../../core/services/document.service';

@Component({
  selector: 'app-document-upload',
  standalone: true,
  templateUrl: './document-upload.component.html',
  styleUrl: './document-upload.component.scss'
})
export class DocumentUploadComponent {
  private static readonly maxFileSizeBytes = 50 * 1024 * 1024;

  @Output() readonly closed = new EventEmitter<void>();
  @Output() readonly uploaded = new EventEmitter<DocumentUploadResponse>();

  protected selectedFile: File | null = null;
  protected errorMessage = '';
  protected successMessage = '';
  protected uploadProgress = 0;
  protected isUploading = false;
  protected isDragging = false;

  constructor(private readonly documentService: DocumentService) {
  }

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.setSelectedFile(file);
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = true;
  }

  protected onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
    const file = event.dataTransfer?.files?.[0] ?? null;
    this.setSelectedFile(file);
  }

  protected upload(): void {
    if (!this.selectedFile || !this.validateFile(this.selectedFile)) {
      return;
    }

    this.isUploading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.uploadProgress = 0;

    this.documentService.uploadFreshDocumentWithProgress(this.selectedFile).subscribe({
      next: event => this.handleUploadEvent(event),
      error: error => this.handleUploadError(error)
    });
  }

  protected clearSelection(): void {
    if (this.isUploading) {
      return;
    }

    this.selectedFile = null;
    this.uploadProgress = 0;
    this.errorMessage = '';
    this.successMessage = '';
  }

  private setSelectedFile(file: File | null): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.uploadProgress = 0;

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
      this.errorMessage = 'File is required and cannot be empty.';
      return false;
    }

    if (file.size > DocumentUploadComponent.maxFileSizeBytes) {
      this.errorMessage = 'File size must not exceed 50 MB.';
      return false;
    }

    return true;
  }

  private handleUploadEvent(event: HttpEvent<DocumentUploadResponse>): void {
    if (event.type === HttpEventType.UploadProgress && event.total) {
      this.uploadProgress = Math.round((100 * event.loaded) / event.total);
      return;
    }

    if (event.type === HttpEventType.Response && event.body) {
      this.uploadProgress = 100;
      this.isUploading = false;
      this.successMessage = 'Document uploaded successfully.';
      this.uploaded.emit(event.body);
    }
  }

  private handleUploadError(error: unknown): void {
    this.isUploading = false;
    this.uploadProgress = 0;

    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      this.errorMessage = error.error.message;
      return;
    }

    this.errorMessage = 'Unable to upload document. Please try again.';
  }
}
