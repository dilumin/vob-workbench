package com.synergen.vobworkbench.repository;

import com.synergen.vobworkbench.model.Patient;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PatientRepository extends MongoRepository<Patient, String> {
    boolean existsByMrn(String mrn);
}
