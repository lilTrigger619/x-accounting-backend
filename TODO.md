# Document Number Auto-Generation Endpoints

## Completed

- [x] 1. Updated `DocumentModule` enum to include `ACCOUNT`
- [x] 2. Created DTOs: `DocumentNumberConfigDto`, `UpsertDocumentNumberConfigDto`, `NumberGenerationResponse`
- [x] 3. Created `DocumentNumberService` with number generation, config retrieval, and config update logic
- [x] 4. Created `DocumentNumberController` under `controller/config/` with endpoints
- [x] 5. Build verified successfully

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/document-numbers/generate/{module}` | Generate next unique number for the module |
| GET | `/api/document-numbers/config/{module}` | Get current numbering config for module |
| PUT | `/api/document-numbers/config/{module}` | Update numbering config for module |
| GET | `/api/document-numbers/modules` | List all available modules |

## Example Usage

### Generate a number
```bash
POST /api/document-numbers/generate/INVOICE
# Response: { "module": "INVOICE", "generatedNumber": "INV-00001" }

POST /api/document-numbers/generate/ACCOUNT
# Response: { "module": "ACCOUNT", "generatedNumber": "ACC-00001" }

POST /api/document-numbers/generate/JOURNAL
# Response: { "module": "JOURNAL", "generatedNumber": "JRN-00001" }
```

### Update config
```bash
PUT /api/document-numbers/config/INVOICE
# Body: { "prefix": "INV", "padding": 6, "includeYear": true, "separator": "-" }
# Result: number format becomes INV-2025-000001
```

