# TODO - Report Template Validation Refactor

## Step 1: Repo understanding checkpoint
- [x] Located current publish endpoint and validation logic inside `ReportTemplateLifecycleServiceImpl.validateInternal`.

## Step 2: Add DTOs
- [ ] Create `ReportTemplateValidationResponse`, `ValidationErrorDto`, `ValidationWarningDto` (records/classes) under `dto/reports`.

## Step 3: Implement validation architecture
- [ ] Add `validation` package with:
  - [ ] `ValidationCoordinator`
  - [ ] `StructureValidator`
  - [ ] `AccountAssignmentValidator`
  - [ ] `BusinessRuleValidator`
  - [ ] (Reuse or wrap existing formula validation logic as needed)

## Step 4: Expose validate endpoint
- [ ] Add `POST /api/v1/report-templates/{id}/validate` to controller returning the DTO.
- [ ] Ensure endpoint never publishes and never creates versions.

## Step 5: Refactor publish workflow
- [ ] Modify publish endpoint/service to call coordinator first.
- [ ] If ANY errors => HTTP 400 + same DTO response.
- [ ] If only warnings => publish proceeds.

## Step 6: Implement SectionType rules
- [ ] Update validation rules for GROUP/DETAIL/SUBTOTAL/TOTAL as specified.
- [ ] Implement specified errors + warnings.

## Step 7: Wiring and cleanup
- [ ] Remove/stop using the old exception-driven validation path for publish/validate.
- [ ] Ensure no duplicated validation logic.

## Step 8: Build & tests
- [ ] Run `./gradlew test` and fix any compilation/test failures.

