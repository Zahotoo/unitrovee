package com.unitrovee;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * base class for integration tests: boots the FULL app against a real, throwaway Postgres
 * spun up by Testcontainers.
 */
@SpringBootTest     // start the complete Spring application context
// @Testcontainers     // let Testcontainers manage container start/stop
public abstract class AbstractIntegrationTest {

    // @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres")
    );

    static {
        postgres.start();
    }
}
