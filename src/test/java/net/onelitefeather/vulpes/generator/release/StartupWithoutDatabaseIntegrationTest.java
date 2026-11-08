package net.onelitefeather.vulpes.generator.release;

import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.sql.DataSource;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The generator reads its content from the backend and needs no database: it starts without any datasource
 * configuration and answers its health endpoint.
 *
 * @author TheMeinerLP
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartupWithoutDatabaseIntegrationTest implements TestPropertyProvider {

    @Override
    public Map<String, String> getProperties() {
        return ReleaseTestProperties.onFreePort();
    }

    @Inject
    ApplicationContext context;

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void healthAnswers200WithoutDatasource() {
        int status = client.toBlocking().exchange(HttpRequest.GET("/health")).getStatus().getCode();

        assertEquals(200, status, "GET /health must answer 200 when the generator starts without a datasource");
    }

    @Test
    void noDataSourceBeanExists() {
        assertFalse(context.containsBean(DataSource.class),
                "the generator must not create a datasource: content comes from the backend REST API");
    }
}
