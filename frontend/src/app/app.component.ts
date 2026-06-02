import { Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { NotificationResponse } from './core/models/notification.model';
import { NotificationService } from './core/services/notification.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [DatePipe, RouterLink, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  protected readonly currentUser = this.authService.currentUser;
  protected readonly unreadCount = this.notificationService.unreadCount;
  protected notifications: NotificationResponse[] = [];
  protected isNotificationsOpen = false;

  constructor(
    private readonly authService: AuthService,
    private readonly notificationService: NotificationService
  ) {
  }

  ngOnInit(): void {
    if (this.authService.token) {
      this.authService.loadCurrentUser().subscribe({
        next: () => this.notificationService.loadSummary().subscribe(),
        error: () => this.authService.logout()
      });
    }
  }

  protected logout(): void {
    this.authService.logout();
  }

  protected toggleNotifications(): void {
    this.isNotificationsOpen = !this.isNotificationsOpen;
    if (this.isNotificationsOpen) {
      this.notificationService.listNotifications().subscribe(notifications => this.notifications = notifications);
    }
  }

  protected markAllRead(): void {
    this.notificationService.markAllRead().subscribe(() => {
      this.notifications = this.notifications.map(notification => ({ ...notification, read: true }));
    });
  }
}
