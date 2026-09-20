import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import type { ApiResponse } from '../models/api.models';
import type { CreateOrderResponse, PaymentResponse, VerifyPaymentRequest } from '../models/payment.models';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = API_BASE_URL;

  createOrder(requestId: string): Observable<CreateOrderResponse> {
    return this.http.post<ApiResponse<CreateOrderResponse>>(`${this.apiBase}/api/payments/create-order/${requestId}`, {})
      .pipe(map(res => res.data));
  }

  verifyPayment(request: VerifyPaymentRequest): Observable<PaymentResponse> {
    return this.http.post<ApiResponse<PaymentResponse>>(`${this.apiBase}/api/payments/verify`, request)
      .pipe(map(res => res.data));
  }

  getPayment(requestId: string): Observable<PaymentResponse> {
    return this.http.get<ApiResponse<PaymentResponse>>(`${this.apiBase}/api/payments/request/${requestId}`)
      .pipe(map(res => res.data));
  }
}
