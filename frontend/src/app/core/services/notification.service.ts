import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { NotificationResponse, NotificationSummaryResponse } from '../models/notification.model';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly baseUrl = environment.apiUrl;
  readonly unreadCount = signal(0);

  constructor(private readonly http: HttpClient) {
  }

  loadSummary(): Observable<NotificationSummaryResponse> {
    return this.http.get<NotificationSummaryResponse>(`${this.baseUrl}/notifications/summary`).pipe(
      tap(summary => this.unreadCount.set(summary.unreadCount))
    );
  }

  listNotifications(): Observable<NotificationResponse[]> {
    return this.http.get<NotificationResponse[]>(`${this.baseUrl}/notifications`);
  }

  markRead(notificationId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/notifications/${notificationId}/read`, {});
  }

  markAllRead(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/notifications/read-all`, {}).pipe(
      tap(() => this.unreadCount.set(0))
    );
  }
}
