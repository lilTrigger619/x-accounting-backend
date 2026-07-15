# Frontend.md

## Report (Endpoints & Behavior)

### Runtime engine

#### Run a report (template-backed)
- **Method/Path:** `POST /api/v1/reports/{templateCode}/engine`
- **Description:** Generates a report using the latest **PUBLISHED** report template matching `templateCode`.
- **Path Params:**
  - `templateCode: String`
- **Query Params:**
  - `from: LocalDate` (ISO date, required)
  - `to: LocalDate` (ISO date, required)
- **Body:** none
- **Response:** `FinancialReportTreeResponseDto`
- **Response (JSON example):**
```json
{
  "reportCode": "string",
  "from": "2025-01-01",
  "to": "2025-12-31",
  "root": {
    "sectionId": null,
    "code": "ROOT",
    "title": "ROOT",
    "sectionType": "SECTION",
    "value": 0,
    "children": [
      {
        "sectionId": 1,
        "code": "string",
        "title": "string",
        "sectionType": "SECTION",
        "value": 0,
        "children": []
      }
    ]
  }
}
```


Controller: `FinancialReportEngineController`


---

#### Pre-built report types (trial balance / balance sheet / cash flow)
These endpoints wrap the engine with a fixed `reportCode` and map the tree to a UI-friendly sections response.

##### Trial Balance
- **Method/Path:** `GET /api/reports/trial-balance`
- **Description:** Runs `TRIAL_BALANCE` report.
- **Query Params (required by validation):**
  - `fromDate` (ISO date)
  - `toDate` (ISO date)
- **Response:** `FinancialReportSectionsResponseDto`

##### Balance Sheet
- **Method/Path:** `GET /api/reports/balance-sheet`
- **Description:** Runs `BALANCE_SHEET` report.
- **Query Params:** `fromDate`, `toDate` (ISO dates)
- **Response:** `FinancialReportSectionsResponseDto`

##### Cash Flow
- **Method/Path:** `GET /api/reports/cash-flow`
- **Description:** Runs `CASH_FLOW` report.
- **Query Params:** `fromDate`, `toDate` (ISO dates)
- **Response:** `FinancialReportSectionsResponseDto`

Controller: `ReportsController`

**Validation rules (shared):**
- `fromDate` and `toDate` must be provided (not null)
- `fromDate` must be `<= toDate`
- neither date can be in the future (compared to current server date)

---

### Report templates (ReportTemplate-based)
Template CRUD and template structure editing are handled by these endpoints.
Execution/generation endpoints for templates are implemented at the engine/service level, but **no dedicated template-generation controller endpoints were found in the inspected controller files**.

#### Create Report Template
- **Method/Path:** `POST /api/v1/report-templates`
- **Description:** Create a new report template.
- **Body (JSON):**
```json
{
  "templateCode": "string",
  "templateName": "string",
  "description": "string",
  "category": "string",
  "status": "ReportTemplateStatus",
  "version": 0,
  "isSystemTemplate": true,
  "createdBy": "string"
}
```
- **Response:** `ReportTemplateResponseDto`


#### Get Template by ID
- **Method/Path:** `GET /api/v1/report-templates/{id}`
- **Response:** `ReportTemplateResponseDto`

#### List Templates
- **Method/Path:** `GET /api/v1/report-templates`
- **Response:** `List<ReportTemplateResponseDto>`

#### Update Template
- **Method/Path:** `PUT /api/v1/report-templates/{id}`
- **Body (JSON):**
```json
{
  "templateCode": "string",
  "templateName": "string",
  "description": "string",
  "category": "string",
  "status": "ReportTemplateStatus",
  "version": 0,
  "isSystemTemplate": true,
  "createdBy": "string"
}
```
- **Response:** `ReportTemplateResponseDto`


#### Delete Template
- **Method/Path:** `DELETE /api/v1/report-templates/{id}`
- **Response:** `204 No Content`

#### Change Template Status
- **Method/Path:** `PATCH /api/v1/report-templates/{id}/status`
- **Query Params:**
  - `status` (`ReportTemplateStatus`)
  - `updatedBy` (optional)
- **Response:** `ReportTemplateResponseDto`

Controller: `ReportTemplateController`

---

### Template Sections
#### Create Section
- **Method/Path:** `POST /api/v1/report-templates/{templateId}/sections`
- **Body (JSON):**
```json
{
  "sectionCode": "string",
  "title": "string",
  "displayOrder": 0,
  "sectionType": "SectionType",
  "formula": "string",
  "visible": true,
  "expandedByDefault": true,
  "parentSectionId": 0
}
```
- **Response:** `ReportTemplateSectionResponseDto`


#### Get Section by ID
- **Method/Path:** `GET /api/v1/report-templates/sections/{sectionId}`
- **Response:** `ReportTemplateSectionResponseDto`

#### List Sections by Template
- **Method/Path:** `GET /api/v1/report-templates/{templateId}/sections`
- **Response:** `List<ReportTemplateSectionResponseDto>`

#### Update Section
- **Method/Path:** `PUT /api/v1/report-templates/sections/{sectionId}`
- **Body (JSON):**
```json
{
  "sectionCode": "string",
  "title": "string",
  "displayOrder": 0,
  "sectionType": "SectionType",
  "formula": "string",
  "visible": true,
  "expandedByDefault": true,
  "parentSectionId": 0
}
```
- **Response:** `ReportTemplateSectionResponseDto`



#### Delete Section
- **Method/Path:** `DELETE /api/v1/report-templates/sections/{sectionId}`
- **Response:** `204 No Content`

Controller: `ReportTemplateSectionController`

---

### Template Section → Account Assignments
#### Assign Account to Section
- **Method/Path:** `POST /api/v1/report-template-sections/{sectionId}/accounts`
- **Body (JSON):**
```json
{
  "reportTemplateSectionId": 0,
  "accountId": 0,
  "displayOrder": 0
}
```
- **Response:** `ReportTemplateSectionAccountResponseDto`


#### List Accounts assigned to Section
- **Method/Path:** `GET /api/v1/report-template-sections/{sectionId}/accounts`
- **Response:** `List<ReportTemplateSectionAccountResponseDto>`

#### Remove Account from Section
- **Method/Path:** `DELETE /api/v1/report-template-sections/{sectionId}/accounts/{accountId}`
- **Response:** `204 No Content`

Controller: `ReportTemplateSectionAccountController`

---

## Notes for Frontend Template Generation
Backend orchestration exists via `TemplateFinancialReportService` and `FinancialReportEngine.generateFromTemplate(template, fromDate, toDate)`, including:
- `generatePublishedReport(templateCode, fromDate, toDate)` (uses latest `PUBLISHED` by templateCode)
- `previewTemplate(templateId, fromDate, toDate)` (allowed `DRAFT` and `PUBLISHED`, **no DB writes**)

However, **the corresponding HTTP endpoints are not present in the inspected controllers**.
If you need UI to call template generation, you’ll likely add a controller exposing these service methods.

