# Docker Compose Rule With Junit5

Workaround to use Docker Compose Rule with Junit5.

## Why ?

[Docker Compose Rule](https://github.com/palantir/docker-compose-rule) only works on Junit4 for now.

An [issue](https://github.com/palantir/docker-compose-rule/issues/138) has been opened to be able to use it with Junit5.

## How to run ?

```shell
mvn clean install
```

## How does it work ?

**src/test/resources/docker-compose.yml**

```yml
version: '3'
services:
  postgres:
    image: 'postgres:10'
    environment:
      - POSTGRES_PASSWORD=ThisIsMySuperPassword
    ports:
      - 5432
    healthcheck:
      test: ["CMD", "psql", "-U", "postgres", "-w", "-c", "SELECT version();"]
      interval: 1s
      timeout: 5s
      retries: 12
  nginx:
    image: 'nginx:1.13'
    ports:
      - 80
```

**src/test/java/DockerExtension**

The concept of extension allows us to "extend" the behavior of a test, like we used to do it with Junit4's `Rule` mechanism.

```java
import com.palantir.docker.compose.DockerComposeRule;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import static com.palantir.docker.compose.connection.waiting.HealthChecks.toHaveAllPortsOpen;

public class DockerComposeExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private DockerComposeRule docker;

    public DockerComposeExtension() {
        docker = DockerComposeRule.builder()
                .pullOnStartup(true)
                .file("src/test/resources/docker-compose.yml")
                .saveLogsTo("target/test-docker-logs")
                .waitingForService("postgres", toHaveAllPortsOpen())
                .waitingForService("nginx", toHaveAllPortsOpen())
                .build();
    }

    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        docker.before();
    }

    public void afterAll(ExtensionContext extensionContext) throws Exception {
        docker.after();
    }

    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(DockerComposeRule.class);
    }

    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return docker;
    }

}

```

**src/test/java/SimpleTest**

And finally, a example of how to use it in a test.

```java
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.DockerPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.mashape.unirest.http.Unirest.get;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(DockerComposeExtension.class)
public class SimpleTest {

    @Test
    public void shouldConnectToDatabase(DockerComposeRule docker) throws SQLException, ClassNotFoundException {
        // Given
        // We need to import the driver https://jdbc.postgresql.org/documentation/head/load.html
        Class.forName("org.postgresql.Driver");

        DockerPort container = docker.containers().container("postgres").port(5432);
        String url = "jdbc:postgresql://" + container.getIp() + ":" + container.getExternalPort() + "/postgres";
        Connection connection = DriverManager.getConnection(url, "postgres", "ThisIsMySuperPassword");

        // When
        ResultSet resultSet = connection.prepareStatement("SELECT 1").executeQuery();

        // Then
        assertNotNull(resultSet);
    }

    @Test
    public void shouldReceiveNginxWelcome(DockerComposeRule docker) throws UnirestException {
        // Given
        DockerPort container = docker.containers().container("nginx").port(80);
        String url = "http://" + container.getIp() + ":" + container.getExternalPort();

        // When
        HttpResponse<String> response = get(url).asString();

        // Then
        assertTrue(response.getBody().contains("Welcome to nginx!"));
    }

}
```
