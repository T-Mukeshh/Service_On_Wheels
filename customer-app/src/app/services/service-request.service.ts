import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, map, timeout } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import type { CreateServiceRequestPayload, ServiceRequestResponse } from '../models/service-request.models';
import type { ApiResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ServiceRequestService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = API_BASE_URL;

  createRequest(body: CreateServiceRequestPayload): Observable<ServiceRequestResponse> {
    return this.http.post<ApiResponse<ServiceRequestResponse>>(`${this.apiBase}/api/service/request`, body)
      .pipe(
        timeout(10000),
        map(res => res.data)
      );
  }

  getMyRequests(): Observable<ServiceRequestResponse[]> {
    return this.http.get<ApiResponse<ServiceRequestResponse[]>>(`${this.apiBase}/api/service/my-requests`)
      .pipe(
        timeout(10000),
        map(res => res.data)
      );
  }
}
