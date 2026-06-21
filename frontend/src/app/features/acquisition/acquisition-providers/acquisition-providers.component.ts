import {Component, computed, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize} from 'rxjs';
import {FormsModule} from '@angular/forms';
import {TableModule} from 'primeng/table';
import {Button} from 'primeng/button';
import {Checkbox} from 'primeng/checkbox';
import {Select} from 'primeng/select';
import {InputText} from 'primeng/inputtext';
import {Password} from 'primeng/password';
import {Tag} from 'primeng/tag';
import {Tooltip} from 'primeng/tooltip';
import {ProgressSpinner} from 'primeng/progressspinner';
import {MessageService} from 'primeng/api';
import {TranslocoDirective, TranslocoService} from '@jsverse/transloco';
import {AcquisitionService} from '../acquisition.service';
import {UserService} from '../../settings/user-management/user.service';
import {AcquisitionMode, AcquisitionProviderConfig, AcquisitionProviderRequest, AvailableAcquisitionProvider} from '../acquisition.model';

interface ProviderForm {
  id: number | null;
  providerKey: string;
  name: string;
  enabled: boolean;
  baseUrl: string;
  apiToken: string;
  mode: AcquisitionMode;
  allowedContentTypesText: string;
  tokenSet: boolean;
}

@Component({
  selector: 'app-acquisition-providers',
  imports: [FormsModule, TableModule, Button, Checkbox, Select, InputText, Password, Tag, Tooltip, ProgressSpinner, TranslocoDirective],
  templateUrl: './acquisition-providers.component.html',
  styleUrl: './acquisition-providers.component.scss',
})
export class AcquisitionProvidersComponent implements OnInit {
  private service = inject(AcquisitionService);
  private userService = inject(UserService);
  private messageService = inject(MessageService);
  private t = inject(TranslocoService);
  private destroyRef = inject(DestroyRef);

  readonly providers = signal<AcquisitionProviderConfig[]>([]);
  readonly available = signal<AvailableAcquisitionProvider[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);

  // Held as a plain field (not a signal) so PrimeNG ngModel two-way binding writes back cleanly.
  formModel: ProviderForm | null = null;

  readonly isAdmin = computed(() => !!this.userService.currentUser()?.permissions?.admin);

  readonly modeOptions: {label: string; value: AcquisitionMode}[] = [
    {label: this.t.translate('settingsAcquisition.mode.request'), value: 'REQUEST'},
    {label: this.t.translate('settingsAcquisition.mode.directDownload'), value: 'DIRECT_DOWNLOAD'},
  ];

  /** Providers available in this build that do not yet have a config row. */
  readonly creatableProviders = computed(() =>
    this.available().filter(a => !a.configured).map(a => ({label: a.displayName, value: a.providerKey})));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.service.getAvailableProviders().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: providers => this.available.set(providers),
      error: () => this.available.set([]),
    });
    this.service.getProviders().pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: providers => this.providers.set(providers),
      error: () => {
        this.providers.set([]);
        this.toastError('settingsAcquisition.loadError');
      },
    });
  }

  startCreate(): void {
    const first = this.creatableProviders()[0];
    this.formModel = {
      id: null,
      providerKey: first ? first.value : '',
      name: first ? first.label : '',
      enabled: false,
      baseUrl: '',
      apiToken: '',
      mode: 'REQUEST',
      allowedContentTypesText: '',
      tokenSet: false,
    };
  }

  startEdit(provider: AcquisitionProviderConfig): void {
    this.formModel = {
      id: provider.id,
      providerKey: provider.providerKey,
      name: provider.name,
      enabled: provider.enabled,
      baseUrl: provider.baseUrl ?? '',
      apiToken: '',
      mode: provider.mode,
      allowedContentTypesText: (provider.allowedContentTypes ?? []).join(', '),
      tokenSet: provider.tokenSet,
    };
  }

  onProviderKeySelected(key: string): void {
    if (!this.formModel) {
      return;
    }
    const match = this.available().find(a => a.providerKey === key);
    this.formModel.providerKey = key;
    if (!this.formModel.name.trim()) {
      this.formModel.name = match?.displayName ?? key;
    }
  }

  cancel(): void {
    this.formModel = null;
  }

  save(): void {
    const f = this.formModel;
    if (!f || !f.providerKey || !f.name.trim()) {
      this.toastError('settingsAcquisition.validation.required');
      return;
    }
    const payload: AcquisitionProviderRequest = {
      providerKey: f.providerKey,
      name: f.name.trim(),
      enabled: f.enabled,
      baseUrl: f.baseUrl.trim(),
      mode: f.mode,
      allowedContentTypes: this.parseContentTypes(f.allowedContentTypesText),
    };
    // On edit, only send a token when the admin typed one (blank keeps the stored token).
    if (f.id === null) {
      payload.apiToken = f.apiToken.trim();
    } else if (f.apiToken.trim().length > 0) {
      payload.apiToken = f.apiToken.trim();
    }

    this.saving.set(true);
    const request$ = f.id === null
      ? this.service.createProvider(payload)
      : this.service.updateProvider(f.id, payload);
    request$.pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.saving.set(false)),
    ).subscribe({
      next: () => {
        this.toastSuccess(f.id === null ? 'settingsAcquisition.createSuccess' : 'settingsAcquisition.updateSuccess');
        this.formModel = null;
        this.load();
      },
      error: () => this.toastError('settingsAcquisition.saveError'),
    });
  }

  toggleEnabled(provider: AcquisitionProviderConfig): void {
    const payload: AcquisitionProviderRequest = {
      providerKey: provider.providerKey,
      name: provider.name,
      enabled: !provider.enabled,
    };
    this.service.updateProvider(provider.id, payload).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.toastSuccess('settingsAcquisition.updateSuccess');
        this.load();
      },
      error: () => this.toastError('settingsAcquisition.saveError'),
    });
  }

  deleteProvider(provider: AcquisitionProviderConfig): void {
    if (!confirm(this.t.translate('settingsAcquisition.deleteConfirm', {name: provider.name}))) {
      return;
    }
    this.service.deleteProvider(provider.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.toastSuccess('settingsAcquisition.deleteSuccess');
        this.load();
      },
      error: () => this.toastError('settingsAcquisition.deleteError'),
    });
  }

  private parseContentTypes(text: string): string[] {
    return text
      .split(',')
      .map(part => part.trim().toUpperCase())
      .filter(part => part.length > 0);
  }

  private toastSuccess(key: string): void {
    this.messageService.add({severity: 'success', summary: this.t.translate('common.success'), detail: this.t.translate(key)});
  }

  private toastError(key: string): void {
    this.messageService.add({severity: 'error', summary: this.t.translate('common.error'), detail: this.t.translate(key)});
  }
}
