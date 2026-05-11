package com.unionsg.xaccounting.security;


import com.unionsg.xaccounting.entity.DocumentNumberConfig;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.repository.DocumentNumberConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DocumentNumberGeneratorService {

    private final DocumentNumberConfigRepository repository;


    @Transactional
    public String generate(

            DocumentModule module
//            Long clientId
    ) {
        System.out.println("the module "+ module.name());
        DocumentNumberConfig config =
                repository
                        .findByModuleAndCompanyIdAndBranchId(
                                module.name(),
                                1L, // always 1 because we have not setup company and branch system yet so the
                                // is done only on the module name

                                1L // always 1 because we have not setup company and branch system yet so the
                                // is done only on the module name
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Document config not found"
                                ));

        resetIfNeeded(config);

        Long nextNumber =
                config.getLastNumber() + 1;

        config.setLastNumber(nextNumber);
        config.setUpdatedAt(LocalDateTime.now());

        repository.save(config);

        return buildNumber(config, nextNumber);
    }


    private void resetIfNeeded(
            DocumentNumberConfig config
    ) {

        LocalDate now = LocalDate.now();

        if (config.getResetYearly()) {

            if (config.getLastResetYear() == null ||
                    config.getLastResetYear() != now.getYear()) {

                config.setLastNumber(0L);
                config.setLastResetYear(now.getYear());
            }
        }

        if (config.getResetMonthly()) {

            if (config.getLastResetMonth() == null ||
                    config.getLastResetMonth() != now.getMonthValue()) {

                config.setLastNumber(0L);
                config.setLastResetMonth(
                        now.getMonthValue()
                );
            }
        }
    }


    private String buildNumber(
            DocumentNumberConfig config,
            Long number
    ) {

        StringBuilder builder =
                new StringBuilder();

        builder.append(config.getPrefix());

        if (config.getIncludeYear()) {

            builder.append(config.getSeparator())
                    .append(LocalDate.now().getYear());
        }

        if (config.getIncludeMonth()) {

            builder.append(config.getSeparator())
                    .append(
                            String.format(
                                    "%02d",
                                    LocalDate.now().getMonthValue()
                            )
                    );
        }

        builder.append(config.getSeparator())
                .append(
                        String.format(
                                "%0" + config.getPadding() + "d",
                                number
                        )
                );

        return builder.toString();
    }

}