package com.synergen.vobworkbench.config;

import com.synergen.vobworkbench.model.MockData;
import com.synergen.vobworkbench.model.MockProcedureRule;
import com.synergen.vobworkbench.model.BenefitType;
import com.synergen.vobworkbench.model.NetworkStatus;
import com.synergen.vobworkbench.model.PlanType;
import com.synergen.vobworkbench.model.Role;
import com.synergen.vobworkbench.model.User;
import com.synergen.vobworkbench.repository.MockDataRepository;
import com.synergen.vobworkbench.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("!test")
public class DevDataSeeder {
    @Bean
    CommandLineRunner seedUsers(UserRepository users, MockDataRepository mockData, PasswordEncoder encoder) {
        return args -> {
            seedUser(users, encoder, "receptionist1", "Receptionist One", Role.RECEPTIONIST);
            seedUser(users, encoder, "specialist1", "Specialist One", Role.SPECIALIST);
            seedUser(users, encoder, "admin1", "Admin One", Role.SUPERVISOR_ADMIN);
            seedUser(users, encoder, "viewer1", "Viewer One", Role.VIEWER);

            if (!mockData.existsById("default") || hasLegacyRules(mockData.findById("default").orElse(null))) {
                mockData.save(defaultMockData());
            }
        };
    }

    private void seedUser(UserRepository users, PasswordEncoder encoder, String username, String fullName, Role role) {
        if (users.existsByUsername(username)) {
            return;
        }
        Instant now = Instant.now();
        User user = new User(null, username, encoder.encode("password123"), fullName, role, true, now, now);
        users.save(user);
    }

    private MockData defaultMockData() {
        return new MockData("default",
                List.of("SynerCare Insurance", "MediPlus Health", "CareFirst Mock"),
                List.of(
                        rule("SynerCare Insurance", PlanType.PPO, "MEM12345", "GRP-100", "CONS-001", "Office Consultation",
                                BenefitType.COPAY_ONLY, true, NetworkStatus.IN_NETWORK, "150", "30", 0, false, "1000", "250", "4000", "500", false),
                        rule("SynerCare Insurance", PlanType.PPO, "MEM12345", "GRP-100", "LAB-001", "Basic Lab Panel",
                                BenefitType.DEDUCTIBLE_THEN_COINSURANCE, true, NetworkStatus.IN_NETWORK, "80", "0", 10, true, "1000", "250", "4000", "500", false),
                        rule("SynerCare Insurance", PlanType.PPO, "MEM12345", "GRP-100", "IMG-001", "X-Ray Imaging",
                                BenefitType.DEDUCTIBLE_THEN_COINSURANCE, true, NetworkStatus.IN_NETWORK, "220", "0", 20, true, "1000", "250", "4000", "500", false),
                        rule("SynerCare Insurance", PlanType.PPO, "MEM12345", "GRP-100", "MRI-001", "MRI Scan",
                                BenefitType.DEDUCTIBLE_THEN_COINSURANCE, true, NetworkStatus.IN_NETWORK, "950", "0", 20, true, "1000", "250", "4000", "500", true),
                        rule("SynerCare Insurance", PlanType.PPO, "MEM12345", "GRP-100", "PT-001", "Physical Therapy Session",
                                BenefitType.COPAY_THEN_COINSURANCE, true, NetworkStatus.IN_NETWORK, "180", "20", 10, false, "1000", "250", "4000", "500", false),
                        rule("SynerCare Insurance", PlanType.PPO, "MEM12345", "GRP-100", "SURG-001", "Outpatient Surgery",
                                BenefitType.DEDUCTIBLE_COPAY_THEN_COINSURANCE, true, NetworkStatus.IN_NETWORK, "3800", "100", 20, true, "1000", "250", "4000", "500", true),
                        rule("MediPlus Health", PlanType.HMO, "MED77881", "HMO-200", "CONS-001", "Office Consultation",
                                BenefitType.COPAY_ONLY, true, NetworkStatus.IN_NETWORK, "140", "20", 0, false, "500", "500", "2500", "900", false),
                        rule("MediPlus Health", PlanType.HMO, "MED77881", "HMO-200", "LAB-001", "Basic Lab Panel",
                                BenefitType.COINSURANCE_ONLY, true, NetworkStatus.IN_NETWORK, "70", "0", 0, false, "500", "500", "2500", "900", false),
                        rule("MediPlus Health", PlanType.HMO, "MED77881", "HMO-200", "IMG-001", "X-Ray Imaging",
                                BenefitType.COINSURANCE_ONLY, true, NetworkStatus.IN_NETWORK, "200", "0", 15, false, "500", "500", "2500", "900", false),
                        rule("MediPlus Health", PlanType.HMO, "MED77881", "HMO-200", "MRI-001", "MRI Scan",
                                BenefitType.DEDUCTIBLE_THEN_COINSURANCE, true, NetworkStatus.IN_NETWORK, "850", "0", 15, true, "500", "500", "2500", "900", true),
                        rule("MediPlus Health", PlanType.HMO, "MED77881", "HMO-200", "PT-001", "Physical Therapy Session",
                                BenefitType.COPAY_ONLY, true, NetworkStatus.IN_NETWORK, "160", "25", 0, false, "500", "500", "2500", "900", false),
                        rule("MediPlus Health", PlanType.HMO, "MED77881", "HMO-200", "SURG-001", "Outpatient Surgery",
                                BenefitType.DEDUCTIBLE_COPAY_THEN_COINSURANCE, true, NetworkStatus.IN_NETWORK, "3500", "150", 15, true, "500", "500", "2500", "900", true),
                        rule("CareFirst Mock", PlanType.POS, "CAR55510", "POS-300", "CONS-001", "Office Consultation",
                                BenefitType.COINSURANCE_ONLY, true, NetworkStatus.OUT_OF_NETWORK, "120", "0", 40, false, "2000", "600", "7000", "1200", false),
                        rule("CareFirst Mock", PlanType.POS, "CAR55510", "POS-300", "LAB-001", "Basic Lab Panel",
                                BenefitType.DEDUCTIBLE_THEN_COINSURANCE, true, NetworkStatus.OUT_OF_NETWORK, "60", "0", 40, true, "2000", "600", "7000", "1200", false),
                        rule("CareFirst Mock", PlanType.POS, "CAR55510", "POS-300", "IMG-001", "X-Ray Imaging",
                                BenefitType.DEDUCTIBLE_THEN_COINSURANCE, true, NetworkStatus.OUT_OF_NETWORK, "180", "0", 50, true, "2000", "600", "7000", "1200", false),
                        rule("CareFirst Mock", PlanType.POS, "CAR55510", "POS-300", "MRI-001", "MRI Scan",
                                BenefitType.DEDUCTIBLE_THEN_COINSURANCE, true, NetworkStatus.OUT_OF_NETWORK, "700", "0", 50, true, "2000", "600", "7000", "1200", true),
                        rule("CareFirst Mock", PlanType.POS, "CAR55510", "POS-300", "PT-001", "Physical Therapy Session",
                                BenefitType.COINSURANCE_ONLY, true, NetworkStatus.OUT_OF_NETWORK, "130", "0", 40, false, "2000", "600", "7000", "1200", false),
                        rule("CareFirst Mock", PlanType.POS, "CAR55510", "POS-300", "SURG-001", "Outpatient Surgery",
                                BenefitType.DEDUCTIBLE_COPAY_THEN_COINSURANCE, true, NetworkStatus.OUT_OF_NETWORK, "2800", "200", 50, true, "2000", "600", "7000", "1200", true),
                        inactiveRule("CareFirst Mock", PlanType.EPO, "EXPIRED01", "EPO-404", "MRI-001", "MRI Scan")
                ));
    }

    private MockProcedureRule rule(String payerName, PlanType planType, String memberId, String groupNumber,
                                   String procedureCode, String procedureName, BenefitType benefitType,
                                   boolean coverageActive, NetworkStatus networkStatus, String allowedAmount,
                                   String copay, int coinsurancePercent, boolean deductibleApplies,
                                   String deductibleTotal, String deductibleMet, String oopMax, String oopMet,
                                   boolean priorAuthorizationRequired) {
        MockProcedureRule rule = new MockProcedureRule();
        rule.setPayerName(payerName);
        rule.setPlanType(planType);
        rule.setMemberId(memberId);
        rule.setGroupNumber(groupNumber);
        rule.setProcedureCode(procedureCode);
        rule.setProcedureName(procedureName);
        rule.setBenefitType(benefitType);
        rule.setCoveragePercent(100);
        rule.setCoverageActive(coverageActive);
        rule.setNetworkStatus(networkStatus);
        rule.setAllowedAmount(new BigDecimal(allowedAmount));
        rule.setCopay(new BigDecimal(copay));
        rule.setCoinsurancePercent(coinsurancePercent);
        rule.setDeductibleApplies(deductibleApplies);
        rule.setDeductibleTotal(new BigDecimal(deductibleTotal));
        rule.setDeductibleMet(new BigDecimal(deductibleMet));
        rule.setDeductibleRemaining(new BigDecimal(deductibleTotal).subtract(new BigDecimal(deductibleMet)).max(BigDecimal.ZERO));
        rule.setOopMax(new BigDecimal(oopMax));
        rule.setOopMet(new BigDecimal(oopMet));
        rule.setPriorAuthorizationRequired(priorAuthorizationRequired);
        rule.setNotes("Synthetic mock benefit rule.");
        return rule;
    }

    private MockProcedureRule inactiveRule(String payerName, PlanType planType, String memberId, String groupNumber,
                                           String procedureCode, String procedureName) {
        MockProcedureRule rule = rule(payerName, planType, memberId, groupNumber, procedureCode, procedureName,
                BenefitType.NOT_COVERED, false, NetworkStatus.UNKNOWN, "0", "0", 0, false, "0", "0", "0", "0", true);
        rule.setCoveragePercent(0);
        rule.setNotes("Coverage terminated before date of service.");
        return rule;
    }

    private boolean hasLegacyRules(MockData mockData) {
        return mockData == null || mockData.getProcedureRules().stream().anyMatch(rule ->
                rule.getCoverageActive() == null
                        || rule.getNetworkStatus() == null
                        || rule.getCopay() == null
                        || rule.getCoinsurancePercent() == null
                        || rule.getDeductibleRemaining() == null
                        || rule.getBenefitType() == null
                        || rule.getAllowedAmount() == null);
    }
}
