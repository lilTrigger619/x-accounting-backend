package com.unionsg.xaccounting.dto.fixedasset;

import com.unionsg.xaccounting.enums.fixedasset.AssetDisposalType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DisposeAssetRequest {
    private AssetDisposalType disposalType;
    private LocalDate disposalDate;
    private BigDecimal proceeds;
    private Long bankAccountId;
    private Long proceedsAccountId;
    private String reason;
}
