package com.unionsg.xaccounting.service.reports.engine;

import com.unionsg.xaccounting.dto.reports.FinancialReportSectionsResponseDto;
import com.unionsg.xaccounting.dto.reports.FinancialReportSectionsResponseNodeDto;
import com.unionsg.xaccounting.dto.reports.FinancialReportTreeNodeDto;
import com.unionsg.xaccounting.dto.reports.FinancialReportTreeResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FinancialReportSectionsMapper {

    public FinancialReportSectionsResponseDto map(FinancialReportTreeResponseDto engineResponse,
                                                   String reportName) {
        return new FinancialReportSectionsResponseDto(
                reportName,
                engineResponse.from(),
                engineResponse.to(),
                toNodes(engineResponse.root().children())
        );
    }

    private List<FinancialReportSectionsResponseNodeDto> toNodes(List<FinancialReportTreeNodeDto> nodes) {
        return nodes.stream()
                .map(this::toNode)
                .toList();
    }

    private FinancialReportSectionsResponseNodeDto toNode(FinancialReportTreeNodeDto node) {
        return new FinancialReportSectionsResponseNodeDto(
                node.title(),
                node.sectionType(),
                node.value(),
                toNodes(node.children())
        );
    }
}

