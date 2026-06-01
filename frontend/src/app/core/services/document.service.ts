import { HttpClient, HttpEvent } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  DocumentDetailsResponse,
  DocumentListResponse,
  DocumentUploadResponse,
  UploadNewVersionResponse
} from '../models/document.model';

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private readonly baseUrl = `${environment.apiUrl}/documents`;

  constructor(private readonly http: HttpClient) {
  }

  uploadFreshDocument(file: File): Observable<DocumentUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<DocumentUploadResponse>(`${this.baseUrl}/upload`, formData);
  }

  uploadFreshDocumentWithProgress(file: File): Observable<HttpEvent<DocumentUploadResponse>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<DocumentUploadResponse>(`${this.baseUrl}/upload`, formData, {
      observe: 'events',
      reportProgress: true
    });
  }

  uploadNewVersion(documentId: string, file: File): Observable<UploadNewVersionResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<UploadNewVersionResponse>(`${this.baseUrl}/${documentId}/versions`, formData);
  }

  listDocuments(): Observable<DocumentListResponse[]> {
    return this.http.get<DocumentListResponse[]>(this.baseUrl);
  }

  getDocumentDetails(documentId: string): Observable<DocumentDetailsResponse> {
    return this.http.get<DocumentDetailsResponse>(`${this.baseUrl}/${documentId}`);
  }

  getViewUrl(documentId: string, versionId: string): string {
    return `${this.baseUrl}/${documentId}/versions/${versionId}/view`;
  }

  getDownloadUrl(documentId: string, versionId: string): string {
    return `${this.baseUrl}/${documentId}/versions/${versionId}/download`;
  }

  getApiAssetUrl(path: string | null): string {
    if (!path) {
      return '';
    }

    if (/^https?:\/\//i.test(path)) {
      return path;
    }

    if (environment.apiUrl.startsWith('/')) {
      return path;
    }

    const apiOrigin = new URL(environment.apiUrl).origin;
    return `${apiOrigin}${path}`;
  }

  deleteDocument(documentId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${documentId}`);
  }

  deleteVersion(documentId: string, versionId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${documentId}/versions/${versionId}`);
  }
}
