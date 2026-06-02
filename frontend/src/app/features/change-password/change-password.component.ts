import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './change-password.component.html',
  styleUrl: './change-password.component.scss'
})
export class ChangePasswordComponent implements OnInit {
  protected currentPassword = '';
  protected newPassword = '';
  protected confirmPassword = '';
  protected errorMessage = '';
  protected isSubmitting = false;
  protected isRequired = false;

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {
  }

  ngOnInit(): void {
    this.authService.loadCurrentUser().subscribe({
      next: user => this.isRequired = user.forcePasswordChange,
      error: () => void this.router.navigate(['/login'])
    });
  }

  protected submit(): void {
    this.errorMessage = '';
    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'New passwords do not match.';
      return;
    }

    this.isSubmitting = true;
    this.authService.changePassword({
      currentPassword: this.currentPassword,
      newPassword: this.newPassword
    }).subscribe({
      next: () => {
        this.isSubmitting = false;
        void this.router.navigate(['/documents']);
      },
      error: error => {
        this.errorMessage = error?.error?.message ?? 'Unable to change password. Please try again.';
        this.isSubmitting = false;
      }
    });
  }
}
