export type RequestStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'TOW_REQUIRED'
  // Phase 4 additions
  | 'ASSIGNED'
  | 'ON_THE_WAY'
  | 'ARRIVED'
  | 'IN_SERVICE';

export interface CreateServiceRequestPayload {
  vehicleType: string;
  vehicleNumber: string;
  problemDescription: string;
  selectedIssue?: string;
  additionalNotes?: string;
  latitude: number;
  longitude: number;
  address: string;
}

export interface ServiceRequestResponse {
  id: string;
  userId: string;
  vehicleType: string;
  vehicleNumber: string;
  problemDescription: string;
  selectedIssue?: string;
  additionalNotes?: string;
  latitude: number;
  longitude: number;
  address: string;
  status: RequestStatus;
  assignedMechanicId?: string;
  createdAt: string;

  // Phase 4 - Mechanic details
  mechanicName?: string;
  mechanicPhone?: string;
  mechanicVehicle?: string;
  mechanicRating?: number;

  // Phase 4 - Audit timestamps
  assignedAt?: string;
  arrivedAt?: string;
  serviceStartedAt?: string;
  completedAt?: string;
  cancelledAt?: string;

  // Phase 5 - Payment
  paymentStatus?: 'CREATED' | 'AUTHORIZED' | 'CAPTURED' | 'FAILED' | 'REFUNDED';
}
