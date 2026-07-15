package com.synergen.vobworkbench.model;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityResult {
    private Boolean coverageActive;
    private NetworkStatus networkStatus;
    private BigDecimal copay;
    private Integer coinsurancePercent;
    private BigDecimal deductibleRemaining;
    private String notes;
    private BigDecimal deductibleTotal;
    private BigDecimal deductibleMet;
    private BigDecimal oopMax;
    private BigDecimal oopMet;
    private BigDecimal oopRemaining;
    private Boolean priorAuthorizationRequired;
    private String benefitSource;
    private String verifiedBy;
    private Instant verifiedAt;
}
