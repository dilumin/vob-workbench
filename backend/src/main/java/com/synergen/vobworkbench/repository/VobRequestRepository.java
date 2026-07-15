package com.synergen.vobworkbench.repository;

import com.synergen.vobworkbench.model.VobRequest;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VobRequestRepository extends MongoRepository<VobRequest, String> {
    List<VobRequest> findByPatientId(String patientId);
    List<VobRequest> findByAssignedTo(String assignedTo);
}
