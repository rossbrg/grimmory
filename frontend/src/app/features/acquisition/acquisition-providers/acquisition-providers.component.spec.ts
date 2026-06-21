import {TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {of} from 'rxjs';
import {MessageService} from 'primeng/api';
import {TranslocoService} from '@jsverse/transloco';

import {AcquisitionProvidersComponent} from './acquisition-providers.component';
import {AcquisitionService} from '../acquisition.service';
import {UserService} from '../../settings/user-management/user.service';
import {AcquisitionProviderConfig} from '../acquisition.model';

function providerConfig(overrides: Partial<AcquisitionProviderConfig> = {}): AcquisitionProviderConfig {
  return {
    id: 1,
    providerKey: 'shelfmark',
    name: 'Shelfmark',
    enabled: false,
    baseUrl: 'https://shelf.example.com',
    mode: 'REQUEST',
    allowedContentTypes: ['EPUB'],
    tokenSet: true,
    ...overrides,
  };
}

describe('AcquisitionProvidersComponent', () => {
  const service = {
    getProviders: vi.fn(),
    getAvailableProviders: vi.fn(),
    createProvider: vi.fn(),
    updateProvider: vi.fn(),
    deleteProvider: vi.fn(),
  };
  const messageService = {add: vi.fn()};
  const translocoService = {translate: vi.fn((key: string) => key)};
  const currentUser = signal<{permissions: {admin: boolean}} | null>({permissions: {admin: true}});
  const userService = {currentUser};

  let component: AcquisitionProvidersComponent;

  beforeEach(() => {
    vi.restoreAllMocks();
    Object.values(service).forEach(fn => fn.mockReset());
    messageService.add.mockReset();
    service.getProviders.mockReturnValue(of([]));
    service.getAvailableProviders.mockReturnValue(of([]));
    service.createProvider.mockReturnValue(of(providerConfig()));
    service.updateProvider.mockReturnValue(of(providerConfig()));
    service.deleteProvider.mockReturnValue(of(undefined));

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        {provide: AcquisitionService, useValue: service},
        {provide: UserService, useValue: userService},
        {provide: MessageService, useValue: messageService},
        {provide: TranslocoService, useValue: translocoService},
      ],
    });
    component = TestBed.runInInjectionContext(() => new AcquisitionProvidersComponent());
  });

  afterEach(() => TestBed.resetTestingModule());

  it('lists only unconfigured providers as creatable', () => {
    component.available.set([
      {providerKey: 'shelfmark', displayName: 'Shelfmark', configured: true},
      {providerKey: 'other', displayName: 'Other', configured: false},
    ]);
    expect(component.creatableProviders()).toEqual([{label: 'Other', value: 'other'}]);
  });

  it('startEdit prefills the form and blanks the token field', () => {
    component.startEdit(providerConfig({name: 'My Shelf', allowedContentTypes: ['EPUB', 'PDF']}));
    expect(component.formModel?.id).toBe(1);
    expect(component.formModel?.name).toBe('My Shelf');
    expect(component.formModel?.apiToken).toBe('');
    expect(component.formModel?.allowedContentTypesText).toBe('EPUB, PDF');
    expect(component.formModel?.tokenSet).toBe(true);
  });

  it('creates a provider with parsed content types and the typed token', () => {
    component.formModel = {
      id: null, providerKey: 'shelfmark', name: 'Shelfmark', enabled: true,
      baseUrl: 'https://shelf.example.com', apiToken: 'tok', mode: 'REQUEST',
      allowedContentTypesText: 'epub, pdf', tokenSet: false,
    };
    component.save();
    expect(service.createProvider).toHaveBeenCalledWith({
      providerKey: 'shelfmark', name: 'Shelfmark', enabled: true,
      baseUrl: 'https://shelf.example.com', mode: 'REQUEST',
      allowedContentTypes: ['EPUB', 'PDF'], apiToken: 'tok',
    });
    expect(component.formModel).toBeNull();
  });

  it('omits the token on edit when left blank (keeps stored token)', () => {
    component.formModel = {
      id: 9, providerKey: 'shelfmark', name: 'Shelfmark', enabled: false,
      baseUrl: '', apiToken: '   ', mode: 'REQUEST', allowedContentTypesText: '', tokenSet: true,
    };
    component.save();
    const payload = service.updateProvider.mock.calls[0][1];
    expect(service.updateProvider).toHaveBeenCalled();
    expect(payload.apiToken).toBeUndefined();
  });

  it('sends the token on edit when a value is typed', () => {
    component.formModel = {
      id: 9, providerKey: 'shelfmark', name: 'Shelfmark', enabled: false,
      baseUrl: '', apiToken: 'fresh', mode: 'REQUEST', allowedContentTypesText: '', tokenSet: true,
    };
    component.save();
    expect(service.updateProvider.mock.calls[0][1].apiToken).toBe('fresh');
  });

  it('toggleEnabled flips the enabled flag', () => {
    component.toggleEnabled(providerConfig({id: 4, enabled: false}));
    expect(service.updateProvider).toHaveBeenCalledWith(4, {providerKey: 'shelfmark', name: 'Shelfmark', enabled: true});
  });

  it('deleteProvider only deletes after confirmation', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    component.deleteProvider(providerConfig({id: 2}));
    expect(service.deleteProvider).not.toHaveBeenCalled();

    vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.deleteProvider(providerConfig({id: 2}));
    expect(service.deleteProvider).toHaveBeenCalledWith(2);
  });
});
