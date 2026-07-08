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

## Notes on Swagger / Caching

- Swagger annotations were added at the controller method level via `@Operation` and `@Tag`.
- Caching was not added in this patch because the project’s existing reporting architecture didn’t expose a cache strategy or caching dependencies in the inspected code.

If you want caching for date-range report generation, we can add `@Cacheable` at the engine-call boundary once the project’s caching configuration is confirmed.

