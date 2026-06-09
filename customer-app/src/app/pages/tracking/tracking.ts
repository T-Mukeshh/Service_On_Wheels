import { isPlatformBrowser, TitleCasePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import {
  AfterViewInit,
  Component,
  inject,
  OnDestroy,
  PLATFORM_ID,
  signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { NotificationService } from '../../services/notification.service';
import { ToastService } from '../../services/toast.service';
import { TrackingService } from '../../services/tracking.service';
import type { TrackingResponse, TrackingStatus } from '../../models/tracking.models';

@Component({
  selector: 'app-tracking',
  imports: [TitleCasePipe, RouterLink],
  templateUrl: './tracking.html',
  styleUrl: './tracking.css',
})
export class TrackingPage implements AfterViewInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly trackingApi = inject(TrackingService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly http = inject(HttpClient);
  private readonly notificationService = inject(NotificationService);
  private readonly toast = inject(ToastService);

  readonly trackingData = signal<TrackingResponse | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly loading = signal(true);
  readonly isOnline = signal(true);

  private L: any;
  private map: any;
  private userMarker: any;
  private mechMarker: any;
  private routeLayer: any;
  private streamSub?: Subscription;
  private routeFetched = false;
  private animationFrame?: number;
  private lastStatus: TrackingStatus | null = null;

  async ngAfterViewInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.isOnline.set(window.navigator.onLine);
      window.addEventListener('online', this.handleOnline);
      window.addEventListener('offline', this.handleOffline);

      try {
        this.L = await import('leaflet');
        this.startTrackingStream();
      } catch (err) {
        console.error('Leaflet load error', err);
        this.errorMessage.set('Failed to load the map. Please refresh.');
        this.loading.set(false);
      }
    }
  }

  ngOnDestroy() {
    window.removeEventListener('online', this.handleOnline);
    window.removeEventListener('offline', this.handleOffline);
    this.streamSub?.unsubscribe();
    if (this.animationFrame) cancelAnimationFrame(this.animationFrame);
    this.map?.remove();
  }

  retry(): void {
    this.errorMessage.set(null);
    this.startTrackingStream();
  }

  private handleOnline = (): void => {
    this.isOnline.set(true);
    if (this.errorMessage()) {
      this.errorMessage.set('Back online. Resuming live tracking...');
      setTimeout(() => this.errorMessage.set(null), 2500);
      this.startTrackingStream();
    }
  };

  private handleOffline = (): void => {
    this.isOnline.set(false);
    this.errorMessage.set('You are offline. Check your connection and retry.');
    this.loading.set(false);
  };

  private startTrackingStream(): void {
    const requestId = this.route.snapshot.paramMap.get('requestId');
    if (!requestId) {
      this.errorMessage.set('Invalid request ID.');
      this.loading.set(false);
      return;
    }

    if (!this.isOnline()) {
      this.errorMessage.set('You are offline. Please reconnect to resume live tracking.');
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.streamSub?.unsubscribe();
    this.streamSub = this.trackingApi.getTrackingStream(requestId).subscribe({
      next: (data) => {
        this.trackingData.set(data);
        this.loading.set(false);
        this.notifyStatusChange(data.trackingStatus, requestId);
        setTimeout(() => {
          if (!this.map) this.initMap();
          this.updateMap(data);
        }, 0);
      },
      error: (err) => {
        console.error('Live tracking error', err);
        this.errorMessage.set(
          this.isOnline()
            ? 'Unable to connect to live tracking. Please try again.'
            : 'You are offline. Check your network and retry.'
        );
        this.loading.set(false);
      },
      complete: () => {
        this.toast.info('Live tracking ended', 'Your tracking session has ended.');
      },
    });
  }

  // ── Map Setup ──────────────────────────────────────────

  private initMap() {
    if (!document.getElementById('tracking-map')) return;

    this.map = this.L.map('tracking-map', {
      zoomControl: false,
      attributionControl: false,
      zoomSnap: 0.1,
      zoomDelta: 0.5,
      wheelPxPerZoomLevel: 120,
    }).setView([20.5937, 78.9629], 5);

    this.L.control.zoom({ position: 'bottomright' }).addTo(this.map);
    this.L.control.attribution({ position: 'bottomleft', prefix: false }).addTo(this.map);

    this.L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/">CARTO</a>',
      subdomains: 'abcd',
      maxZoom: 19,
    }).addTo(this.map);
  }

  // ── Map Updates ────────────────────────────────────────

  private updateMap(data: TrackingResponse) {
    if (!this.map || !this.L) return;

    const { userLat, userLng, mechanicLat, mechanicLng } = data;

    if (userLat && userLng) {
      if (!this.userMarker) {
        const userIcon = this.L.divIcon({
          className: 'custom-user-marker',
          html: `<div class="user-pin">
                   <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5a2.5 2.5 0 010-5 2.5 2.5 0 010 5z"/></svg>
                 </div>`,
          iconSize: [32, 32],
          iconAnchor: [16, 32],
        });
        this.userMarker = this.L.marker([userLat, userLng], { icon: userIcon }).addTo(this.map);
      }
    }

    if (mechanicLat && mechanicLng) {
      if (!this.mechMarker) {
        const mechIcon = this.L.divIcon({
          className: 'custom-mech-marker',
          html: `<div class="mech-pulse-wrapper">
                   <div class="mech-pulse"></div>
                   <div class="mech-pin">
                     <svg viewBox="0 0 24 24" fill="currentColor"><path d="M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-5.99zM6.5 16c-.83 0-1.5-.67-1.5-1.5S5.67 13 6.5 13s1.5.67 1.5 1.5S7.33 16 6.5 16zm11 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zM5 11l1.5-4.5h11L19 11H5z"/></svg>
                   </div>
                 </div>`,
          iconSize: [40, 40],
          iconAnchor: [20, 20],
        });
        this.mechMarker = this.L.marker([mechanicLat, mechanicLng], { icon: mechIcon }).addTo(this.map);
      } else {
        const bearing = this.computeBearing(this.mechMarker.getLatLng().lat, this.mechMarker.getLatLng().lng, mechanicLat, mechanicLng);
        this.animateMechanicMarker(mechanicLat, mechanicLng, bearing);
      }

      if (!this.routeFetched && userLat && userLng) {
        this.routeFetched = true;
        this.fetchRoute(userLat, userLng, mechanicLat, mechanicLng);
      }
    }
  }

  private animateMechanicMarker(targetLat: number, targetLng: number, bearing: number): void {
    if (!this.mechMarker) return;

    const start = this.mechMarker.getLatLng();
    const deltaLat = targetLat - start.lat;
    const deltaLng = targetLng - start.lng;
    const duration = 800;
    const startTime = performance.now();

    const ease = (t: number) => 1 - Math.pow(1 - t, 3);
    const step = (now: number) => {
      const elapsed = Math.min(now - startTime, duration);
      const progress = ease(elapsed / duration);
      this.mechMarker.setLatLng([start.lat + deltaLat * progress, start.lng + deltaLng * progress]);
      this.rotateMechanicPin(bearing);

      if (elapsed < duration) {
        this.animationFrame = requestAnimationFrame(step);
      } else {
        this.animationFrame = undefined;
      }
    };

    if (this.animationFrame) cancelAnimationFrame(this.animationFrame);
    this.animationFrame = requestAnimationFrame(step);
  }

  private rotateMechanicPin(degrees: number): void {
    const element = this.mechMarker?.getElement();
    const pin = element?.querySelector('.mech-pin') as HTMLElement | null;
    if (pin) {
      pin.style.transform = `rotate(${degrees}deg)`;
    }
  }

  private computeBearing(fromLat: number, fromLng: number, toLat: number, toLng: number): number {
    const lat1 = this.toRadians(fromLat);
    const lat2 = this.toRadians(toLat);
    const dLng = this.toRadians(toLng - fromLng);
    const y = Math.sin(dLng) * Math.cos(lat2);
    const x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng);
    return (this.toDegrees(Math.atan2(y, x)) + 360) % 360;
  }

  private toRadians(degrees: number): number {
    return (degrees * Math.PI) / 180;
  }

  private toDegrees(radians: number): number {
    return (radians * 180) / Math.PI;
  }

  private fetchRoute(uLat: number, uLng: number, mLat: number, mLng: number) {
    const url = `https://router.project-osrm.org/route/v1/driving/${mLng},${mLat};${uLng},${uLat}?overview=full&geometries=geojson`;
    this.http.get<any>(url).subscribe({
      next: (res) => {
        if (res.routes?.length > 0) {
          if (this.routeLayer) this.map.removeLayer(this.routeLayer);
          this.routeLayer = this.L.geoJSON(res.routes[0].geometry, {
            style: { color: '#8B1E1E', weight: 4, opacity: 0.7, dashArray: '8, 6' },
          }).addTo(this.map);
          this.map.fitBounds(this.routeLayer.getBounds(), { padding: [60, 60] });
        }
      },
      error: () => {
        this.routeLayer = this.L
          .polyline([[mLat, mLng], [uLat, uLng]], {
            color: '#8B1E1E', weight: 4, opacity: 0.6, dashArray: '8, 6',
          })
          .addTo(this.map);
      },
    });
  }

  private notifyStatusChange(status: TrackingStatus, requestId: string): void {
    if (this.lastStatus === status) return;
    this.lastStatus = status;

    switch (status) {
      case 'ASSIGNED':
        this.notificationService.notifyMechanicAssigned(requestId);
        break;
      case 'ON_THE_WAY':
      case 'ARRIVED':
      case 'IN_SERVICE':
        this.notificationService.notifyMechanicArriving(requestId);
        break;
      case 'COMPLETED':
        this.notificationService.notifyServiceCompleted(requestId);
        break;
    }
  }

  // ── Template Helpers ───────────────────────────────────

  getStatusClass(status: string): string {
    switch (status) {
      case 'PENDING': return 'badge-pending';
      case 'ASSIGNED': return 'badge-accepted';
      case 'ON_THE_WAY':
      case 'ARRIVED':
      case 'IN_SERVICE': return 'badge-progress';
      case 'COMPLETED': return 'badge-completed';
      default: return 'badge-neutral';
    }
  }

  formatStatus(status: string): string {
    return status.replace(/_/g, ' ');
  }

  isStepDone(currentStatus: TrackingStatus, step: TrackingStatus): boolean {
    const order: TrackingStatus[] = ['PENDING', 'ASSIGNED', 'ON_THE_WAY', 'ARRIVED', 'IN_SERVICE', 'COMPLETED'];
    return order.indexOf(currentStatus) >= order.indexOf(step);
  }

  isStepActive(currentStatus: TrackingStatus, step: TrackingStatus): boolean {
    return currentStatus === step;
  }
}
