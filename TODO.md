# TODO.md

## Phase 7.4 – Report Audit Trail

- [x] Inspect template lifecycle + template CRUD paths to ensure all actions log exactly once

- [x] Create entity: ReportTemplateHistory

- [x] Create enum: ReportTemplateHistoryAction

- [x] Create repository: ReportTemplateHistoryRepository (history by templateId, ordered asc)

- [x] Create DTO: ReportTemplateHistoryDto

- [x] Create centralized AuditService: ReportTemplateAuditService (no duplicated logging logic)

- [x] Implement ReportTemplateHistoryService + endpoint:
  - [x] GET /api/v1/report-templates/{id}/history (chronological)

- [x] Wire audit logging into:
  - [x] ReportTemplateServiceImpl: create/update/setStatus/delete
  - [x] ReportTemplateLifecycleServiceImpl: preview/publish/archive/clone
  - [x] VersionHistoryServiceImpl: rollback

- [x] Compile/test build (`./gradlew test`)
