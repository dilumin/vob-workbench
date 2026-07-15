package com.synergen.vobworkbench.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "mockData")
public class MockData {
    @Id
    private String id = "default";

    private List<String> payers = new ArrayList<>();
    private List<MockProcedureRule> procedureRules = new ArrayList<>();
}
