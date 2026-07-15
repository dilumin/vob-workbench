package com.synergen.vobworkbench;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

class TestDatabaseCleanupExtension implements BeforeEachCallback, AfterEachCallback {
    private static final String TEST_DATABASE = "vob_workbench_test";

    @Override
    public void beforeEach(ExtensionContext context) {
        dropTestDatabase(context);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        dropTestDatabase(context);
    }

    private void dropTestDatabase(ExtensionContext context) {
        MongoTemplate mongoTemplate = SpringExtension.getApplicationContext(context).getBean(MongoTemplate.class);
        String databaseName = mongoTemplate.getDb().getName();
        if (!TEST_DATABASE.equals(databaseName)) {
            throw new IllegalStateException("Refusing to clean Mongo database outside " + TEST_DATABASE + ": " + databaseName);
        }
        mongoTemplate.getDb().drop();
    }
}
