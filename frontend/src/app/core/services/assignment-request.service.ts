import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AssignmentRequestResponse } from '../models/assignment-request.model';

@Injectable({
  providedIn: 'root'
})
export class AssignmentRequestService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private readonly http: HttpClient) {
  }

  pendingRequests(): Observable<AssignmentRequestResponse[]> {
    return this.http.get<AssignmentRequestResponse[]>(`${this.baseUrl}/assignment-requests/pending`);
  }

  approve(requestId: string): Observable<AssignmentRequestResponse> {
    return this.http.post<AssignmentRequestResponse>(`${this.baseUrl}/assignment-requests/${requestId}/approve`, {});
  }

  reject(requestId: string, reviewComment: string): Observable<AssignmentRequestResponse> {
    return this.http.post<AssignmentRequestResponse>(`${this.baseUrl}/assignment-requests/${requestId}/reject`, { reviewComment });
  }
}
