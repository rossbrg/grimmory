# Acquisition Providers

Integrated missing-book acquisition: when a search has no local match, permitted users can find and
acquire the book from an external provider. The design is a generic seam — **Shelfmark** is the first
provider, but nothing about Shelfmark is hard-coded into the core.

> Status: developed and tested on the local fork. **Not** submitted upstream. See
> [AGPL & upstreaming](#agpl--upstreaming).

## Concepts

- **Provider implementation** — a Spring bean implementing `org.booklore.acquisition.AcquisitionProvider`,
  identified by a stable `providerKey` (e.g. `shelfmark`). Resolved through `AcquisitionProviderRegistry`;
  core code never branches on a concrete provider.
- **Provider configuration** — an admin-managed row (`acquisition_provider` table) that turns a provider
  on and supplies its per-deployment settings (base URL, token, mode, allowed content types). **Disabled
  by default.**
- **Acquisition request** — a user-triggered acquisition (`acquisition_request` table) with a lifecycle
  status: `QUEUED → DOWNLOADING → IMPORTED` (or `FAILED`).
- **Mode** — `DIRECT_DOWNLOAD` (fetch + import now) or `REQUEST` (record a request to be fulfilled later).

## Permissions

- Configuring providers is **admin-only**.
- Searching providers and triggering an acquisition require the **download** permission (or admin),
  matching how the rest of BookLore gates fetching content. No new permission flag was introduced.

## REST API

Base path `/api/v1/acquisition` (enable OpenAPI to browse it — see below):

| Method & path | Permission | Purpose |
| --- | --- | --- |
| `GET /providers` | admin | List configured providers (token never returned, only `tokenSet`). |
| `GET /providers/available` | admin | List provider implementations in this build. |
| `POST /providers` | admin | Configure a provider (defaults to disabled). |
| `PUT /providers/{id}` | admin | Update a provider. A null `apiToken` keeps the stored token; blank clears it. |
| `DELETE /providers/{id}` | admin | Remove a provider. |
| `GET /search?q=` | download/admin | External candidates from **enabled** providers. No-op (empty) when none enabled. |
| `POST /requests` | download/admin | Trigger acquisition of a candidate. |
| `GET /requests` | download/admin | List requests (admins see all; others see their own). |
| `GET /requests/{id}` | download/admin | One request and its status. |

Provider config changes and acquisition requests are written to the existing **audit log**
(`AuditAction.ACQUISITION_*`), viewable under Settings → Audit Logs.

## UI

- **Settings → Acquisition** (admin): configure providers — enable, base URL, token, mode, allowed
  content types. Provider naming is generic; Shelfmark appears only as a configured provider.
- **Command palette / search**: when a query has no local match, a "Not in your library" line plus a
  permission-gated **Find download options** action surfaces external candidates and lets the user
  acquire one (respecting direct-download vs request mode). Recent acquisitions and their status are
  shown inline.

## Shelfmark provider

`ShelfmarkAcquisitionProvider` (`providerKey = shelfmark`) is a pure API client (no vendored Shelfmark
code). Assumed contract against the configured base URL, with an optional bearer token:

- `GET  {baseUrl}/api/v1/search?q=…` → `{ "results": [ { id, title, author, format, language, year, sizeBytes, coverUrl, description, downloadable } ] }`
- `POST {baseUrl}/api/v1/downloads` body `{ "id", "mode" }` → `{ "id", "status" }`

`allowedContentTypes` filters search results; an unavailable direct download falls back to request mode.

## Local development

```bash
just api test     # backend unit tests (includes org.booklore.acquisition.*, service.acquisition.*)
just api check    # backend verification (boots the context + Flyway, applies V144/V145)
just ui test      # frontend Vitest (includes src/app/features/acquisition/*)
just ui check     # frontend verification (typecheck, lint, stylelint, build, test)
```

### API docs

The OpenAPI surface (incl. the acquisition endpoints) is gated behind `API_DOCS_ENABLED`:

```bash
API_DOCS_ENABLED=true just api run
# Swagger/OpenAPI served at /v3/api-docs (springdoc)
```

## AGPL & upstreaming

Grimmory is AGPL-3.0. The AGPL source-availability obligation is triggered by **distributing or hosting
a modified build**. The current stance:

- The fork lives at `rossbrg/grimmory`; feature work branches off `develop`.
- This feature is kept **local only** for now — no hosted/distributed modified build, so no publish
  obligation is yet triggered.
- If/when a hosted build is run (e.g. the homelab books VM), the corresponding source for that build
  must be published. That is deliberately deferred and tracked separately, not done here.

There is intentionally **no upstream issue or PR** for this work yet (kept local by request).
