import {Component, computed, DestroyRef, effect, inject, input, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize} from 'rxjs';
import {Button} from 'primeng/button';
import {Tag} from 'primeng/tag';
import {ProgressSpinner} from 'primeng/progressspinner';
import {MessageService} from 'primeng/api';
import {TranslocoDirective, TranslocoService} from '@jsverse/transloco';
import {AcquisitionService} from '../acquisition.service';
import {UserService} from '../../settings/user-management/user.service';
import {AcquisitionCandidate, AcquisitionRequest, AcquisitionStatus} from '../acquisition.model';

/**
 * The "not in library" affordance shown when a search has no local match. Permitted users (download
 * or admin) can fetch external candidates from enabled providers and trigger an acquisition; the
 * resulting request status is surfaced inline.
 */
@Component({
  selector: 'app-acquisition-search',
  imports: [Button, Tag, ProgressSpinner, TranslocoDirective],
  templateUrl: './acquisition-search.component.html',
  styleUrl: './acquisition-search.component.scss',
})
export class AcquisitionSearchComponent implements OnInit {
  readonly query = input.required<string>();

  private service = inject(AcquisitionService);
  private userService = inject(UserService);
  private messageService = inject(MessageService);
  private t = inject(TranslocoService);
  private destroyRef = inject(DestroyRef);

  readonly canAcquire = computed(() => {
    const permissions = this.userService.currentUser()?.permissions;
    return !!(permissions?.canDownload || permissions?.admin);
  });

  readonly searching = signal(false);
  readonly searched = signal(false);
  readonly candidates = signal<AcquisitionCandidate[]>([]);
  readonly triggeringId = signal<string | null>(null);
  readonly recentRequests = signal<AcquisitionRequest[]>([]);

  constructor() {
    // Reset candidate state whenever the query changes so stale results are never shown.
    effect(() => {
      this.query();
      this.searched.set(false);
      this.candidates.set([]);
    });
  }

  ngOnInit(): void {
    if (this.canAcquire()) {
      this.refreshRecentRequests();
    }
  }

  findDownloadOptions(): void {
    const query = this.query().trim();
    if (!query) {
      return;
    }
    this.searching.set(true);
    this.service.search(query).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => {
        this.searching.set(false);
        this.searched.set(true);
      }),
    ).subscribe({
      next: response => this.candidates.set(response.candidates ?? []),
      error: () => {
        this.candidates.set([]);
        this.toastError('acquisition.searchError');
      },
    });
  }

  acquire(candidate: AcquisitionCandidate): void {
    this.triggeringId.set(candidate.externalId);
    this.service.trigger({
      providerKey: candidate.providerKey,
      externalId: candidate.externalId,
      title: candidate.title,
      author: candidate.author,
      format: candidate.format,
      directDownloadAvailable: candidate.directDownloadAvailable,
    }).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.triggeringId.set(null)),
    ).subscribe({
      next: request => {
        this.messageService.add({
          severity: request.status === 'FAILED' ? 'error' : 'success',
          summary: this.t.translate('acquisition.triggered'),
          detail: this.t.translate('acquisition.status.' + request.status),
        });
        this.refreshRecentRequests();
      },
      error: () => this.toastError('acquisition.triggerError'),
    });
  }

  refreshRecentRequests(): void {
    this.service.getRequests().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: requests => this.recentRequests.set(requests.slice(0, 5)),
      error: () => this.recentRequests.set([]),
    });
  }

  statusSeverity(status: AcquisitionStatus): 'info' | 'warn' | 'success' | 'danger' {
    switch (status) {
      case 'IMPORTED':
        return 'success';
      case 'DOWNLOADING':
        return 'warn';
      case 'FAILED':
        return 'danger';
      default:
        return 'info';
    }
  }

  private toastError(key: string): void {
    this.messageService.add({severity: 'error', summary: this.t.translate('common.error'), detail: this.t.translate(key)});
  }
}
