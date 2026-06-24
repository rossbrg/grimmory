import {TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {of} from 'rxjs';
import {MessageService} from 'primeng/api';
import {TranslocoService} from '@jsverse/transloco';

import {AcquisitionSearchComponent} from './acquisition-search.component';
import {AcquisitionService} from '../acquisition.service';
import {UserService} from '../../settings/user-management/user.service';
import {AcquisitionCandidate, AcquisitionRequest} from '../acquisition.model';

function candidate(overrides: Partial<AcquisitionCandidate> = {}): AcquisitionCandidate {
  return {providerKey: 'shelfmark', externalId: 'a1', title: 'Dune', author: 'Herbert', format: 'EPUB', directDownloadAvailable: true, ...overrides};
}

describe('AcquisitionSearchComponent', () => {
  const service = {
    search: vi.fn(),
    trigger: vi.fn(),
    getRequests: vi.fn(),
  };
  const messageService = {add: vi.fn()};
  const translocoService = {translate: vi.fn((key: string) => key)};
  const currentUser = signal<{permissions: {canDownload: boolean; admin: boolean}} | null>(null);
  const userService = {currentUser};

  function build(): AcquisitionSearchComponent {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        {provide: AcquisitionService, useValue: service},
        {provide: UserService, useValue: userService},
        {provide: MessageService, useValue: messageService},
        {provide: TranslocoService, useValue: translocoService},
      ],
    });
    return TestBed.runInInjectionContext(() => new AcquisitionSearchComponent());
  }

  beforeEach(() => {
    vi.restoreAllMocks();
    Object.values(service).forEach(fn => fn.mockReset());
    messageService.add.mockReset();
    service.getRequests.mockReturnValue(of([]));
    service.trigger.mockReturnValue(of({id: 1, providerKey: 'shelfmark', externalId: 'a1', mode: 'REQUEST', status: 'QUEUED'} as AcquisitionRequest));
  });

  afterEach(() => TestBed.resetTestingModule());

  it('gates acquisition on download or admin permission', () => {
    currentUser.set(null);
    expect(build().canAcquire()).toBe(false);

    currentUser.set({permissions: {canDownload: true, admin: false}});
    expect(build().canAcquire()).toBe(true);

    currentUser.set({permissions: {canDownload: false, admin: true}});
    expect(build().canAcquire()).toBe(true);

    currentUser.set({permissions: {canDownload: false, admin: false}});
    expect(build().canAcquire()).toBe(false);
  });

  it('maps lifecycle status to a tag severity', () => {
    const c = build();
    expect(c.statusSeverity('QUEUED')).toBe('info');
    expect(c.statusSeverity('DOWNLOADING')).toBe('warn');
    expect(c.statusSeverity('IMPORTED')).toBe('success');
    expect(c.statusSeverity('FAILED')).toBe('danger');
  });

  it('acquire triggers with the candidate snapshot and reports success', () => {
    const c = build();
    c.acquire(candidate());
    expect(service.trigger).toHaveBeenCalledWith({
      providerKey: 'shelfmark', externalId: 'a1', title: 'Dune', author: 'Herbert', format: 'EPUB', directDownloadAvailable: true,
    });
    expect(messageService.add).toHaveBeenCalledWith(expect.objectContaining({severity: 'success'}));
    expect(service.getRequests).toHaveBeenCalled();
  });

  it('acquire reports an error severity when the request comes back FAILED', () => {
    service.trigger.mockReturnValue(of({id: 2, providerKey: 'shelfmark', externalId: 'a1', mode: 'REQUEST', status: 'FAILED'} as AcquisitionRequest));
    const c = build();
    c.acquire(candidate());
    expect(messageService.add).toHaveBeenCalledWith(expect.objectContaining({severity: 'error'}));
  });

  it('keeps only the five most recent requests', () => {
    const many: AcquisitionRequest[] = Array.from({length: 8}, (_, i) => ({
      id: i, providerKey: 'shelfmark', externalId: `a${i}`, mode: 'REQUEST', status: 'QUEUED',
    }));
    service.getRequests.mockReturnValue(of(many));
    const c = build();
    c.refreshRecentRequests();
    expect(c.recentRequests().length).toBe(5);
  });
});
