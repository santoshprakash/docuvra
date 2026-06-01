import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CreateExcelCommentRequest,
  ExcelComment,
  ExcelSheetData,
  ExcelWorkbook
} from '../models/excel.model';

@Injectable({
  providedIn: 'root'
})
export class ExcelPreviewService {
  private readonly baseUrl = `${environment.apiUrl}/documents`;

  constructor(private readonly http: HttpClient) {
  }

  getWorkbook(documentId: string, versionId: string): Observable<ExcelWorkbook> {
    return this.http.get<ExcelWorkbook>(`${this.baseUrl}/${documentId}/versions/${versionId}/excel/workbook`);
  }

  getSheet(documentId: string, versionId: string, sheetIndex: number, startRow = 0, limit = 200): Observable<ExcelSheetData> {
    return this.http.get<ExcelSheetData>(
      `${this.baseUrl}/${documentId}/versions/${versionId}/excel/sheets/${sheetIndex}`,
      { params: { startRow, limit } }
    );
  }

  getComments(documentId: string, versionId: string, sheetIndex: number): Observable<ExcelComment[]> {
    return this.http.get<ExcelComment[]>(
      `${this.baseUrl}/${documentId}/versions/${versionId}/excel/sheets/${sheetIndex}/comments`
    );
  }

  addComment(documentId: string, versionId: string, request: CreateExcelCommentRequest): Observable<ExcelComment> {
    return this.http.post<ExcelComment>(`${this.baseUrl}/${documentId}/versions/${versionId}/excel/comments`, request);
  }

  deleteComment(commentId: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/excel-comments/${commentId}`);
  }
}
