import { HttpClient, HttpEvent } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  DocumentAssignmentResponse,
  DocumentDetailsResponse,
  DocumentListResponse,
  DocumentSearchMatchResponse,
  DocumentUploadResponse,
  OcrStatusResponse,
  UploadNewVersionResponse
} from '../models/document.model';
import { AssignmentRequestResponse } from '../models/assignment-request.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private readonly baseUrl = `${environment.apiUrl}/documents`;

  constructor(
    private readonly http: HttpClient,
    private readonly authService: AuthService
  ) {
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

  getOcrStatus(documentId: string, versionId: string): Observable<OcrStatusResponse> {
    return this.http.get<OcrStatusResponse>(`${this.baseUrl}/${documentId}/versions/${versionId}/ocr/status`);
  }

  searchOcrText(documentId: string, versionId: string, searchText: string): Observable<DocumentSearchMatchResponse[]> {
    return this.http.post<DocumentSearchMatchResponse[]>(`${environment.apiUrl}/document-search/highlight/text`, {
      documentId,
      versionId,
      searchText
    });
  }

  assignDocument(documentId: string, userId: string): Observable<DocumentAssignmentResponse> {
    return this.http.post<DocumentAssignmentResponse>(`${this.baseUrl}/${documentId}/assignments`, { userId });
  }

  removeAssignment(documentId: string, assignmentId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${documentId}/assignments/${assignmentId}`);
  }

  requestAssignment(documentId: string): Observable<AssignmentRequestResponse> {
    return this.http.post<AssignmentRequestResponse>(`${this.baseUrl}/${documentId}/assignment-requests`, {});
  }

  getViewUrl(documentId: string, versionId: string): string {
    return this.withAccessToken(`${this.baseUrl}/${documentId}/versions/${versionId}/view`);
  }

  getOriginalViewUrl(documentId: string, versionId: string): string {
    return this.withAccessToken(`${this.baseUrl}/${documentId}/versions/${versionId}/original/view`);
  }

  getDownloadUrl(documentId: string, versionId: string): string {
    return this.withAccessToken(`${this.baseUrl}/${documentId}/versions/${versionId}/download`);
  }

  getApiAssetUrl(path: string | null): string {
    if (!path) {
      return '';
    }

    if (/^https?:\/\//i.test(path)) {
      return this.withAccessToken(path);
    }

    if (environment.apiUrl.startsWith('/')) {
      return this.withAccessToken(path);
    }

    const apiOrigin = new URL(environment.apiUrl).origin;
    return this.withAccessToken(`${apiOrigin}${path}`);
  }

  deleteDocument(documentId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${documentId}`);
  }

  deleteVersion(documentId: string, versionId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${documentId}/versions/${versionId}`);
  }

  getAuthenticatedFetchOptions(): RequestInit {
    const token = this.authService.token;
    const headers: Record<string, string> = {
      'X-Docuvra-Role': this.authService.openRole
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    return {
      headers
    };
  }

  private withAccessToken(url: string): string {
    const token = this.authService.token;
    if (!token) {
      return url;
    }

    const separator = url.includes('?') ? '&' : '?';
    return `${url}${separator}access_token=${encodeURIComponent(token)}`;
  }
}
