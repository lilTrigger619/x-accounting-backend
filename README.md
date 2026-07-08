# xaccounting

## Reporting Module - Financial Report REST APIs

This project includes a **generic Financial Report Engine** that generates a hierarchical report tree for a given `reportCode` and a date range.

The work in this update adds **thin REST APIs** for the following reports by **reusing the existing engine**:

- `GET /api/reports/trial-balance`
- `GET /api/reports/profit-loss` *(already existed via `ProfitAndLossController`; the new controller avoids duplicating the endpoint to prevent ambiguous mappings)*
- `GET /api/reports/balance-sheet`
- `GET /api/reports/cash-flow`

All endpoints accept:
- `fromDate` (ISO `yyyy-MM-dd`)
- `toDate` (ISO `yyyy-MM-dd`)

They return a hierarchical JSON response (nested sections) mapped from the engine tree.

---

## Endpoints Added / Updated

### 1) Trial Balance
- **GET** `/api/reports/trial-balance`

### 2) Balance Sheet
- **GET** `/api/reports/balance-sheet`

### 3) Cash Flow
- **GET** `/api/reports/cash-flow`

### 4) Profit & Loss
- **GET** `/api/reports/profit-loss`

This endpoint already existed in `ProfitAndLossController`.

To satisfy Spring MVC mapping rules, the new `ReportsController` **does not declare** a `profit-loss` handler method.

---

## Key Design Principle

**No business logic is duplicated.**

Each endpoint:
1. Validates input dates.
2. Calls the existing `FinancialReportEngine.generate(new FinancialReportEngineRequestDto(reportCode, fromDate, toDate))`.
3. Converts the returned engine tree into the response DTO format.

---

## Classes Added (Reporting Module)

### `ReportsController`
**File:** `src/main/java/com/unionsg/xaccounting/controller/ReportsController.java`

**Purpose:**
Provides thin REST endpoints for Trial Balance, Balance Sheet, and Cash Flow.

**Implementation details:**
- Annotated with `@RestController` and `@RequestMapping` not used (methods define full paths).
- Uses constructor injection (`@RequiredArgsConstructor`) for:
  - `FinancialReportEngine engine`
  - `FinancialReportSectionsMapper mapper`
- Provides four private helpers:
  - `trialBalance(fromDate, toDate)`
  - `balanceSheet(fromDate, toDate)`
  - `cashFlow(fromDate, toDate)`
  - `handle(reportCode, reportName, fromDate, toDate)`
  - `validateDates(fromDate, toDate)`

**Date validation rules (`validateDates`)**
- Rejects missing dates: throws `IllegalArgumentException`.
- Rejects invalid ranges: `fromDate` after `toDate` throws `IllegalArgumentException`.
- Rejects future dates: if either date is after today throws `IllegalArgumentException`.

**Engine call (`handle`)**
- Calls the generic engine:
  - `engine.generate(new FinancialReportEngineRequestDto(reportCode, fromDate, toDate))`
- Maps the engine tree result to the response DTO using:
  - `mapper.map(tree, reportName)`

**Profit-loss mapping conflict fix**
- The `profit-loss` endpoint was already present in `ProfitAndLossController`.
- To avoid Spring’s `Ambiguous mapping` error, `ReportsController` intentionally does not expose `GET /api/reports/profit-loss`.

---

### `FinancialReportSectionsMapper`
**File:** `src/main/java/com/unionsg/xaccounting/service/reports/engine/FinancialReportSectionsMapper.java`

**Purpose:**
Maps the engine’s hierarchical tree (`FinancialReportTreeResponseDto`) into the new hierarchical response DTOs.

**Implementation details:**
- Annotated with `@Component`.
- Public method:
  - `FinancialReportSectionsResponseDto map(FinancialReportTreeResponseDto engineResponse, String reportName)`

**Mapping behavior:**
- Response includes:
  - `reportName`
  - `fromDate`
  - `toDate`
  - `sections` mapped from `engineResponse.root().children()`
- Tree node mapping is done recursively:
  - Node `title` -> `title`
  - Node `sectionType` -> `type`
  - Node `value` -> `amount`
  - Node `children` -> `children`

---

## DTOs Added (Reporting Module)

### `FinancialReportSectionsResponseDto` (Java record)
**File:** `src/main/java/com/unionsg/xaccounting/dto/reports/FinancialReportSectionsResponseDto.java`

**Fields:**
- `String reportName`
- `LocalDate fromDate`
- `LocalDate toDate`
- `List<FinancialReportSectionsResponseNodeDto> sections`

Represents the top-level report response.

### `FinancialReportSectionsResponseNodeDto` (Java record)
**File:** `src/main/java/com/unionsg/xaccounting/dto/reports/FinancialReportSectionsResponseNodeDto.java`

**Fields:**
- `String title`
- `SectionType type`
- `BigDecimal amount`
- `List<FinancialReportSectionsResponseNodeDto> children`

Represents a single nested section node.

---

## ControllerAdvice Added (Reporting Module)

### `ReportDateValidationControllerAdvice`
**File:** `src/main/java/com/unionsg/xaccounting/controller/ReportDateValidationControllerAdvice.java`

**Purpose:**
Provides centralized handling for date validation errors triggered by the reporting endpoints.

**Implementation details:**
- Annotated with `@RestControllerAdvice`.
- Handles:
  - `IllegalArgumentException` -> `400 BAD_REQUEST` with `ApiResponse` payload.
  - Generic `Exception` -> `500 INTERNAL_SERVER_ERROR` with `ApiResponse` payload.

**Note:**
This advice is intentionally lightweight and consistent with the existing project’s `ApiResponse` structure.

---

## Report Endpoint Example Response Shape

The returned JSON matches the hierarchical requirement:

```json
{
  "reportName": "Profit & Loss",
  "fromDate": "2026-01-01",
  "toDate": "2026-01-31",
  "sections": [
    {
      "title": "Revenue",
      "type": "SECTION",
      "amount": 200000,
      "children": [
        {}
      ]
    }
  ]
}
```

---

## Custom Report Designer - Report Template APIs

This update adds the **foundation** for a Custom Report Designer, enabling administrators to define entirely new financial reports (templates) without changing Java report engine code.

### What was added
- **Entities**
  - `ReportTemplate`
  - `ReportTemplateSection`
  - `ReportTemplateSectionAccount`
- **DTOs** (Java records) for CRUD and assignments
- **Services** implementing CRUD + business validations
- **Mappers** for entity <-> DTO conversion
- **REST controllers** for template/section/account assignment
- **Validation/business rules** enforced in services:
  - `templateCode` must be unique
  - `sectionCode` must be unique within the same template
  - `displayOrder` cannot be duplicated under the same parent section
  - parent-child relationships cannot form cycles
  - deleting a **PUBLISHED** template is prevented

### Status enum
- `ReportTemplateStatus`: `DRAFT`, `PUBLISHED`, `ARCHIVED`

---

## Template REST Endpoints

Base paths:
- Template: `/api/v1/report-templates`
- Sections: `/api/v1/report-templates/{templateId}/sections` and `/api/v1/report-templates/sections/{sectionId}`
- Section accounts: `/api/v1/report-template-sections/{sectionId}/accounts`

### 1) Create template
- **POST** `/api/v1/report-templates`

Request: `ReportTemplateRequestDto`

### 2) Get template
- **GET** `/api/v1/report-templates/{id}`

### 3) List templates
- **GET** `/api/v1/report-templates`

### 4) Update template
- **PUT** `/api/v1/report-templates/{id}`

### 5) Delete template
- **DELETE** `/api/v1/report-templates/{id}`

Rules:
- Deleting `PUBLISHED` templates throws `TemplatePublishedDeletionException`.

### 6) Update template status
- **PATCH** `/api/v1/report-templates/{id}/status?status=DRAFT&updatedBy=...`

---

## Section REST Endpoints

### 1) Create section
- **POST** `/api/v1/report-templates/{templateId}/sections`

Rules:
- `sectionCode` uniqueness enforced per template
- `displayOrder` uniqueness enforced per parent (including null parent)
- cycle prevention enforced

Request: `ReportTemplateSectionRequestDto`

### 2) Get section
- **GET** `/api/v1/report-templates/sections/{sectionId}`

### 3) List sections by template
- **GET** `/api/v1/report-templates/{templateId}/sections`

### 4) Update section
- **PUT** `/api/v1/report-templates/sections/{sectionId}`

### 5) Delete section
- **DELETE** `/api/v1/report-templates/sections/{sectionId}`

---

## Section Account Assignment REST Endpoints

### 1) Assign account to section
- **POST** `/api/v1/report-template-sections/{sectionId}/accounts`

Request: `ReportTemplateSectionAccountRequestDto`

Rule:
- Duplicate assignment of the same `accountId` to the same section is prevented.

### 2) List assigned accounts
- **GET** `/api/v1/report-template-sections/{sectionId}/accounts`

### 3) Remove assignment
- **DELETE** `/api/v1/report-template-sections/{sectionId}/accounts/{accountId}`

---

## Controller Advice (Error Handling)

`ReportTemplateControllerAdvice` maps template designer exceptions to HTTP statuses:
- **404**: not found
- **409**: conflicts (uniqueness/displayOrder/dup assignment)
- **400**: business validation failures (cycle/bad operations)

---

## Notes on Swagger / Caching

- Swagger annotations were added at the controller method level via `@Operation` and `@Tag`.
- Caching was not added in this patch because the project’s existing reporting architecture didn’t expose a cache strategy or caching dependencies in the inspected code.

If you want caching for date-range report generation, we can add `@Cacheable` at the engine-call boundary once the project’s caching configuration is confirmed.


