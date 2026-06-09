import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, timeout } from 'rxjs';
import { AuthService } from './auth.service';
import type { TrackingResponse } from '../models/tracking.models';

/**
 * Angular service for the tracking API.
 * Supports both existing HTTP fallback and a new live SSE stream.
 */
@Injectable({ providedIn: 'root' })
export class TrackingService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly apiBase = 'http://localhost:8081';

  /**
   * Fetch current tracking state for a service request.
   * Kept for compatibility and fallback.
   */
  getTracking(requestId: string): Observable<TrackingResponse> {
    return this.http
      .get<TrackingResponse>(`${this.apiBase}/api/tracking/${requestId}`)
      .pipe(timeout(10000));
  }

  /**
   * Subscribe to real-time tracking updates using SSE.
   */
  getTrackingStream(requestId: string): Observable<TrackingResponse> {
    const token = this.auth.getToken();
    const url = `${this.apiBase}/api/tracking/stream/${requestId}?access_token=${encodeURIComponent(token ?? '')}`;

    return new Observable<TrackingResponse>((subscriber) => {
      const source = new EventSource(url);

      source.onmessage = (event: MessageEvent) => {
        try {
          subscriber.next(JSON.parse(event.data) as TrackingResponse);
        } catch (error) {
          subscriber.error(new Error('Malformed tracking update received.'));
        }
      };

      source.onerror = () => {
        if (source.readyState === EventSource.CLOSED) {
          subscriber.complete();
        } else {
          subscriber.error(new Error('Unable to connect to live tracking.'));
        }
      };

      return () => source.close();
    });
  }
}
