export interface ApiResponse<T> {
  success: boolean;
  message: string;
  timestamp: string;
  correlationId?: string;
  data: T;
}
