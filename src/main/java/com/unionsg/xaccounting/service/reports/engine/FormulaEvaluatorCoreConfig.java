package com.unionsg.xaccounting.service.reports.engine;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FormulaEvaluatorCoreConfig {

    @Bean
    public FormulaEvaluatorCore formulaEvaluatorCore(FormulaParser parser) {
        return new FormulaEvaluatorCore(parser);
    }
}

