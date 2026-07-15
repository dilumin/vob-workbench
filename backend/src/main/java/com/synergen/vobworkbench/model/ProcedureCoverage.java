package com.synergen.vobworkbench.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcedureCoverage {
    private String procedureCode;
    private BigDecimal insurancePays;
    private BigDecimal patientResponsibility;
    private String procedureName;
    private BigDecimal estimatedCharge;
    private BigDecimal allowedAmount;
    private BigDecimal deductibleApplied;
    private BigDecimal copayApplied;
    private BigDecimal coinsuranceApplied;
    private BigDecimal nonCoveredAmount;
    private Boolean priorAuthorizationRequired;
    private String calculationNote;
}
