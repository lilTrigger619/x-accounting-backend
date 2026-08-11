# TODO: Document Template Sample Preview Endpoint

## Steps
- [x] 1. Create `SamplePreviewRequest` DTO
- [x] 2. Create `DocumentPreviewSampleDataProvider` interface
- [x] 3. Create `InvoiceSampleDataProvider` implementation
- [x] 4. Create `DocumentTemplatePreviewService` interface
- [x] 5. Create `DocumentTemplatePreviewServiceImpl`
- [x] 6. Add `samplePreview` endpoint to `DocumentTemplateController`
- [x] 7. Add tests (provider + service)
- [x] 8. Compile & run tests (BUILD SUCCESSFUL)
- [x] 9. Fix Thymeleaf context-relative link error in `ThymeleafDocumentRenderer` (use WebContext within HTTP request)
- [x] 10. Create `CompanyConfigSeeder` to seed COMPANY config for realistic template rendering
- [x] 11. Fix boolean-coercion type errors in `th:if` conditions in classic/modern/professional templates
- [x] 12. Make CSS links absolute using `baseUrl` variable from request (scheme://host[:port]/contextPath)
- [x] 13. Allow static resources (`/css/**`, `/js/**`, `/images/**`, `/fonts/**`, `/favicon.ico`) through auth in `SecurityConfig`
