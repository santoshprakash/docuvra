import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  protected mode: 'login' | 'signup' = 'login';
  protected usernameOrEmail = '';
  protected username = '';
  protected email = '';
  protected mobile = '';
  protected password = '';
  protected confirmPassword = '';
  protected errorMessage = '';
  protected isSubmitting = false;

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {
  }

  protected submit(): void {
    this.errorMessage = '';
    if (this.mode === 'signup' && this.password !== this.confirmPassword) {
      this.errorMessage = 'Passwords do not match.';
      return;
    }

    this.isSubmitting = true;
    const request = this.mode === 'login'
      ? this.authService.login({ usernameOrEmail: this.usernameOrEmail, password: this.password })
      : this.authService.signup({
          username: this.username,
          email: this.email,
          mobile: this.mobile,
          password: this.password
        });

    request.subscribe({
      next: response => {
        this.isSubmitting = false;
        const target = response.user.forcePasswordChange ? '/change-password' : '/documents';
        void this.router.navigate([target]);
      },
      error: error => {
        this.errorMessage = error?.error?.message ?? 'Unable to continue. Please check the details and try again.';
        this.isSubmitting = false;
      }
    });
  }

  protected switchMode(mode: 'login' | 'signup'): void {
    this.mode = mode;
    this.errorMessage = '';
  }
}
