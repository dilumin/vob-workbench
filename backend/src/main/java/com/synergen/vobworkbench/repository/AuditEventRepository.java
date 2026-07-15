package com.synergen.vobworkbench.repository;

import com.synergen.vobworkbench.model.AuditEvent;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditEventRepository extends MongoRepository<AuditEvent, String> {
    List<AuditEvent> findByRequestIdOrderByTimestampDesc(String requestId);
}
