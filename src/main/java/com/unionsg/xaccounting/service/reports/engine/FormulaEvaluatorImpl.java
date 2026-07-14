package com.unionsg.xaccounting.service.reports.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FormulaEvaluatorImpl implements FormulaEvaluator {


    private final FormulaEvaluatorCore core;

    public FormulaEvaluatorImpl(FormulaEvaluatorCore core) {
        this.core = core;
    }

    @Override
    public Map<String, BigDecimal> evaluate(Map<String, BigDecimal> baseByCode, List<com.unionsg.xaccounting.service.reports.engine.view.ReportSectionView> sections) {
        return core.evaluate(baseByCode, sections);
    }

}


