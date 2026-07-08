package com.unionsg.xaccounting.service.reports.engine;

import com.unionsg.xaccounting.entity.reports.ReportSection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FormulaEvaluatorImpl implements FormulaEvaluator {

    private final FormulaEvaluatorCore core;

    public FormulaEvaluatorImpl(FormulaEvaluatorCore core) {
        this.core = core;
    }

    @Override
    public Map<String, BigDecimal> evaluate(Map<String, BigDecimal> baseByCode, List<ReportSection> sections) {
        return core.evaluate(baseByCode, sections);
    }
}


