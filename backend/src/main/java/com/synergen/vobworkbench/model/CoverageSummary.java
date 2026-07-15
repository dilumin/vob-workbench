package com.synergen.vobworkbench.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoverageSummary {
    private BigDecimal totalEstimatedCharge = BigDecimal.ZERO;
    private BigDecimal totalInsurancePays = BigDecimal.ZERO;
    private BigDecimal totalPatientResponsibility = BigDecimal.ZERO;
    private BigDecimal totalAllowedAmount = BigDecimal.ZERO;
    private BigDecimal totalDeductibleApplied = BigDecimal.ZERO;
    private BigDecimal totalCopayApplied = BigDecimal.ZERO;
    private BigDecimal totalCoinsuranceApplied = BigDecimal.ZERO;
    private BigDecimal totalNonCoveredAmount = BigDecimal.ZERO;
    private List<ProcedureCoverage> procedureCoverages = new ArrayList<>();
}
