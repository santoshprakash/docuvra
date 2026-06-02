import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { of } from 'rxjs';

import { AppComponent } from '../app.component';
import { AuthService } from '../core/services/auth.service';
import { NotificationService } from '../core/services/notification.service';
import { authServiceFor, notifications, supervisorUser } from '../testing/regression-test-data';

describe('notification-regression', () => {
  it('renders notification badge and opens notification list', async () => {
    const authService = authServiceFor(supervisorUser);
    const unreadCount = signal(2);
    const notificationService = {
      unreadCount,
      loadSummary: jasmine.createSpy('loadSummary').and.returnValue(of({ unreadCount: 2 })),
      listNotifications: jasmine.createSpy('listNotifications').and.returnValue(of(notifications)),
      markAllRead: jasmine.createSpy('markAllRead').and.callFake(() => {
        unreadCount.set(0);
        return of(undefined);
      })
    };

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: NotificationService, useValue: notificationService }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('2');

    fixture.nativeElement.querySelector('.notification-button').click();
    fixture.detectChanges();

    expect(notificationService.listNotifications).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Mention');
    expect(fixture.nativeElement.textContent).toContain('staffuser mentioned you.');
  });

  it('marks all notifications read and clears badge count', async () => {
    const authService = authServiceFor(supervisorUser);
    const unreadCount = signal(1);
    const notificationService = {
      unreadCount,
      loadSummary: jasmine.createSpy('loadSummary').and.returnValue(of({ unreadCount: 1 })),
      listNotifications: jasmine.createSpy('listNotifications').and.returnValue(of(notifications)),
      markAllRead: jasmine.createSpy('markAllRead').and.callFake(() => {
        unreadCount.set(0);
        return of(undefined);
      })
    };

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: NotificationService, useValue: notificationService }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.notification-button').click();
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.notification-menu-header button').click();
    fixture.detectChanges();

    expect(notificationService.markAllRead).toHaveBeenCalled();
    expect(unreadCount()).toBe(0);
  });
});
