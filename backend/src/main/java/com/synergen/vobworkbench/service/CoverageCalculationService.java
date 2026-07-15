package com.synergen.vobworkbench.service;

import com.synergen.vobworkbench.model.BenefitType;
import com.synergen.vobworkbench.model.CoverageSummary;
import com.synergen.vobworkbench.model.EligibilityResult;
import com.synergen.vobworkbench.model.InsuranceOrder;
import com.synergen.vobworkbench.model.InsurancePolicy;
import com.synergen.vobworkbench.model.MockData;
import com.synergen.vobworkbench.model.MockProcedureRule;
import com.synergen.vobworkbench.model.Procedure;
import com.synergen.vobworkbench.model.ProcedureCoverage;
import com.synergen.vobworkbench.model.VobRequest;
import com.synergen.vobworkbench.repository.MockDataRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CoverageCalculationService {
    private final MockDataRepository mockDataRepository;

    public CoverageCalculationService(MockDataRepository mockDataRepository) {
        this.mockDataRepository = mockDataRepository;
    }

    public CoverageSummary calculate(VobRequest request) {
        MockData mockData = mockDataRepository.findById("default").orElse(new MockData());
        CoverageSummary summary = new CoverageSummary();
        summary.setProcedureCoverages(new ArrayList<>());

        EligibilityResult eligibility = request.getEligibilityResult();
        InsurancePolicy primaryPolicy = primaryPolicy(request).orElse(null);
        BenefitAccumulator accumulator = new BenefitAccumulator(
                deductibleTotal(eligibility),
                deductibleMet(eligibility),
                eligibility == null ? BigDecimal.ZERO : safe(eligibility.getOopMax()),
                eligibility == null ? BigDecimal.ZERO : safe(eligibility.getOopMet())
        );

        request.getProcedures().forEach(procedure -> {
            Optional<MockProcedureRule> rule = primaryPolicy == null
                    ? Optional.empty()
                    : matchingRule(mockData, primaryPolicy, procedure.getProcedureCode());
            ProcedureCoverage coverage = calculateProcedure(procedure, eligibility, rule, accumulator);
            addToSummary(summary, coverage);
        });

        return summary;
    }

    private ProcedureCoverage calculateProcedure(Procedure procedure, EligibilityResult eligibility,
                                                 Optional<MockProcedureRule> rule,
                                                 BenefitAccumulator accumulator) {
        BigDecimal estimatedCharge = safe(procedure.getEstimatedCharge());
        BigDecimal allowedAmount = rule.map(MockProcedureRule::getAllowedAmount)
                .filter(value -> value.signum() > 0)
                .map(this::money)
                .orElse(estimatedCharge);
        BigDecimal nonCoveredAmount = estimatedCharge.subtract(allowedAmount).max(BigDecimal.ZERO);

        ProcedureCoverage coverage = baseCoverage(procedure, estimatedCharge, allowedAmount, nonCoveredAmount, rule);

        if (eligibility != null && !Boolean.TRUE.equals(eligibility.getCoverageActive())) {
            fullPatientResponsibility(coverage, estimatedCharge, estimatedCharge,
                    "Coverage is inactive or eligibility could not be verified.");
            return coverage;
        }

        if (rule.isEmpty()) {
            if (eligibility != null && Boolean.TRUE.equals(eligibility.getCoverageActive())) {
                return manualEligibilityFallback(coverage, estimatedCharge, eligibility, accumulator);
            }
            fullPatientResponsibility(coverage, estimatedCharge, estimatedCharge,
                    "No configured benefit rule for this procedure.");
            return coverage;
        }

        MockProcedureRule benefit = rule.get();
        BenefitType benefitType = benefitType(benefit);
        if (!isRuleActive(benefit) || benefitType == BenefitType.NOT_COVERED) {
            fullPatientResponsibility(coverage, estimatedCharge, estimatedCharge,
                    "Procedure is not covered by the mock benefit rule.");
            return coverage;
        }

        CostShare costShare = calculateCostShare(allowedAmount, benefitType, benefit, eligibility, accumulator);
        BigDecimal coveredPatientCost = costShare.deductibleApplied()
                .add(costShare.copayApplied())
                .add(costShare.coinsuranceApplied());
        BigDecimal cappedCoveredPatientCost = accumulator.applyOutOfPocket(coveredPatientCost);
        BigDecimal insurancePays = allowedAmount.subtract(cappedCoveredPatientCost).max(BigDecimal.ZERO);
        BigDecimal patientResponsibility = cappedCoveredPatientCost.add(nonCoveredAmount);

        coverage.setDeductibleApplied(costShare.deductibleApplied());
        coverage.setCopayApplied(costShare.copayApplied());
        coverage.setCoinsuranceApplied(costShare.coinsuranceApplied());
        coverage.setInsurancePays(money(insurancePays));
        coverage.setPatientResponsibility(money(patientResponsibility));
        coverage.setCalculationNote("Calculated from mock benefit type " + benefitType + ".");
        return coverage;
    }

    private ProcedureCoverage manualEligibilityFallback(ProcedureCoverage coverage, BigDecimal allowedAmount,
                                                        EligibilityResult eligibility,
                                                        BenefitAccumulator accumulator) {
        BenefitType benefitType = manualBenefitType(eligibility);
        MockProcedureRule manualRule = new MockProcedureRule();
        manualRule.setCopay(eligibility.getCopay());
        manualRule.setCoinsurancePercent(eligibility.getCoinsurancePercent());
        manualRule.setDeductibleApplies(benefitType == BenefitType.DEDUCTIBLE_THEN_COINSURANCE
                || benefitType == BenefitType.DEDUCTIBLE_COPAY_THEN_COINSURANCE);

        CostShare costShare = calculateCostShare(allowedAmount, benefitType, manualRule, eligibility, accumulator);
        BigDecimal coveredPatientCost = costShare.deductibleApplied()
                .add(costShare.copayApplied())
                .add(costShare.coinsuranceApplied());
        BigDecimal cappedCoveredPatientCost = accumulator.applyOutOfPocket(coveredPatientCost);
        BigDecimal insurancePays = allowedAmount.subtract(cappedCoveredPatientCost).max(BigDecimal.ZERO);

        coverage.setAllowedAmount(allowedAmount);
        coverage.setNonCoveredAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        coverage.setDeductibleApplied(costShare.deductibleApplied());
        coverage.setCopayApplied(costShare.copayApplied());
        coverage.setCoinsuranceApplied(costShare.coinsuranceApplied());
        coverage.setInsurancePays(money(insurancePays));
        coverage.setPatientResponsibility(money(cappedCoveredPatientCost));
        coverage.setCalculationNote(eligibility.getNetworkStatus() == com.synergen.vobworkbench.model.NetworkStatus.OUT_OF_NETWORK
                ? "Calculated using manual out-of-network eligibility fallback because no configured mock benefit rule was found."
                : "Calculated using manual eligibility fallback because no configured mock benefit rule was found.");
        return coverage;
    }

    private CostShare calculateCostShare(BigDecimal allowedAmount, BenefitType benefitType, MockProcedureRule rule,
                                         EligibilityResult eligibility, BenefitAccumulator accumulator) {
        BigDecimal deductibleApplied = BigDecimal.ZERO;
        BigDecimal copayApplied = BigDecimal.ZERO;
        BigDecimal coinsuranceBase = allowedAmount;
        BigDecimal copay = safe(firstNonNull(rule.getCopay(), eligibility.getCopay()));
        int coinsurancePercent = safePercent(firstNonNull(rule.getCoinsurancePercent(), eligibility.getCoinsurancePercent()));

        if ((benefitType == BenefitType.DEDUCTIBLE_THEN_COINSURANCE
                || benefitType == BenefitType.DEDUCTIBLE_COPAY_THEN_COINSURANCE)
                && Boolean.TRUE.equals(rule.getDeductibleApplies())) {
            deductibleApplied = accumulator.applyDeductible(allowedAmount);
            coinsuranceBase = allowedAmount.subtract(deductibleApplied).max(BigDecimal.ZERO);
        }

        if (benefitType == BenefitType.COPAY_ONLY
                || benefitType == BenefitType.COPAY_THEN_COINSURANCE
                || benefitType == BenefitType.DEDUCTIBLE_COPAY_THEN_COINSURANCE) {
            copayApplied = copay.min(coinsuranceBase).max(BigDecimal.ZERO);
            coinsuranceBase = coinsuranceBase.subtract(copayApplied).max(BigDecimal.ZERO);
        }

        BigDecimal coinsuranceApplied = switch (benefitType) {
            case COINSURANCE_ONLY, DEDUCTIBLE_THEN_COINSURANCE, COPAY_THEN_COINSURANCE,
                    DEDUCTIBLE_COPAY_THEN_COINSURANCE -> coinsuranceBase
                    .multiply(BigDecimal.valueOf(coinsurancePercent))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case COPAY_ONLY, NOT_COVERED -> BigDecimal.ZERO;
        };

        return new CostShare(money(deductibleApplied), money(copayApplied), money(coinsuranceApplied));
    }

    private ProcedureCoverage baseCoverage(Procedure procedure, BigDecimal estimatedCharge, BigDecimal allowedAmount,
                                           BigDecimal nonCoveredAmount, Optional<MockProcedureRule> rule) {
        ProcedureCoverage coverage = new ProcedureCoverage();
        coverage.setProcedureCode(procedure.getProcedureCode());
        coverage.setProcedureName(rule.map(MockProcedureRule::getProcedureName)
                .filter(StringUtils::hasText)
                .orElse(procedure.getProcedureName()));
        coverage.setEstimatedCharge(estimatedCharge);
        coverage.setAllowedAmount(allowedAmount);
        coverage.setDeductibleApplied(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        coverage.setCopayApplied(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        coverage.setCoinsuranceApplied(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        coverage.setNonCoveredAmount(money(nonCoveredAmount));
        coverage.setInsurancePays(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        coverage.setPatientResponsibility(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        coverage.setPriorAuthorizationRequired(rule.map(MockProcedureRule::getPriorAuthorizationRequired).orElse(false)
                || procedure.isRequiresAuthorization());
        coverage.setCalculationNote("Calculated from mock benefit data.");
        return coverage;
    }

    private void fullPatientResponsibility(ProcedureCoverage coverage, BigDecimal estimatedCharge,
                                           BigDecimal nonCoveredAmount, String note) {
        coverage.setAllowedAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        coverage.setNonCoveredAmount(money(nonCoveredAmount));
        coverage.setInsurancePays(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        coverage.setPatientResponsibility(money(estimatedCharge));
        coverage.setCalculationNote(note);
    }

    private void addToSummary(CoverageSummary summary, ProcedureCoverage coverage) {
        summary.setTotalEstimatedCharge(money(summary.getTotalEstimatedCharge().add(safe(coverage.getEstimatedCharge()))));
        summary.setTotalAllowedAmount(money(summary.getTotalAllowedAmount().add(safe(coverage.getAllowedAmount()))));
        summary.setTotalDeductibleApplied(money(summary.getTotalDeductibleApplied().add(safe(coverage.getDeductibleApplied()))));
        summary.setTotalCopayApplied(money(summary.getTotalCopayApplied().add(safe(coverage.getCopayApplied()))));
        summary.setTotalCoinsuranceApplied(money(summary.getTotalCoinsuranceApplied().add(safe(coverage.getCoinsuranceApplied()))));
        summary.setTotalNonCoveredAmount(money(summary.getTotalNonCoveredAmount().add(safe(coverage.getNonCoveredAmount()))));
        summary.setTotalInsurancePays(money(summary.getTotalInsurancePays().add(safe(coverage.getInsurancePays()))));
        summary.setTotalPatientResponsibility(money(summary.getTotalPatientResponsibility().add(safe(coverage.getPatientResponsibility()))));
        summary.getProcedureCoverages().add(coverage);
    }

    private Optional<MockProcedureRule> matchingRule(MockData mockData, InsurancePolicy policy, String procedureCode) {
        return mockData.getProcedureRules().stream()
                .filter(rule -> equalsIgnoreCase(rule.getPayerName(), policy.getPayerName()))
                .filter(rule -> equalsIgnoreCase(rule.getProcedureCode(), procedureCode))
                .filter(rule -> rule.getPlanType() == null || rule.getPlanType() == policy.getPlanType())
                .filter(rule -> !StringUtils.hasText(rule.getMemberId()) || equalsIgnoreCase(rule.getMemberId(), policy.getMemberId()))
                .filter(rule -> !StringUtils.hasText(rule.getGroupNumber()) || equalsIgnoreCase(rule.getGroupNumber(), policy.getGroupNumber()))
                .findFirst();
    }

    private Optional<InsurancePolicy> primaryPolicy(VobRequest request) {
        return request.getInsurancePolicies().stream()
                .filter(policy -> policy.getInsuranceOrder() == InsuranceOrder.PRIMARY)
                .findFirst()
                .or(() -> request.getInsurancePolicies().stream().findFirst());
    }

    private BenefitType benefitType(MockProcedureRule rule) {
        if (rule.getBenefitType() != null) {
            return rule.getBenefitType();
        }
        if (!isRuleActive(rule) || safePercent(rule.getCoveragePercent()) == 0) {
            return BenefitType.NOT_COVERED;
        }
        if (safe(rule.getDeductibleRemaining()).signum() > 0 && safePercent(rule.getCoinsurancePercent()) > 0) {
            return BenefitType.DEDUCTIBLE_THEN_COINSURANCE;
        }
        if (safe(rule.getCopay()).signum() > 0 && safePercent(rule.getCoinsurancePercent()) > 0) {
            return BenefitType.COPAY_THEN_COINSURANCE;
        }
        if (safe(rule.getCopay()).signum() > 0) {
            return BenefitType.COPAY_ONLY;
        }
        return BenefitType.COINSURANCE_ONLY;
    }

    private BenefitType manualBenefitType(EligibilityResult eligibility) {
        boolean hasDeductible = safe(eligibility.getDeductibleRemaining()).signum() > 0;
        boolean hasCopay = safe(eligibility.getCopay()).signum() > 0;
        boolean hasCoinsurance = safePercent(eligibility.getCoinsurancePercent()) > 0;

        if (hasDeductible && hasCopay && hasCoinsurance) {
            return BenefitType.DEDUCTIBLE_COPAY_THEN_COINSURANCE;
        }
        if (hasDeductible && hasCoinsurance) {
            return BenefitType.DEDUCTIBLE_THEN_COINSURANCE;
        }
        if (hasCopay && hasCoinsurance) {
            return BenefitType.COPAY_THEN_COINSURANCE;
        }
        if (hasCopay) {
            return BenefitType.COPAY_ONLY;
        }
        return BenefitType.COINSURANCE_ONLY;
    }

    private boolean isRuleActive(MockProcedureRule rule) {
        return rule.getCoverageActive() == null || Boolean.TRUE.equals(rule.getCoverageActive());
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private int safePercent(Integer value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, value));
    }

    private BigDecimal safe(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return money(value);
    }

    private BigDecimal deductibleTotal(EligibilityResult eligibility) {
        if (eligibility == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal explicitTotal = safe(eligibility.getDeductibleTotal());
        if (explicitTotal.signum() > 0) {
            return explicitTotal;
        }
        return safe(eligibility.getDeductibleRemaining()).add(safe(eligibility.getDeductibleMet()));
    }

    private BigDecimal deductibleMet(EligibilityResult eligibility) {
        if (eligibility == null || safe(eligibility.getDeductibleTotal()).signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return safe(eligibility.getDeductibleMet());
    }

    private BigDecimal money(BigDecimal value) {
        return value.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private record CostShare(BigDecimal deductibleApplied, BigDecimal copayApplied, BigDecimal coinsuranceApplied) {
    }

    private final class BenefitAccumulator {
        private final BigDecimal deductibleTotal;
        private final BigDecimal oopMax;
        private BigDecimal deductibleMet;
        private BigDecimal oopMet;

        private BenefitAccumulator(BigDecimal deductibleTotal, BigDecimal deductibleMet, BigDecimal oopMax, BigDecimal oopMet) {
            this.deductibleTotal = deductibleTotal;
            this.deductibleMet = deductibleMet.min(deductibleTotal).max(BigDecimal.ZERO);
            this.oopMax = oopMax;
            this.oopMet = oopMax.signum() > 0 ? oopMet.min(oopMax).max(BigDecimal.ZERO) : BigDecimal.ZERO;
        }

        private BigDecimal applyDeductible(BigDecimal allowedAmount) {
            BigDecimal remainingDeductible = deductibleTotal.subtract(deductibleMet).max(BigDecimal.ZERO);
            BigDecimal applied = allowedAmount.min(remainingDeductible).max(BigDecimal.ZERO);
            deductibleMet = deductibleMet.add(applied).min(deductibleTotal);
            return money(applied);
        }

        private BigDecimal applyOutOfPocket(BigDecimal coveredPatientCost) {
            if (oopMax.signum() <= 0) {
                return money(coveredPatientCost);
            }

            BigDecimal remainingOop = oopMax.subtract(oopMet).max(BigDecimal.ZERO);
            BigDecimal capped = coveredPatientCost.min(remainingOop).max(BigDecimal.ZERO);
            oopMet = oopMet.add(capped).min(oopMax);
            return money(capped);
        }
    }
}
