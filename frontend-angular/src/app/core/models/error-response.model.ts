export interface ErrorResponse {
  status: number;
  error: string;
  message: string;
  timestamp: string;
  errors?: { [field: string]: string };
}
