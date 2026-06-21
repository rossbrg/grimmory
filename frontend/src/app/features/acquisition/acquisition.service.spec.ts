import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {TestBed} from '@angular/core/testing';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import {API_CONFIG} from '../../core/config/api-config';
import {AcquisitionService} from './acquisition.service';
import {AcquisitionProviderRequest, AcquisitionTriggerRequest} from './acquisition.model';

describe('AcquisitionService', () => {
  let service: AcquisitionService;
  let http: HttpTestingController;
  const base = `${API_CONFIG.BASE_URL}/api/v1/acquisition`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), AcquisitionService],
    });
    service = TestBed.inject(AcquisitionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  it('lists configured providers', () => {
    service.getProviders().subscribe();
    const req = http.expectOne(`${base}/providers`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('lists available provider implementations', () => {
    service.getAvailableProviders().subscribe();
    const req = http.expectOne(`${base}/providers/available`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('creates a provider', () => {
    const payload: AcquisitionProviderRequest = {providerKey: 'shelfmark', name: 'Shelfmark', enabled: false};
    service.createProvider(payload).subscribe();
    const req = http.expectOne(`${base}/providers`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({id: 1, providerKey: 'shelfmark', name: 'Shelfmark', enabled: false, mode: 'REQUEST', allowedContentTypes: [], tokenSet: false});
  });

  it('updates a provider', () => {
    const payload: AcquisitionProviderRequest = {providerKey: 'shelfmark', name: 'Shelfmark', enabled: true};
    service.updateProvider(7, payload).subscribe();
    const req = http.expectOne(`${base}/providers/7`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('deletes a provider', () => {
    service.deleteProvider(7).subscribe();
    const req = http.expectOne(`${base}/providers/7`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('searches with the q query parameter', () => {
    service.search('dune').subscribe();
    const req = http.expectOne(r => r.url === `${base}/search`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('q')).toBe('dune');
    req.flush({query: 'dune', candidates: []});
  });

  it('triggers an acquisition', () => {
    const payload: AcquisitionTriggerRequest = {providerKey: 'shelfmark', externalId: 'a1', directDownloadAvailable: true};
    service.trigger(payload).subscribe();
    const req = http.expectOne(`${base}/requests`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({id: 1, providerKey: 'shelfmark', externalId: 'a1', mode: 'DIRECT_DOWNLOAD', status: 'QUEUED'});
  });

  it('lists acquisition requests', () => {
    service.getRequests().subscribe();
    const req = http.expectOne(`${base}/requests`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('gets a single acquisition request', () => {
    service.getRequest(3).subscribe();
    const req = http.expectOne(`${base}/requests/3`);
    expect(req.request.method).toBe('GET');
    req.flush({id: 3, providerKey: 'shelfmark', externalId: 'a1', mode: 'REQUEST', status: 'IMPORTED'});
  });
});
