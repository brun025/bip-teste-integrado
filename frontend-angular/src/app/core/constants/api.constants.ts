import { environment } from "../environments/environment.development";

export const API_CONFIG = {
  BASE_URL: environment.apiUrl,
  ENDPOINTS: {
    BENEFICIOS: '/api/v1/beneficios',
    TRANSFER: '/api/v1/beneficios/transfer'
  }
} as const;
