import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, timeout } from 'rxjs';
import type { CreateServiceRequestPayload, ServiceRequestResponse } from '../models/service-request.models';

@Injectable({ providedIn: 'root' })
export class ServiceRequestService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = 'http://localhost:8081';

  createRequest(body: CreateServiceRequestPayload): Observable<ServiceRequestResponse> {
    return this.http.post<ServiceRequestResponse>(`${this.apiBase}/api/service/request`, body).pipe(timeout(10000));
  }

  getMyRequests(): Observable<ServiceRequestResponse[]> {
    return this.http.get<ServiceRequestResponse[]>(`${this.apiBase}/api/service/my-requests`).pipe(timeout(10000));
  }
}
