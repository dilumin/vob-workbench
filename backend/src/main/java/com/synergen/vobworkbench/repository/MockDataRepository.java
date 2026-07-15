package com.synergen.vobworkbench.repository;

import com.synergen.vobworkbench.model.MockData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MockDataRepository extends MongoRepository<MockData, String> {
}
