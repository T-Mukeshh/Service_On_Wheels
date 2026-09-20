import { DatePipe, TitleCasePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import type { ServiceRequestResponse } from '../../models/service-request.models';
import { PaymentService } from '../../services/payment.service';

const STATUS_ORDER = ['PENDING', 'ASSIGNED', 'ON_THE_WAY', 'ARRIVED', 'IN_SERVICE', 'COMPLETED'];

@Component({
  selector: 'app-my-requests',
  imports: [RouterLink, DatePipe, TitleCasePipe],
  templateUrl: './my-requests.html',
  styleUrl: './my-requests.css',
})
export class MyRequests implements OnInit {
  private readonly api = inject(ServiceRequestService);
  private readonly paymentService = inject(PaymentService);
  private readonly toast = inject(ToastService);

  readonly requests = signal<ServiceRequestResponse[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly currentFilter = signal<string>('All');
  readonly payingId = signal<string | null>(null);

  readonly filteredRequests = computed(() => {
    const filter = this.currentFilter();
    if (filter === 'All') return this.requests();
    if (filter === 'Pending') return this.requests().filter(r =>
      ['PENDING','ACCEPTED','IN_PROGRESS','TOW_REQUIRED','ASSIGNED','ON_THE_WAY','ARRIVED','IN_SERVICE'].includes(r.status)
    );
    if (filter === 'Completed') return this.requests().filter(r => r.status === 'COMPLETED');
    return this.requests();
  });

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.api.getMyRequests().subscribe({
      next: (rows) => {
        this.requests.set(rows);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        const msg = extractHttpMessage(err);
        this.errorMessage.set(msg);
        this.toast.error('Failed to load requests', msg);
      },
    });
  }

  setFilter(filter: string): void { this.currentFilter.set(filter); }

  getActiveCount(): number {
    return this.requests().filter(r =>
      ['PENDING','ACCEPTED','IN_PROGRESS','TOW_REQUIRED','ASSIGNED','ON_THE_WAY','ARRIVED','IN_SERVICE'].includes(r.status)
    ).length;
  }

  getCompletedCount(): number {
    return this.requests().filter(r => r.status === 'COMPLETED').length;
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PENDING': return 'badge-pending';
      case 'ACCEPTED': 
      case 'ASSIGNED': return 'badge-accepted';
      case 'IN_PROGRESS':
      case 'ON_THE_WAY':
      case 'ARRIVED':
      case 'IN_SERVICE': return 'badge-progress';
      case 'COMPLETED': return 'badge-completed';
      case 'CANCELLED': return 'badge-cancelled';
      case 'TOW_REQUIRED': return 'badge-tow';
      default: return 'badge-neutral';
    }
  }

  getDotClass(status: string): string {
    switch (status) {
      case 'PENDING': return 'dot-pending';
      case 'ACCEPTED':
      case 'ASSIGNED': return 'dot-accepted';
      case 'IN_PROGRESS':
      case 'ON_THE_WAY':
      case 'ARRIVED':
      case 'IN_SERVICE': return 'dot-progress';
      case 'COMPLETED': return 'dot-completed';
      case 'CANCELLED': return 'dot-cancelled';
      default: return 'dot-cancelled';
    }
  }

  formatStatus(status: string): string {
    return status.replace(/_/g, ' ');
  }

  isStepDone(currentStatus: string, stepStatus: string): boolean {
    const currentIndex = STATUS_ORDER.indexOf(currentStatus);
    const stepIndex = STATUS_ORDER.indexOf(stepStatus);
    if (currentIndex === -1) return false;
    return stepIndex <= currentIndex;
  }

  pay(request: ServiceRequestResponse): void {
    this.payingId.set(request.id);
    this.paymentService.createOrder(request.id).subscribe({
      next: (order) => {
        const options = {
          key: order.keyId,
          amount: order.amount,
          currency: order.currency,
          name: 'Service On Wheels',
          description: `Payment for request ${request.id}`,
          order_id: order.orderId,
          handler: (response: any) => {
            this.verifyPayment(response.razorpay_order_id, response.razorpay_payment_id, response.razorpay_signature);
          },
          prefill: {
            name: 'Customer', // Would be better to pass real user name, but we don't have it in the request payload easily
            email: 'customer@example.com',
            contact: ''
          },
          theme: {
            color: '#8B1E1E'
          },
          modal: {
             ondismiss: () => {
                 this.payingId.set(null);
             }
          }
        };
        const rzp = new window.Razorpay(options);
        rzp.open();
      },
      error: (err) => {
        this.payingId.set(null);
        this.toast.error('Payment Error', extractHttpMessage(err));
      }
    });
  }

  private verifyPayment(orderId: string, paymentId: string, signature: string): void {
    this.paymentService.verifyPayment({
      razorpayOrderId: orderId,
      razorpayPaymentId: paymentId,
      razorpaySignature: signature
    }).subscribe({
      next: (res) => {
        this.payingId.set(null);
        this.toast.success('Payment Successful', 'Thank you for your payment!');
        this.load(); // Reload to update status
      },
      error: (err) => {
        this.payingId.set(null);
        this.toast.error('Verification Error', extractHttpMessage(err));
      }
    });
  }
}

function extractHttpMessage(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    const body = err.error as { message?: string } | undefined;
    return body?.message ?? err.message ?? 'Could not load requests';
  }
  return 'Something went wrong';
}
