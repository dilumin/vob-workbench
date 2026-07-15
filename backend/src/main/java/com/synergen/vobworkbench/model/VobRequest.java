package com.synergen.vobworkbench.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vobRequests")
public class VobRequest {
    @Id
    private String id;

    @Indexed
    private String patientId;

    private LocalDate dateOfService;
    private Priority priority;
    private VobStatus status = VobStatus.PENDING;
    private String assignedTo;
    private String createdBy;
    private List<InsurancePolicy> insurancePolicies = new ArrayList<>();
    private List<Procedure> procedures = new ArrayList<>();
    private EligibilityResult eligibilityResult;
    private CoverageSummary coverageSummary;
    private List<String> notes = new ArrayList<>();
    private boolean locked;

    @Version
    private Long version;

    private Instant createdAt;
    private Instant updatedAt;
}
