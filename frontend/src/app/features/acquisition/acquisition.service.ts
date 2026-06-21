import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {API_CONFIG} from '../../core/config/api-config';
import {
  AcquisitionProviderConfig,
  AcquisitionProviderRequest,
  AcquisitionRequest,
  AcquisitionSearchResponse,
  AcquisitionTriggerRequest,
  AvailableAcquisitionProvider,
} from './acquisition.model';

/**
 * Client for the backend acquisition API. Provider configuration is admin-only; search and trigger
 * are download/admin gated server-side. This service does no permission checks itself.
 */
@Injectable({providedIn: 'root'})
export class AcquisitionService {

  private readonly apiUrl = `${API_CONFIG.BASE_URL}/api/v1/acquisition`;
  private http = inject(HttpClient);

  getProviders(): Observable<AcquisitionProviderConfig[]> {
    return this.http.get<AcquisitionProviderConfig[]>(`${this.apiUrl}/providers`);
  }

  getAvailableProviders(): Observable<AvailableAcquisitionProvider[]> {
    return this.http.get<AvailableAcquisitionProvider[]>(`${this.apiUrl}/providers/available`);
  }

  createProvider(request: AcquisitionProviderRequest): Observable<AcquisitionProviderConfig> {
    return this.http.post<AcquisitionProviderConfig>(`${this.apiUrl}/providers`, request);
  }

  updateProvider(id: number, request: AcquisitionProviderRequest): Observable<AcquisitionProviderConfig> {
    return this.http.put<AcquisitionProviderConfig>(`${this.apiUrl}/providers/${id}`, request);
  }

  deleteProvider(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/providers/${id}`);
  }

  search(query: string): Observable<AcquisitionSearchResponse> {
    const params = new HttpParams().set('q', query);
    return this.http.get<AcquisitionSearchResponse>(`${this.apiUrl}/search`, {params});
  }

  trigger(request: AcquisitionTriggerRequest): Observable<AcquisitionRequest> {
    return this.http.post<AcquisitionRequest>(`${this.apiUrl}/requests`, request);
  }

  getRequests(): Observable<AcquisitionRequest[]> {
    return this.http.get<AcquisitionRequest[]>(`${this.apiUrl}/requests`);
  }

  getRequest(id: number): Observable<AcquisitionRequest> {
    return this.http.get<AcquisitionRequest>(`${this.apiUrl}/requests/${id}`);
  }
}
