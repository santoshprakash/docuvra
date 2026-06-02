import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AnnotationCommentResponse,
  AnnotationRequest,
  AnnotationResponse
} from '../models/annotation.model';

@Injectable({
  providedIn: 'root'
})
export class AnnotationService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private readonly http: HttpClient) {
  }

  createAnnotation(documentId: string, versionId: string, request: AnnotationRequest): Observable<AnnotationResponse> {
    return this.http.post<AnnotationResponse>(
      `${this.baseUrl}/documents/${documentId}/versions/${versionId}/annotations`,
      request
    );
  }

  listAnnotations(documentId: string, versionId: string): Observable<AnnotationResponse[]> {
    return this.http.get<AnnotationResponse[]>(
      `${this.baseUrl}/documents/${documentId}/versions/${versionId}/annotations`
    );
  }

  listPageAnnotations(documentId: string, versionId: string, pageNumber: number): Observable<AnnotationResponse[]> {
    return this.http.get<AnnotationResponse[]>(
      `${this.baseUrl}/documents/${documentId}/versions/${versionId}/pages/${pageNumber}/annotations`
    );
  }

  updateAnnotation(annotationId: string, request: AnnotationRequest): Observable<AnnotationResponse> {
    return this.http.put<AnnotationResponse>(`${this.baseUrl}/annotations/${annotationId}`, request);
  }

  deleteAnnotation(annotationId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/annotations/${annotationId}`);
  }

  deleteCommentAndLinkedAnnotation(commentId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/annotations/comments/${commentId}`);
  }

  createComment(annotationId: string, commentText: string, mentionedUserIds: string[] = []): Observable<AnnotationCommentResponse> {
    return this.http.post<AnnotationCommentResponse>(`${this.baseUrl}/annotations/${annotationId}/comments`, {
      commentText,
      mentionedUserIds
    });
  }

  listComments(annotationId: string): Observable<AnnotationCommentResponse[]> {
    return this.http.get<AnnotationCommentResponse[]>(`${this.baseUrl}/annotations/${annotationId}/comments`);
  }
}
