export type UserRole = 'USER' | 'MECHANIC' | 'ADMIN';

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  phoneNumber: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  userId: string;
  name: string;
  email: string;
  role: UserRole;
  message: string;
}
