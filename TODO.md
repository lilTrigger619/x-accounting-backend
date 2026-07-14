# Phase 3 wiring - TODO

- [x] Verify RuntimeReportSectionAdapter and TemplateReportSectionAdapter map ReportSectionAccount/ReportTemplateSectionAccount -> AccountAssignmentView with required fields.
- [x] Verify FinancialReportBuilder.buildFromTemplate uses TemplateReportAdapter, produces section views + assignment views, aggregates using SectionAggregatorView, evaluates formulas once, resolves tree using ReportSectionTreeResolverView, returns FinancialReportTreeResponseDto.
- [x] Verify FinancialReportEngineImpl.generateFromTemplate delegates directly to FinancialReportBuilder.buildFromTemplate.
- [x] Verify TemplateFinancialReportServiceImpl orchestrates: published lookup by templateCode+PUBLISHED, preview by templateId with allowed statuses, no DB writes.
- [x] Verify ReportTemplateRepository contains findTopByTemplateCodeAndStatusOrderByVersionDesc.
- [x] Run tests/build to ensure compilation success.

