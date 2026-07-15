package com.synergen.vobworkbench.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Procedure {
    private String procedureCode;
    private String procedureName;
    private BigDecimal estimatedCharge;
    private boolean requiresAuthorization;
}
