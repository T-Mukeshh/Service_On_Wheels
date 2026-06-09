import { Injectable, signal } from '@angular/core';

export type NotificationEventType = 'mechanic-assigned' | 'mechanic-arriving' | 'service-completed';

export interface NotificationEvent {
  type: NotificationEventType;
  title: string;
  message: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  readonly events = signal<NotificationEvent[]>([]);

  push(event: NotificationEvent): void {
    this.events.update((items) => [event, ...items]);
  }

  notifyMechanicAssigned(requestId: string): void {
    this.push({
      type: 'mechanic-assigned',
      title: 'Mechanic Assigned',
      message: `A mechanic is on the way for request ${requestId}.`,
      createdAt: new Date().toISOString(),
    });
  }

  notifyMechanicArriving(requestId: string): void {
    this.push({
      type: 'mechanic-arriving',
      title: 'Mechanic Arriving',
      message: `Your mechanic is arriving at your location for request ${requestId}.`,
      createdAt: new Date().toISOString(),
    });
  }

  notifyServiceCompleted(requestId: string): void {
    this.push({
      type: 'service-completed',
      title: 'Service Completed',
      message: `Service request ${requestId} is complete.`,
      createdAt: new Date().toISOString(),
    });
  }
}
