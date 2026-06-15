import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, map, timeout } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { AuthService } from './auth.service';
import type { TrackingResponse } from '../models/tracking.models';
import type { ApiResponse } from '../models/api.models';

/**
 * Angular service for the tracking API.
 * Supports both existing HTTP fallback and a new live SSE stream.
 */
@Injectable({ providedIn: 'root' })
export class TrackingService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly apiBase = API_BASE_URL;

  /**
   * Fetch current tracking state for a service request.
   * Kept for compatibility and fallback.
   */
  getTracking(requestId: string): Observable<TrackingResponse> {
    return this.http
      .get<ApiResponse<TrackingResponse>>(`${this.apiBase}/api/tracking/${requestId}`)
      .pipe(
        timeout(10000),
        map(res => res.data)
      );
  }

  /**
   * Subscribe to real-time tracking updates using SSE.
   */
  getTrackingStream(requestId: string): Observable<TrackingResponse> {
    const token = this.auth.getToken();
    const url = `${this.apiBase}/api/tracking/stream/${encodeURIComponent(requestId)}`;

    return new Observable<TrackingResponse>((subscriber) => {
      if (!token) {
        subscriber.error(new Error('Authentication is required for live tracking.'));
        return;
      }

      const controller = new AbortController();

      void fetch(url, {
        headers: {
          Accept: 'text/event-stream',
          Authorization: `Bearer ${token}`,
        },
        signal: controller.signal,
      })
        .then(async (response) => {
          if (!response.ok || !response.body) {
            throw new Error('Unable to connect to live tracking.');
          }

          const reader = response.body.getReader();
          const decoder = new TextDecoder();
          let buffer = '';

          while (!subscriber.closed) {
            const { done, value } = await reader.read();
            if (done) break;

            buffer += decoder.decode(value, { stream: true });
            const events = buffer.split('\n\n');
            buffer = events.pop() ?? '';

            for (const event of events) {
              const data = event
                .split('\n')
                .filter((line) => line.startsWith('data:'))
                .map((line) => line.slice(5).trim())
                .join('\n');

              if (data) {
                subscriber.next(JSON.parse(data) as TrackingResponse);
              }
            }
          }

          subscriber.complete();
        })
        .catch((error: unknown) => {
          if (!controller.signal.aborted) {
            subscriber.error(
              error instanceof SyntaxError
                ? new Error('Malformed tracking update received.')
                : new Error('Unable to connect to live tracking.'),
            );
          }
        });

      return () => {
        controller.abort();
      };
    });
  }
}
