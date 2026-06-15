import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { inject, Injectable, PLATFORM_ID, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { AUTH_TOKEN_STORAGE_KEY } from '../models/auth-storage';
import type { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.models';
import type { ApiResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  
  // Expose reactive signal for navbar
  readonly isAuthenticated = signal<boolean>(this.hasToken());

  private readonly apiBase = API_BASE_URL;

  register(body: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<ApiResponse<AuthResponse>>(`${this.apiBase}/api/auth/register`, body)
      .pipe(
        map(res => res.data),
        tap((data) => this.saveToken(data.token))
      );
  }

  login(body: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<ApiResponse<AuthResponse>>(`${this.apiBase}/api/auth/login`, body)
      .pipe(
        map(res => res.data),
        tap((data) => this.saveToken(data.token))
      );
  }

  logout(): void {
    if (!isPlatformBrowser(this.platformId) || typeof localStorage === 'undefined') {
      return;
    }
    localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
    this.isAuthenticated.set(false);
  }

  saveToken(token: string): void {
    if (!isPlatformBrowser(this.platformId) || typeof localStorage === 'undefined') {
      return;
    }
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token);
    this.isAuthenticated.set(true);
  }

  getToken(): string | null {
    if (!isPlatformBrowser(this.platformId) || typeof localStorage === 'undefined') {
      return null;
    }
    return localStorage.getItem(AUTH_TOKEN_STORAGE_KEY);
  }

  private hasToken(): boolean {
    if (!isPlatformBrowser(this.platformId) || typeof localStorage === 'undefined') {
      return false;
    }
    return !!localStorage.getItem(AUTH_TOKEN_STORAGE_KEY);
  }

  isLoggedIn(): boolean {
    return this.isAuthenticated();
  }

  forgotPassword(email: string): Observable<{ message: string }> {
    return this.http.post<ApiResponse<any>>(`${this.apiBase}/api/auth/forgot-password`, { email })
      .pipe(map(res => ({ message: res.message })));
  }

  resetPassword(token: string, password: string): Observable<{ message: string }> {
    return this.http.post<ApiResponse<any>>(`${this.apiBase}/api/auth/reset-password`, { token, password })
      .pipe(map(res => ({ message: res.message })));
  }

  validateResetToken(token: string): Observable<{ message: string; email: string }> {
    return this.http.get<ApiResponse<{ email: string }>>(`${this.apiBase}/api/auth/validate-reset-token`, {
      params: { token },
    }).pipe(map(res => ({ message: res.message, email: res.data.email })));
  }
}
