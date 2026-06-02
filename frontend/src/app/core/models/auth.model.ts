export type UserRole = 'NORMAL_USER' | 'STAFF' | 'SUPERVISOR';

export interface CurrentUserResponse {
  userId: string | null;
  username: string;
  email: string | null;
  mobile: string | null;
  role: UserRole;
  forcePasswordChange: boolean;
  loginEnabled: boolean;
}

export interface AuthResponse {
  token: string;
  user: CurrentUserResponse;
}

export interface UserResponse {
  userId: string;
  username: string;
  email: string;
  mobile: string;
  role: UserRole;
  active: boolean;
  forcePasswordChange: boolean;
  createdAt: string;
  lastLoginAt: string | null;
}
