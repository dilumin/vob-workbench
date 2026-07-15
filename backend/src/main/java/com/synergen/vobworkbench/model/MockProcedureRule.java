package com.synergen.vobworkbench.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MockProcedureRule {
    private String payerName;
    private String procedureCode;
    private Integer coveragePercent;
    private Boolean coverageActive;
    private NetworkStatus networkStatus;
    private BigDecimal copay;
    private Integer coinsurancePercent;
    private BigDecimal deductibleRemaining;
    private PlanType planType;
    private String memberId;
    private String groupNumber;
    private String procedureName;
    private BenefitType benefitType;
    private BigDecimal allowedAmount;
    private BigDecimal deductibleTotal;
    private BigDecimal deductibleMet;
    private BigDecimal oopMax;
    private BigDecimal oopMet;
    private Boolean deductibleApplies;
    private Boolean priorAuthorizationRequired;
    private String notes;
}
