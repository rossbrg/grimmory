export type AcquisitionMode = 'DIRECT_DOWNLOAD' | 'REQUEST';

export type AcquisitionStatus = 'QUEUED' | 'DOWNLOADING' | 'IMPORTED' | 'FAILED';

/** Admin-facing view of a configured provider. The API token is never returned, only {@link tokenSet}. */
export interface AcquisitionProviderConfig {
  id: number;
  providerKey: string;
  name: string;
  enabled: boolean;
  baseUrl?: string;
  mode: AcquisitionMode;
  allowedContentTypes: string[];
  tokenSet: boolean;
  createdAt?: string;
  updatedAt?: string;
}

/** Create/update payload. A null/undefined apiToken keeps the stored token; a blank string clears it. */
export interface AcquisitionProviderRequest {
  providerKey: string;
  name: string;
  enabled?: boolean;
  baseUrl?: string;
  apiToken?: string | null;
  mode?: AcquisitionMode;
  allowedContentTypes?: string[];
}

/** A provider implementation available in this build (from the backend registry). */
export interface AvailableAcquisitionProvider {
  providerKey: string;
  displayName: string;
  configured: boolean;
}

/** An external acquisition candidate, kept separate from local library results. */
export interface AcquisitionCandidate {
  providerKey: string;
  externalId: string;
  title?: string;
  author?: string;
  format?: string;
  language?: string;
  sizeBytes?: number;
  year?: number;
  coverUrl?: string;
  description?: string;
  directDownloadAvailable: boolean;
}

export interface AcquisitionSearchResponse {
  query: string;
  candidates: AcquisitionCandidate[];
}

export interface AcquisitionTriggerRequest {
  providerKey: string;
  externalId: string;
  title?: string;
  author?: string;
  format?: string;
  directDownloadAvailable?: boolean;
  mode?: AcquisitionMode;
}

export interface AcquisitionRequest {
  id: number;
  providerKey: string;
  externalId: string;
  title?: string;
  author?: string;
  format?: string;
  mode: AcquisitionMode;
  status: AcquisitionStatus;
  errorMessage?: string;
  resultBookId?: number;
  requestedByUserId?: number;
  requestedByUsername?: string;
  createdAt?: string;
  updatedAt?: string;
}
