package com.synergen.vobworkbench.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.util.StringUtils;

@Configuration
public class MongoConfig {
    @Bean
    MongoClient mongoClient(@Value("${app.mongodb.connection-string:mongodb://localhost:27017}") String connectionString) {
        return MongoClients.create(connectionString);
    }

    @Bean
    MongoDatabaseFactory mongoDatabaseFactory(
            MongoClient mongoClient,
            @Value("${app.mongodb.database:vob_workbench}") String database
    ) {
        if (!StringUtils.hasText(database)) {
            throw new IllegalStateException("Mongo database name must be configured.");
        }
        return new SimpleMongoClientDatabaseFactory(mongoClient, database);
    }
}
