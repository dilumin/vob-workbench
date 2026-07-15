package com.synergen.vobworkbench.model;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsurancePolicy {
    private String payerName;
    private String memberId;
    private String groupNumber;
    private PlanType planType;
    private InsuranceOrder insuranceOrder;
    private LocalDate coverageStart;
    private LocalDate coverageEnd;
    private boolean active = true;
}
