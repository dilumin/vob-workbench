package com.synergen.vobworkbench;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.synergen.vobworkbench.model.BenefitType;
import com.synergen.vobworkbench.model.CoverageSummary;
import com.synergen.vobworkbench.model.EligibilityResult;
import com.synergen.vobworkbench.model.InsuranceOrder;
import com.synergen.vobworkbench.model.InsurancePolicy;
import com.synergen.vobworkbench.model.MockData;
import com.synergen.vobworkbench.model.MockProcedureRule;
import com.synergen.vobworkbench.model.NetworkStatus;
import com.synergen.vobworkbench.model.PlanType;
import com.synergen.vobworkbench.model.Procedure;
import com.synergen.vobworkbench.model.VobRequest;
import com.synergen.vobworkbench.repository.MockDataRepository;
import com.synergen.vobworkbench.service.CoverageCalculationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CoverageCalculationServiceTests {
    @Test
    void copayOnlyUsesFixedPatientCopay() {
        CoverageSummary summary = calculate(BenefitType.COPAY_ONLY, "150", "30", 0, false, "0", "0", "0", "0", "150");

        assertThat(summary.getTotalPatientResponsibility()).isEqualByComparingTo("30.00");
        assertThat(summary.getTotalInsurancePays()).isEqualByComparingTo("120.00");
    }

    @Test
    void deductibleThenCoinsuranceAppliesRemainingDeductibleFirst() {
        CoverageSummary summary = calculate(BenefitType.DEDUCTIBLE_THEN_COINSURANCE, "1000", "0", 20, true,
                "1000", "800", "0", "0", "1000");

        assertThat(summary.getTotalPatientResponsibility()).isEqualByComparingTo("360.00");
        assertThat(summary.getTotalInsurancePays()).isEqualByComparingTo("640.00");
    }

    @Test
    void copayThenCoinsuranceAppliesCopayBeforePercentageShare() {
        CoverageSummary summary = calculate(BenefitType.COPAY_THEN_COINSURANCE, "500", "30", 20, false,
                "0", "0", "0", "0", "500");

        assertThat(summary.getTotalPatientResponsibility()).isEqualByComparingTo("124.00");
        assertThat(summary.getTotalInsurancePays()).isEqualByComparingTo("376.00");
    }

    @Test
    void deductibleCopayThenCoinsuranceCombinesAllPatientCostShare() {
        CoverageSummary summary = calculate(BenefitType.DEDUCTIBLE_COPAY_THEN_COINSURANCE, "1000", "30", 20, true,
                "1000", "800", "0", "0", "1000");

        assertThat(summary.getTotalPatientResponsibility()).isEqualByComparingTo("384.00");
        assertThat(summary.getTotalInsurancePays()).isEqualByComparingTo("616.00");
    }

    @Test
    void notCoveredMakesPatientResponsibleForFullEstimatedCharge() {
        CoverageSummary summary = calculate(BenefitType.NOT_COVERED, "0", "0", 0, false,
                "0", "0", "0", "0", "1000");

        assertThat(summary.getTotalPatientResponsibility()).isEqualByComparingTo("1000.00");
        assertThat(summary.getTotalInsurancePays()).isEqualByComparingTo("0.00");
    }

    @Test
    void outOfPocketCapLimitsCoveredPatientCostButNotNonCoveredAmount() {
        CoverageSummary summary = calculate(BenefitType.COINSURANCE_ONLY, "1000", "0", 30, false,
                "0", "0", "1000", "900", "1200");

        assertThat(summary.getTotalPatientResponsibility()).isEqualByComparingTo("300.00");
        assertThat(summary.getTotalInsurancePays()).isEqualByComparingTo("900.00");
        assertThat(summary.getTotalNonCoveredAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    void manualEligibilityFallbackUsesCopayAndCoinsuranceWhenNoMockRuleExists() {
        CoverageSummary summary = calculateWithoutMockRule(true, NetworkStatus.IN_NETWORK, "180", "10", 20, "0");

        assertThat(summary.getTotalAllowedAmount()).isEqualByComparingTo("180.00");
        assertThat(summary.getTotalCopayApplied()).isEqualByComparingTo("10.00");
        assertThat(summary.getTotalCoinsuranceApplied()).isEqualByComparingTo("34.00");
        assertThat(summary.getTotalPatientResponsibility()).isEqualByComparingTo("44.00");
        assertThat(summary.getTotalInsurancePays()).isEqualByComparingTo("136.00");
        assertThat(summary.getTotalNonCoveredAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void manualEligibilityFallbackUsesCopayOnlyWhenNoMockRuleExists() {
        CoverageSummary summary = calculateWithoutMockRule(true, NetworkStatus.IN_NETWORK, "180", "10", 0, "0");

        assertThat(summary.getTotalPatientResponsibility()).isEqualByComparingTo("10.00");
        assertThat(summary.getTotalInsurancePays()).isEqualByComparingTo("170.00");
    }

    @Test
    void manualEligibilityFallbackUsesDeductibleCopayAndCoinsuranceWhenNoMockRuleExists() {
        CoverageSummary summary = calculateWithoutMockRule(true, NetworkStatus.IN_NETWORK, "500", "20", 10, "100");

        assertThat(summary.getTotalDeductibleApplied()).isEqualByComparingTo("100.00");
        assertThat(summary.getTotalCopayApplied()).isEqualByComparingTo("20.00");
        assertThat(summary.getTotalCoinsuranceApplied()).isEqualByComparingTo("38.00");
        assertThat(summary.getTotalPatientResponsibility()).isEqualByComparingTo("158.00");
        assertThat(summary.getTotalInsurancePays()).isEqualByComparingTo("342.00");
    }

    @Test
    void inactiveManualEligibilityWithoutMockRuleKeepsFullPatientResponsibility() {
        CoverageSummary summary = calculateWithoutMockRule(false, NetworkStatus.IN_NETWORK, "180", "10", 20, "0");

        assertThat(summary.getTotalPatientResponsibility()).isEqualByComparingTo("180.00");
        assertThat(summary.getTotalInsurancePays()).isEqualByComparingTo("0.00");
        assertThat(summary.getTotalNonCoveredAmount()).isEqualByComparingTo("180.00");
    }

    @Test
    void configuredMockRuleStillWinsOverManualFallback() {
        CoverageSummary summary = calculateWithDifferentManualEligibility();

        assertThat(summary.getTotalPatientResponsibility()).isEqualByComparingTo("60.00");
        assertThat(summary.getTotalInsurancePays()).isEqualByComparingTo("120.00");
        assertThat(summary.getTotalNonCoveredAmount()).isEqualByComparingTo("30.00");
    }

    private CoverageSummary calculate(BenefitType benefitType, String allowedAmount, String copay, int coinsurance,
                                      boolean deductibleApplies, String deductibleTotal, String deductibleMet,
                                      String oopMax, String oopMet, String estimatedCharge) {
        MockDataRepository repository = mock(MockDataRepository.class);
        MockProcedureRule rule = rule(benefitType, allowedAmount, copay, coinsurance, deductibleApplies,
                deductibleTotal, deductibleMet, oopMax, oopMet);
        when(repository.findById("default")).thenReturn(Optional.of(new MockData("default",
                List.of("SynerCare Insurance"), List.of(rule))));

        VobRequest request = request(estimatedCharge);
        request.setEligibilityResult(eligibility(copay, coinsurance, deductibleTotal, deductibleMet, oopMax, oopMet));

        return new CoverageCalculationService(repository).calculate(request);
    }

    private CoverageSummary calculateWithoutMockRule(boolean coverageActive, NetworkStatus networkStatus,
                                                     String estimatedCharge, String copay, int coinsurance,
                                                     String deductibleRemaining) {
        MockDataRepository repository = mock(MockDataRepository.class);
        when(repository.findById("default")).thenReturn(Optional.of(new MockData("default", List.of(), List.of())));

        VobRequest request = request(estimatedCharge);
        EligibilityResult eligibility = new EligibilityResult();
        eligibility.setCoverageActive(coverageActive);
        eligibility.setNetworkStatus(networkStatus);
        eligibility.setCopay(new BigDecimal(copay));
        eligibility.setCoinsurancePercent(coinsurance);
        eligibility.setDeductibleRemaining(new BigDecimal(deductibleRemaining));
        eligibility.setNotes("Manual eligibility entered by specialist.");
        request.setEligibilityResult(eligibility);

        return new CoverageCalculationService(repository).calculate(request);
    }

    private CoverageSummary calculateWithDifferentManualEligibility() {
        MockDataRepository repository = mock(MockDataRepository.class);
        MockProcedureRule rule = rule(BenefitType.COPAY_ONLY, "150", "30", 0, false, "0", "0", "0", "0");
        when(repository.findById("default")).thenReturn(Optional.of(new MockData("default",
                List.of("SynerCare Insurance"), List.of(rule))));

        VobRequest request = request("180");
        request.setEligibilityResult(eligibility("10", 20, "0", "0", "0", "0"));

        return new CoverageCalculationService(repository).calculate(request);
    }

    private MockProcedureRule rule(BenefitType benefitType, String allowedAmount, String copay, int coinsurance,
                                   boolean deductibleApplies, String deductibleTotal, String deductibleMet,
                                   String oopMax, String oopMet) {
        MockProcedureRule rule = new MockProcedureRule();
        rule.setPayerName("SynerCare Insurance");
        rule.setPlanType(PlanType.PPO);
        rule.setMemberId("MEM12345");
        rule.setGroupNumber("GRP-100");
        rule.setProcedureCode("IMG-001");
        rule.setProcedureName("X-Ray Imaging");
        rule.setBenefitType(benefitType);
        rule.setCoverageActive(benefitType != BenefitType.NOT_COVERED);
        rule.setNetworkStatus(NetworkStatus.IN_NETWORK);
        rule.setAllowedAmount(new BigDecimal(allowedAmount));
        rule.setCopay(new BigDecimal(copay));
        rule.setCoinsurancePercent(coinsurance);
        rule.setDeductibleApplies(deductibleApplies);
        rule.setDeductibleTotal(new BigDecimal(deductibleTotal));
        rule.setDeductibleMet(new BigDecimal(deductibleMet));
        rule.setDeductibleRemaining(new BigDecimal(deductibleTotal).subtract(new BigDecimal(deductibleMet)).max(BigDecimal.ZERO));
        rule.setOopMax(new BigDecimal(oopMax));
        rule.setOopMet(new BigDecimal(oopMet));
        rule.setPriorAuthorizationRequired(false);
        return rule;
    }

    private VobRequest request(String estimatedCharge) {
        VobRequest request = new VobRequest();
        request.setDateOfService(LocalDate.now().plusDays(1));
        request.setInsurancePolicies(List.of(new InsurancePolicy("SynerCare Insurance", "MEM12345", "GRP-100",
                PlanType.PPO, InsuranceOrder.PRIMARY, LocalDate.now().minusDays(1), null, true)));
        request.setProcedures(List.of(new Procedure("IMG-001", "X-Ray Imaging", new BigDecimal(estimatedCharge), false)));
        return request;
    }

    private EligibilityResult eligibility(String copay, int coinsurance, String deductibleTotal, String deductibleMet,
                                          String oopMax, String oopMet) {
        BigDecimal deductibleRemaining = new BigDecimal(deductibleTotal).subtract(new BigDecimal(deductibleMet)).max(BigDecimal.ZERO);
        EligibilityResult result = new EligibilityResult();
        result.setCoverageActive(true);
        result.setNetworkStatus(NetworkStatus.IN_NETWORK);
        result.setCopay(new BigDecimal(copay));
        result.setCoinsurancePercent(coinsurance);
        result.setDeductibleTotal(new BigDecimal(deductibleTotal));
        result.setDeductibleMet(new BigDecimal(deductibleMet));
        result.setDeductibleRemaining(deductibleRemaining);
        result.setOopMax(new BigDecimal(oopMax));
        result.setOopMet(new BigDecimal(oopMet));
        result.setOopRemaining(new BigDecimal(oopMax).subtract(new BigDecimal(oopMet)).max(BigDecimal.ZERO));
        result.setPriorAuthorizationRequired(false);
        result.setBenefitSource("MOCK_271");
        result.setNotes("Active mock benefit.");
        return result;
    }
}
