import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthResponse, CurrentUserResponse, UserResponse, UserRole } from '../models/auth.model';

interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

interface SignupRequest {
  username: string;
  email: string;
  mobile: string;
  password: string;
}

interface CreateUserRequest {
  username: string;
  email: string;
  mobile: string;
  temporaryPassword: string;
  role: Exclude<UserRole, 'NORMAL_USER'>;
}

interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = `${environment.apiUrl}`;
  private readonly tokenKey = 'docuvra.auth.token';

  readonly currentUser = signal<CurrentUserResponse | null>(null);

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router
  ) {
  }

  get token(): string | null {
    return window.localStorage.getItem(this.tokenKey);
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/login`, request).pipe(
      tap(response => this.storeSession(response))
    );
  }

  signup(request: SignupRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/signup`, request).pipe(
      tap(response => this.storeSession(response))
    );
  }

  loadCurrentUser(): Observable<CurrentUserResponse> {
    return this.http.get<CurrentUserResponse>(`${this.baseUrl}/auth/me`).pipe(
      tap(user => this.currentUser.set(user))
    );
  }

  changePassword(request: ChangePasswordRequest): Observable<CurrentUserResponse> {
    return this.http.post<CurrentUserResponse>(`${this.baseUrl}/auth/change-password`, request).pipe(
      tap(user => this.currentUser.set(user))
    );
  }

  listUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.baseUrl}/users`);
  }

  listMentionableUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.baseUrl}/users/mentionable`);
  }

  createUser(request: CreateUserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.baseUrl}/users`, request);
  }

  logout(): void {
    window.localStorage.removeItem(this.tokenKey);
    this.currentUser.set(null);
    void this.router.navigate(['/login']);
  }

  private storeSession(response: AuthResponse): void {
    window.localStorage.setItem(this.tokenKey, response.token);
    this.currentUser.set(response.user);
  }
}
