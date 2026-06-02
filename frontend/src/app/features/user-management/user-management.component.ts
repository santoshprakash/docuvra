import { DatePipe } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { UserResponse, UserRole } from '../../core/models/auth.model';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.scss'
})
export class UserManagementComponent implements OnInit {
  protected users: UserResponse[] = [];
  protected username = '';
  protected email = '';
  protected mobile = '';
  protected temporaryPassword = '';
  protected role: Exclude<UserRole, 'NORMAL_USER'> = 'STAFF';
  protected errorMessage = '';
  protected successMessage = '';
  protected isLoading = false;
  protected isCreating = false;

  constructor(private readonly authService: AuthService) {
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  protected loadUsers(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.authService.listUsers().subscribe({
      next: users => {
        this.users = users;
        this.isLoading = false;
      },
      error: error => {
        this.errorMessage = error?.error?.message ?? 'Unable to load users.';
        this.isLoading = false;
      }
    });
  }

  protected createUser(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.isCreating = true;
    this.authService.createUser({
      username: this.username,
      email: this.email,
      mobile: this.mobile,
      temporaryPassword: this.temporaryPassword,
      role: this.role
    }).subscribe({
      next: user => {
        this.users = [user, ...this.users];
        this.username = '';
        this.email = '';
        this.mobile = '';
        this.temporaryPassword = '';
        this.role = 'STAFF';
        this.successMessage = 'User created.';
        this.isCreating = false;
      },
      error: error => {
        this.errorMessage = error?.error?.message ?? 'Unable to create user.';
        this.isCreating = false;
      }
    });
  }

  protected roleLabel(role: UserRole): string {
    return role.replace('_', ' ').toLowerCase();
  }
}
