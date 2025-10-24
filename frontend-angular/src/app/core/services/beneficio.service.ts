import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Beneficio, BeneficioCreateDto, BeneficioUpdateDto } from '../models/beneficio.model';
import { TransferRequest } from '../models/transfer-request.model';
import { API_CONFIG } from '../constants/api.constants';

@Injectable({
  providedIn: 'root'
})
export class BeneficioService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = API_CONFIG.BASE_URL + API_CONFIG.ENDPOINTS.BENEFICIOS;

  findAll(): Observable<Beneficio[]> {
    return this.http.get<Beneficio[]>(this.baseUrl);
  }

  findById(id: number): Observable<Beneficio> {
    return this.http.get<Beneficio>(`${this.baseUrl}/${id}`);
  }

  create(dto: BeneficioCreateDto): Observable<Beneficio> {
    return this.http.post<Beneficio>(this.baseUrl, dto);
  }

  update(id: number, dto: BeneficioUpdateDto): Observable<Beneficio> {
    return this.http.put<Beneficio>(`${this.baseUrl}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  transfer(request: TransferRequest): Observable<void> {
    const transferUrl = API_CONFIG.BASE_URL + API_CONFIG.ENDPOINTS.TRANSFER;
    return this.http.post<void>(transferUrl, request);
  }
}
